/*
 *
 *  * Copyright 2017 Skymind,Inc.
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

import org.deeplearning4j.nn.weights.WeightInit
import org.scalatest._
import org.deeplearning4j.scalnet.layers.{Dense, Layer}
import org.deeplearning4j.scalnet.layers.convolutional.Convolution2D
import org.deeplearning4j.scalnet.layers.pooling.MaxPooling2D
import org.deeplearning4j.scalnet.layers.reshaping.{Flatten3D, Unflatten3D}
import org.deeplearning4j.scalnet.regularizers.L2

/**
  * Created by maxpumperla on 29/06/17.
  */

class SequentialTest extends FunSpec with BeforeAndAfter {

  var model: Sequential = Sequential()
  val shape = 100
  val wrongInputShape = 10
  val nbRows: Int = 28
  val nbColumns: Int = 28
  val nbChannels: Int = 1
  val nbOutput: Int = 10
  val weightDecay: Double = 0.0005
  val momentum: Double = 0.9
  val learningRate: Double = 0.01

  before {
    model = new Sequential()
  }

  describe("A Sequential network") {

    it("without layers should produce a MatchError when compiled") {
      assertThrows[scala.MatchError] {
        model = new Sequential()
        model.compile(null)
      }
    }

    it("should infer the correct shape of an incorrectly initialized layer") {
      model.add(Dense(shape, shape))
      model.add(Dense(shape, wrongInputShape))
      assert(model.getLayers.last.inputShape == List(shape))
    }

    it("should propagate the correct shape of all layers and preprocessors") {
      model.add(Unflatten3D(List(nbRows, nbColumns, nbChannels), nIn = nbRows * nbColumns))
      model.add(Convolution2D(nFilter = 20, kernelSize = List(5, 5), stride = List(1, 1),
        weightInit = WeightInit.XAVIER, regularizer = L2(weightDecay)))
      model.add(MaxPooling2D(kernelSize = List(2, 2), stride = List(2, 2)))
      model.add(Convolution2D(nFilter = 50, kernelSize = List(5, 5), stride = List(1, 1),
        weightInit = WeightInit.XAVIER, regularizer = L2(weightDecay)))
      model.add(MaxPooling2D(kernelSize = List(2, 2), stride = List(2, 2)))
      model.add(Flatten3D())

      val preprocessorOutShapes = model.getPreprocessors.values.map(_.outputShape)
      assert(preprocessorOutShapes == List(List(nbRows, nbColumns, nbChannels), List(4 * 4 * 50)))

      val layerOutShapes = model.getLayers.map(_.outputShape)
      assert(layerOutShapes == List(List(24, 24, 20), List(12, 12, 20),
        List(8, 8, 50), List(4, 4, 50)))

    }
  }
}