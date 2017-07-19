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

package org.deeplearning4j.scalnet.models

import org.deeplearning4j.nn.conf.inputs.InputType
import org.deeplearning4j.nn.conf.{MultiLayerConfiguration, NeuralNetConfiguration}
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.optimize.api.IterationListener
import org.deeplearning4j.scalnet.layers.{Layer, Node}
import org.deeplearning4j.scalnet.optimizers.Optimizer
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

/**
  * Simple DL4J-style sequential neural net architecture with one input
  * node and one output node for each node in computational graph.
  *
  * Wraps DL4J MultiLayerNetwork. Enforces DL4J model construction
  * pattern: adds pre-processing layers automatically but requires
  * user to specify output layer explicitly.
  *
  * @author David Kale
  */


class NeuralNet(val inputType: Option[InputType] = None, val rngSeed: Long = 0) extends Model {

  private val log: Logger = LoggerFactory.getLogger(this.getClass)

  def add(layer: Node): Unit = {
    layers = layers :+ layer
  }

  override def compile(lossFunction: LossFunction, optimizer: Optimizer = defaultOptimizer): Unit = {
    val builder = buildModelConfig(optimizer, rngSeed)
    buildOutput(lossFunction)

    var listBuilder: NeuralNetConfiguration.ListBuilder = builder.iterations(1).list()
    inputType foreach (i => listBuilder.setInputType(i))

    for ((layer, layerIndex) <- layers.zipWithIndex) {
      log.info("Layer " + layerIndex + ": " + layer.getClass.getSimpleName)
      listBuilder.layer(layerIndex, layer.asInstanceOf[Layer].compile)
    }

    listBuilder = listBuilder.pretrain(false).backprop(true)
    val conf: MultiLayerConfiguration = listBuilder.build()
    model = new MultiLayerNetwork(conf)
    model.init()
  }

  override def fit(iter: DataSetIterator, nbEpoch: Int = defaultEpochs, listeners: List[IterationListener]): Unit = {
    model.setListeners(listeners.asJavaCollection)
    for (epoch <- 0 until nbEpoch) {
      log.info("Epoch " + epoch)
      model.fit(iter)
    }
  }
}

object NeuralNet {
  def apply(inputType: InputType = null, rngSeed: Long = 0): NeuralNet = new NeuralNet(Option(inputType), rngSeed)
}