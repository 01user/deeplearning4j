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

package org.datavec.api.transform.sequence.window;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import org.datavec.api.transform.metadata.ColumnMetaData;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.writable.Writable;
import org.datavec.api.transform.Transform;
import org.datavec.api.transform.reduce.IReducer;
import org.datavec.api.transform.schema.SequenceSchema;

import java.util.ArrayList;
import java.util.List;

/**
 * Idea: do two things.
 * First, apply a window function to the sequence data.
 * Second: Reduce that window of data into a single value by using a Reduce function
 *
 * @author Alex Black
 */
@JsonIgnoreProperties({"inputSchema"})
@EqualsAndHashCode(exclude = {"inputSchema"})
public class ReduceSequenceByWindowTransform implements Transform {

    private IReducer reducer;
    private WindowFunction windowFunction;
    private Schema inputSchema;

    public ReduceSequenceByWindowTransform(@JsonProperty("reducer") IReducer reducer, @JsonProperty("windowFunction") WindowFunction windowFunction){
        this.reducer = reducer;
        this.windowFunction = windowFunction;
    }


    @Override
    public Schema transform(Schema inputSchema) {
        if(inputSchema != null && !(inputSchema instanceof SequenceSchema)){
            throw new IllegalArgumentException("Invalid input: input schema must be a SequenceSchema");
        }

        //Some window functions may make changes to the schema (adding window start/end times, for example)
        inputSchema = windowFunction.transform(inputSchema);

        //Approach here: The reducer gives us a schema for one time step -> simply convert this to a sequence schema...
        Schema oneStepSchema = reducer.transform(inputSchema);
        List<ColumnMetaData> meta = oneStepSchema.getColumnMetaData();

        return new SequenceSchema(meta);
    }

    @Override
    public void setInputSchema(Schema inputSchema) {
        this.inputSchema = inputSchema;
        this.windowFunction.setInputSchema(inputSchema);
        reducer.setInputSchema(windowFunction.transform(inputSchema));
    }

    @Override
    public Schema getInputSchema() {
        return inputSchema;
    }

    @Override
    public List<Writable> map(List<Writable> writables) {
        throw new UnsupportedOperationException("ReduceSequenceByWindownTransform can only be applied on sequences");
    }

    @Override
    public List<List<Writable>> mapSequence(List<List<Writable>> sequence) {

        //List of windows, which are all small sequences...
        List<List<List<Writable>>> sequenceAsWindows = windowFunction.applyToSequence(sequence);

        List<List<Writable>> out = new ArrayList<>();

        for(List<List<Writable>> window : sequenceAsWindows ){
            List<Writable> reduced = reducer.reduce(window);
            out.add(reduced);
        }

        return out;
    }

    @Override
    public String toString(){
        return "ReduceSequencbyWindowTransform(reducer=" + reducer + ",windowFunction=" + windowFunction + ")";
    }
}
