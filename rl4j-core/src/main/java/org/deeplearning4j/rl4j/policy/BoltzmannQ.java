package org.deeplearning4j.rl4j.policy;

import lombok.AllArgsConstructor;
import org.deeplearning4j.rl4j.network.dqn.IDQN;
import org.deeplearning4j.rl4j.space.Encodable;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.Random;

import static org.nd4j.linalg.ops.transforms.Transforms.exp;

/**
 * @author rubenfiszel (ruben.fiszel@epfl.ch) on 8/10/16.
 */
@AllArgsConstructor
public class BoltzmannQ<O extends Encodable> extends Policy<O, Integer> {

    final private IDQN dqn;
    final private Random rd = new Random(123);

    public Integer nextAction(INDArray input) {

        INDArray output = dqn.output(input);
        INDArray exp = exp(output);

        double sum = exp.sum(1).getDouble(0);
        double picked = rd.nextDouble() * sum;
        for (int i = 0; i < exp.columns(); i++) {
            if (picked < exp.getDouble(i))
                return i;
        }
        return -1;

    }


}
