package jbu.zab;

import jbu.zab.msg.NetworkZabMessage;
import jbu.zab.msg.Propose;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * Contains all information of other peer and communication method
 */
public abstract class Peer {

    private UUID id;
    private InetAddress inetAddress;
    private int port;

    public Peer(UUID id, String host, int port) throws UnknownHostException {
        this.id = id;
        this.inetAddress = InetAddress.getByName(host);
        this.port = port;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Peer peer = (Peer) o;

        if (id != null ? !id.equals(peer.id) : peer.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    abstract void send(NetworkZabMessage networkZabMessage);
}
