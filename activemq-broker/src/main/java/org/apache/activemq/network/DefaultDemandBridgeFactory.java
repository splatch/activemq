package org.apache.activemq.network;

import org.apache.activemq.transport.Transport;

/**
 * Default implementation of demanded bridge facotry.
 */
public class DefaultDemandBridgeFactory implements DemandForwardingBridgeFactory {

    @Override
    public DemandForwardingBridge createBridge(NetworkBridgeConfiguration configuration, Transport localTransport, Transport remoteTransport, NetworkBridgeListener listener) {
        if (configuration.isConduitSubscriptions()) {
            // dynamicOnly determines whether durables are auto bridged
            return attachListener(new DurableConduitBridge(configuration, localTransport, remoteTransport), listener);
        }
        return attachListener(new DemandForwardingBridge(configuration, localTransport, remoteTransport), listener);
    }

    private DemandForwardingBridge attachListener(DemandForwardingBridge bridge, NetworkBridgeListener listener) {
        if (listener != null) {
            bridge.setNetworkBridgeListener(listener);
        }
        return bridge;
    }

}
