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

package org.datavec.api.transform.schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.datavec.api.transform.ColumnType;
import org.datavec.api.transform.metadata.ColumnMetaData;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Created by Alex on 11/03/2016.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SequenceSchema extends Schema {
    private final Integer minSequenceLength;
    private final Integer maxSequenceLength;

    public SequenceSchema(List<ColumnMetaData> columnMetaData) {
        this(columnMetaData, null, null);
    }

    public SequenceSchema(@JsonProperty("columns") List<ColumnMetaData> columnMetaData, @JsonProperty("minSequenceLength") Integer minSequenceLength,
                          @JsonProperty("maxSequenceLength") Integer maxSequenceLength) {
        super(columnMetaData);
        this.minSequenceLength = minSequenceLength;
        this.maxSequenceLength = maxSequenceLength;
    }

    private SequenceSchema(Builder builder) {
        super(builder);
        this.minSequenceLength = builder.minSequenceLength;
        this.maxSequenceLength = builder.maxSequenceLength;
    }

    @Override
    public SequenceSchema newSchema(List<ColumnMetaData> columnMetaData) {
        return new SequenceSchema(columnMetaData, minSequenceLength, maxSequenceLength);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int nCol = numColumns();

        int maxNameLength = 0;
        for (String s : getColumnNames()) {
            maxNameLength = Math.max(maxNameLength, s.length());
        }

        //Header:
        sb.append("SequenceSchema(");

        if (minSequenceLength != null) sb.append("minSequenceLength=").append(minSequenceLength);
        if (maxSequenceLength != null) {
            if (minSequenceLength != null) sb.append(",");
            sb.append("maxSequenceLength=").append(maxSequenceLength);
        }

        sb.append(")\n");
        sb.append(String.format("%-6s", "idx")).append(String.format("%-" + (maxNameLength + 8) + "s", "name"))
                .append(String.format("%-15s", "type")).append("meta data").append("\n");

        for (int i = 0; i < nCol; i++) {
            String colName = getName(i);
            ColumnType type = getType(i);
            ColumnMetaData meta = getMetaData(i);
            String paddedName = String.format("%-" + (maxNameLength + 8) + "s", "\"" + colName + "\"");
            sb.append(String.format("%-6d", i))
                    .append(paddedName)
                    .append(String.format("%-15s", type))
                    .append(meta).append("\n");
        }

        return sb.toString();
    }

    public static class Builder extends Schema.Builder {

        private Integer minSequenceLength;
        private Integer maxSequenceLength;

        public Builder minSequenceLength(int minSequenceLength) {
            this.minSequenceLength = minSequenceLength;
            return this;
        }

        public Builder maxSequenceLength(int maxSequenceLength) {
            this.maxSequenceLength = maxSequenceLength;
            return this;
        }


        @Override
        public SequenceSchema build() {
            return new SequenceSchema(this);
        }
    }
}
