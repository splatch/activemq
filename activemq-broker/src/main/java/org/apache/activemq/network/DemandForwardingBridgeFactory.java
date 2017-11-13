package org.apache.activemq.network;

import org.apache.activemq.transport.Transport;

/**
 * Encapsulation of bridge creation logic which are created on the fly.
 */
public interface DemandForwardingBridgeFactory {

    /**
     * Create a network bridge between two specified transports.
     *
     * @param configuration Bridge configuration.
     * @param localTransport Local side of bridge.
     * @param remoteTransport Remote side of bridge.
     * @param listener Bridge listener.
     * @return the NetworkBridge
     */
    DemandForwardingBridge createBridge(NetworkBridgeConfiguration configuration, Transport localTransport, Transport remoteTransport, final NetworkBridgeListener listener);

}
