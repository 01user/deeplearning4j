/*
 *
 *  * Copyright 2016 Skymind,Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */
package org.arbiter.deeplearning4j;

import org.arbiter.deeplearning4j.evaluator.DL4JClassificationEvaluator;
import org.arbiter.deeplearning4j.layers.ConvolutionLayerSpace;
import org.arbiter.deeplearning4j.layers.DenseLayerSpace;
import org.arbiter.deeplearning4j.layers.OutputLayerSpace;
import org.arbiter.deeplearning4j.saver.local.LocalMultiLayerNetworkSaver;
import org.arbiter.deeplearning4j.scoring.TestSetLossScoreFunction;
import org.arbiter.deeplearning4j.task.DL4JTaskCreator;
import org.arbiter.optimize.api.CandidateGenerator;
import org.arbiter.optimize.api.data.DataProvider;
import org.arbiter.optimize.api.termination.MaxCandidatesCondition;
import org.arbiter.optimize.api.termination.MaxTimeCondition;
import org.arbiter.optimize.config.OptimizationConfiguration;
import org.arbiter.optimize.executor.CandidateExecutor;
import org.arbiter.optimize.executor.local.LocalCandidateExecutor;
import org.arbiter.optimize.parameter.continuous.ContinuousParameterSpace;
import org.arbiter.optimize.parameter.discrete.DiscreteParameterSpace;
import org.arbiter.optimize.parameter.integer.IntegerParameterSpace;
import org.arbiter.optimize.randomsearch.RandomSearchGenerator;
import org.arbiter.optimize.runner.OptimizationRunner;
import org.arbiter.optimize.ui.ArbiterUIServer;
import org.arbiter.optimize.ui.listener.UIOptimizationRunnerStatusListener;
import org.arbiter.util.WebUtils;
import org.deeplearning4j.datasets.iterator.DataSetIterator;
import org.deeplearning4j.datasets.iterator.impl.IrisDataSetIterator;
import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator;
import org.deeplearning4j.earlystopping.EarlyStoppingConfiguration;
import org.deeplearning4j.earlystopping.saver.InMemoryModelSaver;
import org.deeplearning4j.earlystopping.scorecalc.DataSetLossCalculator;
import org.deeplearning4j.earlystopping.termination.MaxEpochsTerminationCondition;
import org.deeplearning4j.earlystopping.termination.MaxScoreIterationTerminationCondition;
import org.deeplearning4j.earlystopping.termination.MaxTimeIterationTerminationCondition;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.junit.Ignore;
import org.junit.Test;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Not strictly a unit test. Rather: part example, part debugging on MNIST */
public class MNISTOptimizationTest {

    private static Logger log = LoggerFactory.getLogger(MNISTOptimizationTest.class);

    public static void main(String[] args) throws Exception {

        EarlyStoppingConfiguration esConf = new EarlyStoppingConfiguration.Builder()
                .epochTerminationConditions(new MaxEpochsTerminationCondition(3))
                .iterationTerminationConditions(
                        new MaxTimeIterationTerminationCondition(5, TimeUnit.MINUTES),
                        new MaxScoreIterationTerminationCondition(6.0) //Random score: -log_e(0.1) ~= 2.3
                        )
                .scoreCalculator(new DataSetLossCalculator(
                        new MnistDataSetIterator(64, 2000, false, false, true, 123), true))
                .modelSaver(new InMemoryModelSaver())
                .build();

        //Define: network config (hyperparameter space)
        MultiLayerSpace mls = new MultiLayerSpace.Builder()
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .learningRate(new ContinuousParameterSpace(0.0001, 0.2))
                .regularization(true)
                .l2(new ContinuousParameterSpace(0.0001, 0.05))
                .dropOut(new ContinuousParameterSpace(0.2, 0.7))
                .iterations(1)
                .addLayer(new ConvolutionLayerSpace.Builder()
                        .nIn(1).nOut(new IntegerParameterSpace(5, 30))
                        .kernelSize(new DiscreteParameterSpace<>(new int[]{3, 3}, new int[]{4, 4}, new int[]{5, 5}))
                        .stride(new DiscreteParameterSpace<>(new int[]{1, 1}, new int[]{2, 2}))
                        .activation(new DiscreteParameterSpace<>("relu","softplus","leakyrelu"))
                        .build(), new IntegerParameterSpace(1, 2), true) //1-2 identical layers
                .addLayer(new DenseLayerSpace.Builder().nIn(4).nOut(new IntegerParameterSpace(2, 10))
                        .activation(new DiscreteParameterSpace<String>("relu", "tanh"))
                        .build(), new IntegerParameterSpace(0, 1), true)   //0 to 1 layers
                .addLayer(new OutputLayerSpace.Builder().nOut(10).activation("softmax")
                        .lossFunction(LossFunctions.LossFunction.MCXENT).build())
                .earlyStoppingConfiguration(esConf)
                .cnnInputSize(28,28,1)
                .pretrain(false).backprop(true).build();

        //Define configuration:
        CandidateGenerator<DL4JConfiguration> candidateGenerator = new RandomSearchGenerator<>(mls);
        DataProvider<DataSetIterator> dataProvider = new MnistDataSetProvider();


        String modelSavePath = new File(System.getProperty("java.io.tmpdir"),"ArbiterMNISTSmall\\").getAbsolutePath();

        File f = new File(modelSavePath);
        if(f.exists()) f.delete();
        f.mkdir();
        if(!f.exists()) throw new RuntimeException();

        OptimizationConfiguration<DL4JConfiguration,MultiLayerNetwork,DataSetIterator,Evaluation> configuration
                = new OptimizationConfiguration.Builder<DL4JConfiguration,MultiLayerNetwork,DataSetIterator,Evaluation>()
                .candidateGenerator(candidateGenerator)
                .dataProvider(dataProvider)
                .modelSaver(new LocalMultiLayerNetworkSaver(modelSavePath))
                .scoreFunction(new TestSetLossScoreFunction(true))
                .terminationConditions(new MaxTimeCondition(120, TimeUnit.MINUTES),
                        new MaxCandidatesCondition(100))
                .build();

        CandidateExecutor<DL4JConfiguration,MultiLayerNetwork,DataSetIterator,Evaluation> executor =
                new LocalCandidateExecutor<>(new DL4JTaskCreator(new DL4JClassificationEvaluator()),1);

        OptimizationRunner<DL4JConfiguration,MultiLayerNetwork,DataSetIterator,Evaluation> runner
                = new OptimizationRunner<>(configuration, executor);

        ArbiterUIServer server = new ArbiterUIServer();
        String[] str = new String[]{"server", "dropwizard.yml"};
        server.run(str);
        WebUtils.tryOpenBrowser("http://localhost:8080/arbiter", log);    //TODO don't hardcode
        runner.addListeners(new UIOptimizationRunnerStatusListener(server));

        runner.execute();


        System.out.println("----- COMPLETE -----");
    }


    private static class MnistDataSetProvider implements DataProvider<DataSetIterator>{

        @Override
        public DataSetIterator trainData(Map<String, Object> dataParameters) {
            try{
                if(dataParameters == null || dataParameters.isEmpty()){
                    return new MnistDataSetIterator(64, 10000, false, true, true, 123);
                }
                if(dataParameters.containsKey("batchsize")){
                    int b = (Integer)dataParameters.get("batchsize");
                    return new MnistDataSetIterator(b, 10000, false, true, true, 123);
                }
                return new MnistDataSetIterator(64, 10000, false, true, true, 123);
            } catch(Exception e){
                throw new RuntimeException(e);
            }
        }

        @Override
        public DataSetIterator testData(Map<String, Object> dataParameters) {
            return trainData(dataParameters);
        }

        @Override
        public String toString(){
            return "MnistDataSetProvider()";
        }
    }
}
