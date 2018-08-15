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

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.nd4j.parameterserver.distributed.v2.messages.VoidMessage;
import org.nd4j.parameterserver.distributed.v2.messages.pairs.handshake.HandshakeRequest;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

@Slf4j
public class DummyTransportTest {

    @Test
    public void testBasicConnection_1() throws Exception {
        val counter = new AtomicInteger(0);
        val connector = new DummyTransport.Connector();
        val transportA = new DummyTransport("alpha", connector);
        val transportB = new DummyTransport("beta", connector);

        connector.register(transportA, transportB);
        transportB.addInterceptor(HandshakeRequest.class, new DummyTransport.MessageCallable() {
            @Override
            public void apply(VoidMessage message) {
                // just increment
                counter.incrementAndGet();
            }
        });

        transportA.sendMessage(new HandshakeRequest(), "beta");

        // we expect that message was deliviered, and connector works
        assertEquals(1, counter.get());
    }
}