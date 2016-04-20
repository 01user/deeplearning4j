package org.nd4j.jita.conf;

import lombok.Data;
import org.nd4j.jita.allocator.enums.Aggressiveness;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.jcublas.ops.executioner.JCudaExecutioner;
import org.nd4j.nativeblas.NativeOps;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author raver119@gmail.com
 */
@Data
public class Configuration implements Serializable {

    /**
     * Keep this value between 0.01 and 0.95 please
     */
    private double maxDeviceMemoryUsed = 0.85;

    /**
     * Minimal number of activations for relocation threshold
     */
    private int minimumRelocationThreshold = 5;

    /**
     * Minimal guaranteed TTL for memory chunk
     */
    private long minimumTTLMilliseconds = 10 * 1000L;

    /**
     * Number of buckets/garbage collectors for host memory
     */
    private int numberOfHostMemoryBuckets = 8;

    /**
     * Deallocation aggressiveness
     */
    private Aggressiveness hostDeallocAggressiveness = Aggressiveness.REASONABLE;

    private Aggressiveness gpuDeallocAggressiveness = Aggressiveness.REASONABLE;

    /**
     * Allocation aggressiveness
     */
    private Aggressiveness gpuAllocAggressiveness = Aggressiveness.REASONABLE;


    /**
     * Maximum allocated per-device memory, in bytes
     */
    private long maximumDeviceAllocation = 1024 * 1024 * 1024L;


    /**
     * Maximum allocatable zero-copy/pinned/pageable memory
     */
    private long maximumZeroAllocation = Runtime.getRuntime().maxMemory();

    /**
     * True if allowed, false if relocation required
     */
    private boolean crossDeviceAccessAllowed = false;

    /**
     * True, if allowed, false otherwise
     */
    private boolean zeroCopyFallbackAllowed = false;

    /**
     * Maximum length of single memory chunk
     */
    private long maximumSingleAllocation = Long.MAX_VALUE;

    private List<Integer> availableDevices = new ArrayList<>();

    public void setMinimumRelocationThreshold(int threshold) {
        this.maximumDeviceAllocation = Math.max(2, threshold);
    }

    /**
     * This method allows you to specify max per-device memory use.
     *
     * PLEASE NOTE: Accepted value range is 0.01 > x < 0.95
     *
     * @param percentage
     */
    public void setMaxDeviceMemoryUsed(double percentage) {
        if (percentage < 0.02 || percentage > 0.95) {
            this.maxDeviceMemoryUsed = 0.85;
        } else this.maxDeviceMemoryUsed = percentage;
    }

    public Configuration() {
        NativeOps nativeOps = ((JCudaExecutioner) Nd4j.getExecutioner()).getNativeOps();

        int cnt = (int) nativeOps.getAvailableDevices();
        for (int i = 0; i < cnt; i++) {
            availableDevices.add(i);
        }
    }
}
