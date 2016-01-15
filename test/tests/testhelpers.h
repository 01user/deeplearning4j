/*
 * testhelpers.h
 *
 *  Created on: Jan 1, 2016
 *      Author: agibsonccc
 */

#ifndef TESTHELPERS_H_
#define TESTHELPERS_H_
#include <CppUTest/TestHarness.h>
#include <templatemath.h>
int arrsEquals(int rank, int *comp1, int *comp2);

template<typename T>
int arrsEquals(int rank, T *comp1, T *comp2) {
	int ret = 1;
	for (int i = 0; i < rank; i++) {
		T eps = nd4j::math::nd4j_abs(comp1[i] - comp2[i]);
		ret = ret && (eps < 1e-3);
	}

	return ret;
}

template <typename T>
class Data {
public:
	T scalar;
	T *data;
	T *y;
	T *result;
	T *extraParams;
	T *assertion;
	int *xShape;
	int *yShape;
	int *resultShape;
	int rank;
	int yRank;
	int resultRank;
	int *dimension;
	int dimensionLength;

};


/**
 * Get the shape info buffer
 * for the given rank and shape.
 */
int *shapeBuffer(int rank, int *shape) {
	int *stride = shape::calcStrides(shape, rank);
	shape::ShapeInformation * shapeInfo = (shape::ShapeInformation *) malloc(
			sizeof(shape::ShapeInformation));
	shapeInfo->shape = shape;
	shapeInfo->stride = stride;
	shapeInfo->offset = 0;
	shapeInfo->rank = rank;
	int elementWiseStride = shape::computeElementWiseStride(rank, shape, stride,
			0);
	shapeInfo->elementWiseStride = elementWiseStride;
	int *shapeInfoBuffer = shape::toShapeBuffer(shapeInfo);
	free(shapeInfo);
	return shapeInfoBuffer;
}

void assertBufferProperties(int *shapeBuffer) {
	CHECK(shape::rank(shapeBuffer) >= 2);
	CHECK(shape::length(shapeBuffer) >= 1);
	CHECK(shape::elementWiseStride(shapeBuffer) >= 1);
}


nd4j::buffer::Buffer<int> * shapeIntBuffer(int rank ,int*shape) {
	int *shapeBuffRet = shapeBuffer(rank,shape);
	nd4j::buffer::Buffer<int> *ret = nd4j::buffer::createBuffer(shapeBuffRet,shape::shapeInfoLength(rank));
	return ret;
}

nd4j::buffer::Buffer<int> * gpuInformationBuffer(int blockSize,int gridSize,int sharedMemorySize) {
	int *ret = (int *) malloc(sizeof(int) * 4);
	ret[0] = blockSize;
	ret[1] = gridSize;
	ret[2] = sharedMemorySize;
	ret[3] = sharedMemorySize;
	nd4j::buffer::Buffer<int> *ret2 = nd4j::buffer::createBuffer(ret,4);
	return ret2;
}


template <typename T>
class BaseTest {


public:
	BaseTest() {
	}
	BaseTest(int rank,int opNum,Data<T> *data,int extraParamsLength) {
		this->rank = rank;
		this->baseData = data;
		this->opNum = opNum;
		this->extraParamsLength = extraParamsLength;
		init();
	}



	virtual ~BaseTest() {
		nd4j::array::NDArrays<T>::freeNDArrayOnGpuAndCpu(&data);
		freeAssertion();
	}


	virtual nd4j::buffer::Buffer<int> * gpuInformationBuffer() {
		int *ret = (int *) malloc(sizeof(int) * 4);
		ret[0] = blockSize;
		ret[1] = gridSize;
		ret[2] = sMemSize;
		ret[3] = sMemSize;
		nd4j::buffer::Buffer<int> *ret2 = nd4j::buffer::createBuffer(ret,4);
		return ret2;
	}



protected:
	int rank;
	int *shape = NULL;
	int *stride = NULL;
	Data<T> *baseData;
	nd4j::array::NDArray<T> *data = NULL;
	nd4j::array::NDArray<T> *result = NULL;
	T *assertion = NULL;
	T *extraParams = NULL;
	int blockSize = 500;
	int gridSize = 256;
	int sMemSize = 20000;
	nd4j::buffer::Buffer<T> *extraParamsBuff = NULL;
	int length;
	int opNum;
	int extraParamsLength;
	virtual void executeCudaKernel(){}
	virtual void freeOpAndOpFactory() {};
	virtual void createOperationAndOpFactory() {}
	virtual void run() {}


	virtual void init() {
		shape = this->baseData->xShape;
		rank = this->baseData->rank;
		stride = shape::calcStrides(shape, rank);
		data = nd4j::array::NDArrays<T>::createFrom(rank, shape, stride, 0,
				0.0);
		initializeData();
		length = nd4j::array::NDArrays<T>::length(data);
		extraParams = this->baseData->extraParams;
		extraParamsBuff = nd4j::buffer::createBuffer(extraParams,extraParamsLength);
		assertion = this->baseData->assertion;

	}

	virtual void freeAssertion() {
		//printf("About to free assertion\n");
		if(assertion != NULL)
			free(assertion);
		if(extraParamsBuff != NULL)
			nd4j::buffer::freeBuffer(&extraParamsBuff);
	}

	virtual void initializeData() {
		for (int i = 0; i < length; i++)
			data->data->data[i] = i + 1;
	}

};

template <typename T>
class PairWiseTest : public BaseTest<T> {
protected:
	int yRank;
	int *yShape;
	int *yStride;
	typedef BaseTest<T> super;
	nd4j::array::NDArray<T> *yData;

public:
	PairWiseTest() {
	}
	//BaseTest(int rank,int opNum,Data<T> *data,int extraParamsLength)
	PairWiseTest(int rank,int opNum,Data<T> *data,int extraParamsLength)
	:BaseTest<T>(rank,opNum,data,extraParamsLength)  {
		init();
	}
	virtual ~PairWiseTest() {}
	virtual void init() override {
		yRank = this->baseData->yRank;
		yShape = (int *) malloc(sizeof(int) * yRank);
		yStride = shape::calcStrides(yShape,yRank);
		yData = nd4j::array::NDArrays<T>::createFrom(yRank, yShape, yStride, 0,
				0.0);
	}


};



template <typename T>
class TwoByTwoTest : public BaseTest<T> {
public:
	virtual ~TwoByTwoTest() {}
	TwoByTwoTest(int rank,int opNum,Data<T> *data,int extraParamsLength) : BaseTest<T>(rank,opNum,data,extraParamsLength) {}
	TwoByTwoTest(int opNum,Data<T> *data,int extraParamsLength) : BaseTest<T>(2,opNum,data,extraParamsLength) {}
	virtual void initShape() override {
		for(int i = 0; i < 2; i++) {
			this->shape[i] = 2;
		}
	}
protected:
	typedef BaseTest<T> super;
};






#endif /* TESTHELPERS_H_ */
