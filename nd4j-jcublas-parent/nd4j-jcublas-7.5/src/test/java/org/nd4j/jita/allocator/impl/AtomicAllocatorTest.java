package org.nd4j.jita.allocator.impl;

import org.junit.Before;
import org.junit.Test;
import org.nd4j.jita.allocator.Allocator;
import org.nd4j.jita.conf.Configuration;
import org.nd4j.jita.conf.CudaEnvironment;
import org.nd4j.jita.conf.DeviceInformation;
import org.nd4j.jita.mover.UmaMover;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.jcublas.buffer.allocation.PinnedMemoryStrategy;
import org.nd4j.linalg.jcublas.context.ContextHolder;
import org.nd4j.linalg.jcublas.context.CudaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 * @author raver119@gmail.com
 */
public class AtomicAllocatorTest {
    private Allocator allocator;
    private CudaEnvironment singleDevice4GBcc52;


    private static Logger log = LoggerFactory.getLogger(AtomicAllocatorTest.class);

    @Before
    public void setUp() throws Exception {
        if (allocator == null) {
            singleDevice4GBcc52 = new CudaEnvironment();

            DeviceInformation device1 = new DeviceInformation();
            device1.setDeviceId(1);
            device1.setCcMajor(5);
            device1.setCcMinor(2);
            device1.setTotalMemory(4 * 1024 * 1024 * 1024L);
            device1.setAvailableMemory(4 * 1024 * 1024 * 1024L);

            singleDevice4GBcc52.addDevice(device1);

            allocator = AtomicAllocator.getInstance();
            allocator.setEnvironment(singleDevice4GBcc52);
            allocator.applyConfiguration(new Configuration());
            allocator.setMover(new UmaMover());
        }
    }

    @Test
    public void testGpuBlas1() throws Exception {
        // reset to default MemoryStrategy, most probable is Pinned
        ContextHolder.getInstance().forceMemoryStrategyForThread(new PinnedMemoryStrategy());

        INDArray array1 = Nd4j.create(new float[]{1.01f, 1.01f, 1.01f, 1.01f, 1.01f, 1.01f, 1.01f, 1.01f, 1.01f, 1.01f, 1.01f, 1.01f, 1.01f, 1.01f, 1.01f});
        INDArray array2 = Nd4j.create(new float[]{1.10f, 1.10f, 1.10f, 1.10f, 1.10f, 1.10f, 1.10f, 1.10f, 1.10f, 1.10f, 1.10f, 1.10f, 1.10f, 1.10f, 1.10f});


        double dotWrapped = 0;

        long time1 = 0;
        long time2 = 0;
        long exec[] = new long[10];
        for (int x = 0; x < exec.length; x++) {
            time1 = System.nanoTime();
            dotWrapped = Nd4j.getBlasWrapper().dot(array1, array2);
            time2 = System.nanoTime();

            log.info("Execution time: [" + (time2 - time1) + "] ns");
            exec[x] = time2 - time1;
        }

        assertEquals(16.665000915527344, dotWrapped, 0.001d);
    }

    @Test
    public void testGpuBlas2() throws Exception {
        assertEquals("JcublasLevel1", Nd4j.getBlasWrapper().level1().getClass().getSimpleName());

        INDArray array1 = Nd4j.create(new float[]{1.01f, 1.01f, 1.01f, 1.01f, 1.01f, 1.01f, 1.01f, 1.01f, 1.01f, 1.01f, 1.01f, 1.01f, 1.01f, 1.01f, 1.01f});
        INDArray array2 = Nd4j.create(new float[]{1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f});

        Nd4j.getBlasWrapper().axpy(new Float(0.75f), array1, array2);

        assertEquals(1.7574999332427979, array2.getDouble(0), 0.00001);
        assertEquals(1.7574999332427979, array2.getDouble(1), 0.00001);


    }
}