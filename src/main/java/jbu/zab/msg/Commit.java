package jbu.zab.msg;

public class Commit extends NetworkZabMessage {

    // Epoch and txnId of ack
    private int epoch;
    private int txnId;

    public Commit(int epoch, int txnId) {
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
        return ZabMessageType.COMMIT;
    }
}
