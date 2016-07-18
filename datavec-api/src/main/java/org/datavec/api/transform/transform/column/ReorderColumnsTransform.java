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

package org.datavec.api.transform.transform.column;

import org.datavec.api.transform.schema.Schema;
import org.datavec.api.writable.Writable;
import org.datavec.api.transform.Transform;
import org.datavec.api.transform.metadata.ColumnMetaData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Rearrange the order of the columns.
 * Note: A partial list of columns can be used here. Any columns that are not explicitly mentioned
 * will be placed after those that are in the output, without changing their relative order.
 *
 * @author Alex Black
 */
public class ReorderColumnsTransform implements Transform {

    private final List<String> newOrder;
    private Schema inputSchema;
    private int[] outputOrder;  //Mapping from in to out. so output[i] = input.get(outputOrder[i])

    /**
     *
     * @param newOrder    A partial or complete order of the columns in the output
     */
    public ReorderColumnsTransform(String... newOrder){
        this(Arrays.asList(newOrder));
    }

    /**
     *
     * @param newOrder    A partial or complete order of the columns in the output
     */
    public ReorderColumnsTransform(List<String> newOrder){
        this.newOrder = newOrder;
    }

    @Override
    public Schema transform(Schema inputSchema) {
        for(String s : newOrder){
            if(!inputSchema.hasColumn(s)){
                throw new IllegalStateException("Input schema does not contain column with name \"" + s + "\"");
            }
        }
        if(inputSchema.numColumns() < newOrder.size()) throw new IllegalArgumentException("Schema has " + inputSchema.numColumns() +
            " column but newOrder has " + newOrder.size() + " columns");

        List<String> origNames = inputSchema.getColumnNames();
        List<ColumnMetaData> origMeta = inputSchema.getColumnMetaData();
        List<ColumnMetaData> outMeta = new ArrayList<>();

        boolean[] taken = new boolean[origNames.size()];
        for(String s : newOrder){
            int idx = inputSchema.getIndexOfColumn(s);
            outMeta.add(origMeta.get(idx));
            taken[idx] = true;
        }

        for( int i=0; i<taken.length; i++ ){
            if(taken[i]) continue;
            outMeta.add(origMeta.get(i));
        }

        return inputSchema.newSchema(outMeta);
    }

    @Override
    public void setInputSchema(Schema inputSchema) {
        for(String s : newOrder){
            if(!inputSchema.hasColumn(s)){
                throw new IllegalStateException("Input schema does not contain column with name \"" + s + "\"");
            }
        }
        if(inputSchema.numColumns() < newOrder.size()) throw new IllegalArgumentException("Schema has " + inputSchema.numColumns() +
                " columns but newOrder has " + newOrder.size() + " columns");

        List<String> origNames = inputSchema.getColumnNames();
        outputOrder = new int[origNames.size()];

        boolean[] taken = new boolean[origNames.size()];
        int j=0;
        for(String s : newOrder){
            int idx = inputSchema.getIndexOfColumn(s);
            taken[idx] = true;
            outputOrder[j++] = idx;
        }

        for( int i=0; i<taken.length; i++ ){
            if(taken[i]) continue;
            outputOrder[j++] = i;
        }
    }

    @Override
    public Schema getInputSchema() {
        return inputSchema;
    }

    @Override
    public List<Writable> map(List<Writable> writables) {
        List<Writable> out = new ArrayList<>();
        for(int i : outputOrder){
            out.add(writables.get(i));
        }
        return out;
    }

    @Override
    public List<List<Writable>> mapSequence(List<List<Writable>> sequence) {
        List<List<Writable>> out = new ArrayList<>();
        for(List<Writable> step : sequence){
            out.add(map(step));
        }
        return out;
    }
}
