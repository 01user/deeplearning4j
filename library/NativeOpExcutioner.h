//
// Created by agibsonccc on 1/28/16.
//

#ifndef NATIVEOPERATIONS_NATIVEOPEXCUTIONER_H
#define NATIVEOPERATIONS_NATIVEOPEXCUTIONER_H

#include <broadcasting.h>
#include <indexreduce.h>
#include <pairwise_transform.h>
#include <reduce.h>
#include <reduce3.h>
#include <summarystatsreduce.h>
#include <transform.h>
#include <scalar.h>

/**
 * Native op executioner:
 *
 */
template <typename T>
class NativeOpExcutioner {
private:
    functions::broadcast::BroadcastOpFactory<T> *broadcastOpFactory = new functions::broadcast::BroadcastOpFactory<T>();
    functions::indexreduce::IndexReduceOpFactory<T> *indexReduceOpFactory = new functions::indexreduce::IndexReduceOpFactory<T>();
    functions::pairwise_transforms::PairWiseTransformOpFactory<T> *pairWiseTransformOpFactory = new functions::pairwise_transforms::PairWiseTransformOpFactory<T>();
    functions::reduce::ReduceOpFactory *reduceOpFactory = new functions::reduce::ReduceOpFactory<T>();
    functions::reduce3::Reduce3OpFactory *reduce3OpFactory = new functions::reduce3::Reduce3OpFactory<T>();
    functions::scalar::ScalarOpFactory *scalarOpFactory = new functions::scalar::ScalarOpFactory<T>();
    functions::summarystats::SummaryStatsReduceOpFactory *summaryStatsReduceOpFactory = new functions::summarystats::SummaryStatsReduceOpFactory<T>();
    functions::transform::TransformOpFactory *transformOpFactory = new functions::transform::TransformOpFactory<T>();

public:
    ~NativeOpExcutioner() {
        delete broadcastOpFactory;
        delete indexReduceOpFactory;
        delete pairWiseTransformOpFactory;
        delete reduceOpFactory;
        delete reduce3OpFactory;
        delete scalarOpFactory;
        delete summaryStatsReduceOpFactory;
        delete transformOpFactory;
    }

    /**
     *
     * @param opNum
     * @param x
     * @param xShapeInfo
     * @param extraParams
     * @param result
     * @param resultShapeInfo
     */
    T execIndexReduceScalar(int opNum,
                            T *x,
                            int *xShapeInfo,
                            T *extraParams) {
        functions::indexreduce::IndexReduce<T> *op = indexReduceOpFactory->getOp(opNum);
        T ret = op->execScalar(x,xShapeInfo,extraParams);
        delete op;
        return ret;

    }

    /**
     *
     * @param opNum
     * @param x
     * @param xShapeInfo
     * @param extraParams
     * @param result
     * @param resultShapeInfoBuffer
     * @param dimension
     * @param dimensionLength
     */
    void execIndexReduce(int opNum,
                         T *x,
                         int *xShapeInfo,
                         T *extraParams,
                         T *result,
                         int *resultShapeInfoBuffer,
                         int *dimension, int dimensionLength) {
        functions::indexreduce::IndexReduce<T> *op = indexReduceOpFactory->getOp(opNum);
        op->exec(x,xShapeInfo,extraParams,result,resultShapeInfoBuffer);
        delete op;
    }

    /**
     *
     * @param opNum
     * @param x
     * @param xShapeInfo
     * @param y
     * @param yShapeInfo
     * @param result
     * @param resultShapeInfo
     * @param dimension
     * @param dimensionLength
     */
    void execBroadcast(int opNum,
                       T *x,
                       int *xShapeInfo,
                       T *y,
                       int *yShapeInfo,
                       T *result,
                       int *resultShapeInfo,
                       int *dimension, int dimensionLength) {

        functions::broadcast::Broadcast *broadcast<T> = broadcastOpFactory->getOp(opNum);
        broadcast->exec(x,xShapeInfo,y,yShapeInfo,result,resultShapeInfo,dimension,dimensionLength);
        delete broadcast;
    }


    /**
     *
     * @param opNum
     * @param dx
     * @param xStride
     * @param y
     * @param yStride
     * @param result
     * @param resultStride
     * @param extraParams
     * @param n
     */
    void execPairwiseTransform(int opNum,
                               T *dx,
                               int xStride,
                               T *y,
                               int yStride,
                               T *result,
                               int resultStride,
                               T *extraParams, int n) {
        functions::pairwise_transforms::PairWiseTransform<T> *op = pairWiseTransformOpFactory->getOp(opNum);
        op->exec(dx,xStride,y,yStride,result,resultStride,extraParams,n);
        delete op;
    }


    /**
     *
     * @param opNum
     * @param x
     * @param xShapeInfo
     * @param extraParams
     * @param result
     * @param resultShapeInfo
     */
    void execReduce(int opNum,
                    T *x,
                    int *xShapeInfo,
                    T *extraParams,
                    T *result,
                    int *resultShapeInfo) {
        functions::reduce::ReduceFunction<T> *reduceFunction = reduceOpFactory->create(opNum);
        reduceFunction->exec(x,xShapeInfo,extraParams,result,resultShapeInfo);
        delete reduceFunction;
    }

    /**
     *
     * @param opNum
     * @param x
     * @param xShapeInfo
     * @param extraParams
     * @return
     */
    T execReduceScalar(int opNum,
                       T *x,
                       int *xShapeInfo,
                       T *extraParams) {
        functions::reduce::ReduceFunction *reduceFunction = reduceOpFactory->create(opNum);
        T ret = reduceFunction->exec(x,xShapeInfo,extraParams);
        delete reduceFunction;
        return ret;
    }
    /**
     *
     * @param opNum
     * @param x
     * @param xShapeInfo
     * @param extraParamsVals
     * @param y
     * @param yShapeInfo
     * @param result
     * @param resultShapeInfo
     */
    void execReduce3(int opNum,
                     T *x,
                     int *xShapeInfo,
                     T *extraParamsVals,
                     T *y,
                     int *yShapeInfo,
                     T *result, int *resultShapeInfo) {
        functions::reduce3::Reduce3 *reduce3 = reduce3OpFactory->getOp(opNum);
        reduce3->exec(x,xShapeInfo,extraParamsVals,y,yShapeInfo,result,resultShapeInfo);
        delete reduce3;

    }

    /**
     *
     * @param opNum
     * @param x
     * @param xShapeInfo
     * @param extraParamsVals
     * @param y
     * @param yShapeInfo
     * @param result
     * @param resultShapeInfoBuffer
     * @param dimension
     * @param dimensionLength
     */
    void execReduce3(int opNum,
                     T *x,
                     int *xShapeInfo,
                     T *extraParamsVals,
                     T *y,
                     int *yShapeInfo,
                     T *result,
                     int *resultShapeInfoBuffer,
                     int *dimension,
                     int dimensionLength) {
        functions::reduce3::Reduce3 *reduce3 = reduce3OpFactory->getOp(opNum);
        reduce3->exec(x,xShapeInfo,extraParamsVals,y,yShapeInfo,result,resultShapeInfo,dimension,dimensionLength);
        delete reduce3;

    }

    /**
     *
     * @param opNum
     * @param x
     * @param xStride
     * @param result
     * @param resultStride
     * @param scalar
     * @param extraParams
     * @param n
     */
    void execScalar(int opNum,
                    T *x,
                    int xStride,
                    T *result,
                    int resultStride,
                    T scalar,
                    T *extraParams,
                    int n) {
        functions::scalar::ScalarTransform *scalarTransform = scalarOpFactory->getOp(opNum);
        scalarTransform->transform(x,xStride,result,resultStride,scalarTransform,extraParams,n);
        delete scalarTransform;


    }

    /**
     *
     * @param opNum
     * @param x
     * @param xShapeInfo
     * @param extraParams
     * @param result
     * @param resultShapeInfo
     */
    void execSummaryStats(int opNum,T *x,
                          int *xShapeInfo,
                          T *extraParams,
                          T *result,
                          int *resultShapeInfo) {
        functions::summarystats::SummaryStatsReduce *op = summaryStatsReduceOpFactory->getOp(opNum);
        op->exec(x,xShapeInfo,extraParams,result,resultShapeInfo);
        delete op;
    }

    /**
    *
    * @param opNum
    * @param x
    * @param xShapeInfo
    * @param extraParams
    * @param result
    * @param resultShapeInfo
    */
    T execSummaryStatsScalar(int opNum,T *x,
                             int *xShapeInfo,
                             T *extraParams) {
        functions::summarystats::SummaryStatsReduce *op = summaryStatsReduceOpFactory->getOp(opNum);
        T ret = op->execScalar(x,xShapeInfo,extraParams);
        delete op;
        return ret;
    }

    /**
     *
     * @param opNum
     * @param x
     * @param xShapeInfo
     * @param extraParams
     * @param result
     * @param resultShapeInfoBuffer
     * @param dimension
     * @param dimensionLength
     */
    void execSummaryStats(int opNum,T *x,
                          int *xShapeInfo,
                          T *extraParams,
                          T *result,
                          int *resultShapeInfoBuffer,
                          int *dimension, int dimensionLength) {
        functions::summarystats::SummaryStatsReduce *op = summaryStatsReduceOpFactory->getOp(opNum);
        op->exec(x,xShapeInfo,extraParams,result,resultShapeInfo,dimension,dimensionLength);
        delete op;

    }

    /**
     *
     * @param opNum
     * @param dx
     * @param xStride
     * @param result
     * @param resultStride
     * @param extraParams
     * @param n
     */
    void execTransform(int opNum,T *dx, int xStride, T *result, int resultStride,
                       T *extraParams, int n) {
        functions::transform::Transform *transform = transformOpFactory->getOp(opNum);
        transform->exec(x,xStride,result,resultStride,extraParams,n);
        delete transform;

    }

};


#endif //NATIVEOPERATIONS_NATIVEOPEXCUTIONER_H
