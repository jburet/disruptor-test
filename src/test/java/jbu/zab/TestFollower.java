package jbu.zab;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SingleThreadedClaimStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import jbu.zab.event.MsgEvent;
import jbu.zab.msg.*;
import jbu.zab.transport.Peer;
import org.junit.Test;

import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class TestFollower {

    @Test
    public void write_propose_then_ack() throws UnknownHostException, InterruptedException {
        final CountDownLatch count = new CountDownLatch(1);
        Follower f = new Follower(new Peer(UUID.randomUUID()) {
            @Override
            public void send(NetworkZabMessage networkZabMessage) {
                // must receive an ack
                if (!(networkZabMessage instanceof Ack)) {
                    fail("Not receive ack as first message");
                }
                count.countDown();
            }
        }, null);


        f.receivePropose(new Propose(1, 1, new ApplicationData()));
        boolean messageReceived = count.await(100, TimeUnit.SECONDS);
        if (!messageReceived) {
            fail("Timeout");
        }
    }


    @Test
    public void write_propose_and_commit_then_application_receive_data() throws UnknownHostException, InterruptedException {
        final CountDownLatch receiveApplicationCount = new CountDownLatch(1);
        Disruptor<MsgEvent<ApplicationData>> disruptor =
                new Disruptor<MsgEvent<ApplicationData>>(MsgEvent.NEWDATA_EVENT_FACTORY, Executors.newSingleThreadExecutor(),
                        new SingleThreadedClaimStrategy(1),
                        new SleepingWaitStrategy());
        disruptor.handleEventsWith(new EventHandler<MsgEvent<ApplicationData>>() {
            public void onEvent(MsgEvent<ApplicationData> event, long sequence, boolean endOfBatch) throws Exception {
                receiveApplicationCount.countDown();
            }
        });
        RingBuffer<MsgEvent<ApplicationData>> buffer = disruptor.start();

        final CountDownLatch count = new CountDownLatch(1);
        final Follower f = new Follower(new Peer(UUID.randomUUID()) {
            @Override
            public void send(NetworkZabMessage networkZabMessage) {
                // must receive an ack
                if (!(networkZabMessage instanceof Ack)) {
                    fail("Not receive ack as first message");
                }

                count.countDown();
            }
        }, buffer);


        f.receivePropose(new Propose(1, 1, new ApplicationData()));
        boolean messageReceived = count.await(1, TimeUnit.SECONDS);
        f.receiveCommit(new Commit(1, 1));
        if (!messageReceived) {
            fail("Timeout ack");
        }
        messageReceived = receiveApplicationCount.await(100, TimeUnit.SECONDS);
        if (!messageReceived) {
            fail("Timeout receive application");
        }

    }
}
