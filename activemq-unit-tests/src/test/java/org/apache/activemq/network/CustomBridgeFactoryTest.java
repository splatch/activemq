/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.network;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.command.Message;
import org.apache.activemq.transport.Transport;
import org.apache.activemq.xbean.BrokerFactoryBean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.debugging.LoggingListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javax.jms.Connection;
import javax.jms.Session;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class CustomBridgeFactoryTest {

    private BrokerService localBroker;
    private BrokerService remoteBroker;
    private Connection localConnection;
    private Connection remoteConnection;
    private Session localSession;
    private Session remoteSession;

    @Test
    public void listenerCalled() {
        NetworkConnector connector = localBroker.getNetworkConnectors().get(0);

        assertTrue(connector.getBridgeFactory() instanceof CustomNetworkBridgeFactory);
    }

    @Before
    public void setUp() throws Exception {
        doSetUp(true);
    }

    @After
    public void tearDown() throws Exception {
        doTearDown();
    }

    protected void doTearDown() throws Exception {
        localConnection.close();
        remoteConnection.close();
        localBroker.stop();
        remoteBroker.stop();
    }

    protected void doSetUp(boolean deleteAllMessages) throws Exception {
        remoteBroker = createRemoteBroker();
        remoteBroker.setDeleteAllMessagesOnStartup(deleteAllMessages);
        remoteBroker.start();
        remoteBroker.waitUntilStarted();
        localBroker = createLocalBroker();
        localBroker.setDeleteAllMessagesOnStartup(deleteAllMessages);
        localBroker.start();
        localBroker.waitUntilStarted();
        URI localURI = localBroker.getVmConnectorURI();
        ActiveMQConnectionFactory fac = new ActiveMQConnectionFactory(localURI);
        fac.setAlwaysSyncSend(true);
        fac.setDispatchAsync(false);
        localConnection = fac.createConnection();
        localConnection.setClientID("clientId");
        localConnection.start();
        URI remoteURI = remoteBroker.getVmConnectorURI();
        fac = new ActiveMQConnectionFactory(remoteURI);
        remoteConnection = fac.createConnection();
        remoteConnection.setClientID("clientId");
        remoteConnection.start();
        localSession = localConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        remoteSession = remoteConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    protected String getRemoteBrokerURI() {
        return "org/apache/activemq/network/remoteBroker.xml";
    }

    protected String getLocalBrokerURI() {
        return "org/apache/activemq/network/localBroker-custom-factory.xml";
    }

    protected BrokerService createBroker(String uri) throws Exception {
        Resource resource = new ClassPathResource(uri);
        BrokerFactoryBean factory = new BrokerFactoryBean(resource);
        resource = new ClassPathResource(uri);
        factory = new BrokerFactoryBean(resource);
        factory.afterPropertiesSet();
        BrokerService result = factory.getBroker();
        return result;
    }

    protected BrokerService createLocalBroker() throws Exception {
        return createBroker(getLocalBrokerURI());
    }

    protected BrokerService createRemoteBroker() throws Exception {
        return createBroker(getRemoteBrokerURI());
    }

    static class CustomNetworkBridgeFactory implements BridgeFactory {

        private final NetworkBridgeListener listener;

        CustomNetworkBridgeFactory() {
            this(Mockito.mock(NetworkBridgeListener.class));
        }

        CustomNetworkBridgeFactory(NetworkBridgeListener listener) {
            this.listener = listener;
        }

        public NetworkBridgeListener getListener() {
            return listener;
        }

        @Override
        public DemandForwardingBridge createNetworkBridge(NetworkBridgeConfiguration configuration, Transport localTransport, Transport remoteTransport, NetworkBridgeListener listener) {
            DemandForwardingBridge bridge = new DemandForwardingBridge(configuration, localTransport, remoteTransport);
            bridge.setNetworkBridgeListener(new CompositeNetworkBridgeListener(this.listener, listener, new LoggingBridgeListener()));
            return bridge;
        }
    }

    static class CompositeNetworkBridgeListener implements NetworkBridgeListener {

        private final List<NetworkBridgeListener> listeners;

        public CompositeNetworkBridgeListener(NetworkBridgeListener ... wrapped) {
            this.listeners = Arrays.asList(wrapped);
        }

        @Override
        public void bridgeFailed() {
            for (NetworkBridgeListener listener : listeners) {
                listener.bridgeFailed();
            }
        }

        @Override
        public void onStart(NetworkBridge bridge) {
            for (NetworkBridgeListener listener : listeners) {
                listener.onStart(bridge);
            }
        }

        @Override
        public void onStop(NetworkBridge bridge) {
            for (NetworkBridgeListener listener : listeners) {
                listener.onStop(bridge);
            }
        }

        @Override
        public void onOutboundMessage(NetworkBridge bridge, Message message) {
            for (NetworkBridgeListener listener : listeners) {
                listener.onOutboundMessage(bridge, message);
            }
        }

        @Override
        public void onInboundMessage(NetworkBridge bridge, Message message) {
            for (NetworkBridgeListener listener : listeners) {
                listener.onInboundMessage(bridge, message);
            }
        }
    }

    static class LoggingBridgeListener implements NetworkBridgeListener {

        @Override
        public void bridgeFailed() {
            System.out.println("Bridge failed");
        }

        @Override
        public void onStart(NetworkBridge bridge) {
            System.out.println("Bridge started " + bridge);
        }

        @Override
        public void onStop(NetworkBridge bridge) {
            System.out.println("Bridge stopped " + bridge);
        }

        @Override
        public void onOutboundMessage(NetworkBridge bridge, Message message) {
            System.out.println("Bridge outbound message " + bridge + " " + message);
        }

        @Override
        public void onInboundMessage(NetworkBridge bridge, Message message) {
            System.out.println("Bridge inbound message " + bridge + " " + message);
        }
    }

}
