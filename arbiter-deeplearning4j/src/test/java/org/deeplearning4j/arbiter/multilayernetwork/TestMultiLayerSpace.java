/*-
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
package org.deeplearning4j.arbiter.multilayernetwork;

import org.deeplearning4j.arbiter.DL4JConfiguration;
import org.deeplearning4j.arbiter.MultiLayerSpace;
import org.deeplearning4j.arbiter.layers.*;
import org.deeplearning4j.arbiter.optimize.api.Candidate;
import org.deeplearning4j.arbiter.optimize.api.CandidateGenerator;
import org.deeplearning4j.arbiter.optimize.api.ParameterSpace;
import org.deeplearning4j.arbiter.optimize.api.data.DataProvider;
import org.deeplearning4j.arbiter.optimize.api.data.DataSetIteratorProvider;
import org.deeplearning4j.arbiter.optimize.api.saving.ResultSaver;
import org.deeplearning4j.arbiter.optimize.api.score.ScoreFunction;
import org.deeplearning4j.arbiter.optimize.api.termination.MaxCandidatesCondition;
import org.deeplearning4j.arbiter.optimize.api.termination.TerminationCondition;
import org.deeplearning4j.arbiter.optimize.candidategenerator.RandomSearchGenerator;
import org.deeplearning4j.arbiter.optimize.config.OptimizationConfiguration;
import org.deeplearning4j.arbiter.optimize.parameter.FixedValue;
import org.deeplearning4j.arbiter.optimize.parameter.continuous.ContinuousParameterSpace;
import org.deeplearning4j.arbiter.optimize.parameter.discrete.DiscreteParameterSpace;
import org.deeplearning4j.arbiter.optimize.parameter.integer.IntegerParameterSpace;
import org.deeplearning4j.arbiter.optimize.runner.IOptimizationRunner;
import org.deeplearning4j.arbiter.optimize.runner.LocalOptimizationRunner;
import org.deeplearning4j.arbiter.saver.local.multilayer.LocalMultiLayerNetworkSaver;
import org.deeplearning4j.arbiter.scoring.multilayer.TestSetAccuracyScoreFunction;
import org.deeplearning4j.arbiter.task.MultiLayerNetworkTaskCreator;
import org.deeplearning4j.arbiter.util.LeafUtils;
import org.deeplearning4j.datasets.iterator.ExistingDataSetIterator;
import org.deeplearning4j.nn.conf.ConvolutionMode;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.conf.layers.variational.BernoulliReconstructionDistribution;
import org.deeplearning4j.nn.conf.layers.variational.GaussianReconstructionDistribution;
import org.deeplearning4j.nn.conf.layers.variational.ReconstructionDistribution;
import org.deeplearning4j.nn.conf.layers.variational.VariationalAutoencoder;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.junit.Test;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.ILossFunction;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;
import org.nd4j.linalg.lossfunctions.impl.LossMCXENT;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class TestMultiLayerSpace {

    @Test
    public void testBasic(){

        MultiLayerConfiguration expected = new NeuralNetConfiguration.Builder()
                .learningRate(0.005)
                .seed(12345)
                .list()
                .layer(0, new DenseLayer.Builder().nIn(10).nOut(10).build())
                .layer(1, new DenseLayer.Builder().nIn(10).nOut(10).build())
                .layer(2, new OutputLayer.Builder().lossFunction(LossFunction.MCXENT).nIn(10).nOut(5).build())
                .backprop(true).pretrain(false)
                .build();

        MultiLayerSpace mls = new MultiLayerSpace.Builder()
                .learningRate(0.005)
                .seed(12345)
                .addLayer(new DenseLayerSpace.Builder().nIn(10).nOut(10).build(), new FixedValue<>(2), true) //2 identical layers
                .addLayer(new OutputLayerSpace.Builder().lossFunction(LossFunction.MCXENT).nIn(10).nOut(5).build())
                .backprop(true).pretrain(false)
                .build();

        int nParams = mls.numParameters();
        assertEquals(0,nParams);

        MultiLayerConfiguration conf = mls.getValue(new double[0]).getMultiLayerConfiguration();

        assertEquals(expected, conf);
    }

    @Test
    public void testILossFunctionGetsSet() {
        ILossFunction lossFunction = new LossMCXENT(Nd4j.create(new float[]{1f, 2f}));

        MultiLayerConfiguration expected = new NeuralNetConfiguration.Builder()
                .learningRate(0.005)
                .seed(12345)
                .list()
                .layer(0, new DenseLayer.Builder().nIn(10).nOut(10).build())
                .layer(1, new DenseLayer.Builder().nIn(10).nOut(10).build())
                .layer(2, new OutputLayer.Builder().lossFunction(lossFunction).nIn(10).nOut(5).build())
                .backprop(true).pretrain(false)
                .build();

        MultiLayerSpace mls = new MultiLayerSpace.Builder()
                .learningRate(0.005)
                .seed(12345)
                .addLayer(new DenseLayerSpace.Builder().nIn(10).nOut(10).build(), new FixedValue<>(2), true) //2 identical layers
                .addLayer(new OutputLayerSpace.Builder().iLossFunction(lossFunction).nIn(10).nOut(5).build())
                .backprop(true).pretrain(false)
                .build();

        int nParams = mls.numParameters();
        assertEquals(0,nParams);

        MultiLayerConfiguration conf = mls.getValue(new double[0]).getMultiLayerConfiguration();

        assertEquals(expected, conf);
    }

    @Test
    public void testBasic2(){

        MultiLayerSpace mls = new MultiLayerSpace.Builder()
                .learningRate(new ContinuousParameterSpace(0.0001,0.1))
                .regularization(true)
                .l2(new ContinuousParameterSpace(0.2,0.5))
                .convolutionMode(ConvolutionMode.Same)
                .addLayer(new ConvolutionLayerSpace.Builder().nIn(3).nOut(3).kernelSize(2,2).stride(1,1).build())
                .addLayer(new DenseLayerSpace.Builder().nIn(10).nOut(10)
                        .activation(new DiscreteParameterSpace<>(Activation.RELU,Activation.TANH))
                        .build(),
                        new IntegerParameterSpace(1,3),true)    //1-3 identical layers
                .addLayer(new OutputLayerSpace.Builder().nIn(10).nOut(10)
                        .activation(Activation.SOFTMAX).build())
                .pretrain(false).backprop(true).build();

        int nParams = mls.numParameters();
        assertEquals(4,nParams);

        //Assign numbers to each leaf ParameterSpace object (normally done by candidate generator - manual here for testing)
        List<ParameterSpace> noDuplicatesList = LeafUtils.getUniqueObjects(mls.collectLeaves());

        //Second: assign each a number
        int c=0;
        for( ParameterSpace ps : noDuplicatesList){
            int np = ps.numParameters();
            if(np == 1){
                ps.setIndices(c++);
            } else {
                int[] values = new int[np];
                for( int j=0; j<np; j++ ) values[c++] = j;
                ps.setIndices(values);
            }
        }


        int[] nLayerCounts = new int[3];
        int reluCount = 0;
        int tanhCount = 0;

        Random r = new Random(12345);

        for( int i=0; i<50; i++ ){

            double[] rvs = new double[nParams];
            for( int j=0; j<rvs.length; j++ ) rvs[j] = r.nextDouble();


            MultiLayerConfiguration conf = mls.getValue(rvs).getMultiLayerConfiguration();
            assertEquals(false, conf.isPretrain());
            assertEquals(true, conf.isBackprop());

            int nLayers = conf.getConfs().size();
            assertTrue(nLayers >= 3 && nLayers <= 5);   //1 conv + 1-3 dense layers + 1 output layer: 2 to 4

            int nLayersExOutputLayer = nLayers - 1;
            nLayerCounts[nLayersExOutputLayer-2]++;

            for( int j=0; j<nLayers; j++ ){
                NeuralNetConfiguration layerConf = conf.getConf(j);

                double lr = layerConf.getLayer().getLearningRate();
                assertTrue(lr >= 0.0001 && lr <= 0.1);
                assertEquals(true, layerConf.isUseRegularization());
                double l2 = layerConf.getLayer().getL2();
                assertTrue( l2 >= 0.2 && l2 <= 0.5);

                if(j == nLayers-1) { //Output layer
                    assertEquals("softmax", layerConf.getLayer().getActivationFn().toString());
                } else if(j == 0){
                    //Conv layer
                    ConvolutionLayer cl = (ConvolutionLayer) layerConf.getLayer();
                    assertEquals(3, cl.getNIn());
                    assertEquals(3, cl.getNOut());
                    assertEquals(ConvolutionMode.Same, cl.getConvolutionMode());
                } else {
                    String actFn = layerConf.getLayer().getActivationFn().toString();
                    assertTrue("relu".equals(actFn) || "tanh".equals(actFn));
                    if("relu".equals(actFn)) reluCount++;
                    else tanhCount++;
                }
            }
        }

        for( int i=0; i<3; i++ ){
            assertTrue(nLayerCounts[i] >= 5);    //Expect approx equal (50/3 each), but some variation randomly
        }

        System.out.println("Number of layers: " + Arrays.toString(nLayerCounts));
        System.out.println("ReLU vs. Tanh: " + reluCount + "\t" + tanhCount);

    }

    @Test
    public void testGlobalPoolingBasic(){

        MultiLayerConfiguration expected = new NeuralNetConfiguration.Builder()
                .learningRate(0.005)
                .seed(12345)
                .list()
                .layer(0, new GravesLSTM.Builder().nIn(10).nOut(10).build())
                .layer(1, new GlobalPoolingLayer.Builder().poolingType(PoolingType.SUM).pnorm(7).build())
                .layer(2, new OutputLayer.Builder().lossFunction(LossFunction.MCXENT).nIn(10).nOut(5).build())
                .backprop(true).pretrain(false)
                .build();

        MultiLayerSpace mls = new MultiLayerSpace.Builder()
                .learningRate(0.005)
                .seed(12345)
                .addLayer(new GravesLSTMLayerSpace.Builder().nIn(10).nOut(10).build())
                .addLayer(new GlobalPoolingLayerSpace.Builder().poolingType(PoolingType.SUM).pNorm(7).build())
                .addLayer(new OutputLayerSpace.Builder().lossFunction(LossFunction.MCXENT).nIn(10).nOut(5).build())
                .backprop(true).pretrain(false)
                .build();

        int nParams = mls.numParameters();
        assertEquals(0,nParams);

        MultiLayerConfiguration conf = mls.getValue(new double[0]).getMultiLayerConfiguration();

        assertEquals(expected, conf);
    }


    @Test
    public void testVariationalAutoencoderLayerSpaceBasic(){
        MultiLayerSpace mls = new MultiLayerSpace.Builder()
                .learningRate(0.005)
                .seed(12345)
                .addLayer(new VariationalAutoencoderLayerSpace.Builder()
                        .nIn(new IntegerParameterSpace(50,75)).nOut(200)
                        .encoderLayerSizes(234,567)
                        .decoderLayerSizes(123,456)
                        .reconstructionDistribution(new DiscreteParameterSpace<ReconstructionDistribution>(
                                new GaussianReconstructionDistribution(),
                                new BernoulliReconstructionDistribution()))
                        .build()
                )
                .backprop(false).pretrain(true)
                .build();

        int numParams = mls.numParameters();

        //Assign numbers to each leaf ParameterSpace object (normally done by candidate generator - manual here for testing)
        List<ParameterSpace> noDuplicatesList = LeafUtils.getUniqueObjects(mls.collectLeaves());

        //Second: assign each a number
        int c=0;
        for( ParameterSpace ps : noDuplicatesList){
            int np = ps.numParameters();
            if(np == 1){
                ps.setIndices(c++);
            } else {
                int[] values = new int[np];
                for( int j=0; j<np; j++ ) values[c++] = j;
                ps.setIndices(values);
            }
        }

        double[] zeros = new double[numParams];

        DL4JConfiguration configuration = mls.getValue(zeros);

        MultiLayerConfiguration conf = configuration.getMultiLayerConfiguration();
        assertEquals(1, conf.getConfs().size());

        NeuralNetConfiguration nnc = conf.getConf(0);
        VariationalAutoencoder vae = (VariationalAutoencoder) nnc.getLayer();

        assertEquals(50, vae.getNIn());
        assertEquals(200, vae.getNOut());

        assertArrayEquals(new int[]{234,567}, vae.getEncoderLayerSizes());
        assertArrayEquals(new int[]{123,456}, vae.getDecoderLayerSizes());

        assertTrue( vae.getOutputDistribution() instanceof GaussianReconstructionDistribution);



        double[] ones = new double[numParams];
        for( int i=0; i<ones.length; i++ ) ones[i] = 1.0;

        configuration = mls.getValue(ones);

        conf = configuration.getMultiLayerConfiguration();
        assertEquals(1, conf.getConfs().size());

        nnc = conf.getConf(0);
        vae = (VariationalAutoencoder) nnc.getLayer();

        assertEquals(75, vae.getNIn());
        assertEquals(200, vae.getNOut());

        assertArrayEquals(new int[]{234,567}, vae.getEncoderLayerSizes());
        assertArrayEquals(new int[]{123,456}, vae.getDecoderLayerSizes());

        assertTrue( vae.getOutputDistribution() instanceof BernoulliReconstructionDistribution);
    }

    @Test
    public void testInputTypeBasic(){

        ParameterSpace<Integer> layerSizeHyperparam = new IntegerParameterSpace(20,60);

        MultiLayerSpace hyperparameterSpace = new MultiLayerSpace.Builder()
                .regularization(true)
                .l2(0.0001)
                .weightInit(WeightInit.XAVIER)
                .updater(Updater.NESTEROVS).momentum(0.9)
                .addLayer(new ConvolutionLayerSpace.Builder()
                        .kernelSize(5, 5)
                        .nIn(1)
                        .stride(1, 1)
                        .nOut(layerSizeHyperparam)
                        .activation(Activation.IDENTITY)
                        .build())
                .addLayer(new SubsamplingLayerSpace.Builder()
                        .poolingType(SubsamplingLayer.PoolingType.MAX)
                        .kernelSize(2,2)
                        .stride(2,2)
                        .build())
                .addLayer(new ConvolutionLayerSpace.Builder()
                        .kernelSize(5, 5)
                        //Note that nIn need not be specified in later layers
                        .stride(1, 1)
                        .nOut(50)
                        .activation(Activation.IDENTITY)
                        .build())
                .addLayer(new SubsamplingLayerSpace.Builder()
                        .poolingType(SubsamplingLayer.PoolingType.MAX)
                        .kernelSize(2,2)
                        .stride(2,2)
                        .build())
                .addLayer(new DenseLayerSpace.Builder().activation(Activation.RELU)
                        .nOut(500).build())
                .addLayer(new OutputLayerSpace.Builder()
                        .lossFunction(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .nOut(10)
                        .activation(Activation.SOFTMAX)
                        .build())
                .setInputType(InputType.convolutionalFlat(28, 28, 1))
                .backprop(true).pretrain(false).build();


        DataSetIterator mnistTrain = new ExistingDataSetIterator(
                Collections.singletonList(new DataSet( Nd4j.create(1, 1, 28, 28), Nd4j.create(10) )));

        DataSetIterator mnistTest = new ExistingDataSetIterator(
                Collections.singletonList(new DataSet( Nd4j.create(1, 1, 28, 28), Nd4j.create(10) )));

        DataProvider<Object> dataProvider = new DataSetIteratorProvider(mnistTrain, mnistTest);

        String baseSaveDirectory = "arbiterExample2/";
        File f = new File(baseSaveDirectory);
        if(f.exists()) f.delete();
        f.mkdir();
        ResultSaver<DL4JConfiguration,MultiLayerNetwork,Object> modelSaver =
                new LocalMultiLayerNetworkSaver<>(baseSaveDirectory);

        ScoreFunction<MultiLayerNetwork,Object> scoreFunction = new TestSetAccuracyScoreFunction();

        int maxCandidates = 4;
        TerminationCondition[] terminationConditions;
        terminationConditions = new TerminationCondition[]{new MaxCandidatesCondition(maxCandidates)};

        //Given these configuration options, let's put them all together:
        OptimizationConfiguration<DL4JConfiguration, MultiLayerNetwork, Object, Object> configuration
                = new OptimizationConfiguration.Builder<DL4JConfiguration, MultiLayerNetwork, Object, Object>()
                .candidateGenerator(new RandomSearchGenerator<>(hyperparameterSpace,null))
                .dataProvider(dataProvider)
                .modelSaver(modelSaver)
                .scoreFunction(scoreFunction)
                .terminationConditions(terminationConditions)
                .build();

        IOptimizationRunner<DL4JConfiguration,MultiLayerNetwork,Object> runner
                = new LocalOptimizationRunner<>(configuration, new MultiLayerNetworkTaskCreator<>());
        runner.execute();

        assertEquals(maxCandidates, runner.getResults().size());
    }


    @Test
    public void testSameRanges(){

        ParameterSpace<Double> l1Hyperparam = new ContinuousParameterSpace(0.001, 0.1);
        ParameterSpace<Double> l2Hyperparam = new ContinuousParameterSpace(0.001, 0.1);

        MultiLayerSpace hyperparameterSpace = new MultiLayerSpace.Builder()
                .addLayer(new DenseLayerSpace.Builder().nIn(10).nOut(10).build())
                .l1(l1Hyperparam)
                .l2(l2Hyperparam)
                .build();

        CandidateGenerator c = new RandomSearchGenerator<>(hyperparameterSpace,null);

        Candidate candidate = c.getCandidate();
    }
}
