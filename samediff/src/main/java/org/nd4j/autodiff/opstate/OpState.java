package org.nd4j.autodiff.opstate;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.*;
import org.nd4j.linalg.api.ops.aggregates.Aggregate;
import org.nd4j.linalg.factory.Nd4j;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 * Describes the type of operation that needs to happen
 * @author Adam Gibson
 */
@Data
@Builder
@EqualsAndHashCode
public class OpState implements Serializable {
    private long n;
    private OpType opType;
    private String opName;
    private int opNum;
    private Number scalarValue;
    private String[] vertexIds;
    private String id;
    private int[] axes;
    private Object[] extraArgs;
    private Object[] extraArgsWithoutInPlace;
    private NDArrayInformation result;


    /**
     * Creates an op state from
     * the given op.
     * @param op
     * @param arrToVertexID a map of {@link INDArray}
     *                      to vertex id (this map
     *                      is typically a reference map
     *                      {@link java.util.IdentityHashMap})
     * @return
     */
    public static OpState fromOp(Op op, Map<INDArray,Integer> arrToVertexID) {
        OpState opState = OpState.builder()
                .extraArgs(op.extraArgs())
                .opNum(op.opNum())
                .n(op.n()).vertexIds(null)
                .id(UUID.randomUUID().toString())
                .opName(op.name()).vertexIds(new String[]{
                        String.valueOf(arrToVertexID.get(op.x()))
                        ,String.valueOf(arrToVertexID.get(op.y()))
                })
                .opType(opTypeFromOp(op))
                .build();
        NDArrayInformation ndArrayInformation = NDArrayInformation.newInfo(op.z());
        ndArrayInformation.setOwner(opState);
        opState.setResult(ndArrayInformation);
        return opState;
    }


    /**
     * Create an {@link OpType}
     * based on the type of {@link Op}
     * @param op the input op
     * @return the optype based on
     * the given op
     */
    public static OpType opTypeFromOp(Op op) {
       if(op instanceof ScalarOp)
           return OpType.SCALAR_TRANSFORM;
       else if(op instanceof Accumulation)
           return OpType.ACCUMULATION;
       else if(op instanceof IndexAccumulation)
           return OpType.INDEX_ACCUMULATION;
       else if(op instanceof GridOp)
           return OpType.AGGREGATE;
       else if(op instanceof TransformOp)
           return OpType.TRANSFORM;
       else if(op instanceof ShapeOp)
           return OpType.SHAPE;
       else if(op instanceof BroadcastOp)
           return OpType.BROADCAST;
       throw new IllegalStateException("Illegal op type " + op.getClass().getName());
    }

    public boolean isInPlace() {
        return getInPlace(extraArgs);
    }

    public Object[] getExtraArgs() {
        if(extraArgs == null || extraArgs.length <= 0)
            return null;
        if(extraArgsWithoutInPlace == null || extraArgsWithoutInPlace.length <= 0) {
            extraArgsWithoutInPlace = new Object[extraArgs.length > 1 ? extraArgs.length - 1 : 1];
            int count = 0;
            for(int i = 0; i < extraArgs.length; i++) {
                if(!(extraArgs[i] instanceof Boolean))
                    extraArgsWithoutInPlace[count++] = extraArgs[i];
            }
        }
        return extraArgsWithoutInPlace;
    }

    public void setExtraArgs(Object[] extraArgs) {
        this.extraArgs = extraArgs;
    }

    protected boolean getInPlace(Object[] extraArgs) {
        if(extraArgs == null) {
            return false;
        }
        else {
            for(int i = 0; i < extraArgs.length; i++) {
                if(extraArgs[i] instanceof Boolean)
                    return (Boolean) extraArgs[i];
            }
        }

        return false;
    }

    public  enum OpType {
        SCALAR_TRANSFORM,
        ACCUMULATION,
        TRANSFORM,
        BROADCAST,
        INDEX_ACCUMULATION,
        AGGREGATE,
        SHAPE
    }



}
