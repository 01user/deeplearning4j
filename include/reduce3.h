/*
 * reduce3.h
 *
 *  Created on: Dec 28, 2015
 *      Author: agibsonccc
 */

#ifndef REDUCE3_H_
#define REDUCE3_H_

#define EXTRA_PARAMS_LENGTH 10

#include <op.h>
#include <templatemath.h>
#include <helper_cuda.h>
#include <sharedmem.h>
namespace functions {
namespace reduce3 {

template<typename T>
class Reduce3: public virtual functions::ops::Op<T> {

public:

	virtual
#ifdef __CUDACC__
	__host__  __device__

#endif
	inline T postProcess(T reduction, int n,T **extraParamsRef) = 0;

	virtual
#ifdef __CUDACC__
	__inline__ __host__ __device__
#endif
	T startingValue(T *input) = 0;

	virtual
#ifdef __CUDACC__
	__inline__ __host__ __device__
#endif
	T * generateExtraParams() = 0;
	virtual
#ifdef __CUDACC__
	__inline__ __host__ __device__
#endif
	void finalizeExtraParams(T **extraParamsRef)  = 0;

	/**
	 *
	 * @param d1
	 * @param d2
	 * @param extraParams
	 * @return
	 */
	//an op for the kernel
	virtual
#ifdef __CUDACC__
	__host__  __device__

#endif
	inline T op(T d1, T d2, T **extraParamsRef) = 0;

	//calculate an update of the reduce operation
	/**
	 *
	 * @param old
	 * @param opOutput
	 * @param extraParams
	 * @return
	 */
	virtual
#ifdef __CUDACC__
	__host__  __device__

#endif
	inline T update(T old, T opOutput, T **extraParamsRef) = 0;

	/**
	 *
	 * @param old
	 * @param opOutput
	 * @param extraParams
	 * @return
	 */
	virtual
#ifdef __CUDACC__
	__host__  __device__

#endif
	inline T merge(T old, T opOutput, T **extraParamsRef) = 0;

#ifdef __CUDACC__
	/**
	 * Aggregate shared memory
	 * @param sPartialsRef
	 * @param tid
	 * @param extraParams
	 */
	virtual __inline__ __device__ void aggregatePartials(T **sPartialsRef, int tid, T **extraParamsRef) {
		// start the shared memory loop on the next power of 2 less
		// than the block size.  If block size is not a power of 2,
		// accumulate the intermediate sums in the remainder range.
		T *sPartials = *sPartialsRef;
		int floorPow2 = blockDim.x;

		if (floorPow2 & (floorPow2 - 1)) {
			while (floorPow2 & (floorPow2 - 1)) {
				floorPow2 &= floorPow2 - 1;
			}
			if (tid >= floorPow2) {
				sPartials[tid - floorPow2] = update(sPartials[tid - floorPow2], sPartials[tid], extraParamsRef);
			}
			__syncthreads();
		}

		for (int activeThreads = floorPow2 >> 1; activeThreads; activeThreads >>= 1) {
			if (tid < activeThreads) {
				sPartials[tid] = update(sPartials[tid], sPartials[tid + activeThreads], extraParamsRef);
			}
			__syncthreads();
		}
	}

	/**

	 Perform a reduction
	 @param n the number of elements
	 @param xOffset the starting offset
	 @param dx the data to perform the reduction on
	 @param incx the increment on which to perform the reduction
	 @param extraParams extra parameters used for calculations
	 @param result where to store the result of the reduction
	 */
	virtual __inline__ __device__ void transform(
			int n, T *dx, int *xShapeInfo,
			T *dy,
			int *yShapeInfo, T *extraParams,
			T *result,
			int *resultShapeInfo,
			int *gpuInformation,
			int *dimension,
			int dimensionLength, int postProcessOrNot) {
		/**
		 * Gpu information for the problem
		 */
		int tid = threadIdx.x;

		__shared__ volatile int resultScalar;

		__shared__ int xElementWiseStride;
		__shared__ int xOffset;

		__shared__ int yElementWiseStride;
		__shared__ int yOffset;

		//shared memory space for storing intermediate results
		SharedMemory <T> val;
		volatile T *sPartials = val.getPointer();
		T startingVal = this->startingValue(dx);

		__shared__ T *extraParamsVals;
		if(tid == 0) {
			extraParamsVals = (T *) malloc(sizeof(T) * EXTRA_PARAMS_LENGTH);
			for(int i = 0; i < EXTRA_PARAMS_LENGTH; i++) {
				extraParamsVals[i] = startingVal;
			}
		}
		__syncthreads();
		int numElements = gpuInformation[2] / sizeof(T);
		for (int i = tid; i < numElements; i += blockDim.x)
			sPartials[i] = startingVal;
		__syncthreads();

		sPartials[tid] = startingVal;
		sPartials[(1 + tid) * 2] = startingVal;
		__syncthreads();


		//length for the tad
		__shared__ int reductionIndexesPerBlock;
		__shared__ int tensorsForDimension;

		//starting index for tad
		__shared__ volatile int currentBlockOffset;
		//ending index for tad
		__shared__ volatile int endingOffset;
		//length for the tad
		__shared__ volatile int xLength;

		__shared__ volatile int resultLength;


		__shared__ int elementsPerTad;

		//only compute the tad indexes once
		__shared__ shape::TADPermuteInfo xTadInfo;
		__shared__ shape::TADPermuteInfo yTadInfo;


		T reduction = this->startingValue(dx);
		if (tid == 0) {
			if (dimensionLength == 1) {
				if (dimension[0] == shape::MAX_DIMENSION)
					resultScalar = 1;
				else
					resultScalar = 0;
			}
			else
				resultScalar = 0;
			tensorsForDimension = shape::tensorsAlongDimension(xShapeInfo, dimension, dimensionLength);

			xOffset = shape::offset(xShapeInfo);
			xElementWiseStride = shape::elementWiseStride(xShapeInfo);
			xLength = shape::length(xShapeInfo);


			resultLength = shape::prod(shape::shapeOf(resultShapeInfo), shape::rank(resultShapeInfo));
			xOffset = shape::offset(xShapeInfo);
			xElementWiseStride = shape::elementWiseStride(xShapeInfo);
			elementsPerTad = xLength / resultLength;

			yElementWiseStride = shape::elementWiseStride(yShapeInfo);
			yOffset = shape::offset(yShapeInfo);
			if (gridDim.x >= resultLength) {
				reductionIndexesPerBlock = 1;
			}
			else {
				reductionIndexesPerBlock = resultLength / gridDim.x;
			}
		}

		__syncthreads();

		T curr, currY;

		if (resultScalar) {
			if(blockIdx.x >= resultLength)
				return;
			unsigned int i = xOffset + blockIdx.x * xElementWiseStride + tid;
			unsigned int j = yOffset + blockIdx.x * yElementWiseStride + tid;
			unsigned int gridSize = blockDim.x * gridDim.x * xElementWiseStride;
			unsigned int gridSizeY = blockDim.x * gridDim.x * yElementWiseStride;
			if(xOffset == 0 && yOffset == 0 && xElementWiseStride == 1 && yElementWiseStride == 1) {
				while (i  < n && j  < n) {
					curr = dx[i];
					currY = dy[j];

					/**
					 * Find why extra params vals
					 * aren't getting updated properly.
					 *
					 */
					reduction = update(reduction, op(curr, currY, &extraParamsVals), &extraParamsVals);
					__syncthreads();
					printf("First value is %f and second value is %f with d1 %f and d2 %f"
							"\n",extraParamsVals[0],extraParamsVals[1],curr,currY);

					i += gridSize;
					j += gridSizeY;
				}

			}
			else {
				// we reduce multiple elements per thread.  The number is determined by the
				// number of active thread blocks (via gridDim).  More blocks will result
				// in a larger gridSize and therefore fewer elements per thread
				while (i * xElementWiseStride < n && j * yElementWiseStride < n) {
					curr = dx[i];
					currY = dy[j];
					reduction = update(reduction, op(curr, currY, &extraParamsVals), &extraParamsVals);
					__syncthreads();
					i += gridSize;
					j += gridSizeY;
				}

			}


			// each thread puts its local sum into shared memory
			sPartials[tid] = reduction;
			__syncthreads();

			T **sPartialsRef = (T **) &sPartials;
			aggregatePartials(sPartialsRef, tid, &extraParamsVals);
			/**
			 * Look at something that uses the extra params
			 * and aggregates the extra values propelry.
			 *This will be used in summary stats too.
			 */
			// write result for this block to global mem
			if (tid == 0) {
				if (postProcessOrNot) {
					result[blockIdx.x] = postProcess(sPartials[0], xLength,&extraParamsVals);
				}
				else {
					result[blockIdx.x] = sPartials[0];
				}


			}

		}

		else if (!resultScalar) {
			__shared__ int *tadShapeBuffer;
			if(tid == 0) {
				xTadInfo = shape::tadInfo(xShapeInfo, dimension, dimensionLength);
				yTadInfo = shape::tadInfo(yShapeInfo, dimension, dimensionLength);
				tadShapeBuffer = shape::shapeBuffer(xTadInfo.tensorShapeLength,xTadInfo.tensorShape);
			}
			__syncthreads();

			if (reductionIndexesPerBlock * blockIdx.x >= resultLength)
				return;

			int tadsPerReductionIndex = tensorsForDimension / resultLength;

			//minimum number of threads needed for each reduction index
			int tadsNeeded = reductionIndexesPerBlock * tadsPerReductionIndex;

			//don't need all threads
			if (tid >= tadsNeeded)
				return;
			else {
				//process each tad
				//tad wrt the thread
				int currTad = tid + (blockIdx.x * reductionIndexesPerBlock);
				int offsetForTad = shape::offset(currTad, xShapeInfo, dimensionLength, xTadInfo);
				int yOffsetForTad = shape::offset(currTad, yShapeInfo, dimensionLength, yTadInfo);
				if(xElementWiseStride > 1 && yElementWiseStride > 1) {
					//update the reduction for the thread for the current tad
					//note here that we compute the offset and then accumulate in shared memory
					for (int element = 0;
							element < elementsPerTad; element++, offsetForTad += xElementWiseStride,yOffsetForTad += yElementWiseStride) {
						sPartials[tid] = update(sPartials[tid], op(dx[offsetForTad],dy[yOffsetForTad],&extraParamsVals), &extraParamsVals);
						__syncthreads();
					}
				}
				else {
					//update the reduction for the thread for the current tad
					//note here that we compute the offset and then accumulate in shared memory
					for (int element = 0;
							element < elementsPerTad; element++, offsetForTad += xElementWiseStride,yOffsetForTad += yElementWiseStride) {
						sPartials[tid] = update(sPartials[tid], op(dx[offsetForTad],dy[yOffsetForTad],&extraParamsVals), &extraParamsVals);
						__syncthreads();
					}
				}


			}

			//first thread for a reduction index
			if (tid % tadsPerReductionIndex == 0 && tadsPerReductionIndex > 1) {
				/**
				 * Each reduction index is handled by k tads
				 * which need to be combined in each thread.
				 *
				 * Since the TADS to be combined
				 * are to be next to each other
				 * we can assume that
				 * the items in shared memory
				 * can be combined and collapsed
				 * in to the first thread's
				 * entry.
				 *
				 * This follows a similar pattern
				 * for global block wise reduction
				 * and computing parallel sums
				 * in other reduction implementations.
				 *
				 */
				for (int i = 1; i < tadsPerReductionIndex; i++) {
					sPartials[tid] = update(sPartials[tid], sPartials[tid + i], &extraParamsVals);
					__syncthreads();
				}
			}

			__syncthreads();

			//after all the threads are done processing each first item in shared memory
			//should correspond to the final value for the particular reduction index
			//that was set for this block.
			if (tid == 0) {
				printf("Reduction indexes per block %d\n",reductionIndexesPerBlock);
				for (int i = 0; i < reductionIndexesPerBlock; i++) {
					int reductionIndexToProcess = i + blockIdx.x * reductionIndexesPerBlock;
					if (postProcessOrNot) {
						printf("About to post process with x length %d\n",xLength);
						result[reductionIndexToProcess] = postProcess(sPartials[i], xLength,&extraParamsVals);
					}
					else {
						result[reductionIndexToProcess] = sPartials[i];
					}
				}

				free(tadShapeBuffer);
				shape::freePermuteInfo(xTadInfo);
				shape::freePermuteInfo(yTadInfo);

			}

		}

		if(tid == 0)
			this->finalizeExtraParams(&extraParamsVals);

	}
#endif

	void exec(
			T *x,
			int *xShapeInfo,
			T *extraParamsVals,
			T *y, int *yShapeInfo,
			T *result, int *resultShapeInfo) {


		T startingVal = this->startingValue(x);
		int length = shape::length(xShapeInfo);
		int xElementWiseStride = shape::elementWiseStride(xShapeInfo);
		int yElementWiseStride = shape::elementWiseStride(yShapeInfo);
		int resultElementWiseStride = shape::elementWiseStride(resultShapeInfo);
		if (xElementWiseStride == 1 && resultElementWiseStride == 1) {
#pragma omp simd
			for (int i = 0; i < length; i++) {
				startingVal = update(startingVal, op(x[i], y[i], &extraParamsVals),
						&extraParamsVals);
			}

			result[0] = postProcess(startingVal, length,&extraParamsVals);

		} else {
#pragma omp simd

			for (int i = 0; i < length; i++) {
				startingVal = update(startingVal,
						op(x[i * xElementWiseStride], y[i * yElementWiseStride],
								&extraParamsVals), &extraParamsVals);
			}

			result[0] = postProcess(startingVal, length,&extraParamsVals);

		}

	}

	void exec(T *x, int *xShapeInfo, T *extraParamsVals, T *y, int *yShapeInfo,
			T *result, int *resultShapeInfoBuffer, int *dimension,
			int dimensionLength) {
		if(shape::isScalar(resultShapeInfoBuffer)) {
			exec(x,xShapeInfo,extraParamsVals,y,yShapeInfo,result,resultShapeInfoBuffer);
			return;
		}
		shape::TADPermuteInfo tadPermuteInfo = shape::tadInfo(xShapeInfo,
				dimension, dimensionLength);
		int resultLength = shape::length(resultShapeInfoBuffer);
		int tadElementWiseStride = shape::computeElementWiseStride(
				tadPermuteInfo.xRank, tadPermuteInfo.permutedShape,
				tadPermuteInfo.permutedStrides,
				shape::order(xShapeInfo) == 'f');
		int tadLength = tadPermuteInfo.tensorShapeProd;
#pragma omp simd
		for (int i = 0; i < shape::length(xShapeInfo); i++) {
			int reductionIndex = shape::reductionIndexForLinear(i,
					tadElementWiseStride, tadLength, resultLength,
					resultLength);
			result[reductionIndex] = update(result[reductionIndex],
					op(x[i], y[i], &extraParamsVals), &extraParamsVals);
		}
#pragma omp simd
		for (int i = 0; i < resultLength; i++) {
			result[i] = postProcess(result[i], tadLength,&extraParamsVals);
		}
	}

#ifdef __CUDACC__
	__host__ __device__
#endif
	virtual ~Reduce3() {
	}

#ifdef __CUDACC__
	__host__ __device__
#endif
	Reduce3() {
	}

};

namespace ops {
template<typename T>
class CosineSimilarity: public virtual Reduce3<T> {
public:

	virtual
#ifdef __CUDACC__
	__inline__ __host__ __device__
#endif
	T * generateExtraParams() {
		T *extraParams = (T *) malloc(sizeof(T) * 2);
		return extraParams;
	}
	virtual
#ifdef __CUDACC__
	__inline__ __host__ __device__
#endif
	void finalizeExtraParams(T **extraParams)  {
		free(*extraParams);
	}
	virtual
#ifdef __CUDACC__
	__inline__ __host__ __device__
#endif
	T startingValue(T *input) {
		return 0.0;
	}
#ifdef __CUDACC__
	__host__ __device__
#endif
	inline T postProcess(T reduction, int n,T **extraParamsRef) {
		T *extraParams = *extraParamsRef;
		return reduction / (nd4j::math::nd4j_sqrt<T>(extraParams[0]) * nd4j::math::nd4j_sqrt<T>(extraParams[1]));
	}
	/**
	 *
	 * @param d1
	 * @param d2
	 * @param extraParams
	 * @return
	 */
	//an op for the kernel
	virtual
#ifdef __CUDACC__
	__host__  __device__

#endif
	inline T op(T d1, T d2, T **extraParamsRef) {
		T *extraParams = *extraParamsRef;
		extraParams[0] += d1 * d1;
		extraParams[1] += d2 * d2;
		return (d1 * d2);
	}

	//calculate an update of the reduce operation
	/**
	 *
	 * @param old
	 * @param opOutput
	 * @param extraParams
	 * @return
	 */
	virtual
#ifdef __CUDACC__
	__host__  __device__

#endif
	inline T update(T old, T opOutput, T **extraParamsRef) {
		return old + opOutput;
	}

	/**
	 *
	 * @param old
	 * @param opOutput
	 * @param extraParams
	 * @return
	 */
	virtual
#ifdef __CUDACC__
	__host__  __device__

#endif
	inline T merge(T old, T opOutput, T **extraParamsRef) {
		return update(old, opOutput, extraParamsRef);
	}

	/** Name of the op
	 * @return the name of the operation
	 */
	virtual
#ifdef __CUDACC__
	inline __host__

#endif
	std::string name() {
		return std::string("cosinesimilarity_strided");
	}
#ifdef __CUDACC__
	__host__ __device__
#endif
	virtual ~CosineSimilarity() {
	}
#ifdef __CUDACC__
	__host__ __device__
#endif
	CosineSimilarity() {
	}
};

template<typename T>
class EuclideanDistance: public virtual Reduce3<T> {
public:
	virtual
#ifdef __CUDACC__
	__inline__ __host__ __device__
#endif
	T * generateExtraParams() {
		return NULL;
	}
	virtual
#ifdef __CUDACC__
	__inline__ __host__ __device__
#endif
	void finalizeExtraParams(T **extraParamsRef)  {
		//no-op
		free(*extraParamsRef);
	}
	virtual
#ifdef __CUDACC__
	__inline__ __host__ __device__
#endif
	T startingValue(T *input) {
		return 0.0;
	}
#ifdef __CUDACC__
	__host__ __device__
#endif
	inline T postProcess(T reduction, int n,T **extraParamsRef) {
		return nd4j::math::nd4j_sqrt<T>(reduction);
	}
	/**
	 *
	 * @param d1
	 * @param d2
	 * @param extraParams
	 * @return
	 */
	//an op for the kernel
	virtual
#ifdef __CUDACC__
	__host__  __device__

#endif
	inline T op(T d1, T d2, T **extraParamsRef) {
		T ret = d1 - d2;
		return ret * ret;
	}

	//calculate an update of the reduce operation
	/**
	 *
	 * @param old
	 * @param opOutput
	 * @param extraParams
	 * @return
	 */
	virtual
#ifdef __CUDACC__
	__host__  __device__

#endif
	inline T update(T old, T opOutput, T **extraParamsRef) {
		return opOutput + old;
	}

	/**
	 *
	 * @param old
	 * @param opOutput
	 * @param extraParams
	 * @return
	 */
	virtual
#ifdef __CUDACC__
	__host__  __device__

#endif
	inline T merge(T old, T opOutput, T **extraParamsRef) {
		return update(old, opOutput, extraParamsRef);
	}

	/** Name of the op
	 * @return the name of the operation
	 */
	virtual
#ifdef __CUDACC__
	inline __host__

#endif
	std::string name() {
		return std::string("euclidean_strided");
	}
#ifdef __CUDACC__
	__host__ __device__
#endif
	virtual ~EuclideanDistance() {
	}
#ifdef __CUDACC__
	__host__ __device__
#endif
	EuclideanDistance() {
	}
};

template<typename T>
class ManhattanDistance: public virtual Reduce3<T> {
public:
	virtual
#ifdef __CUDACC__
	__inline__ __host__ __device__
#endif
	T * generateExtraParams() {
		return NULL;
	}
	virtual
#ifdef __CUDACC__
	__inline__ __host__ __device__
#endif
	void finalizeExtraParams(T **extraParamsRef)  {
		//no op
		free(*extraParamsRef);
	}
	virtual
#ifdef __CUDACC__
	__inline__ __host__ __device__
#endif
	T startingValue(T *input) {
		return 0.0;
	}
#ifdef __CUDACC__
	__host__ __device__
#endif
	inline T postProcess(T reduction, int n,T **extraParamsRef) {
		return reduction;
	}
	/**
	 *
	 * @param d1
	 * @param d2
	 * @param extraParams
	 * @return
	 */
	//an op for the kernel
	virtual
#ifdef __CUDACC__
	__host__  __device__

#endif
	inline T op(T d1, T d2, T **extraParamsRef) {
		return nd4j::math::nd4j_abs<T>(d1 - d2);
	}

	//calculate an update of the reduce operation
	/**
	 *
	 * @param old
	 * @param opOutput
	 * @param extraParams
	 * @return
	 */
	virtual
#ifdef __CUDACC__
	__host__  __device__

#endif
	inline T update(T old, T opOutput, T **extraParamsRef) {
		return old + opOutput;
	}

	/**
	 *
	 * @param old
	 * @param opOutput
	 * @param extraParams
	 * @return
	 */
	virtual
#ifdef __CUDACC__
	__host__  __device__

#endif
	inline T merge(T old, T opOutput, T **extraParamsRef) {
		return update(old, opOutput, extraParamsRef);
	}

	/** Name of the op
	 * @return the name of the operation
	 */
	virtual
#ifdef __CUDACC__
	inline __host__

#endif
	std::string name() {
		return std::string("manhattan_strided");
	}
#ifdef __CUDACC__
	__host__ __device__
#endif
	virtual ~ManhattanDistance() {
	}
#ifdef __CUDACC__
	__host__ __device__
#endif
	ManhattanDistance() {
	}
};

}

template<typename T>
class Reduce3OpFactory {
public:

#ifdef __CUDACC__
	__host__ __device__
#endif
	Reduce3OpFactory() {
	}


#ifdef __CUDACC__
	__inline__ __host__ __device__
#endif
	Reduce3<T> * getOp(int op) {
		if (op == 0)
			return new functions::reduce3::ops::ManhattanDistance<T>();
		else if (op == 1)
			return new functions::reduce3::ops::EuclideanDistance<T>();
		else if (op == 2)
			return new functions::reduce3::ops::CosineSimilarity<T>();
		return NULL;
	}
};

}
}

#ifdef __CUDACC__
__constant__ functions::reduce3::Reduce3OpFactory<double> *reduce3OpFactory;
__constant__ functions::reduce3::Reduce3OpFactory<float> *reduce3OpFactoryFloat;

extern "C"
__host__ void setupReduce3Factories() {
	/*printf("Setting up transform factories\n");
	functions::reduce3::Reduce3OpFactory<double> *newOpFactory =  new functions::reduce3::Reduce3OpFactory<double>();
	functions::reduce3::Reduce3OpFactory<float> *newOpFactoryFloat =  new functions::reduce3::Reduce3OpFactory<float>();
	checkCudaErrors(cudaMemcpyToSymbol(reduce3OpFactory, newOpFactory, sizeof( functions::reduce3::Reduce3OpFactory<double> )));
	checkCudaErrors(cudaMemcpyToSymbol(reduce3OpFactoryFloat, newOpFactory, sizeof( functions::reduce3::Reduce3OpFactory<float>)));
	delete(newOpFactory);
	delete(newOpFactoryFloat);*/
}


template <typename T>
__device__ void reduce3Generic(
		int opNum,
		int n, T *dx, int *xShapeInfo,
		T *dy,
		int *yShapeInfo, T *extraParams, T *result,
		int *resultShapeInfo, int *gpuInformation,
		int *dimension,
		int dimensionLength, int postProcessOrNot) {
	__shared__ functions::reduce3::Reduce3<T> * op;
	__shared__ functions::reduce3::Reduce3OpFactory<T> *reduce3OpFactory;

	if(threadIdx.x == 0)
		reduce3OpFactory = new functions::reduce3::Reduce3OpFactory<T>();
	__syncthreads();

	if(threadIdx.x == 0)
		op = reduce3OpFactory->getOp(opNum);
	__syncthreads();
	op->transform(n,dx,xShapeInfo,dy,yShapeInfo,extraParams,result,resultShapeInfo,gpuInformation,dimension,dimensionLength,postProcessOrNot);
	if(threadIdx.x == 0) {
		free(op);
		free(reduce3OpFactory);
	}

}

extern "C" __global__ void reduce3Double(
		int opNum,
		int n, double *dx, int *xShapeInfo,
		double *dy,
		int *yShapeInfo, double *extraParams, double *result,
		int *resultShapeInfo, int *gpuInformation,
		int *dimension,
		int dimensionLength, int postProcessOrNot) {
	reduce3Generic<double>(opNum,n,dx,xShapeInfo,dy,yShapeInfo,extraParams,result,resultShapeInfo,gpuInformation,dimension,dimensionLength,postProcessOrNot);

}
extern "C" __global__ void reduce3Float(
		int opNum,
		int n, float *dx, int *xShapeInfo,
		float *dy,
		int *yShapeInfo, float *extraParams,
		float *result,
		int *resultShapeInfo,
		int *gpuInformation,
		int *dimension,
		int dimensionLength, int postProcessOrNot) {
	reduce3Generic<float>(opNum,n,dx,xShapeInfo,dy,yShapeInfo,extraParams,result,resultShapeInfo,gpuInformation,dimension,dimensionLength,postProcessOrNot);

}

#endif



#endif /* REDUCE3_H_ */
