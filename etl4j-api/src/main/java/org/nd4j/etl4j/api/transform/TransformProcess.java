package org.nd4j.etl4j.api.transform;

import org.nd4j.etl4j.api.transform.analysis.columns.ColumnAnalysis;
import org.nd4j.etl4j.api.transform.analysis.columns.NumericalColumnAnalysis;
import org.nd4j.etl4j.api.transform.rank.CalculateSortedRank;
import org.nd4j.etl4j.api.transform.sequence.ConvertFromSequence;
import org.nd4j.etl4j.api.transform.sequence.ConvertToSequence;
import org.nd4j.etl4j.api.transform.sequence.SequenceComparator;
import org.nd4j.etl4j.api.transform.sequence.SequenceSplit;
import org.nd4j.etl4j.api.transform.sequence.window.ReduceSequenceByWindowTransform;
import org.nd4j.etl4j.api.transform.sequence.window.WindowFunction;
import org.nd4j.etl4j.api.transform.transform.categorical.CategoricalToOneHotTransform;
import org.nd4j.etl4j.api.transform.transform.column.ReorderColumnsTransform;
import org.nd4j.etl4j.api.transform.transform.integer.IntegerColumnsMathOpTransform;
import org.nd4j.etl4j.api.transform.transform.longtransform.LongColumnsMathOpTransform;
import io.skymind.echidna.api.transform.real.*;
import lombok.Data;
import org.canova.api.writable.Writable;
import org.nd4j.etl4j.api.transform.analysis.DataAnalysis;
import org.nd4j.etl4j.api.transform.filter.Filter;
import org.nd4j.etl4j.api.transform.reduce.IReducer;
import org.nd4j.etl4j.api.transform.schema.Schema;
import org.nd4j.etl4j.api.transform.schema.SequenceSchema;
import org.nd4j.etl4j.api.transform.transform.column.DuplicateColumnsTransform;
import org.nd4j.etl4j.api.transform.transform.column.RemoveColumnsTransform;
import org.nd4j.etl4j.api.transform.transform.column.RenameColumnsTransform;
import org.nd4j.etl4j.api.transform.transform.integer.IntegerMathOpTransform;
import org.nd4j.etl4j.api.transform.transform.longtransform.LongMathOpTransform;
import org.nd4j.etl4j.api.transform.transform.normalize.Normalize;
import org.nd4j.etl4j.api.transform.transform.time.TimeMathOpTransform;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * A TransformProcess defines an ordered list of transformations to be executed on some data
 *
 * @author Alex Black
 */
@Data
public class TransformProcess implements Serializable {

    private final Schema initialSchema;
    private List<DataAction> actionList;

    private TransformProcess(Builder builder) {
        actionList = builder.actionList;
        initialSchema = builder.initialSchema;

        //Calculate and set the schemas for each tranformation:
        Schema currInputSchema = builder.initialSchema;
        for (DataAction d : actionList) {
            if (d.getTransform() != null) {
                Transform t = d.getTransform();
                t.setInputSchema(currInputSchema);
                currInputSchema = t.transform(currInputSchema);
            } else if (d.getFilter() != null) {
                //Filter -> doesn't change schema. But we DO need to set the schema in the filter...
                d.getFilter().setInputSchema(currInputSchema);
            } else if (d.getConvertToSequence() != null) {
                if (currInputSchema instanceof SequenceSchema) {
                    throw new RuntimeException("Cannot convert to sequence: schema is already a sequence schema: " + currInputSchema);
                }
                ConvertToSequence cts = d.getConvertToSequence();
                cts.setInputSchema(currInputSchema);
                currInputSchema = cts.transform(currInputSchema);
            } else if (d.getConvertFromSequence() != null) {
                ConvertFromSequence cfs = d.getConvertFromSequence();
                if (!(currInputSchema instanceof SequenceSchema)) {
                    throw new RuntimeException("Cannot convert from sequence: schema is not a sequence schema: " + currInputSchema);
                }
                cfs.setInputSchema((SequenceSchema) currInputSchema);
                currInputSchema = cfs.transform((SequenceSchema) currInputSchema);
            } else if (d.getSequenceSplit() != null) {
                d.getSequenceSplit().setInputSchema(currInputSchema);
                continue;   //no change to sequence schema
            } else if (d.getReducer() != null) {
                IReducer reducer = d.getReducer();
                reducer.setInputSchema(currInputSchema);
                currInputSchema = reducer.transform(currInputSchema);
            } else if(d.getCalculateSortedRank() != null){
                CalculateSortedRank csr = d.getCalculateSortedRank();
                csr.setInputSchema(currInputSchema);
                currInputSchema = csr.transform(currInputSchema);
            } else {
                throw new RuntimeException("Unknown action: " + d);
            }
        }
    }

    public List<DataAction> getActionList() {
        return actionList;
    }

    /**
     * Get the Schema of the output data, after executing the process
     *
     * @return Schema of the output data
     */
    public Schema getFinalSchema() {
        return getSchemaAfterStep(actionList.size());
    }

    /**
     * Return the schema after executing all steps up to and including the specified step.
     * Steps are indexed from 0: so getSchemaAfterStep(0) is after one transform has been executed.
     *
     * @param step Index of the step
     * @return Schema of the data, after that (and all prior) steps have been executed
     */
    public Schema getSchemaAfterStep(int step) {
        Schema currInputSchema = initialSchema;
        int i = 0;
        for (DataAction d : actionList) {
            if (d.getTransform() != null) {
                Transform t = d.getTransform();
                currInputSchema = t.transform(currInputSchema);
            } else if (d.getFilter() != null) {
                continue; //Filter -> doesn't change schema
            } else if (d.getConvertToSequence() != null) {
                if (currInputSchema instanceof SequenceSchema) {
                    throw new RuntimeException("Cannot convert to sequence: schema is already a sequence schema: " + currInputSchema);
                }
                ConvertToSequence cts = d.getConvertToSequence();
                currInputSchema = cts.transform(currInputSchema);
            } else if (d.getConvertFromSequence() != null) {
                ConvertFromSequence cfs = d.getConvertFromSequence();
                if (!(currInputSchema instanceof SequenceSchema)) {
                    throw new RuntimeException("Cannot convert from sequence: schema is not a sequence schema: " + currInputSchema);
                }
                currInputSchema = cfs.transform((SequenceSchema) currInputSchema);
            } else if (d.getSequenceSplit() != null) {
                continue;   //Sequence split -> no change to schema
            } else if (d.getReducer() != null) {
                IReducer reducer = d.getReducer();
                currInputSchema = reducer.transform(currInputSchema);
            } else if( d.getCalculateSortedRank() != null){
                CalculateSortedRank csr = d.getCalculateSortedRank();
                currInputSchema = csr.transform(currInputSchema);
            } else {
                throw new RuntimeException("Unknown action: " + d);
            }
            if (i++ == step) return currInputSchema;
        }
        return currInputSchema;
    }


    /**
     * Execute the full sequence of transformations for a single example. May return null if example is filtered
     * <b>NOTE:</b> Some TransformProcess operations cannot be done on examples individually. Most notably, ConvertToSequence
     * and ConvertFromSequence operations require the full data set to be processed at once
     *
     * @param input
     * @return
     */
    public List<Writable> execute(List<Writable> input) {
        List<Writable> currValues = input;

        for (DataAction d : actionList) {
            if (d.getTransform() != null) {
                Transform t = d.getTransform();
                currValues = t.map(currValues);

            } else if (d.getFilter() != null) {
                Filter f = d.getFilter();
                if (f.removeExample(currValues)) return null;
            } else if (d.getConvertToSequence() != null) {
                throw new RuntimeException("Cannot execute examples individually: TransformProcess contains a ConvertToSequence operation");
            } else if (d.getConvertFromSequence() != null) {
                throw new RuntimeException("Unexpected operation: TransformProcess contains a ConvertFromSequence operation");
            } else if (d.getSequenceSplit() != null) {
                throw new RuntimeException("Cannot execute examples individually: TransformProcess contains a SequenceSplit operation");
            } else {
                throw new RuntimeException("Unknown action: " + d);
            }
        }

        return currValues;
    }

    public List<List<Writable>> executeSequenceToSequence(List<List<Writable>> input) {
        List<List<Writable>> currValues = input;

        for (DataAction d : actionList) {
            if (d.getTransform() != null) {
                Transform t = d.getTransform();
                currValues = t.mapSequence(currValues);

            } else if (d.getFilter() != null) {
//                Filter f = d.getFilter();
//                if (f.removeExample(currValues)) return null;
                throw new RuntimeException("Sequence filtering not yet implemnted here");
            } else if (d.getConvertToSequence() != null) {
                throw new RuntimeException("Cannot execute examples individually: TransformProcess contains a ConvertToSequence operation");
            } else if (d.getConvertFromSequence() != null) {
                throw new RuntimeException("Unexpected operation: TransformProcess contains a ConvertFromSequence operation");
            } else if (d.getSequenceSplit() != null) {
                throw new RuntimeException("Cannot execute examples individually: TransformProcess contains a SequenceSplit operation");
            } else {
                throw new RuntimeException("Unknown action: " + d);
            }
        }

        return currValues;
    }

    /**
     * Execute the full sequence of transformations for a single time series (sequence). May return null if example is filtered
     */
    public List<List<Writable>> executeSequence(List<List<Writable>> inputSequence) {


        throw new UnsupportedOperationException("Not yet implemented");
    }


    /**
     * Builder class for constructing a TransformProcess
     */
    public static class Builder {

        private List<DataAction> actionList = new ArrayList<>();
        private Schema initialSchema;

        public Builder(Schema initialSchema) {
            this.initialSchema = initialSchema;
        }

        /**
         * Add a transformation to be executed after the previously-added operations have been executed
         *
         * @param transform Transform to execute
         */
        public Builder transform(Transform transform) {
            actionList.add(new DataAction(transform));
            return this;
        }

        /**
         * Add a filter operation to be executed after the previously-added operations have been executed
         *
         * @param filter Filter operation to execute
         */
        public Builder filter(Filter filter) {
            actionList.add(new DataAction(filter));
            return this;
        }

        /**
         * Remove all of the specified columns, by name
         *
         * @param columnNames Names of the columns to remove
         */
        public Builder removeColumns(String... columnNames) {
            return transform(new RemoveColumnsTransform(columnNames));
        }

        /**
         * Remove all of the specified columns, by name
         *
         * @param columnNames Names of the columns to remove
         */
        public Builder removeColumns(Collection<String> columnNames) {
            return transform(new RemoveColumnsTransform(columnNames.toArray(new String[columnNames.size()])));
        }

        /**
         * Rename a single column
         *
         * @param oldName Original column name
         * @param newName New column name
         */
        public Builder renameColumn(String oldName, String newName) {
            return transform(new RenameColumnsTransform(oldName, newName));
        }

        /**
         * Rename multiple columns
         *
         * @param oldNames List of original column names
         * @param newNames List of new column names
         */
        public Builder renameColumns(List<String> oldNames, List<String> newNames) {
            return transform(new RenameColumnsTransform(oldNames, newNames));
        }

        /**
         * Reorder the columns using a partial or complete new ordering.
         * If only some of the column names are specified for the new order, the remaining columns will be placed at
         * the end, according to their current relative ordering
         * @param newOrder    Names of the columns, in the order they will appear in the output
         */
        public Builder reorderColumns(String... newOrder){
            return transform(new ReorderColumnsTransform(newOrder));
        }

        /**
         * Duplicate a single column
         *
         * @param columnName Name of the column to duplicate
         * @param newName    Name of the new (duplicate) column
         */
        public Builder duplicateColumn(String columnName, String newName) {
            return transform(new DuplicateColumnsTransform(Collections.singletonList(columnName), Collections.singletonList(newName)));
        }


        /**
         * Duplicate a set of columns
         *
         * @param columnNames Names of the columns to duplicate
         * @param newNames    Names of the new (duplicated) columns
         */
        public Builder duplicateColumns(List<String> columnNames, List<String> newNames) {
            return transform(new DuplicateColumnsTransform(columnNames, newNames));
        }

        /**
         * Perform a mathematical operation (add, subtract, scalar max etc) on the specified integer column, with a scalar
         *
         * @param columnName The integer column to perform the operation on
         * @param mathOp     The mathematical operation
         * @param scalar     The scalar value to use in the mathematical operation
         */
        public Builder integerMathOp(String columnName, MathOp mathOp, int scalar) {
            return transform(new IntegerMathOpTransform(columnName, mathOp, scalar));
        }

        /**
         * Calculate and add a new integer column by performing a mathematical operation on a number of existing columns.
         * New column is added to the end.
         *
         * @param newColumnName    Name of the new/derived column
         * @param mathOp           Mathematical operation to execute on the columns
         * @param columnNames      Names of the columns to use in the mathematical operation
         */
        public Builder integerColumnsMathOp(String newColumnName, MathOp mathOp, String... columnNames){
            return transform(new IntegerColumnsMathOpTransform(newColumnName,mathOp,columnNames));
        }

        /**
         * Perform a mathematical operation (add, subtract, scalar max etc) on the specified long column, with a scalar
         *
         * @param columnName The long column to perform the operation on
         * @param mathOp     The mathematical operation
         * @param scalar     The scalar value to use in the mathematical operation
         */
        public Builder longMathOp(String columnName, MathOp mathOp, long scalar) {
            return transform(new LongMathOpTransform(columnName, mathOp, scalar));
        }

        /**
         * Calculate and add a new long column by performing a mathematical operation on a number of existing columns.
         * New column is added to the end.
         *
         * @param newColumnName    Name of the new/derived column
         * @param mathOp           Mathematical operation to execute on the columns
         * @param columnNames      Names of the columns to use in the mathematical operation
         */
        public Builder longColumnsMathOp(String newColumnName, MathOp mathOp, String... columnNames){
            return transform(new LongColumnsMathOpTransform(newColumnName,mathOp,columnNames));
        }

        /**
         * Perform a mathematical operation (add, subtract, scalar max etc) on the specified double column, with a scalar
         *
         * @param columnName The double column to perform the operation on
         * @param mathOp     The mathematical operation
         * @param scalar     The scalar value to use in the mathematical operation
         */
        public Builder doubleMathOp(String columnName, MathOp mathOp, double scalar) {
            return transform(new DoubleMathOpTransform(columnName, mathOp, scalar));
        }

        /**
         * Calculate and add a new double column by performing a mathematical operation on a number of existing columns.
         * New column is added to the end.
         *
         * @param newColumnName    Name of the new/derived column
         * @param mathOp           Mathematical operation to execute on the columns
         * @param columnNames      Names of the columns to use in the mathematical operation
         */
        public Builder doubleColumnsMathOp(String newColumnName, MathOp mathOp, String... columnNames){
            return transform(new DoubleColumnsMathOpTransform(newColumnName,mathOp,columnNames));
        }

        /**
         * Perform a mathematical operation (add, subtract, scalar min/max only) on the specified time column
         *
         * @param columnName   The integer column to perform the operation on
         * @param mathOp       The mathematical operation
         * @param timeQuantity The quantity used in the mathematical op
         * @param timeUnit     The unit that timeQuantity is specified in
         */
        public Builder timeMathOp(String columnName, MathOp mathOp, long timeQuantity, TimeUnit timeUnit) {
            return transform(new TimeMathOpTransform(columnName, mathOp, timeQuantity, timeUnit));
        }


        /**
         * Convert the specified columns from a categorical representation to a one-hot representation.
         * This involves the creation of multiple new columns each.
         *
         * @param columnNames Names of the categorical columns to convert to a one-hot representation
         */
        public Builder categoricalToOneHot(String... columnNames) {
            for (String s : columnNames) {
                transform(new CategoricalToOneHotTransform(s));
            }
            return this;
        }

        /**
         * Normalize the specified column with a given type of normalization
         *
         * @param column Column to normalize
         * @param type   Type of normalization to apply
         * @param da     DataAnalysis object
         */
        public Builder normalize(String column, Normalize type, DataAnalysis da) {

            ColumnAnalysis ca = da.getColumnAnalysis(column);
            if (!(ca instanceof NumericalColumnAnalysis))
                throw new IllegalStateException("Column \"" + column + "\" analysis is not numerical. "
                        + "Column is not numerical?");

            NumericalColumnAnalysis nca = (NumericalColumnAnalysis) ca;
            double min = nca.getMinDouble();
            double max = nca.getMaxDouble();
            double mean = nca.getMean();
            double sigma = nca.getSampleStdev();

            switch (type) {
                case MinMax:
                    return transform(new MinMaxNormalizer(column, min, max));
                case MinMax2:
                    return transform(new MinMaxNormalizer(column, min, max, -1, 1));
                case Standardize:
                    return transform(new StandardizeNormalizer(column, mean, sigma));
                case SubtractMean:
                    return transform(new SubtractMeanNormalizer(column, mean));
                case Log2Mean:
                    return transform(new Log2Normalizer(column, mean, min, 0.5));
                case Log2MeanExcludingMin:
                    long countMin = nca.getCountMinValue();

                    //mean including min value: (sum/totalCount)
                    //mean excluding min value: (sum - countMin*min)/(totalCount - countMin)
                    double meanExMin = (mean * ca.getCountTotal() - countMin * min) / (ca.getCountTotal() - countMin);
                    return transform(new Log2Normalizer(column, meanExMin, min, 0.5));
                default:
                    throw new RuntimeException("Unknown/not implemented normalization type: " + type);
            }
        }

        /**
         * Convert a set of independent records/examples into a sequence, according to some key.
         * Within each sequence, values are ordered using the provided {@link SequenceComparator}
         *
         * @param keyColumn  Column to use as a key (values with the same key will be combined into sequences)
         * @param comparator A SequenceComparator to order the values within each sequence (for example, by time or String order)
         */
        public Builder convertToSequence(String keyColumn, SequenceComparator comparator) {
            actionList.add(new DataAction(new ConvertToSequence(keyColumn, comparator)));
            return this;
        }


        /**
         * Convert a sequence to a set of individual values (by treating each value in each sequence as a separate example)
         */
        public Builder convertFromSequence() {
            actionList.add(new DataAction(new ConvertFromSequence()));
            return this;
        }

        /**
         * Split sequences into 1 or more other sequences. Used for example to split large sequences into a set of smaller sequences
         *
         * @param split SequenceSplit that defines how splits will occur
         */
        public Builder splitSequence(SequenceSplit split) {
            actionList.add(new DataAction(split));
            return this;
        }

        /**
         * Reduce (i.e., aggregate/combine) a set of examples (typically by key).
         * <b>Note</b>: In the current implementation, reduction operations can be performed only on standard (i.e., non-sequence) data
         *
         * @param reducer Reducer to use
         */
        public Builder reduce(IReducer reducer) {
            actionList.add(new DataAction(reducer));
            return this;
        }

        /**
         * Reduce (i.e., aggregate/combine) a set of sequence examples - for each sequence individually - using a window function.
         * For example, take all records/examples in each 24-hour period (i.e., using window function), and convert them into
         * a singe value (using the reducer). In this example, the output is a sequence, with time period of 24 hours.
         * @param reducer           Reducer to use to reduce each window
         * @param windowFunction    Window function to find apply on each sequence individually
         */
        public Builder reduceSequenceByWindow(IReducer reducer, WindowFunction windowFunction){
            actionList.add(new DataAction(new ReduceSequenceByWindowTransform(reducer, windowFunction)));
            return this;
        }

        /**
         *  CalculateSortedRank: calculate the rank of each example, after sorting example.
         * For example, we might have some numerical "score" column, and we want to know for the rank (sort order) for each
         * example, according to that column.<br>
         * The rank of each example (after sorting) will be added in a new Long column. Indexing is done from 0; examples will have
         * values 0 to dataSetSize-1.<br>
         *
         * Currently, CalculateSortedRank can only be applied on standard (i.e., non-sequence) data
         * Furthermore, the current implementation can only sort on one column
         *
         * @param newColumnName    Name of the new column (will contain the rank for each example)
         * @param sortOnColumn     Column to sort on
         * @param comparator       Comparator used to sort examples
         */
        public Builder calculateSortedRank(String newColumnName, String sortOnColumn, Comparator<Writable> comparator){
            actionList.add(new DataAction(new CalculateSortedRank(newColumnName, sortOnColumn, comparator)));
            return this;
        }

        /**
         *  CalculateSortedRank: calculate the rank of each example, after sorting example.
         * For example, we might have some numerical "score" column, and we want to know for the rank (sort order) for each
         * example, according to that column.<br>
         * The rank of each example (after sorting) will be added in a new Long column. Indexing is done from 0; examples will have
         * values 0 to dataSetSize-1.<br>
         *
         * Currently, CalculateSortedRank can only be applied on standard (i.e., non-sequence) data
         * Furthermore, the current implementation can only sort on one column
         *
         * @param newColumnName    Name of the new column (will contain the rank for each example)
         * @param sortOnColumn     Column to sort on
         * @param comparator       Comparator used to sort examples
         * @param ascending        If true: sort ascending. False: descending
         *
         */
        public Builder calculateSortedRank(String newColumnName, String sortOnColumn, Comparator<Writable> comparator, boolean ascending){
            actionList.add(new DataAction(new CalculateSortedRank(newColumnName, sortOnColumn, comparator, ascending)));
            return this;
        }

        /**
         * Create the TransformProcess object
         */
        public TransformProcess build() {
            return new TransformProcess(this);
        }
    }


}
