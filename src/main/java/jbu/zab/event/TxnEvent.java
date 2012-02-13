package jbu.zab.event;

import com.lmax.disruptor.EventFactory;

public class TxnEvent {

    private Txn txn;

    public Txn getTxn() {
        return txn;
    }

    public void setTxn(Txn txn) {
        this.txn = txn;
    }

    public final static EventFactory<TxnEvent> TXN_EVENT_FACTORY = new EventFactory<TxnEvent>() {
        public TxnEvent newInstance() {
            return new TxnEvent();
        }
    };

    public final static EventFactory<ProposeTxnEvent> PROPOSE_TXN_EVENT_EVENT_FACTORY = new EventFactory<ProposeTxnEvent>() {
        public ProposeTxnEvent newInstance() {
            return new ProposeTxnEvent();
        }
    };
}
