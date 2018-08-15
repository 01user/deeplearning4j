/*******************************************************************************
 * Copyright (c) 2015-2018 Skymind, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.nd4j.parameterserver.distributed.v2.transport.impl;

import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.nd4j.linalg.exception.ND4JIllegalStateException;
import org.nd4j.linalg.primitives.Atomic;
import org.nd4j.linalg.primitives.Optional;
import org.nd4j.parameterserver.distributed.enums.MeshBuildMode;
import org.nd4j.parameterserver.distributed.v2.chunks.VoidChunk;
import org.nd4j.parameterserver.distributed.v2.messages.ResponseMessage;
import org.nd4j.parameterserver.distributed.v2.messages.RequestMessage;
import org.nd4j.parameterserver.distributed.v2.messages.VoidMessage;
import org.nd4j.parameterserver.distributed.v2.messages.INDArrayMessage;
import org.nd4j.parameterserver.distributed.v2.messages.impl.MeshUpdateMessage;
import org.nd4j.parameterserver.distributed.v2.messages.pairs.handshake.HandshakeRequest;
import org.nd4j.parameterserver.distributed.v2.messages.pairs.handshake.HandshakeResponse;
import org.nd4j.parameterserver.distributed.v2.transport.Transport;
import org.nd4j.parameterserver.distributed.v2.util.MeshOrganizer;
import org.nd4j.parameterserver.distributed.v2.util.MessageSplitter;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author raver119@gmail.com
 */
@Slf4j
public abstract  class BaseTransport  implements Transport {
    // this stream is for delivering messages from this host to other hosts in the network
    protected final MessageFlow<VoidMessage> outgoingFlow = new MessageFlow<>();

    // this stream is for receiving INDArray messages from the network
    protected final MessageFlow<INDArrayMessage> incomingFlow = new MessageFlow<>();

    // here we're storing reference to mesh
    protected final Atomic<MeshOrganizer> mesh = new Atomic<>();

    // this is Id of this Transport instance
    protected String id;

    // this is simple storage for replies
    protected final Map<String, ResponseMessage> replies = new ConcurrentHashMap<>();

    protected BaseTransport() {
        mesh.set(new MeshOrganizer(MeshBuildMode.SYMMETRIC_MODE));
    }

    @Override
    public Consumer<VoidMessage> outgoingConsumer() {
        return outgoingFlow;
    }

    @Override
    public Publisher<INDArrayMessage> incomingPublisher() {
        return incomingFlow;
    }

    @Override
    public synchronized void launch() {


        // first of all we introduce ourselves to master

        // this flow gets converted to VoidChunks and sent to upstream and downstreams
        val d = Flowable.fromPublisher(outgoingFlow).subscribe(voidMessage -> {
            if (mesh.get() == null) {
                log.warn("Mesh wasn't received yet!");
                return;
            }

            // we're tagging this message as originated locally
            voidMessage.setOriginatorId(id);

            // and propagating message across mesh network
            propagateMessage(voidMessage);
        });
    }

    @Override
    public void propagateMessage(@NonNull VoidMessage voidMessage) throws IOException {
        val node = mesh.get().getNodeById(id);
        val root = mesh.get().getRootNode();
        val upstream = node.getUpstreamNode();
        val downstreams = node.getDownstreamNodes();

        // if this is INDArrayMessage we'll split it into chunks
        if (voidMessage instanceof INDArrayMessage) {
            // TODO: make chunk size configurable
            val chunks = MessageSplitter.getInstance().split(voidMessage, 65536);
            // send chunks to the upstream
            if (!node.isRootNode())
                chunks.forEach(c -> sendMessage(c, upstream.getId()));

            // and send chunks to all downstreams
            downstreams.parallelStream().forEach(n -> {
                chunks.forEach(c -> sendMessage(c, n.getId()));
            });
        } else {
            // send message to the upstream
            if (!node.isRootNode())
                sendMessage(voidMessage, upstream.getId());

            // and send message for all downstreams
            downstreams.forEach(n -> sendMessage(voidMessage, n.getId()));
        }
    }

    /**
     * This method puts INDArray to the flow read by parameter server
     * @param message
     */
    private void forwardToParameterServer(INDArrayMessage message) {
        try {
            incomingFlow.accept(message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void processMessage(VoidMessage message) {
        /**
         * TODO: we need better isolation here
         */
        if (message instanceof VoidChunk) {
            // we merge chunks to get full INDArrayMessage
            Optional<INDArrayMessage> opt = MessageSplitter.getInstance().merge((VoidChunk) message);

            // if this chunk was the last message, we'll forward it to parameter server for actual use
            if (opt.isPresent())
                this.processMessage(opt.get());
        } else if (message instanceof INDArrayMessage) {
            // just forward message
            forwardToParameterServer((INDArrayMessage) message);
        } else if (message instanceof HandshakeRequest) {
            if (!mesh.get().isKnownNode(this.id())) {
                mesh.get().getRootNode().setId(this.id);
            }

            // first we add new node to the mesh
            mesh.get().addNode(message.getOriginatorId());

            // send response
            val response = new HandshakeResponse(0L, mesh.get());
            sendMessage(response, message.getOriginatorId());

            // update all other nodes with new mesh
            try {
                propagateMessage(new MeshUpdateMessage(mesh.get()));
            } catch (Exception e) {
                log.error("Wasn't able to propagate message from [{}]", id());
                throw new RuntimeException(e);
            }
        } else if (message instanceof HandshakeResponse) {
            if (mesh.get() == null)
                mesh.set(((HandshakeResponse) message).getMesh());
            else {
                // we update only if new mesh is older that existing one
                if (mesh.get().getVersion() < ((HandshakeResponse) message).getMesh().getVersion())
                    mesh.set(((HandshakeResponse) message).getMesh());
            }

         // this is default handler for message pairs
        } else if (message instanceof ResponseMessage) {
            // in this case we store message to the map, to be fetched later
            val reply = (ResponseMessage) message;
            replies.putIfAbsent(reply.getRequestId(), reply);

        } else if (message instanceof MeshUpdateMessage) {
            val newMesh = ((MeshUpdateMessage) message).getMesh();

            // first check shouldn't ever be the case
            if (mesh.get() == null)
                mesh.set(newMesh);
            else {
                // we update only if new mesh is older that existing one
                if (mesh.get().getVersion() < newMesh.getVersion())
                    mesh.set(newMesh);
            }
        } else {
            throw new ND4JIllegalStateException("Unknown message received: [" + message.getClass().getCanonicalName() + "]");
        }
    }

    @Override
    public <T extends ResponseMessage> T sendMessageBlocking(RequestMessage message, String id) throws InterruptedException {
        // we send message to the node first
        sendMessage(message, id);

        // and then we just block until we get response
        ResponseMessage r = null;
        while ((r = replies.get(message.getRequestId())) == null) {
            Thread.sleep(10);
        }

        //and return reply back
        return (T) r;
    }

    protected void handshake() {

    }

    /**
     * Generic Publisher/Consumer implementation for interconnect
     * @param <T>
     */
    public static class MessageFlow<T> implements Consumer<T>, Publisher<T> {
        private List<Subscriber<? super T>> subscribers = new CopyOnWriteArrayList<>();

        @Override
        public void accept(T voidMessage) throws Exception {
            // just propagate messages further away
            subscribers.forEach(s -> s.onNext(voidMessage));
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            // we're just maintaining list of
            subscribers.add(subscriber);
        }
    }
}
