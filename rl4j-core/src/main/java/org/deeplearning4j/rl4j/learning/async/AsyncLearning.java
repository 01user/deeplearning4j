package org.deeplearning4j.rl4j.learning.async;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.rl4j.space.ActionSpace;
import org.deeplearning4j.rl4j.space.Encodable;
import org.deeplearning4j.rl4j.learning.Learning;
import org.deeplearning4j.rl4j.network.NeuralNet;

/**
 * @author rubenfiszel (ruben.fiszel@epfl.ch) 7/25/16.
 *
 * Async learning always follow the same pattern in RL4J
 * -launch the Global thread
 * -launch the "save threads"
 * -periodically evaluate the model of the global thread for monitoring purposes
 *
 */
@Slf4j
public abstract class AsyncLearning<O extends Encodable, A, AS extends ActionSpace<A>, NN extends NeuralNet>
                extends Learning<O, A, AS, NN> {


    public AsyncLearning(AsyncConfiguration conf) {
        super(conf);
    }

    public abstract AsyncConfiguration getConfiguration();

    protected abstract AsyncThread newThread(int i);

    protected abstract AsyncGlobal<NN> getAsyncGlobal();

    protected void startGlobalThread() {
        getAsyncGlobal().start();
    }

    protected boolean isTrainingComplete() {
        return getAsyncGlobal().isTrainingComplete();
    }

    public void launchThreads() {
        startGlobalThread();
        for (int i = 0; i < getConfiguration().getNumThread(); i++) {
            newThread(i).start();
        }
        log.info("Threads launched.");
    }

    @Override
    public int getStepCounter() {
        return getAsyncGlobal().getT().get();
    }

    public void train() {

        try {
            log.info("AsyncLearning training starting.");
            launchThreads();

            //this is simply for stat purposes
            getDataManager().writeInfo(this);
            synchronized (this) {
                while (!isTrainingComplete() && getAsyncGlobal().isRunning()) {
                    getPolicy().play(getMdp(), getHistoryProcessor());
                    getDataManager().writeInfo(this);
                    wait(20000);
                }
            }
        } catch (Exception e) {
            log.error("Training failed.", e);
            e.printStackTrace();
        }
    }


}
