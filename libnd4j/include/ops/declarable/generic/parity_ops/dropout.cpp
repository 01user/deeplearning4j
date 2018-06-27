//
// Created by GS <sgazeos@gmail.com>
//

#include <op_boilerplate.h>
#if NOT_EXCLUDED(OP_dropout)

#include <ops/declarable/headers/parity_ops.h>
#include <ops/declarable/helpers/dropout.h>

namespace nd4j {
namespace ops {


//////////////////////////////////////////////////////////////////////////
CONFIGURABLE_OP_IMPL(dropout, 1, 1, true, 1, 1) {
    NDArray<T>* input   = INPUT_VARIABLE(0); // lookup param

    NDArray<T>* reduceShape = nullptr; // this param is optional
    NDArray<T>* output  = OUTPUT_VARIABLE(0); // 
    
    int seed = INT_ARG(0);
    
    T probValue = T_ARG(0); 
    if (block.width() > 1)
        reduceShape = INPUT_VARIABLE(1);

    REQUIRE_TRUE(probValue > T(0.f) && probValue <= T(1.f), 0, "dropout: Probability should be with range 0 to 1.");

    if (probValue == T(1.0)) {
        *output = *input;
        return ND4J_STATUS_OK;
    }
    nd4j::random::RandomBuffer* rng = block.getRNG();
    
    if (rng == nullptr)
        return ND4J_STATUS_BAD_RNG;

    return helpers::dropOutFunctor(rng, input, output, reduceShape, seed, probValue);
}

//////////////////////////////////////////////////////////////////////////
CONFIGURABLE_OP_IMPL(dropout_bp, 2, 1, false, 1, 1) {
    NDArray<T>* input   = INPUT_VARIABLE(0); // lookup param
    NDArray<T>* gradOut   = INPUT_VARIABLE(1); // lookup param

    NDArray<T>* reduceShape = nullptr; // this param is optional
    NDArray<T>* output  = OUTPUT_VARIABLE(0); // 
    
    int seed = INT_ARG(0);
    
    T probValue = T_ARG(0); 
    if (block.width() > 2)
        reduceShape = INPUT_VARIABLE(2);

    REQUIRE_TRUE(probValue > T(0.f) && probValue <= T(1.f), 0, "dropout_bp: Probability should be with range 0 to 1.");
    if (probValue == T(1.0)) {
        output->assign(T(0.f)); // fill up output with 0 
        return ND4J_STATUS_OK;
    }

    nd4j::random::RandomBuffer* rng = block.getRNG();
    if (rng == nullptr)
        return ND4J_STATUS_BAD_RNG;

    REQUIRE_TRUE(helpers::dropOutFunctor(rng, input, output, reduceShape, seed, probValue) == ND4J_STATUS_OK, 0, "dropout_bp: Cannot backprop dropout." );

    for (Nd4jLong e = 0; e < output->lengthOf(); e++) {
        if ((*output)(e) == T(0.f)) (*output)(e) = (*gradOut)(e) / probValue;
        else (*output)(e) = T(0.f);
    }

    return ND4J_STATUS_OK;
}

}
}

#endif