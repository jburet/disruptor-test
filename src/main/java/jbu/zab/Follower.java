package jbu.zab;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import jbu.zab.event.*;
import jbu.zab.msg.*;
import jbu.zab.transport.Peer;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Zab follower algorithm
 */
public class Follower {

    private static final int STD_RING_SIZE = 64;
    // Global executors
    private Executor executor = Executors.newCachedThreadPool(new ThreadFactory() {
        private AtomicInteger counter = new AtomicInteger(0);

        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("Follower thread " + counter.incrementAndGet());
            return t;
        }
    });

    // Message queue
    // NewEpoch
    private RingBuffer<MsgEvent<NewEpoch>> newEpochQueue;
    private EventHandler<MsgEvent<NewEpoch>> newEpochHandler;

    // NewLeader
    private RingBuffer<MsgEvent<NewLeader>> newLeaderQueue;
    private EventHandler<MsgEvent<NewLeader>> newLeaderHandler;

    // CommitLeader
    private RingBuffer<MsgEvent<CommitLeader>> commitLeaderQueue;
    private EventHandler<MsgEvent<CommitLeader>> commitLeaderHandler;

    // Propose
    private RingBuffer<MsgEvent<Propose>> proposeQueue;
    private EventHandler<MsgEvent<Propose>> proposeHandler;

    // Commit
    private RingBuffer<MsgEvent<Commit>> commitQueue;
    private EventHandler<MsgEvent<Commit>> commitHandler;


    private RingBuffer<ProposeTxnEvent> proposeTxnQueue;
    private Sequence proposeTxnSequence;

    // leader
    private Peer leader;

    // application callback
    private RingBuffer<MsgEvent<ApplicationData>> applicationCallback;

    // commit txn synchro
    // FIXME Verify disruptor always use same thread for event processing....
    // MUST NOT BE SHARED BY THREAD. ONLY USE BY COMMIT TASK
    private long currentTxnSeq = 0;
    private int currentTxnId = -1;
    private int currentTxnEpoch;
    private ApplicationData currentAppData;


    public Follower(Peer leader, RingBuffer<MsgEvent<ApplicationData>> applicationCallback) {
        this.newEpochHandler = new EventHandler<MsgEvent<NewEpoch>>() {
            public void onEvent(final MsgEvent<NewEpoch> newEpochMsgEvent, final long sequence, final boolean endOfBatch) throws Exception {
                processNewEpoch(newEpochMsgEvent.getMsg());
            }
        };
        this.newLeaderHandler = new EventHandler<MsgEvent<NewLeader>>() {
            public void onEvent(final MsgEvent<NewLeader> newLeaderMsgEvent, final long sequence, final boolean endOfBatch) throws Exception {
                processNewLeader(newLeaderMsgEvent.getMsg());
            }
        };
        this.commitLeaderHandler = new EventHandler<MsgEvent<CommitLeader>>() {
            public void onEvent(final MsgEvent<CommitLeader> commitLeaderMsgEvent, final long sequence, final boolean endOfBatch) throws Exception {
                processCommitLeader(commitLeaderMsgEvent.getMsg());
            }
        };
        this.proposeHandler = new EventHandler<MsgEvent<Propose>>() {
            public void onEvent(final MsgEvent<Propose> proposeMsgEvent, final long sequence, final boolean endOfBatch) throws Exception {
                processPropose(proposeMsgEvent.getMsg());
            }
        };
        this.commitHandler = new EventHandler<MsgEvent<Commit>>() {
            public void onEvent(final MsgEvent<Commit> commitMsgEvent, final long sequence, final boolean endOfBatch) throws Exception {
                processCommit(commitMsgEvent.getMsg());
            }
        };
        // External message queue
        this.newEpochQueue = instanciateRingWithHandler(MsgEvent.NEWPOCH_EVENT_FACTORY, newEpochHandler, STD_RING_SIZE);
        this.newLeaderQueue = instanciateRingWithHandler(MsgEvent.NEWLEADER_EVENT_FACTORY, newLeaderHandler, STD_RING_SIZE);
        this.commitLeaderQueue = instanciateRingWithHandler(MsgEvent.COMMITLEADER_EVENT_FACTORY, commitLeaderHandler, STD_RING_SIZE);
        this.proposeQueue = instanciateRingWithHandler(MsgEvent.PROPOSE_EVENT_FACTORY, proposeHandler, 16);
        this.commitQueue = instanciateRingWithHandler(MsgEvent.COMMIT_EVENT_FACTORY, commitHandler, STD_RING_SIZE);

        // internal queue
        // Only one txn at time
        this.proposeTxnSequence = new Sequence();
        this.proposeTxnQueue = instanciateRing(ProposeTxnEvent.PROPOSE_TXN_EVENT_EVENT_FACTORY, 1, proposeTxnSequence);

        this.applicationCallback = applicationCallback;

        this.leader = leader;
    }

    // FIXME Deduplicate with leader
    private RingBuffer instanciateRingWithHandler(EventFactory eventFactory, EventHandler handler, int ringSize) {
        Disruptor<MsgEvent> disruptor =
                new Disruptor<MsgEvent>(eventFactory, executor,
                        new SingleThreadedClaimStrategy(ringSize),
                        new SleepingWaitStrategy());
        disruptor.handleEventsWith(handler);
        return disruptor.start();
    }

    private RingBuffer instanciateRing(EventFactory eventFactory, int ringSize, Sequence sequence) {
        RingBuffer<MsgEvent> ringBuffer =
                new RingBuffer<MsgEvent>(eventFactory,
                        new SingleThreadedClaimStrategy(ringSize),
                        new SleepingWaitStrategy());
        ringBuffer.setGatingSequences(sequence);
        return ringBuffer;
    }

    // receive message
    public void receivePropose(Propose propose) {
        long seq = this.proposeQueue.next();
        this.proposeQueue.get(seq).setMsg(propose);
        this.proposeQueue.publish(seq);
    }

    public void receiveCommit(Commit commit) {
        long seq = this.commitQueue.next();
        this.commitQueue.get(seq).setMsg(commit);
        this.commitQueue.publish(seq);
    }

    // processing
    private void processCommit(Commit commit) {
        // get last txn info
        if (proposeTxnSequence.get() > currentTxnSeq || currentTxnId < 0) {
            // get new txn
            currentTxnSeq = proposeTxnSequence.get();
            this.currentTxnId = proposeTxnQueue.get(currentTxnSeq).getTxnId();
            this.currentTxnEpoch = proposeTxnQueue.get(currentTxnSeq).getEpoch();
            this.currentAppData = proposeTxnQueue.get(currentTxnSeq).getApplicationData();
        }

        // verify commit info else throw away commit
        if (commit.getEpoch() == this.currentTxnEpoch && commit.getTxnId() == this.currentTxnId) {
            long seq = this.applicationCallback.next();
            this.applicationCallback.get(seq).setMsg(this.currentAppData);
            this.applicationCallback.publish(seq);
            proposeTxnSequence.set(currentTxnSeq + 1);
        }

    }

    private void processPropose(Propose propose) {
        // create a new proposeTxn
        ProposeTxn txn = new ProposeTxn(propose.getEpoch(), propose.getTxnId(), propose.getApplicationData());

        // Add this txn in process queue
        long seq = this.proposeTxnQueue.next();
        ProposeTxnEvent evt = this.proposeTxnQueue.get(seq);
        evt.setTxnId(propose.getTxnId());
        evt.setEpoch(propose.getEpoch());
        evt.setApplicationData(propose.getApplicationData());
        this.proposeTxnQueue.publish(seq);

        // send ack
        leader.send(new Ack(propose.getEpoch(), propose.getTxnId()));
    }

    private void processCommitLeader(CommitLeader commitLeader) {
        //To change body of created methods use File | Settings | File Templates.
    }

    private void processNewLeader(NewLeader newLeader) {
        //To change body of created methods use File | Settings | File Templates.
    }

    private void processNewEpoch(NewEpoch newEpoch) {
    }
}
