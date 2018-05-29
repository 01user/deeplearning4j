package org.nd4j.linalg.concurrency;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

@Slf4j
public class VariableBarrierImpl implements VariableBarrier {
    protected AtomicInteger phase = new AtomicInteger(0);
    protected AtomicInteger consumers = new AtomicInteger(0);


    protected AtomicInteger first = new AtomicInteger(0);
    protected AtomicInteger second = new AtomicInteger(0);

    @Override
    public void registerConsumers(int numberOfConsumers) {
        while (phase.get() != 0 && phase.get() >= 0)
            LockSupport.parkNanos(5);

        // we dont want to overwrite bypass state
        if (phase.get() == 0) {
            consumers.set(numberOfConsumers);
            log.info("Register set {} consumers", consumers);
            phase.set(1);
        }
    }

    @Override
    public void synchronizedBlock() {
        // waiting till we're on right phase
        while (phase.get() != 1 && phase.get() >= 0)
            LockSupport.parkNanos(5);

        // last thread updates variable IF we're no on bypass mode
        if (first.incrementAndGet() == consumers.get() && phase.get() >= 0) {
            log.info("Synchronized block passed by {} consumers", consumers.get());

            second.set(0);
            phase.set(2);
        }
    }

    @Override
    public void desynchronizedBlock() {
        // waiting till we're on right phase
        while (phase.get() != 2 && phase.get() >= 0)
            LockSupport.parkNanos(5);

        // last thread sets phase to 0 if we're NOT on bypass mode
        if (second.incrementAndGet() == consumers.get() && phase.get() >= 0) {
            log.info("Desynchronized block passed by {} consumers", consumers.get());

            first.set(0);
            phase.set(0);
        }
    }

    @Override
    public void bypassEverything() {
        phase.set(-1);
    }
}
