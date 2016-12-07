package org.nd4j.linalg.activations.impl;

//import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.Pair;
import org.nd4j.linalg.activations.IActivation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.transforms.LeakyReLU;
import org.nd4j.linalg.api.ops.impl.transforms.PReLU;
import org.nd4j.linalg.api.ops.impl.transforms.RectifedLinear;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.BooleanIndexing;
import org.nd4j.linalg.indexing.conditions.Conditions;


import java.util.Arrays;

/**
 * Created by susaneraly on 12/1/16.
 */

public class ActivationRRelu implements IActivation {

    //How is the seed set for repeatability? This should be the seed from the conf.
    protected double l,u;
    private INDArray alpha; //save the alpha's for the backward pass

    public ActivationRRelu() {
        this(1/8.0,1/3.0);
    }

    public ActivationRRelu(double l, double u) {
        this.l = l;
        this.u = u;
    }


    @Override
    public INDArray computeActivation(INDArray in) {
        return Nd4j.getExecutioner().execAndReturn(new PReLU(in,alpha));
    }

    public INDArray computeActivation(INDArray in, boolean training) {
        if (!training) {
            return Nd4j.getExecutioner().execAndReturn(new LeakyReLU(in.dup(), (l+u)/2));
        }
        else {
            return computeActivation(in);
        }
    }

    @Override
    public INDArray computeGradient(INDArray in) {
        /*
            gradient
                1; x>=0
                alpha; x<0
         */
        //if (overWriteAlpha(in.shape())) {
            //should never have to overwrite alpha here, throw error/warning?
            //only as a result of user error
                //computegradient was called before compute activation
                //computegradient is called with a different shape than the shape compute activation was called with
        //}
        INDArray gradients = in.dup();
        BooleanIndexing.replaceWhere(gradients, 1.0, Conditions.greaterThanOrEqual(0.0));
        BooleanIndexing.replaceWhere(gradients, getAlpha(), Conditions.lessThan(0));
        return gradients;
    }

    public INDArray computeGradient(INDArray in, boolean training) {
        if (!training) {
            //why would you ever need the gradient for test?
            return Nd4j.getExecutioner().execAndReturn(new LeakyReLU(in.dup(), (l+u)/2).derivative());
        }
        else {
            return computeGradient(in);
        }
    }


    @Override
    public Pair<INDArray, INDArray> computeGradientAndActivation(INDArray in) {
        return new Pair<INDArray, INDArray>(
                computeActivation(in),
                computeGradient(in)
        ); //thread safety?

    }

}
