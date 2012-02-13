package jbu.zab.event;

import com.lmax.disruptor.EventFactory;

public class ProposeTxnEvent {

    private ProposeTxn proposeTxn;

    public ProposeTxn getProposeTxn() {
        return proposeTxn;
    }

    public void setProposeTxn(ProposeTxn proposeTxn) {
        this.proposeTxn = proposeTxn;
    }

    public final static EventFactory<ProposeTxnEvent> PROPOSE_TXN_EVENT_EVENT_FACTORY = new EventFactory<ProposeTxnEvent>() {
        public ProposeTxnEvent newInstance() {
            return new ProposeTxnEvent();
        }
    };
}
