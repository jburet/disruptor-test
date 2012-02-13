package jbu.zab;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import jbu.zab.event.MsgEvent;
import jbu.zab.event.Txn;
import jbu.zab.event.TxnEvent;
import jbu.zab.msg.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Zab leader algorithm
 */
public class Leader {

    private static final int STD_RING_SIZE = 32;
    // Global executors
    private Executor executor = Executors.newCachedThreadPool(new ThreadFactory() {
        private AtomicInteger counter = new AtomicInteger(0);

        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("Leader thread " + counter.incrementAndGet());
            return t;
        }
    });

    // Message queue
    // CEpoch
    private RingBuffer<MsgEvent<CEpoch>> cepochQueue;
    private EventHandler<MsgEvent<CEpoch>> cepochHandler;

    // AckEpoch
    private RingBuffer<MsgEvent<AckEpoch>> ackEpochQueue;
    private EventHandler<MsgEvent<AckEpoch>> ackEpochHandler;

    // AckNewleader
    private RingBuffer<MsgEvent<AckNewLeader>> ackNewLeaderQueue;
    private EventHandler<MsgEvent<AckNewLeader>> ackNewLeaderHandler;

    // Write request from application queue
    private RingBuffer<MsgEvent<ApplicationData>> applicationDataQueue;
    private EventHandler<MsgEvent<ApplicationData>> applicationDataHandler;

    private RingBuffer<TxnEvent> txnQueue;

    // Ack message from follower
    private RingBuffer<MsgEvent<Ack>> ackQueue;
    private EventHandler<MsgEvent<Ack>> ackHandler;

    // Current follower peers
    private Set<Peer> followers = new HashSet<Peer>();
    private int currentEpoch;
    private AtomicInteger txnSequence = new AtomicInteger(0);

    // Ack txn synchro
    // MUST NOT BE SHARED BY THREAD. ONLY USE BY ACK TASK
    private long currentTxnSeq = 0;
    private Txn currentTxn;
    private int ackCount = 0;


    public Leader() {
        this.cepochHandler = new EventHandler<MsgEvent<CEpoch>>() {
            public void onEvent(final MsgEvent<CEpoch> cEpochMsgEvent, final long sequence, final boolean endOfBatch) throws Exception {
                processCepoch(cEpochMsgEvent.getMsg());
            }
        };
        this.ackEpochHandler = new EventHandler<MsgEvent<AckEpoch>>() {
            public void onEvent(final MsgEvent<AckEpoch> ackEpochMsgEvent, final long sequence, final boolean endOfBatch) throws Exception {
                processAckEpoch(ackEpochMsgEvent.getMsg());
            }
        };
        this.ackNewLeaderHandler = new EventHandler<MsgEvent<AckNewLeader>>() {
            public void onEvent(final MsgEvent<AckNewLeader> ackNewLeaderMsgEvent, final long sequence, final boolean endOfBatch) throws Exception {
                processAcKNewLeader(ackNewLeaderMsgEvent.getMsg());
            }
        };
        this.applicationDataHandler = new EventHandler<MsgEvent<ApplicationData>>() {
            public void onEvent(final MsgEvent<ApplicationData> applicationDataMsgEvent, final long sequence, final boolean endOfBatch) throws Exception {
                processApplicationData(applicationDataMsgEvent.getMsg());
            }
        };
        this.ackHandler = new EventHandler<MsgEvent<Ack>>() {
            public void onEvent(final MsgEvent<Ack> ackMsgEvent, final long sequence, final boolean endOfBatch) throws Exception {
                processAck(ackMsgEvent.getMsg());
            }
        };
        // External message queue
        this.cepochQueue = instanciateRingWithHandler(MsgEvent.CEPOCH_EVENT_FACTORY, cepochHandler, STD_RING_SIZE);
        this.ackNewLeaderQueue = instanciateRingWithHandler(MsgEvent.ACKNEWLEADER_EVENT_FACTORY, ackNewLeaderHandler, STD_RING_SIZE);
        this.applicationDataQueue = instanciateRingWithHandler(MsgEvent.NEWDATA_EVENT_FACTORY, applicationDataHandler, STD_RING_SIZE);
        this.ackQueue = instanciateRingWithHandler(MsgEvent.ACK_EVENT_FACTORY, ackHandler, STD_RING_SIZE);

        // internal queue
        // Only one txn at time
        this.txnQueue = instanciateRing(TxnEvent.TXN_EVENT_FACTORY, 1);
    }

    // receive message
    void receiveApplicationData(ApplicationData applicationData) {
        long seq = this.applicationDataQueue.next();
        this.applicationDataQueue.get(seq).setMsg(applicationData);
        this.applicationDataQueue.publish(seq);
    }

    void receiveAck(Ack ack) {
        long seq = this.ackQueue.next();
        this.ackQueue.get(seq).setMsg(ack);
        this.ackQueue.publish(seq);
    }

    // status change method
    void newFollower(Peer follower) {
        followers.add(follower);
    }


    private RingBuffer instanciateRingWithHandler(EventFactory eventFactory, EventHandler handler, int ringSize) {
        Disruptor<MsgEvent> disruptor =
                new Disruptor<MsgEvent>(eventFactory, executor,
                        new SingleThreadedClaimStrategy(ringSize),
                        new SleepingWaitStrategy());
        disruptor.handleEventsWith(handler);
        return disruptor.start();
    }

    private RingBuffer instanciateRing(EventFactory eventFactory, int ringSize) {
        Disruptor<MsgEvent<CEpoch>> disruptor =
                new Disruptor<MsgEvent<CEpoch>>(eventFactory, executor,
                        new SingleThreadedClaimStrategy(ringSize),
                        new SleepingWaitStrategy());

        return disruptor.start();
    }

    // Message handler
    private void processApplicationData(ApplicationData msg) {
        Set<Peer> currentPeer = new HashSet<Peer>(followers);
        // Create a new Txn with current peer, current epoch, and next txnid
        int newTxnId = txnSequence.getAndIncrement();
        Txn txn = new Txn(currentPeer, currentEpoch, newTxnId);

        // Add this txn in process queue
        long seq = this.txnQueue.next();
        this.txnQueue.get(seq).setTxn(txn);
        this.txnQueue.publish(seq);

        // Create a new Proprose msg
        // Send the propose to all current follower
        for (Peer p : currentPeer) {
            p.send(new Propose(currentEpoch, newTxnId, msg));
        }
    }

    private void processAck(Ack ack) {
        // check if new transaction
        if (txnQueue.getCursor() > currentTxnSeq || currentTxn == null) {
            // get new txn
            this.currentTxn = txnQueue.get(++currentTxnSeq).getTxn();
        }

        // if ack is for a previous txn throw it
        // Cannot receive ack with id > currentTxnId....
        if (ack.getTxnId() == currentTxn.getTxnId()) {
            ackCount++;
            if (ackCount >= (currentTxn.getCurrentPeer().size() / 2 + 1)) {
                for (Peer p : currentTxn.getCurrentPeer()) {
                    p.send(new Commit(currentEpoch, currentTxn.getTxnId()));
                }
            }
        }
    }

    private void processAcKNewLeader(AckNewLeader msg) {
        //To change body of created methods use File | Settings | File Templates.
    }

    private void processCepoch(CEpoch msg) {
        //To change body of created methods use File | Settings | File Templates.
    }

    private void processAckEpoch(AckEpoch msg) {
        //To change body of created methods use File | Settings | File Templates.
    }
}
