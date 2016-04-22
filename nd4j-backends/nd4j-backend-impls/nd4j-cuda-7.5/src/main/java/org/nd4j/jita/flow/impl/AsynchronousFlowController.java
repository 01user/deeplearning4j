package org.nd4j.jita.flow.impl;

import org.nd4j.jita.allocator.Allocator;
import org.nd4j.jita.allocator.enums.AllocationStatus;
import org.nd4j.jita.allocator.enums.CudaConstants;
import org.nd4j.jita.allocator.pointers.cuda.cudaStream_t;
import org.nd4j.jita.flow.FlowController;
import org.nd4j.jita.allocator.impl.AllocationPoint;
import org.nd4j.jita.allocator.pointers.cuda.cudaEvent_t;
import org.nd4j.jita.allocator.utils.AllocationUtils;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.jcublas.context.CudaContext;
import org.nd4j.nativeblas.NativeOps;
import org.nd4j.nativeblas.NativeOpsHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author raver119@gmail.com
 */
public class AsynchronousFlowController implements FlowController{
    private volatile Allocator allocator;

    private static Logger log = LoggerFactory.getLogger(AsynchronousFlowController.class);

    protected NativeOps nativeOps = NativeOpsHolder.getInstance().getDeviceNativeOps();

    @Override
    public void init(Allocator allocator) {
        this.allocator = allocator;
    }

    @Override
    public void synchronizeToHost(AllocationPoint point) {
        if (!point.isActualOnHostSide()) {

            if (!point.isConstant())
                waitTillFinished(point);

            //  log.info("Synchronization started... " + point.getShape());

            // if this piece of memory is device-dependant, we'll also issue copyback once
            if (point.getAllocationStatus() == AllocationStatus.DEVICE && !point.isActualOnHostSide()) {
                CudaContext context = (CudaContext) allocator.getDeviceContext().getContext();
/*
                JCuda.cudaMemcpyAsync(
                        new Pointer(point.getHostPointer().address()),
                        new Pointer(point.getDevicePointer().address()),
                        AllocationUtils.getRequiredMemory(point.getShape()),
                        cudaMemcpyKind.cudaMemcpyDeviceToHost,
                        context.getSpecialStream()
                );*/
                if (nativeOps.memcpyAsync(
                        point.getHostPointer().address(),
                        point.getDevicePointer().address(),
                        AllocationUtils.getRequiredMemory(point.getShape()),
                        CudaConstants.cudaMemcpyDeviceToHost,
                        context.getSpecialStream().address()) == 0)
                    throw new IllegalStateException("MemcpyAsync failed");

                context.syncSpecialStream();
            }// else log.info("Not [DEVICE] memory, skipping...");


            // updating host read timer
            point.tickHostRead();
            //log.info("After sync... isActualOnHostSide: {}", point.isActualOnHostSide());
        }
    }

    @Override
    public void waitTillFinished(AllocationPoint point) {
        cudaEvent_t event = point.getLastEvent();
        if (event != null) {
            nativeOps.eventSynchronize(event.address());
            nativeOps.destroyEvent(event.address());
            point.setLastEvent(null);
        }
    }

    public void registerAction(INDArray result, INDArray... operands) {
        if (result == null) return;
        CudaContext context = (CudaContext) allocator.getDeviceContext().getContext();

        cudaEvent_t event = new cudaEvent_t(nativeOps.createEvent());

        //JCuda.cudaEventCreateWithFlags(event, JCuda.cudaEventBlockingSync);
        //JCuda.cudaEventRecord(event, context.getOldStream());
        nativeOps.registerEvent(event.address(), context.getOldStream().address());

        AllocationPoint point = allocator.getAllocationPoint(result);
        point.setLastEvent(event);
        point.tickDeviceWrite();
    }

    @Override
    public cudaStream_t prepareAction(INDArray result, INDArray... operands) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
