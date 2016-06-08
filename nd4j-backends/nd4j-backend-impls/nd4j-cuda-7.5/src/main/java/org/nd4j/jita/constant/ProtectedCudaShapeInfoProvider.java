package org.nd4j.jita.constant;

import org.nd4j.jita.allocator.impl.AtomicAllocator;
import org.nd4j.jita.conf.Configuration;
import org.nd4j.jita.conf.CudaEnvironment;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.ndarray.BaseShapeInfoProvider;
import org.nd4j.linalg.api.ndarray.ShapeInfoProvider;
import org.nd4j.linalg.api.shape.ShapeDescriptor;
import org.nd4j.linalg.factory.Nd4j;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author raver119@gmail.com
 */
public class ProtectedCudaShapeInfoProvider extends BaseShapeInfoProvider {

    private AtomicAllocator allocator = AtomicAllocator.getInstance();

    private AtomicLong cacheHit = new AtomicLong(1);
    private AtomicLong cacheMiss = new AtomicLong(1);

    private Semaphore lock = new Semaphore(1);

    private Configuration configuration = CudaEnvironment.getInstance().getConfiguration();

    private static final ConstantProtector protector = ConstantProtector.getInstance();

    private static ProtectedCudaShapeInfoProvider ourInstance = new ProtectedCudaShapeInfoProvider();


    private ProtectedCudaShapeInfoProvider() {

    }

    public static ProtectedCudaShapeInfoProvider getInstance() {
        return ourInstance;
    }

    @Override
    public DataBuffer createShapeInformation(int[] shape, int[] stride, int offset, int elementWiseStride, char order) {
        offset = 0;

        Integer deviceId = allocator.getDeviceId();

        ShapeDescriptor descriptor = new ShapeDescriptor(shape, stride, offset, elementWiseStride, order);

        if (!protector.containsDataBuffer(deviceId, descriptor)) {
//            logger.info("Cache miss");
            DataBuffer buffer = super.createShapeInformation(shape, stride, offset, elementWiseStride, order);

            Nd4j.getConstantHandler().moveToConstantSpace(buffer);

            //deviceCache.get(deviceId).put(descriptor, buffer);
            protector.persistDataBuffer(deviceId, descriptor, buffer);

            cacheMiss.incrementAndGet();
            return buffer;
        } else {
            //logger.info("Cache hit");
            cacheHit.incrementAndGet();
        }

        return protector.getDataBuffer(deviceId, descriptor); //deviceCache.get(deviceId).get(descriptor);
    }
}
