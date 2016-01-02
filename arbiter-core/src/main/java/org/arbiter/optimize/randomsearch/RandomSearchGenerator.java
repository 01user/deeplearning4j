/*
 *
 *  * Copyright 2016 Skymind,Inc.
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
 *
 */
package org.arbiter.optimize.randomsearch;

import org.arbiter.optimize.api.Candidate;
import org.arbiter.optimize.api.CandidateGenerator;
import org.arbiter.optimize.api.ModelParameterSpace;

import java.util.concurrent.atomic.AtomicInteger;

public class RandomSearchGenerator<T> implements CandidateGenerator<T> {

    private ModelParameterSpace<T> parameterSpace;
    private AtomicInteger candidateCounter = new AtomicInteger(0);

    public RandomSearchGenerator( ModelParameterSpace<T> parameterSpace ){
        this.parameterSpace = parameterSpace;
    }

    @Override
    public Candidate<T> getCandidate() {
        return new Candidate<T>(parameterSpace.randomCandidate(),candidateCounter.getAndIncrement());
    }

    @Override
    public void reportResults(Object result) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public ModelParameterSpace<T> getParameterSpace() {
        return parameterSpace;
    }

    @Override
    public String toString(){
        return "RandomSearchCandidateGenerator()";
    }
}
