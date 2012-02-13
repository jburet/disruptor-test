package jbu.zab.transport;

import jbu.zab.Follower;
import jbu.zab.Leader;
import jbu.zab.msg.Ack;
import jbu.zab.msg.Commit;
import jbu.zab.msg.NetworkZabMessage;
import jbu.zab.msg.Propose;

import java.net.UnknownHostException;
import java.util.UUID;

public class VmPeer extends Peer {

    private Follower follower = null;
    private Leader leader = null;

    public VmPeer(UUID id)  {
        super(id);
    }

    public Follower getFollower() {
        return follower;
    }

    public void setFollower(Follower follower) {
        this.follower = follower;
    }

    public Leader getLeader() {
        return leader;
    }

    public void setLeader(Leader leader) {
        this.leader = leader;
    }

    @Override
    public void send(NetworkZabMessage networkZabMessage) {
        switch (networkZabMessage.getMessageType()) {
            case ACK:
                leader.receiveAck((Ack) networkZabMessage);
                break;
            case ACK_EPOCH:
                break;
            case ACK_NEW_LEADER:
                break;
            case C_EPOCH:
                break;
            case COMMIT:
                follower.receiveCommit((Commit) networkZabMessage);
                break;
            case COMMIT_LEADER:
                break;
            case NEW_EPOCH:
                break;
            case NEW_LEADER:
                break;
            case PROPOSE:
                follower.receivePropose((Propose) networkZabMessage);
                break;


        }
    }
}
