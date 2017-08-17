package org.nd4j.autodiff.functions.impl.unary.reduce;

import org.nd4j.autodiff.ArrayField;
import org.nd4j.autodiff.functions.AbstractReduceUnaryFunction;
import org.nd4j.autodiff.functions.DifferentialFunction;
import org.nd4j.autodiff.samediff.SameDiff;

public class Min extends AbstractReduceUnaryFunction<ArrayField> {

    public Min(SameDiff sameDiff, DifferentialFunction<ArrayField> i_v, int[] dimensions) {
        super(sameDiff, i_v, dimensions);
    }



    @Override
    public ArrayField doGetValue() {
        return sameDiff.getArrayFactory().max(arg().doGetValue(),dimensions);
    }


    @Override
    public String functionName() {
        return new org.nd4j.linalg.api.ops.impl.accum.Min().name();
    }


    @Override
    public DifferentialFunction<ArrayField> diff(DifferentialFunction<ArrayField> i_v1) {
        validateDifferentialFunctionsameDiff(i_v1);
        return sameDiff.getFunctionFactory().doGradChoose(this,i_v1,dimensions);
    }
}
