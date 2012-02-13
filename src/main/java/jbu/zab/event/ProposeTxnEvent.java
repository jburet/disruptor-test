package jbu.zab.event;

import com.lmax.disruptor.EventFactory;
import jbu.zab.msg.ApplicationData;

public class ProposeTxnEvent {

    // Epoch of txn
    private int epoch;

    // id of txn
    private int txnId;

    // Payload
    private ApplicationData applicationData;

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

    public ApplicationData getApplicationData() {
        return applicationData;
    }

    public void setApplicationData(ApplicationData applicationData) {
        this.applicationData = applicationData;
    }

    public final static EventFactory<ProposeTxnEvent> PROPOSE_TXN_EVENT_EVENT_FACTORY = new EventFactory<ProposeTxnEvent>() {
        public ProposeTxnEvent newInstance() {
            return new ProposeTxnEvent();
        }
    };

}
