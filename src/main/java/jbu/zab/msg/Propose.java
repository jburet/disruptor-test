package jbu.zab.msg;

public class Propose extends NetworkZabMessage {


    // Epoch and txnId of ack
    private int epoch;
    private int txnId;
    private ApplicationData applicationData;

    public Propose(int epoch, int txnId, ApplicationData applicationData) {
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

    @Override
    public ZabMessageType getMessageType() {
        return ZabMessageType.PROPOSE;
    }
}
