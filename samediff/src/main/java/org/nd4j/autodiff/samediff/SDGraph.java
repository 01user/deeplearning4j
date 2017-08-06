package org.nd4j.autodiff.samediff;

import com.google.common.base.Preconditions;
import com.rits.cloning.Cloner;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nd4j.autodiff.graph.Graph;
import org.nd4j.autodiff.graph.api.Edge;
import org.nd4j.autodiff.graph.api.Vertex;
import org.nd4j.autodiff.opstate.NDArrayInformation;
import org.nd4j.autodiff.opstate.OpExecAction;
import org.nd4j.autodiff.opstate.OpExecOrder;
import org.nd4j.autodiff.opstate.OpState;

import java.util.*;

/**
 * Graph data structure for tensors
 *
 * @author Adam Gibson
 */
@NoArgsConstructor
@Data
public class SDGraph extends Graph<NDArrayInformation,OpState> {

    protected SameDiff sameDiff;

    public SDGraph(SDGraph gradGraph) {
        setEdges(gradGraph.getEdges());
        setVertices(gradGraph.getVertices());
        setFrozen(gradGraph.isFrozen());
        setIncomingEdges(gradGraph.getIncomingEdges());
        setGraphApply(gradGraph.getGraphApply());
    }


    @Builder
    private SDGraph(boolean allowMultipleEdges,
                    Map<Integer, List<Edge<OpState>>> edges,
                    Map<Integer, Vertex<NDArrayInformation>> vertices,
                    boolean frozen,
                    Map<Integer, List<Edge<OpState>>> incomingEdges,
                    SameDiff sameDiff) {
        super(allowMultipleEdges, edges, vertices, frozen, incomingEdges);
        this.sameDiff = sameDiff;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Get the output vertices
     * @return
     */
    public List<NDArrayInformation> getOutputs() {
        List<NDArrayInformation> ret = new ArrayList<>();
        for (int i : getVertices().keySet()) {
            if (getEdgesOut(i).size() < 1)
                ret.add(getVertex(i).getValue());
        }

        return ret;
    }

    /**
     * Get the input vertices
     * @return
     */
    public List<NDArrayInformation> getInputs() {
        List<NDArrayInformation> ret = new ArrayList<>();
        for (int i : getVertices().keySet()) {
            if (getVertexInDegree(i) < 1)
                ret.add(getVertex(i).getValue());
        }

        return ret;
    }


    public SDGraph optimize() {
        OpExecOrder opOrder = getOpOrder();

        for (OpExecAction opExecAction : opOrder.getActions()) {

        }

        return this;
    }


    /**
     *
     * @return
     */
    public OpExecOrder getOpOrder() {
        int[] order = topologicalSort();
        List<OpExecAction> ret = new ArrayList<>();
        //iterate over op execution order skipping
        // nodes that are only inputs
        //the goal is to get all of the needed op executions
        for (int i = 0; i < order.length; i++) {
            //skip vertices that are only inputs
            if (getVertexInDegree(order[i]) < 1) {
                continue;
            }

            int numInputs = Math.max(1, getVertexInDegree(order[i]));
            int inputsCount = 0;
            NDArrayInformation[] inputs = new NDArrayInformation[numInputs];
            int[] inputIds = new int[numInputs];
            List<Edge<OpState>> inputOpStates = getIncomingEdges().get(order[i]);
            //get the inputs for this this output array
            for (Edge<OpState> edge : inputOpStates) {
                inputIds[inputsCount] = edge.getFrom();
                Preconditions.checkNotNull(getInformationFor(edge.getFrom()));
                inputs[inputsCount] = getInformationFor(edge.getFrom());
                inputsCount++;
            }

            Preconditions.checkState(inputsCount == numInputs, "Not all inputs were filled.");
            //add edges
            Edge<OpState> opStateEdge = inputOpStates.get(0);
            for(int j = 0; j < inputs.length; j++)
                Preconditions.checkNotNull(inputs[j],"Input " + j + " of edge " + opStateEdge.getFrom() + " -> " + opStateEdge.getTo() + " was null.");

            ret.add(OpExecAction.builder()
                    .output(opStateEdge.getValue().getResult())
                    .opState(opStateEdge.getValue())
                    .inputs(inputs)
                    .inputsIds(inputIds)
                    .outputId(order[i])
                    .build());

        }



        return OpExecOrder.builder().actions(ret).build();
    }

    /**
     * {@link NDArrayInformation}
     * accessor for a given vertex
     * @param vertex the vertex id
     * @return the information for the vertex
     */
    public NDArrayInformation getInformationFor(int vertex) {
        Vertex<NDArrayInformation> ndArrayInformation = getVertex(vertex);
        if(ndArrayInformation == null)
            return null;
        return ndArrayInformation.getValue();
    }


    /**
     * Topological sort over vertex ids
     * @return
     */
    public int[] topologicalSort() {
        LinkedList<Integer> noIncoming = new LinkedList<>();
        Map<Integer, Set<Integer>> inputEdges = new HashMap<>(); //key: vertex. Values: vertices that the key vertex receives input from
        Map<Integer, Set<Integer>> outputEdges = new HashMap<>(); //key: vertex. Values: vertices that the key vertex outputs to
        int[] ret = new int[numVertices()];
        Collection<Integer> vertices = getVertices().keySet();
        for (int i : vertices) {
            if (getVertexInDegree(i) < 1) {
                noIncoming.add(i);
            }

            List<Edge<OpState>> edges = getEdgesOut(i);
            Set<Integer> outVertices = new HashSet<>();
            Set<Integer> currInputs = new TreeSet<>();
            for (Edge<OpState> edge : edges) {
                outVertices.add(edge.getTo());
                Set<Integer> outputSetForInputIdx = outputEdges.get(i);
                if (outputSetForInputIdx == null) {
                    outputSetForInputIdx = new HashSet<>();
                    outputEdges.put(i, outputSetForInputIdx);
                }

                outputSetForInputIdx.add(edge.getTo()); //input vertex outputs to the current vertex
            }

            if( getIncomingEdges().get(i) != null) {
                for (Edge<OpState> edge : getIncomingEdges().get(i)) {
                    currInputs.add(edge.getFrom());

                }

                inputEdges.put(i, currInputs);
            }
            else
                inputEdges.put(i, currInputs);

        }

        int outCounter = 0;
        while (!noIncoming.isEmpty() && outCounter < ret.length) {
            int next = noIncoming.removeFirst();
            ret[outCounter++] = next;
            Set<Integer> vertexOutputsTo = outputEdges.get(next);
            //Remove edges next -> vertexOuputsTo[...] from graph;
            if (vertexOutputsTo != null) {
                for (Integer v : vertexOutputsTo) {
                    Set<Integer> set = inputEdges.get(v);
                    if (set != null)
                        set.remove(next);
                    if (set == null || set.isEmpty()) {
                        noIncoming.add(v); //No remaining edges for vertex i -> add to list for processing
                    }
                }
            }
        }

        //If any edges remain in the graph: graph has cycles:
        for (Map.Entry<Integer, Set<Integer>> entry : inputEdges.entrySet()) {
            Set<Integer> set = entry.getValue();
            if (set == null)
                continue;
            if (!set.isEmpty())
                throw new IllegalStateException("Graph has cycles");
        }

        return ret;
    }
}
