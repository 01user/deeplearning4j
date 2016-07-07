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

package org.datavec.api.transform.analysis.sequence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Created by Alex on 12/03/2016.
 */
@AllArgsConstructor @Data @Builder
public class SequenceLengthAnalysis {

    private final long totalNumSequences;
    private final int minSeqLength;
    private final int maxSeqLength;
    private final long countZeroLength;
    private final long countOneLength;
    private final double meanLength;
    private final double[] histogramBuckets;
    private final long[] histogramBucketCounts;

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("SequenceLengthAnalysis(")
                .append("totalNumSequences=").append(totalNumSequences)
                .append(",minSeqLength=").append(minSeqLength)
                .append(",maxSeqLength=").append(maxSeqLength)
                .append(",countZeroLength=").append(countZeroLength)
                .append(",countOneLength=").append(countOneLength)
                .append(",meanLength=").append(meanLength)
                .append(")");
        return sb.toString();
    }

}
