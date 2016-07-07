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
import org.datavec.api.writable.Writable;

/**
 * Normalize by substracting the mean
 */
public class SubtractMeanNormalizer extends BaseDoubleTransform {

    private final double mean;

    public SubtractMeanNormalizer(String columnName, double mean){
        super(columnName);
        this.mean = mean;
    }

    @Override
    public Writable map(Writable writable) {
        return new DoubleWritable(writable.toDouble()-mean);
    }

    @Override
    public String toString() {
        return "SubstractMean(mean=" + mean + ")";
    }
}
