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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.datavec.api.transform.metadata.ColumnMetaData;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.transform.Transform;
import org.datavec.api.writable.Writable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Rename one or more columns
 *
 * @author Alex Black
 */
@JsonIgnoreProperties({"inputSchema"})
public class RenameColumnsTransform implements Transform {

    private final List<String> oldNames;
    private final List<String> newNames;
    private Schema inputSchema;

    public RenameColumnsTransform(String oldName, String newName){
        this(Collections.singletonList(oldName), Collections.singletonList(newName));
    }

    public RenameColumnsTransform(List<String> oldNames, List<String> newNames ){
        if(oldNames.size() != newNames.size()) throw new IllegalArgumentException("Invalid input: old/new names lists differ in length");
        this.oldNames = oldNames;
        this.newNames = newNames;
    }

    @Override
    public Schema transform(Schema inputSchema) {
        List<String> inputNames = inputSchema.getColumnNames();
        List<String> outputNames = new ArrayList<>(oldNames.size());

        List<ColumnMetaData> outputMeta = new ArrayList<>();
        for(String s : inputNames){
            int idx = oldNames.indexOf(s);
            if(idx >= 0){
                //Switch the old and new names
                ColumnMetaData meta = inputSchema.getMetaData(s);
                meta.setName(newNames.get(idx));
                outputMeta.add(meta);
            } else {
                outputMeta.add(inputSchema.getMetaData(s));
            }
        }

        return inputSchema.newSchema(outputMeta);
    }

    @Override
    public void setInputSchema(Schema inputSchema) {
        this.inputSchema = inputSchema;
    }

    @Override
    public Schema getInputSchema() {
        return inputSchema;
    }

    @Override
    public List<Writable> map(List<Writable> writables) {
        //No op
        return writables;
    }

    @Override
    public List<List<Writable>> mapSequence(List<List<Writable>> sequence) {
        //No op
        return sequence;
    }

    @Override
    public String toString(){
        return "RenameColumnsTransform(oldNames=" + oldNames + ",newNames=" + newNames + ")";
    }
}
