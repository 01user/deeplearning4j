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

package org.datavec.api.transform.transform.real;

import org.datavec.api.io.data.DoubleWritable;
import org.datavec.api.writable.Writable;

/**
 * Normalize using (x-mean)/sigma.
 * Also known as a standard score, standardization etc.
 *
 * @author Alex Black
 */
public class StandardizeNormalizer extends BaseDoubleTransform {

    protected final double mean;
    protected final double sigma;

    public StandardizeNormalizer(String columnName, double mean, double sigma) {
        super(columnName);
        this.mean = mean;
        this.sigma = sigma;
    }


    @Override
    public Writable map(Writable writable) {
        double val = writable.toDouble();
        return new DoubleWritable((val - mean) / sigma);
    }

    @Override
    public String toString() {
        return "StandardizeNormalizer(mean=" + mean + ",sigma=" + sigma + ")";
    }
}
