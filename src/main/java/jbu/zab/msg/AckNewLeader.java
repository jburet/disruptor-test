package jbu.zab.msg;

public class AckNewLeader extends NetworkZabMessage{

    @Override
    public ZabMessageType getMessageType() {
        return ZabMessageType.ACK_NEW_LEADER;
    }
}
