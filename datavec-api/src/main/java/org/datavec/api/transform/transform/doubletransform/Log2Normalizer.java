/*
 *  * Copyright 2016 Skymind, Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 */

package org.datavec.api.transform.transform.doubletransform;

import org.datavec.api.writable.DoubleWritable;
import org.datavec.api.transform.metadata.ColumnMetaData;
import org.datavec.api.transform.metadata.DoubleMetaData;
import org.datavec.api.writable.Writable;

/**
 * Normalize by taking scale * log2((in-columnMin)/(mean-columnMin) + 1)
 * Maps values in range (columnMin to infinity) to (0 to infinity)
 * Most suitable for values with a geometric/negative exponential type distribution.
 *
 * @author Alex Black
 */
public class Log2Normalizer extends BaseDoubleTransform {

    protected static final double log2 = Math.log(2);
    protected final double columnMean;
    protected final double columnMin;
    protected final double scalingFactor;

    public Log2Normalizer(String columnName, double columnMean, double columnMin, double scalingFactor) {
        super(columnName);
        if (Double.isNaN(columnMean) || Double.isInfinite(columnMean))
            throw new IllegalArgumentException("Invalid input");
        this.columnMean = columnMean;
        this.columnMin = columnMin;
        this.scalingFactor = scalingFactor;
    }

    public Writable map(Writable writable) {
        double val = writable.toDouble();
        if (Double.isNaN(val)) return new DoubleWritable(0);
        return new DoubleWritable(normMean(val));
    }

    private double log2(double x) {
        return Math.log(x) / log2;
    }

    private double normMean(double in) {
        return scalingFactor * log2((in - columnMin) / (columnMean - columnMin) + 1);
    }

    @Override
    public ColumnMetaData getNewColumnMetaData(String newColumnName, ColumnMetaData oldColumnMeta) {
        return new DoubleMetaData(newColumnName, 0.0, null);
    }

    @Override
    public String toString() {
        return "Log2Normalizer(columnMean=" + columnMean + ",columnMin=" + columnMin + ",scalingFactor=" + scalingFactor + ")";
    }

}
