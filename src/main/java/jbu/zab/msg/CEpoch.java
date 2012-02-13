package jbu.zab.msg;

public class CEpoch extends NetworkZabMessage {
    @Override
    public ZabMessageType getMessageType() {
        return ZabMessageType.C_EPOCH;
    }
}
