/*
 * Copyright 2015 Skymind,Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.nd4j.linalg.jblas.sampling;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.sampling.Sampling;
import org.nd4j.linalg.util.ArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by agibsonccc on 9/9/14.
 */
public class SamplingTests {
    private static Logger log = LoggerFactory.getLogger(SamplingTests.class);


    @Test
    public void testNormal() {
        RandomGenerator rgen = new MersenneTwister(123);
        INDArray create = Nd4j.create(ArrayUtil.floatCopyOf(new double[]{-7.107345154508948E-4, 2.0385881362017244E-4, 0.0010431993287056684, -6.159201730042696E-4, 1.6545047401450574E-4, -3.2999785616993904E-4, 3.824937157332897E-4, -5.944876465946436E-4, -9.169760742224753E-4, 4.256776301190257E-4, 3.863394958898425E-4, -6.384158623404801E-4, -0.0012161752674728632, 4.098936915397644E-5, -2.0747774397023022E-4, 1.0528514394536614E-5, 0.0011771972058340907, -3.1577813206240535E-4, 0.001052041887305677, -2.1263127564452589E-4, -4.418340395204723E-4, 6.033279350958765E-4, 1.3582913379650563E-4, 3.1447052606381476E-5, -1.1697690933942795E-4, 1.9324175082147121E-4, -2.9811158310621977E-4, 4.571006284095347E-4, -0.0011589869391173124, -3.8391619455069304E-5, -0.001260544522665441, -8.689297828823328E-5, -3.697737120091915E-4, -9.308812441304326E-4, 1.6903740470297635E-4, 5.043628043495119E-4, -5.4862292017787695E-5, 1.9653525669127703E-6, 3.842957958113402E-4, 2.18122688238509E-4, -0.0011314463336020708, 7.142349204514176E-6, 2.9471813468262553E-4, -4.0678662480786443E-4, 3.6997447023168206E-4, 6.509708473458886E-4, -2.245271170977503E-4, 7.266714819706976E-4, 1.505711697973311E-4, 6.707172724418342E-4}), new int[]{2, 25});
        INDArray normal = Sampling.normal(rgen, create, 1.0f);

    }


}
