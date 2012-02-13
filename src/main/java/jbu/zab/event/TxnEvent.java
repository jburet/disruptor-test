package jbu.zab.event;

import com.lmax.disruptor.EventFactory;
import jbu.zab.transport.Peer;

import java.util.Set;

public class TxnEvent {

    private Set<Peer> currentPeer;

    // Epoch of txn
    private int epoch;

    // id of txn
    private int txnId;

    public Set<Peer> getCurrentPeer() {
        return currentPeer;
    }

    public void setCurrentPeer(Set<Peer> currentPeer) {
        this.currentPeer = currentPeer;
    }

    public int getEpoch() {
        return epoch;
    }

    public void setEpoch(int epoch) {
        this.epoch = epoch;
    }

    public int getTxnId() {
        return txnId;
    }

    public void setTxnId(int txnId) {
        this.txnId = txnId;
    }

    public final static EventFactory<TxnEvent> TXN_EVENT_FACTORY = new EventFactory<TxnEvent>() {
        public TxnEvent newInstance() {
            return new TxnEvent();
        }
    };


}
