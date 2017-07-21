package org.deeplearning4j.arbiter.optimize.runner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.deeplearning4j.arbiter.optimize.api.Candidate;
import org.deeplearning4j.arbiter.optimize.api.OptimizationResult;
import org.deeplearning4j.arbiter.optimize.api.TaskCreator;
import org.deeplearning4j.arbiter.optimize.api.data.DataProvider;
import org.deeplearning4j.arbiter.optimize.api.score.ScoreFunction;
import org.deeplearning4j.arbiter.optimize.config.OptimizationConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LocalOptimizationRunner: execute hyperparameter optimization
 * locally (on current machine, in current JVM).
 *
 * @author Alex Black
 */
public class LocalOptimizationRunner extends BaseOptimizationRunner {

    public static final int DEFAULT_MAX_CONCURRENT_TASKS = 1;

    private final int maxConcurrentTasks;

    private TaskCreator taskCreator;
    private ListeningExecutorService executor;

    public LocalOptimizationRunner(OptimizationConfiguration config, TaskCreator taskCreator) {
        this(DEFAULT_MAX_CONCURRENT_TASKS, config, taskCreator);
    }

    public LocalOptimizationRunner(int maxConcurrentTasks, OptimizationConfiguration config,
                    TaskCreator taskCreator) {
        super(config);
        if (maxConcurrentTasks <= 0)
            throw new IllegalArgumentException("maxConcurrentTasks must be > 0 (got: " + maxConcurrentTasks + ")");
        this.maxConcurrentTasks = maxConcurrentTasks;
        this.taskCreator = taskCreator;

        ExecutorService exec = Executors.newFixedThreadPool(maxConcurrentTasks, new ThreadFactory() {
            private AtomicLong counter = new AtomicLong(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
                t.setName("LocalCandidateExecutor-" + counter.getAndIncrement());
                return t;
            }
        });
        executor = MoreExecutors.listeningDecorator(exec);

        init();
    }

    @Override
    protected int maxConcurrentTasks() {
        return maxConcurrentTasks;
    }

    @Override
    protected ListenableFuture<OptimizationResult> execute(Candidate candidate,
                    DataProvider dataProvider, ScoreFunction scoreFunction) {
        return execute(Collections.singletonList(candidate), dataProvider, scoreFunction).get(0);
    }

    @Override
    protected List<ListenableFuture<OptimizationResult>> execute(List<Candidate> candidates,
                    DataProvider dataProvider, ScoreFunction scoreFunction) {
        List<ListenableFuture<OptimizationResult>> list = new ArrayList<>(candidates.size());
        for (Candidate candidate : candidates) {
            Callable<OptimizationResult> task =
                            taskCreator.create(candidate, dataProvider, scoreFunction, statusListeners);
            list.add(executor.submit(task));
        }
        return list;
    }

    @Override
    protected void shutdown() {
        executor.shutdownNow();
    }
}
