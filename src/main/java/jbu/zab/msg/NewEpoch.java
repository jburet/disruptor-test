package jbu.zab.msg;

public class NewEpoch extends NetworkZabMessage {

    // Epoch and txnId of ack
    private int epoch;
    private int txnId;

    public NewEpoch(int epoch, int txnId) {
        this.epoch = epoch;
        this.txnId = txnId;
    }

    public int getEpoch() {
        return epoch;
    }

    public int getTxnId() {
        return txnId;
    }

    @Override
    public ZabMessageType getMessageType() {
        return ZabMessageType.NEW_EPOCH;
    }
}
