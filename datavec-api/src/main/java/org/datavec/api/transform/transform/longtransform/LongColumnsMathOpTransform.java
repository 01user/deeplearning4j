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

package org.datavec.api.transform.transform.longtransform;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.datavec.api.writable.LongWritable;
import org.datavec.api.transform.transform.doubletransform.DoubleColumnsMathOpTransform;
import org.datavec.api.transform.MathOp;
import org.datavec.api.transform.metadata.LongMetaData;
import org.datavec.api.transform.transform.BaseColumnsMathOpTransform;
import org.datavec.api.writable.Writable;
import org.datavec.api.transform.metadata.ColumnMetaData;

/**
 * Add a new long column, calculated from one or more other columns. A new column (with the specified name) is added
 * as the final column of the output. No other columns are modified.<br>
 * For example, if newColumnName=="newCol", mathOp==MathOp.Add, and columns=={"col1","col2"}, then the output column
 * with name "newCol" has value col1+col2.<br>
 * <b>NOTE</b>: Division here is using long division (long output). Use {@link DoubleColumnsMathOpTransform}
 * if a decimal output value is required.
 *
 * @author Alex Black
 * @see LongMathOpTransform To do an in-place mathematical operation of a long column and a long scalar value
 */
public class LongColumnsMathOpTransform extends BaseColumnsMathOpTransform {

    public LongColumnsMathOpTransform(@JsonProperty("newColumnName") String newColumnName, @JsonProperty("mathOp") MathOp mathOp,
                                      @JsonProperty("columns") String... columns) {
        super(newColumnName, mathOp, columns);
    }

    @Override
    protected ColumnMetaData derivedColumnMetaData(String newColumnName) {
        return new LongMetaData(newColumnName);
    }

    @Override
    protected Writable doOp(Writable... input) {
        switch (mathOp) {
            case Add:
                long sum = 0;
                for (Writable w : input) sum += w.toLong();
                return new LongWritable(sum);
            case Subtract:
                return new LongWritable(input[0].toLong() - input[1].toLong());
            case Multiply:
                long product = 1;
                for (Writable w : input) product *= w.toLong();
                return new LongWritable(product);
            case Divide:
                return new LongWritable(input[0].toLong() / input[1].toLong());
            case Modulus:
                return new LongWritable(input[0].toLong() % input[1].toLong());
            case ReverseSubtract:
            case ReverseDivide:
            case ScalarMin:
            case ScalarMax:
            default:
                throw new RuntimeException("Invalid mathOp: " + mathOp);    //Should never happen
        }
    }
}
