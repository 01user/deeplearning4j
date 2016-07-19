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

package org.datavec.api.transform.condition.column;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.datavec.api.transform.condition.SequenceConditionMode;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.transform.condition.Condition;
import org.datavec.api.writable.Writable;

import java.util.List;

/**
 * Abstract class for column conditions
 *
 * @author Alex Black
 */
@JsonIgnoreProperties({"columnIdx","schema","sequenceMode"})
public abstract class BaseColumnCondition implements Condition {

    public static final SequenceConditionMode DEFAULT_SEQUENCE_CONDITION_MODE = SequenceConditionMode.Or;

    protected final String column;
    protected int columnIdx = -1;
    protected Schema schema;
    protected SequenceConditionMode sequenceMode;

    protected BaseColumnCondition(String column, SequenceConditionMode sequenceConditionMode) {
        this.column = column;
        this.sequenceMode = sequenceConditionMode;
    }

    @Override
    public void setInputSchema(Schema schema) {
        columnIdx = schema.getColumnNames().indexOf(column);
        if (columnIdx < 0) {
            throw new IllegalStateException("Invalid state: column \"" + column + "\" not present in input schema");
        }
        this.schema = schema;
    }

    @Override
    public Schema getInputSchema(){
        return schema;
    }

    @Override
    public boolean condition(List<Writable> list) {
        return columnCondition(list.get(columnIdx));
    }

    @Override
    public boolean conditionSequence(List<List<Writable>> list) {
        switch (sequenceMode) {
            case And:
                for (List<Writable> l : list) {
                    if (!condition(l)) return false;
                }
                return true;
            case Or:
                for (List<Writable> l : list) {
                    if (condition(l)) return true;
                }
                return false;
            case NoSequenceMode:
                throw new IllegalStateException("Column condition " + toString() + " does not support sequence execution");
            default:
                throw new RuntimeException("Unknown/not implemented sequence mode: " + sequenceMode);
        }
    }

    public abstract boolean columnCondition(Writable writable);

    @Override
    public abstract String toString();
}
