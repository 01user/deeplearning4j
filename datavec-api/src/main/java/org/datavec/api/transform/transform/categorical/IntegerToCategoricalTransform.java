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

package org.datavec.api.transform.transform.categorical;

import org.datavec.api.transform.metadata.CategoricalMetaData;
import org.datavec.api.writable.Text;
import org.datavec.api.writable.Writable;
import org.datavec.api.transform.metadata.ColumnMetaData;
import org.datavec.api.transform.transform.BaseColumnTransform;

import java.util.*;

/**
 * Convert an integer column to a categorical column, using a provided {@code Map<Integer,String>}
 *
 * @author Alex Black
 */
public class IntegerToCategoricalTransform extends BaseColumnTransform {

    private final Map<Integer, String> map;

    public IntegerToCategoricalTransform(String columnName, Map<Integer, String> map) {
        super(columnName);
        this.map = map;
    }

    public IntegerToCategoricalTransform(String columnName, List<String> list) {
        super(columnName);
        this.map = new LinkedHashMap<>();
        int i = 0;
        for (String s : list) map.put(i++, s);
    }

    @Override
    public ColumnMetaData getNewColumnMetaData(String newColumnName, ColumnMetaData oldColumnType) {
        return new CategoricalMetaData(newColumnName, new ArrayList<>(map.values()));
    }

    @Override
    public Writable map(Writable columnWritable) {
        return new Text(map.get(columnWritable.toInt()));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("IntegerToCategoricalTransform(map=[");
        List<Integer> list = new ArrayList<>(map.keySet());
        Collections.sort(list);
        boolean first = true;
        for (Integer i : list) {
            if (!first) sb.append(",");
            sb.append(i).append("=\"").append(map.get(i)).append("\"");
            first = false;
        }
        sb.append("])");
        return sb.toString();
    }
}
