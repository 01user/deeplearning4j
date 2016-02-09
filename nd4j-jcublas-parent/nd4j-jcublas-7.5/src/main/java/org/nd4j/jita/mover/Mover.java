package org.nd4j.jita.mover;

import org.nd4j.jita.allocator.impl.AllocationPoint;
import org.nd4j.jita.allocator.impl.AllocationShape;
import org.nd4j.jita.allocator.enums.AllocationStatus;
import org.nd4j.jita.allocator.locks.Lock;
import org.nd4j.jita.conf.Configuration;
import org.nd4j.jita.conf.CudaEnvironment;

/**
 * Mover interface describes methods for data transfers between host and devices
 *
 * @author raver119@gmail.com
 */
public interface Mover {

    void init(Configuration configuration, CudaEnvironment environment, Lock locker);

    /**
     * Allocate specified memory chunk on specified device/host
     *
     * @param targetMode valid arguments are DEVICE, ZERO
     * @return
     */
    Object alloc(AllocationStatus targetMode,AllocationShape shape);


    /**
     *  Relocates specific chunk of memory from one storage to another
     *
     * @param currentStatus
     * @param targetStatus
     * @param point
     */
    void relocate(AllocationStatus currentStatus, AllocationStatus targetStatus, AllocationPoint point);

    /**
     * Copies memory from device to host, if needed.
     * Device copy is preserved as is.
     *
     * @param point
     */
    void copyback(AllocationPoint point);

    /**
     * This method frees memory chunk specified by allocation point
     *
     * @param point
     */
    void free(AllocationPoint point);
}
