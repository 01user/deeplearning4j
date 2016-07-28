package org.nd4j.linalg.dataset.api.preprocessor;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.broadcast.BroadcastAddOp;
import org.nd4j.linalg.api.ops.impl.broadcast.BroadcastDivOp;
import org.nd4j.linalg.api.ops.impl.broadcast.BroadcastMulOp;
import org.nd4j.linalg.api.ops.impl.broadcast.BroadcastSubOp;
import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Standard scaler calculates a moving column wise
 * variance and mean
 * http://www.johndcook.com/blog/standard_deviation/
 */
public class NormalizerStandardize implements DataNormalization {
    private static Logger logger = LoggerFactory.getLogger(NormalizerStandardize.class);
    private INDArray mean,std, meanLabel, stdLabel;
    private int runningTotal , labelRunningTotal = 0;
    private int batchCount,labelbatchCount = 0;
    private int featureRank = 2;
    private INDArray featureMeanStd, labelMeanStd;
    private INDArray featureMean, featureStd, labelMean, labelStd;
    private boolean fitLabels = false;

    private INDArray fit(INDArray theArray) {
        INDArray theMean, theStd;
        theMean = theArray.mean(0);
        theStd = theArray.std(0);
        theStd.addi(Nd4j.scalar(Nd4j.EPS_THRESHOLD));
        if (theStd.min(1) == Nd4j.scalar(Nd4j.EPS_THRESHOLD))
            logger.info("API_INFO: Std deviation found to be zero. Transform will round upto epsilon to avoid nans.");
        return Nd4j.vstack(theMean,theStd).dup();
    }

    private void runnningFit(INDArray thenewArray, INDArray currentMeanStd, int batchCount, int runningTotal, boolean allDone) {
        if (!allDone) {
            INDArray currentMean = currentMeanStd.getRow(0);
            INDArray currentStd = currentMeanStd.getRow(1);
            // m_newM = m_oldM + (x - m_oldM)/m_n;
            INDArray xMinusMean = thenewArray.subRowVector(currentMean);
            INDArray newMean = currentMean.add(xMinusMean.sum(0).divi(runningTotal));
            // Using http://i.stanford.edu/pub/cstr/reports/cs/tr/79/773/CS-TR-79-773.pdf
            // for a version of calc variance when dataset is partitioned into two sample sets
            // Also described in https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Parallel_algorithm
            // delta = mean_B - mean_A; A is data seen so far, B is the current batch
            // M2 is the var*n
            // M2 = M2_A + M2_B + delta^2 * nA * nB/(nA+nB)
            INDArray meanB = thenewArray.mean(0);
            INDArray deltaSq = Transforms.pow(meanB.subRowVector(currentMean), 2);
            INDArray deltaSqScaled = deltaSq.mul(((float) runningTotal - batchCount) * batchCount / (float) runningTotal);
            INDArray mtwoB = Transforms.pow(thenewArray.std(0), 2);
            mtwoB.muli(batchCount);
            currentStd.addi(mtwoB);
            currentStd.addi(deltaSqScaled);
            currentMean = newMean;
        }
        else {
            currentMeanStd.getRow(1).divi(runningTotal);
            currentMeanStd.putRow(0,Transforms.sqrt(currentMeanStd.getRow(1));
            currentMeanStd.getRow(1).addi(Nd4j.scalar(Nd4j.EPS_THRESHOLD));
            if (currentMeanStd.getRow(0).min(1) == Nd4j.scalar(Nd4j.EPS_THRESHOLD))
                logger.info("API_INFO: Std deviation found to be zero. Transform will round upto epsilon to avoid nans.");
        }
    }

    /**
     * Flag to specify if the labels/outputs in the dataset should be also normalized
     * default value is false
     * @param fitLabels
     */

    public void fitLabel(boolean fitLabels) {
        this.fitLabels = fitLabels;
    }

    /**
     * Fit the given model with dataset
     * to calculate mean and std dev with
     * @param dataSet
     */
    public void fit(DataSet dataSet) {
        featureRank = dataSet.getFeatureMatrix().rank();

        INDArray theFeatures = dataSet.getFeatureMatrix();
        if (featureRank == 3) theFeatures = tailor3d2d(dataSet,false);
        if (featureRank == 4) theFeatures = tailor4d2d(dataSet,false);
        featureMeanStd = fit(theFeatures);

        featureMean = featureMeanStd.getRow(0).dup();
        featureStd = featureMeanStd.getRow(1).dup();

        if (fitLabels) {
            INDArray theLabels = dataSet.getLabels();
            if (featureRank == 3) theLabels = tailor3d2d(dataSet,true);
            if (featureRank == 4) theLabels = tailor4d2d(dataSet,true);
            labelMeanStd = fit(theLabels);
            labelMean = labelMeanStd.getRow(0).dup();
            labelStd = labelMeanStd.getRow(1).dup();
        }

    }

    /**
     * Fit the given model with a given iterator
     * to calculate mean and std dev with
     * @param iterator
     */
    public void fit(DataSetIterator iterator) {
        while(iterator.hasNext()) {
            DataSet next = iterator.next();
            batchCount = next.getFeaturesMaskArray() != null ? next.getFeaturesMaskArray().sumNumber().intValue() :  next.getFeatures().size(0);
            runningTotal += batchCount;
            if(featureMeanStd == null) {
                this.fit(next);
                featureMeanStd.getRow(1).muli(batchCount);
                if (fitLabels) {
                    labelMeanStd.getRow(1).muli(batchCount);
                    labelbatchCount = next.getLabelsMaskArray() != null ? next.getLabelsMaskArray().sumNumber().intValue() :  next.getFeatures().size(0);
                    labelRunningTotal += batchCount;
                }
            }
            else {
                this.runnningFit(next.getFeatures(),featureMeanStd,batchCount,runningTotal,false);
                if (fitLabels) this.runnningFit(next.getLabels(),labelMeanStd,labelbatchCount,labelRunningTotal,false);
            }
        }
        this.runnningFit(featureMeanStd,featureMeanStd,batchCount,runningTotal,true);
        featureMean = featureMeanStd.getRow(0).dup();
        featureStd = featureMeanStd.getRow(1).dup();
        if (fitLabels) {
            this.runnningFit(labelMeanStd,labelMeanStd,labelbatchCount,labelRunningTotal,true);
            labelMean = labelMeanStd.getRow(0).dup();
            labelStd = labelMeanStd.getRow(1).dup();
        }
        iterator.reset();
    }

    @Override
    public void preProcess(DataSet toPreProcess) {
        if (featureMeanStd == null) throw new RuntimeException("API_USE_ERROR: Preprocessors have to be explicitly fit before use. Usage: .fit(dataset) or .fit(datasetiterator)");
        INDArray theFeatures = toPreProcess.getFeatures();
        INDArray theLabels = toPreProcess.getLabels();
        this.preProcess(theFeatures,true);
        if (fitLabels) this.preProcess(theLabels,false);
    }

    private void preProcess(INDArray theFeatures, boolean isFeatures) {
        INDArray mean, std;
        mean = isFeatures ? featureMean : labelMean;
        std = isFeatures ? featureStd : labelStd;
        if (featureRank == 2) {
            theFeatures.subiRowVector(mean);
            theFeatures.diviRowVector(std);
        }
        // if feature Rank is 3 (time series) samplesxfeaturesxtimesteps
        // if feature Rank is 4 (images) samplesxchannelsxrowsxcols
        // both cases operations should be carried out in dimension 1
        else {
            Nd4j.getExecutioner().execAndReturn(new BroadcastSubOp(theFeatures,mean,theFeatures,1));
            Nd4j.getExecutioner().execAndReturn(new BroadcastDivOp(theFeatures,std,theFeatures,1));
        }

    }

    /**
     * Transform the given dataset
     * @param toPreProcess
     */
    @Override
    public void transform(DataSet toPreProcess) {
        this.preProcess(toPreProcess);
    }

    /**
     * Transform the given INDArray
     * @param theFeatures
     */
    public void transform(INDArray theFeatures) {
        this.transform(theFeatures,true);
    }

    public void transform(INDArray theArray, boolean isFeatures) {
        this.preProcess(theArray,isFeatures);
    }

    /**
     * Not supported
     * @param toPreProcessIter the dataset to transform
     */
    @Override
    public void transform(DataSetIterator toPreProcessIter) {
        logger.info("Transform with an iterator is NOT supported. Use setPreProcessor on the iterator instead.");
    }

    /**
     * Revert the data to what it was before transform
     * @param toPreProcess the dataset to revert back
     */
    public void revert(DataSet toPreProcess) {
        if (featureMean== null || featureStd == null) throw new RuntimeException("API_USE_ERROR: Preprocessors have to be explicitly fit before use. Usage: .fit(dataset) or .fit(datasetiterator)");
        if (featureRank == 2) {
            toPreProcess.getFeatures().muliRowVector(featureStd);
            toPreProcess.getFeatures().addiRowVector(featureMean);
        }
        else {
            Nd4j.getExecutioner().execAndReturn(new BroadcastMulOp(toPreProcess.getFeatures(),featureStd,toPreProcess.getFeatures(),1));
            Nd4j.getExecutioner().execAndReturn(new BroadcastAddOp(toPreProcess.getFeatures(),featureMean,toPreProcess.getFeatures(),1));
        }
    }

    public void revert(DataSetIterator toPreProcessIter) {
        while (toPreProcessIter.hasNext()) {
            this.revert(toPreProcessIter.next());
        }
        toPreProcessIter.reset();
    }

    public INDArray getMean() {
        if (featureMean == null) throw new RuntimeException("API_USE_ERROR: Preprocessors have to be explicitly fit before use. Usage: .fit(dataset) or .fit(datasetiterator)");
        if (fitLabels) return Nd4j.vstack(featureMean,labelMean);
        return featureMean;
    }

    public INDArray getStd() {
        if (featureStd == null) throw new RuntimeException("API_USE_ERROR: Preprocessors have to be explicitly fit before use. Usage: .fit(dataset) or .fit(datasetiterator)");
        if (fitLabels) return Nd4j.vstack(featureStd,labelStd);
        return featureStd;
    }

    /**
     * Load the given mean and std
     *@param statistics the statistics to laod
     * @throws IOException
     */
    @Override
    public void load(File...statistics) throws IOException {
        this.mean = Nd4j.readBinary(statistics[0]);
        this.std = Nd4j.readBinary(statistics[1]);
    }

    /**
     * Save the current mean and std
     * @param statistics the statistics to save
     * @throws IOException
     */
    @Override
    public void save(File...statistics) throws IOException {
        Nd4j.saveBinary(this.mean,statistics[0]);
        Nd4j.saveBinary(this.std,statistics[1]);
    }

    private INDArray tailor3d2d(DataSet dataset, boolean areFeatures) {
        /* A 2d dataset has dimemsions sample x features
         * A 3d dataset is a timeseries with dimensions sample x features x timesteps
         * A 3d dataset can also have a mask associated with it in case samples are of varying time steps
         * Each sample has a mask associated with it that is applied to all features.
         * Masks are of dimension sample x timesteps
         */
        INDArray theArray, theMask;
        theArray = areFeatures ? dataset.getFeatures() : dataset.getLabels();
        theMask = areFeatures ? dataset.getFeaturesMaskArray() : dataset.getLabelsMaskArray();

        int instances = theArray.size(0);
        int features = theArray.size(1);
        int timesteps = theArray.size(2);

        boolean hasMasks = theMask != null;
        INDArray in2d = Nd4j.create(features,timesteps*instances);

        int tads = theArray.tensorssAlongDimension(2,0);
        // the number of tads are the number of features
        for(int i = 0; i < tads; i++){
            INDArray thisTAD = theArray.tensorAlongDimension(i, 2, 0);
            //mask is samples x timesteps
            if (hasMasks)
                //if there are masks they are multiplied with the mask array to wipe out the values associated with it
                //to wipe out the values associated with it to wipe out the values associated with it
                thisTAD.muli(theMask);
            //Each row is now values for a given feature across all time steps, across all samples
            in2d.putRow(i, Nd4j.toFlattened('c',thisTAD));
        }
        //Must transpose to return a matrix compatible with 2d viz samples x features
        in2d = in2d.transpose();
        //flatten mask
        if (hasMasks) {
            //only need rows where columnMask is 1
            INDArray columnMask = Nd4j.toFlattened('c',theMask).transpose();
            int actualSamples = columnMask.sumNumber().intValue();
            INDArray in2dMask = Nd4j.create(actualSamples,features);
            int j = 0;
            for (int i=0; i < timesteps*instances; i++){
                if (columnMask.getInt(i, 0) != 0) {
                    in2dMask.putRow(j, in2d.getRow(i));
                    j++;
                }
            }
            return in2dMask;
        }
        return in2d;
    }

    private INDArray tailor4d2d(DataSet dataset, boolean areFeatures) {
        INDArray theArray;
        theArray = areFeatures ? dataset.getFeatures() : dataset.getLabels();
        int instances = theArray.size(0);
        int channels = theArray.size(1);
        int height = theArray.size(2);
        int width = theArray.size(3);

        INDArray in2d = Nd4j.create(channels,height*width*instances);

        int tads = theArray.tensorssAlongDimension(3,2,0);
        for(int i = 0; i < tads; i++){
            INDArray thisTAD = theArray.tensorAlongDimension(i, 3, 2, 0);
            in2d.putRow(i, Nd4j.toFlattened(thisTAD));
        }
        return in2d.transposei();
    }

}
