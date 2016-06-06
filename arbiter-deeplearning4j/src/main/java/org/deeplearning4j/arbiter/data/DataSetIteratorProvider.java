package org.deeplearning4j.arbiter.data;

import org.deeplearning4j.arbiter.optimize.api.data.DataProvider;
import org.deeplearning4j.datasets.iterator.DataSetIterator;

import java.util.Map;

/**
 * DataSetIteratorProvider: a simple DataProver that takes (and returns) DataSetIterators, one for the training
 * data and one for the test data.
 * <p>
 * <b>NOTE</b>: This is NOT thread safe, as the underlying DataSetIterators are generally not thread safe.
 * Thus, using this in multi-threaded training scenarios is not safe.
 * An alternative (or custom) implementation of DataProvider should be used if thread safety is required
 *
 * @author Alex Black
 */
public class DataSetIteratorProvider implements DataProvider<DataSetIterator> {

    private final DataSetIterator trainData;
    private final DataSetIterator testData;
    private final boolean resetBeforeReturn;

    public DataSetIteratorProvider(DataSetIterator trainData, DataSetIterator testData) {
        this(trainData, testData, true);
    }

    public DataSetIteratorProvider(DataSetIterator trainData, DataSetIterator testData, boolean resetBeforeReturn) {
        this.trainData = trainData;
        this.testData = testData;
        this.resetBeforeReturn = resetBeforeReturn;
    }

    @Override
    public DataSetIterator trainData(Map<String, Object> dataParameters) {
        if (resetBeforeReturn) trainData.reset();    //Same iterator might be used multiple times by different models
        return trainData;
    }

    @Override
    public DataSetIterator testData(Map<String, Object> dataParameters) {
        if (resetBeforeReturn) testData.reset();
        return testData;
    }

    @Override
    public String toString() {
        return "DataSetIteratorProvider(trainData=" + trainData.toString() + ", testData=" + testData.toString() + ", resetBeforeReturn=" + resetBeforeReturn + ")";
    }
}
