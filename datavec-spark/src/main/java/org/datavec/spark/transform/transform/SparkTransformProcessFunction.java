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

package org.datavec.spark.transform.transform;

import lombok.AllArgsConstructor;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.datavec.api.writable.Writable;
import org.datavec.api.transform.TransformProcess;

import java.util.Collections;
import java.util.List;

/**
 * Spark function for executing a transform process
 */
@AllArgsConstructor
public class SparkTransformProcessFunction implements FlatMapFunction<List<Writable>,List<Writable>> {

    private final TransformProcess transformProcess;

    @Override
    public Iterable<List<Writable>> call(List<Writable> v1) throws Exception {
        List<Writable> newList = transformProcess.execute(v1);
        if(newList == null) return Collections.emptyList();   //Example was filtered out
        else return Collections.singletonList(newList);
    }
}
