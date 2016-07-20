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

package org.datavec.api.transform.condition;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.writable.Writable;

import java.util.List;

/**
 * BooleanCondition: used for creating compound conditions, such as AND(ConditionA, ConditionB, ...)<br>
 * As a BooleanCondition is a condition, these can be chained together, like NOT(OR(AND(...),AND(...)))
 *
 * @author Alex Black
 */
@EqualsAndHashCode
public class BooleanCondition implements Condition {

    public enum Type {AND, OR, NOT, XOR}

    private final Type type;
    private final Condition[] conditions;

    public BooleanCondition(@JsonProperty("type") Type type, @JsonProperty("conditions") Condition... conditions) {
        if (conditions == null || conditions.length < 1)
            throw new IllegalArgumentException("Invalid input: conditions must be non-null and have at least 1 element");
        switch (type) {
            case NOT:
                if (conditions.length != 1)
                    throw new IllegalArgumentException("Invalid input: NOT conditions must have exactly 1 element");
                break;
            case XOR:
                if (conditions.length != 2)
                    throw new IllegalArgumentException("Invalid input: XOR conditions must have exactly 2 elements");
                break;
        }
        this.type = type;
        this.conditions = conditions;
    }

    @Override
    public boolean condition(List<Writable> list) {
        switch (type) {
            case AND:
                for (Condition c : conditions) {
                    boolean thisCond = c.condition(list);
                    if (!thisCond) return false; //Any false -> AND is false
                }
                return true;
            case OR:
                for (Condition c : conditions) {
                    boolean thisCond = c.condition(list);
                    if (thisCond) return true;   //Any true -> OR is true
                }
                return false;
            case NOT:
                return !conditions[0].condition(list);
            case XOR:
                return conditions[0].condition(list) ^ conditions[1].condition(list);
            default:
                throw new RuntimeException("Unknown condition type: " + type);
        }
    }

    @Override
    public boolean conditionSequence(List<List<Writable>> sequence) {
        switch (type) {
            case AND:
                for (Condition c : conditions) {
                    boolean thisCond = c.conditionSequence(sequence);
                    if (!thisCond) return false; //Any false -> AND is false
                }
                return true;
            case OR:
                for (Condition c : conditions) {
                    boolean thisCond = c.conditionSequence(sequence);
                    if (thisCond) return true;   //Any true -> OR is true
                }
                return false;
            case NOT:
                return !conditions[0].conditionSequence(sequence);
            case XOR:
                return conditions[0].conditionSequence(sequence) ^ conditions[1].conditionSequence(sequence);
            default:
                throw new RuntimeException("Unknown condition type: " + type);
        }
    }

    @Override
    public void setInputSchema(Schema schema) {
        for (Condition c : conditions) {
            c.setInputSchema(schema);
        }
    }

    @Override
    public Schema getInputSchema() {
        return conditions[0].getInputSchema();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BooleanCondition(").append(type);
        for (Condition c : conditions) {
            sb.append(",").append(c.toString());
        }
        sb.append(")");
        return sb.toString();
    }


    public static Condition AND(Condition... conditions) {
        return new BooleanCondition(Type.AND, conditions);
    }

    public static Condition OR(Condition... conditions) {
        return new BooleanCondition(Type.OR, conditions);
    }

    public static Condition NOT(Condition condition) {
        return new BooleanCondition(Type.NOT, condition);
    }

    public static Condition XOR(Condition first, Condition second) {
        return new BooleanCondition(Type.XOR, first, second);
    }


}
