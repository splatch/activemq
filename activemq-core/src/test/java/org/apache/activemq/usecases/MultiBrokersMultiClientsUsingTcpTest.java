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
package org.apache.activemq.usecases;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.apache.activemq.network.DemandForwardingBridge;
import org.apache.activemq.network.NetworkBridgeConfiguration;
import org.apache.activemq.transport.TransportFactory;

/**
 * @version $Revision: 1.1.1.1 $
 */
public class MultiBrokersMultiClientsUsingTcpTest extends MultiBrokersMultiClientsTest {
    protected List<DemandForwardingBridge> bridges;

    protected void bridgeAllBrokers(String groupName) throws Exception {
        for (int i = 1; i <= BROKER_COUNT; i++) {
            for (int j = 1; j <= BROKER_COUNT; j++) {
                if (i != j) {
                    bridgeBrokers("Broker" + i, "Broker" + j);
                }
            }
        }

        maxSetupTime = 5000;
    }

    protected void bridgeBrokers(BrokerService localBroker, BrokerService remoteBroker) throws Exception {
        List<TransportConnector> remoteTransports = remoteBroker.getTransportConnectors();
        List<TransportConnector> localTransports = localBroker.getTransportConnectors();

        URI remoteURI;
        URI localURI;
        if (!remoteTransports.isEmpty() && !localTransports.isEmpty()) {
            remoteURI = remoteTransports.get(0).getConnectUri();
            localURI = localTransports.get(0).getConnectUri();

            // Ensure that we are connecting using tcp
            if (remoteURI.toString().startsWith("tcp:") && localURI.toString().startsWith("tcp:")) {
                NetworkBridgeConfiguration config = new NetworkBridgeConfiguration();
                config.setBrokerName(localBroker.getBrokerName());
                DemandForwardingBridge bridge = new DemandForwardingBridge(config, TransportFactory.connect(localURI), TransportFactory.connect(remoteURI));
                bridge.setBrokerService(localBroker);
                bridges.add(bridge);

                bridge.start();
            } else {
                throw new Exception("Remote broker or local broker is not using tcp connectors");
            }
        } else {
            throw new Exception("Remote broker or local broker has no registered connectors.");
        }
    }

    public void setUp() throws Exception {
        super.setUp();

        // Assign a tcp connector to each broker
        int j = 0;
        for (Iterator<BrokerItem> i = brokers.values().iterator(); i.hasNext();) {
            i.next().broker.addConnector("tcp://localhost:" + (61616 + j++));
        }

        bridges = new ArrayList<DemandForwardingBridge>();
    }
}
