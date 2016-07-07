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

import org.datavec.api.transform.metadata.ColumnMetaData;
import org.datavec.api.transform.metadata.DoubleMetaData;
import org.datavec.api.writable.DoubleWritable;
import org.datavec.api.writable.Writable;

/**
 * Normalizer to map (min to max) -> (newMin-to newMax) linearly. <br>
 * <p>
 * Mathematically: (newMax-newMin)/(max-min) * (x-min) + newMin
 *
 * @author Alex Black
 */
public class MinMaxNormalizer extends BaseDoubleTransform {

    protected final double min;
    protected final double max;
    protected final double newMin;
    protected final double newMax;
    protected final double ratio;

    public MinMaxNormalizer(String columnName, double min, double max) {
        this(columnName, min, max, 0, 1);
    }

    public MinMaxNormalizer(String columnName, double min, double max, double newMin, double newMax) {
        super(columnName);
        this.min = min;
        this.max = max;
        this.newMin = newMin;
        this.newMax = newMax;
        this.ratio = (newMax - newMin) / (max - min);
    }

    @Override
    public Writable map(Writable writable) {
        double val = writable.toDouble();
        if (Double.isNaN(val)) return new DoubleWritable(0);
        return new DoubleWritable(ratio * (val - min) + newMin);
    }

    @Override
    public ColumnMetaData getNewColumnMetaData(ColumnMetaData oldColumnMeta) {
        return new DoubleMetaData(newMin, newMax);
    }

    @Override
    public String toString() {
        return "MinMaxNormalizer(min=" + min + ",max=" + max + ",newMin=" + newMin + ",newMax=" + newMax + ")";
    }

}
