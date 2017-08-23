//
// @author raver119@gmail.com
//

#include <graph/generated/node_generated.h>
#include <graph/generated/graph_generated.h>
#include <graph/generated/result_generated.h>

#include <Variable.h>
#include <VariableSpace.h>
#include <Node.h>
#include <GraphExecutioner.h>
#include <loops/scalar.h>
#include <loops/pairwise_transform.h>
#include <loops/transform.h>

namespace nd4j{
    namespace graph {

        /**
         *
         * @param graph
         * @param node
         * @param variableSpace
         * @return
         */
        template <typename T>
        static Nd4jStatus executeFlatNode(nd4j::graph::Graph<T> *graph, nd4j::graph::Node<T> *node, nd4j::graph::VariableSpace<T> *variableSpace) {
            OpType opType = node->opType();
            int opNum = node->opNum();

            nd4j_printf("Executing node_%i{%i}\n", node->id(), opNum);
            fflush(stdout);

            if (opType == OpType_TRANSFORM) {
                int in = node->input()->at(0);

                auto x = variableSpace->getVariable(in);
                auto z = x;

                // if node has only one input - that's regular TRANSFORM
                if (node->input()->size() == 1) {

                    // if output of previous node is used in different code branches - duplicate it

                    if (in > 0)
                        if (graph->getMapped()->at(in)->output()->size() > 1) {
                            auto array = new NDArray<T>(x->getNDArray());
                            z = new Variable<T>(array);
                            z->setName(node->getName());
                        };

                    functions::transform::Transform<T>::template exec(opNum, x->getNDArray()->_buffer,
                                                                      x->getNDArray()->_shapeInfo,
                                                                      z->getNDArray()->_buffer,
                                                                      z->getNDArray()->_shapeInfo, node->extraParams(),
                            // FIXME: for some cases we NEED these vars
                                                                      nullptr, nullptr);

                } else {
                    // otherwise that's PAIRWISE op

                    auto y = variableSpace->getVariable(node->input()->at(1));

                    if (node->output()->size() > 0) {
                        z = new Variable<T>(new NDArray<T>(x->getNDArray()));
                        z->setName(node->getName());
                    }


                    functions::pairwise_transforms::PairWiseTransform<T>::template exec(opNum, x->getNDArray()->_buffer, x->getNDArray()->_shapeInfo, y->getNDArray()->_buffer, y->getNDArray()->_shapeInfo,
                                                                                        z->getNDArray()->_buffer, z->getNDArray()->_shapeInfo, node->extraParams());
                }

                variableSpace->putVariable(node->id(), z);

                if (node->hasExternalOutputs()) {
                    for (int e = 0; e < node->output()->size(); e++) {
                        if (node->output()->at(e) >= 0)
                            continue;

                        auto out = variableSpace->getVariable(node->output()->at(e));

                        if (out->isEmpty()) {
                            out->setNDArray(z->getNDArray()->dup(z->getNDArray()->ordering()));
                        } else {
                            // assign output
                            if (out->getNDArray() != z->getNDArray())
                                out->getNDArray()->assign(z->getNDArray());
                        }
                    }
                }
            }  else if (opType == OpType_SCALAR) {
                int in = node->input()->at(0);

                auto x = variableSpace->getVariable(in);

                // if output of previous node is used in different code branches - duplicate it
                auto z = x;
                if (in > 0)
                    if (graph->getMapped()->at(in)->output()->size() > 1) {
                        auto array = new NDArray<T>(x->getNDArray());
                        z = new Variable<T>(array);
                        z->setName(node->getName());
                    };

                nd4j_verbose("xLength: %i\n", x->getNDArray()->lengthOf());
                nd4j_verbose("SCALAR BEFORE: X[0]: %f; X[1]: %f; scalar: %f\n", x->getNDArray()->getScalar(0), x->getNDArray()->getScalar(1), node->scalar());

                functions::scalar::ScalarTransform<T>::transform(opNum, x->getNDArray()->_buffer,
                                                                      x->getNDArray()->_shapeInfo,
                                                                      z->getNDArray()->_buffer,
                                                                      z->getNDArray()->_shapeInfo,
                                                                      node->scalar(),
                                                                      node->extraParams());

                nd4j_verbose("SCALAR AFTER: Z[0]: %f; Z[1]: %f; scalar: %f\n", z->getNDArray()->getScalar(0), z->getNDArray()->getScalar(1), node->scalar());

                variableSpace->putVariable(node->id(), z);

                if (node->hasExternalOutputs()) {
                    for (int e = 0; e < node->output()->size(); e++) {
                        if (node->output()->at(e) > 0)
                            continue;

                        auto out = variableSpace->getVariable(node->output()->at(e));

                        if (out->isEmpty()) {
                            out->setNDArray(z->getNDArray()->dup(z->getNDArray()->ordering()));
                        } else {
                            // assign output
                            if (out->getNDArray() != z->getNDArray())
                                out->getNDArray()->assign(z->getNDArray());
                        }
                    }
                }
            }else if (opType == OpType_SUMMARYSTATS) {
                auto x = variableSpace->getVariable(node->input()->at(0));

                auto z = x;
                // if there's no dimensions set - it's reduceToScalar
                if (node->getDimensions()->size() == 0 || (node->getDimensions()->size() == 1 && node->getDimensions()->at(0) == MAX_INT)) {
                    z = new Variable<T>(new NDArray<T>(1,1, 'c'));
                    z->setName(node->getName());
                    z->getNDArray()->_buffer[0] = functions::summarystats::SummaryStatsReduce<T>::template execScalar(opNum, node->scalar() != 0.0, x->getNDArray()->_buffer, x->getNDArray()->_shapeInfo, node->extraParams());

                } else {
                    // dimensional reduction
                    shape::TAD *tad = new shape::TAD(x->getNDArray()->_shapeInfo, node->getDimensionsPtr(), node->getDimensions()->size());
                    tad->createTadOnlyShapeInfo();
                    tad->createOffsets();

                    int resultLength = x->getNDArray()->lengthOf() / shape::length(tad->shapeInfoOnlyShapeAndStride());

                    z = new Variable<T>(new NDArray<T>(1, resultLength, 'c'));
                    z->setName(node->getName());

                    functions::summarystats::SummaryStatsReduce<T>::template exec(opNum, node->scalar() != 0.0, x->getNDArray()->_buffer, x->getNDArray()->_shapeInfo, node->extraParams(), z->getNDArray()->_buffer, z->getNDArray()->_shapeInfo,
                                                                            node->getDimensionsPtr() , node->getDimensions()->size());

                    delete tad;
                }

                variableSpace->putVariable(node->id(), z);

                if (node->hasExternalOutputs()) {
                    for (int e = 0; e < node->output()->size(); e++) {
                        if (node->output()->at(e) > 0)
                            continue;

                        auto out = variableSpace->getVariable(node->output()->at(e));

                        if (out->isEmpty()) {
                            out->setNDArray(z->getNDArray()->dup(z->getNDArray()->ordering()));
                        } else {
                            // assign output
                            if (out->getNDArray() != z->getNDArray())
                                out->getNDArray()->assign(z->getNDArray());
                        }
                    }
                }
            } else if (opType == OpType_ACCUMULATION) {
                auto x = variableSpace->getVariable(node->input()->at(0));
                auto z = x;

                //  regular accumulation with 1 argument
                if (node->input()->size() == 1) {
                    // if there's no dimensions set - it's reduceToScalar
                    if (node->getDimensions()->size() == 0 ||
                        (node->getDimensions()->size() == 1 && node->getDimensions()->at(0) == MAX_INT)) {
                        nd4j_verbose("ACCUM SCALAR BEFORE: X[0]: %f; X[1]: %f; xLength: %f\n",
                                     x->getNDArray()->getScalar(0), x->getNDArray()->getScalar(1),
                                     x->getNDArray()->lengthOf());

                        z = new Variable<T>(new NDArray<T>(1, 1, 'c'));
                        z->setName(node->getName());
                        z->getNDArray()->_buffer[0] = functions::reduce::ReduceFunction<T>::template execScalar(opNum,
                                                                                                                x->getNDArray()->_buffer,
                                                                                                                x->getNDArray()->_shapeInfo,
                                                                                                                node->extraParams());

                        nd4j_verbose("ACCUM SCALAR  AFTER: Z[0]: %f; xLength: %i;\n", z->getNDArray()->getScalar(0),
                                     x->getNDArray()->lengthOf());
                    } else {
                        // dimensional reduction
                        shape::TAD *tad = new shape::TAD(x->getNDArray()->_shapeInfo, node->getDimensionsPtr(),
                                                         node->getDimensions()->size());
                        tad->createTadOnlyShapeInfo();
                        tad->createOffsets();

                        int resultLength =
                                x->getNDArray()->lengthOf() / shape::length(tad->shapeInfoOnlyShapeAndStride());

                        z = new Variable<T>(new NDArray<T>(1, resultLength, 'c'));

                        functions::reduce::ReduceFunction<T>::template exec(opNum, x->getNDArray()->_buffer,
                                                                            x->getNDArray()->_shapeInfo,
                                                                            node->extraParams(),
                                                                            z->getNDArray()->_buffer,
                                                                            z->getNDArray()->_shapeInfo,
                                                                            node->getDimensionsPtr(),
                                                                            node->getDimensions()->size(),
                                                                            tad->tadOnlyShapeInfo, tad->tadOffsets);

                        delete tad;
                    }
                } else {
                    // otherwise we're on reduce3, and expect 2 inputs

                    auto y = variableSpace->getVariable(node->input()->at(1));

                    // if there's no dimensions set - it's reduceToScalar
                    if (node->getDimensions()->size() == 0 ||
                        (node->getDimensions()->size() == 1 && node->getDimensions()->at(0) == MAX_INT)) {
                        nd4j_verbose("ACCUM3 SCALAR BEFORE: X[0]: %f; X[1]: %f; xLength: %f\n",
                                     x->getNDArray()->getScalar(0), x->getNDArray()->getScalar(1),
                                     x->getNDArray()->lengthOf());

                        z = new Variable<T>(new NDArray<T>(1, 1, 'c'));
                        z->getNDArray()->_buffer[0] = functions::reduce3::Reduce3<T>::template execScalar(opNum,
                                                                                                                x->getNDArray()->_buffer,
                                                                                                                x->getNDArray()->_shapeInfo,
                                                                                                                node->extraParams(),
                                                                                                                y->getNDArray()->_buffer,
                                                                                                                y->getNDArray()->_shapeInfo);

                        nd4j_verbose("ACCUM3 SCALAR  AFTER: Z[0]: %f; xLength: %i;\n", z->getNDArray()->getScalar(0),
                                     x->getNDArray()->lengthOf());
                    } else {
                        // dimensional reduction
                        shape::TAD *tad = new shape::TAD(x->getNDArray()->_shapeInfo, node->getDimensionsPtr(),
                                                         node->getDimensions()->size());
                        tad->createTadOnlyShapeInfo();
                        tad->createOffsets();

                        int resultLength =
                                x->getNDArray()->lengthOf() / shape::length(tad->shapeInfoOnlyShapeAndStride());

                        z = new Variable<T>(new NDArray<T>(1, resultLength, 'c'));
                        z->setName(node->getName());
                        functions::reduce3::Reduce3<T>::template exec(opNum, x->getNDArray()->_buffer,
                                                                            x->getNDArray()->_shapeInfo,
                                                                            node->extraParams(),
                                                                            y->getNDArray()->_buffer,
                                                                            y->getNDArray()->_shapeInfo,
                                                                            z->getNDArray()->_buffer,
                                                                            z->getNDArray()->_shapeInfo,
                                                                            node->getDimensionsPtr(),
                                                                            node->getDimensions()->size(),
                                                                            tad->tadOnlyShapeInfo, tad->tadOffsets);

                        delete tad;
                    }
                }

                variableSpace->putVariable(node->id(), z);

                if (node->hasExternalOutputs()) {
                    for (int e = 0; e < node->output()->size(); e++) {
                        if (node->output()->at(e) > 0)
                            continue;

                        auto out = variableSpace->getVariable(node->output()->at(e));

                        if (out->isEmpty()) {
                            out->setNDArray(z->getNDArray()->dup(z->getNDArray()->ordering()));
                        } else {
                            // assign output
                            if (out->getNDArray() != z->getNDArray())
                                out->getNDArray()->assign(z->getNDArray());
                        }
                    }
                }
            } else if (opType == OpType_INDEX_ACCUMULATION) {

                auto x = variableSpace->getVariable(node->input()->at(0));

                auto z = x;
                // if there's no dimensions set - it's reduceToScalar
                if (node->getDimensions()->size() == 0 || (node->getDimensions()->size() == 1 && node->getDimensions()->at(0) == MAX_INT)) {
                    z = new Variable<T>(new NDArray<T>(1,1, 'c'));
                    z->setName(node->getName());
                    z->getNDArray()->_buffer[0] = (T) functions::indexreduce::IndexReduce<T>::template execScalar(opNum, x->getNDArray()->_buffer, x->getNDArray()->_shapeInfo, node->extraParams());

                } else {
                    // dimensional reduction
                    shape::TAD *tad = new shape::TAD(x->getNDArray()->_shapeInfo, node->getDimensionsPtr(), node->getDimensions()->size());
                    tad->createTadOnlyShapeInfo();
                    tad->createOffsets();

                    int resultLength = x->getNDArray()->lengthOf() / shape::length(tad->shapeInfoOnlyShapeAndStride());

                    z = new Variable<T>(new NDArray<T>(1, resultLength, 'c'));
                    z->setName(node->getName());
                    functions::indexreduce::IndexReduce<T>::template exec(opNum, x->getNDArray()->_buffer, x->getNDArray()->_shapeInfo, node->extraParams(), z->getNDArray()->_buffer, z->getNDArray()->_shapeInfo,
                                                                            node->getDimensionsPtr() , node->getDimensions()->size(),
                                                                            tad->tadOnlyShapeInfo, tad->tadOffsets);

                    delete tad;
                }

                variableSpace->putVariable(node->id(), z);

                if (node->hasExternalOutputs()) {
                    for (int e = 0; e < node->output()->size(); e++) {
                        if (node->output()->at(e) > 0)
                            continue;

                        auto out = variableSpace->getVariable(node->output()->at(e));

                        if (out->isEmpty()) {
                            out->setNDArray(z->getNDArray()->dup(z->getNDArray()->ordering()));
                        } else {
                            // assign output
                            if (out->getNDArray() != z->getNDArray())
                                out->getNDArray()->assign(z->getNDArray());
                        }
                    }
                }

            } else if (opType == OpType_BROADCAST) {
                auto x = variableSpace->getVariable(node->input()->at(0));
                auto y = variableSpace->getVariable(node->input()->at(1));

                auto z = x;

                auto tad = new shape::TAD(x->getNDArray()->_shapeInfo, node->getDimensionsPtr(), node->getDimensions()->size());
                tad->createTadOnlyShapeInfo();
                tad->createOffsets();

                functions::broadcast::Broadcast<T>::exec(opNum,
                                                             x->getNDArray()->_buffer, x->getNDArray()->_shapeInfo,
                                                             y->getNDArray()->_buffer, y->getNDArray()->_shapeInfo,
                                                             z->getNDArray()->_buffer, z->getNDArray()->_shapeInfo,
                                                             node->getDimensionsPtr(), node->getDimensions()->size(),
                                                             tad->tadOnlyShapeInfo, tad->tadOffsets,

                                                             // FIXME: this is bad. We have use case of different tads for broadcast
                                                             tad->tadOnlyShapeInfo, tad->tadOffsets);


                delete tad;

                variableSpace->putVariable(node->id(), z);

                if (node->hasExternalOutputs()) {
                    for (int e = 0; e < node->output()->size(); e++) {
                        if (node->output()->at(e) > 0)
                            continue;

                        auto out = variableSpace->getVariable(node->output()->at(e));

                        if (out->isEmpty()) {
                            out->setNDArray(z->getNDArray()->dup(z->getNDArray()->ordering()));
                        } else {
                            // assign output
                            if (out->getNDArray() != z->getNDArray())
                                out->getNDArray()->assign(z->getNDArray());
                        }
                    }
                }
            }

            return ND4J_STATUS_OK;
        }


        /**
         *
         * @param graph
         * @return
         */
        template <typename T>
        Nd4jStatus nd4j::graph::GraphExecutioner<T>::execute(nd4j::graph::Graph<T> *graph) {
            graph->buildGraph();
            auto __variableSpace = graph->getVariableSpace();

            bool pe = graph->getExecutorConfiguration()->_executionMode == ExecutionMode_AUTO;

            // we loop through op layers here
            for (int l = 0; l < graph->getOnion()->size(); l++) {
                int layerSize = graph->getOnion()->count(l) == 1 ? graph->getOnion()->at(l)->size() : 0;

#pragma omp parallel for if (layerSize > 1 && pe) schedule(dynamic) proc_bind(spread)
                for (int n = 0; n < layerSize; n++) {
                    auto node = graph->getOnion()->at(l)->at(n);

                    executeFlatNode(graph, node, __variableSpace);
                }
            }

            return ND4J_STATUS_OK;
        }


        template <typename T>
        Nd4jPointer nd4j::graph::GraphExecutioner<T>::executeFlatBuffer(Nd4jPointer pointer) {
            uint8_t *buffer = reinterpret_cast<uint8_t *>(pointer);

            nd4j_printf("Trying to restore graph\n", 0);

            auto restoredGraph = GetFlatGraph(buffer);

            nd4j_printf("Graph restored\n", 0);

            // converting FlatGraph to internal representation
            auto nativeGraph = new Graph<T>(restoredGraph);

            nd4j_printf("Going to execute graph\n", 0);

            // executing internal representation
            GraphExecutioner<T>::execute(nativeGraph);

            nd4j_printf("Building output...\n", 0);

            flatbuffers::FlatBufferBuilder builder(1024);

            // now, we'll prepare output, depending on given outputmode
            auto outputs = nativeGraph->fetchOutputs();
            std::vector<flatbuffers::Offset<FlatVariable>> variables_vector;

            for (int e = 0; e < outputs->size(); e++) {
                auto var = outputs->at(e);

                auto fShape = builder.CreateVector(var->getNDArray()->getShapeAsVector());
                auto fBuffer = builder.CreateVector(var->getNDArray()->getBufferAsVector());
                auto fName = builder.CreateString(*(var->getName()));

                auto fv = CreateFlatVariable(builder, var->id(), fName, fShape, fBuffer, -1);

                variables_vector.push_back(fv);
            }

            nd4j_printf("Returning %i variables back\n", variables_vector.size());

            auto varVectors = builder.CreateVector(variables_vector);
            auto result = CreateFlatResult(builder, restoredGraph->id(), varVectors);
            builder.Finish(result);

            // we might want to keep this graph for future
            delete nativeGraph;

            return (Nd4jPointer) builder.GetBufferPointer();
        }
    }
}