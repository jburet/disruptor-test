package jbu.zab.event;

import com.lmax.disruptor.EventFactory;
import jbu.zab.msg.*;

public class MsgEvent<T extends ZabMessage> {

    private T msg;

    public T getMsg() {
        return this.msg;
    }

    public void setMsg(T msg) {
        this.msg = msg;
    }

    public final static EventFactory<MsgEvent<CEpoch>> CEPOCH_EVENT_FACTORY = new EventFactory<MsgEvent<CEpoch>>() {
        public MsgEvent<CEpoch> newInstance() {
            return new MsgEvent<CEpoch>();
        }
    };

    public final static EventFactory<MsgEvent<AckNewLeader>> ACKNEWLEADER_EVENT_FACTORY = new EventFactory<MsgEvent<AckNewLeader>>() {
        public MsgEvent<AckNewLeader> newInstance() {
            return new MsgEvent<AckNewLeader>();
        }
    };

    public final static EventFactory<MsgEvent<Ack>> ACK_EVENT_FACTORY = new EventFactory<MsgEvent<Ack>>() {
        public MsgEvent<Ack> newInstance() {
            return new MsgEvent<Ack>();
        }
    };

    public final static EventFactory<MsgEvent<ApplicationData>> NEWDATA_EVENT_FACTORY = new EventFactory<MsgEvent<ApplicationData>>() {
        public MsgEvent<ApplicationData> newInstance() {
            return new MsgEvent<ApplicationData>();
        }
    };

        public final static EventFactory<MsgEvent<NewEpoch>> NEWPOCH_EVENT_FACTORY = new EventFactory<MsgEvent<NewEpoch>>() {
        public MsgEvent<NewEpoch> newInstance() {
            return new MsgEvent<NewEpoch>();
        }
    };

        public final static EventFactory<MsgEvent<NewLeader>> NEWLEADER_EVENT_FACTORY = new EventFactory<MsgEvent<NewLeader>>() {
        public MsgEvent<NewLeader> newInstance() {
            return new MsgEvent<NewLeader>();
        }
    };

        public final static EventFactory<MsgEvent<CommitLeader>> COMMITLEADER_EVENT_FACTORY = new EventFactory<MsgEvent<CommitLeader>>() {
        public MsgEvent<CommitLeader> newInstance() {
            return new MsgEvent<CommitLeader>();
        }
    };

        public final static EventFactory<MsgEvent<Propose>> PROPOSE_EVENT_FACTORY = new EventFactory<MsgEvent<Propose>>() {
        public MsgEvent<Propose> newInstance() {
            return new MsgEvent<Propose>();
        }
    };

            public final static EventFactory<MsgEvent<Commit>> COMMIT_EVENT_FACTORY = new EventFactory<MsgEvent<Commit>>() {
        public MsgEvent<Commit> newInstance() {
            return new MsgEvent<Commit>();
        }
    };
}
