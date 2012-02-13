package jbu.zab.event;

import jbu.zab.Peer;

import java.util.Set;

public class Txn {
    // Peers implied in txn
    final private Set<Peer> currentPeer;

    // Epoch of txn
    final private int epoch;

    // id of txn
    final private int txnId;

    public Txn(Set<Peer> currentPeer, int epoch, int txnId) {
        this.currentPeer = currentPeer;
        this.epoch = epoch;
        this.txnId = txnId;
    }

    public Set<Peer> getCurrentPeer() {
        return currentPeer;
    }

    public int getEpoch() {
        return epoch;
    }

    public int getTxnId() {
        return txnId;
    }
}
