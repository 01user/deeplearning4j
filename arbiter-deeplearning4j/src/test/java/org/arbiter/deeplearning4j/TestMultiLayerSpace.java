package org.arbiter.deeplearning4j;

import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.arbiter.deeplearning4j.layers.DenseLayerSpace;
import org.arbiter.deeplearning4j.layers.OutputLayerSpace;
import org.arbiter.optimize.distribution.DegenerateIntegerDistribution;
import org.arbiter.optimize.parameter.FixedValue;
import org.arbiter.optimize.parameter.continuous.ContinuousParameterSpace;
import org.arbiter.optimize.parameter.discrete.DiscreteParameterSpace;
import org.arbiter.optimize.parameter.integer.IntegerParameterSpace;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.junit.Test;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestMultiLayerSpace {

    @Test
    public void testBasic(){

        MultiLayerConfiguration expected = new NeuralNetConfiguration.Builder()
                .learningRate(0.005)
                .seed(12345)
                .list(3)
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

        MultiLayerConfiguration conf = mls.randomCandidate().getMultiLayerConfiguration();

        assertEquals(expected, conf);
    }

    @Test
    public void testBasic2(){

        MultiLayerSpace mls = new MultiLayerSpace.Builder()
                .learningRate(new ContinuousParameterSpace(0.0001,0.1))
                .regularization(true)
                .l2(new ContinuousParameterSpace(0.2,0.5))
                .addLayer(new DenseLayerSpace.Builder().nIn(10).nOut(10)
                        .activation(new DiscreteParameterSpace<>("relu","tanh"))
                        .build(),
                        new IntegerParameterSpace(1,3),true)    //1-3 identical layers
                .addLayer(new OutputLayerSpace.Builder().nIn(10).nOut(10)
                        .activation("softmax").build())
                .pretrain(false).backprop(true).build();

        int[] nLayerCounts = new int[3];
        int reluCount = 0;
        int tanhCount = 0;
        for( int i=0; i<50; i++ ){

            MultiLayerConfiguration conf = mls.randomCandidate().getMultiLayerConfiguration();
            assertEquals(false, conf.isPretrain());
            assertEquals(true, conf.isBackprop());

            int nLayers = conf.getConfs().size();
            assertTrue(nLayers >= 2 && nLayers <= 4);   //1-3 dense layers + 1 output layer: 2 to 4

            int nLayersExOutputLayer = nLayers - 1;
            nLayerCounts[nLayersExOutputLayer-1]++;

            for( int j=0; j<nLayers; j++ ){
                NeuralNetConfiguration layerConf = conf.getConf(j);

                double lr = layerConf.getLayer().getLearningRate();
                assertTrue(lr >= 0.0001 && lr <= 0.1);
                assertEquals(true, layerConf.isUseRegularization());
                double l2 = layerConf.getLayer().getL2();
                assertTrue( l2 >= 0.2 && l2 <= 0.5);

                if(j == nLayers-1){ //Output layer
                    assertEquals("softmax",layerConf.getLayer().getActivationFunction());
                } else {
                    String actFn = layerConf.getLayer().getActivationFunction();
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


}
