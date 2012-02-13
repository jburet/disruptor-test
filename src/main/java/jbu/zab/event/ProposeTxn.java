package jbu.zab.event;

import jbu.zab.Peer;
import jbu.zab.msg.ApplicationData;

import java.util.Set;

public class ProposeTxn {

    // Epoch of txn
    final private int epoch;

    // id of txn
    final private int txnId;

    // Payload
    final private ApplicationData applicationData;

    public ProposeTxn(int epoch, int txnId, ApplicationData applicationData) {
        this.epoch = epoch;
        this.txnId = txnId;
        this.applicationData = applicationData;
    }

    public int getEpoch() {
        return epoch;
    }

    public int getTxnId() {
        return txnId;
    }

    public ApplicationData getApplicationData() {
        return applicationData;
    }
}
