package jbu.zab;

import jbu.zab.msg.*;
import org.junit.Test;

import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class TestLeader {

    @Test
    public void when_write_application_data_then_receive_propose() throws UnknownHostException, InterruptedException {
        Leader l = new Leader();
        final CountDownLatch count = new CountDownLatch(1);
        l.newFollower(new Peer(UUID.randomUUID(), "", 8080) {
            @Override
            void send(NetworkZabMessage networkZabMessage) {
                count.countDown();
            }
        });
        l.receiveApplicationData(new ApplicationData());
        boolean messageReceived = count.await(1, TimeUnit.SECONDS);
        assertTrue(messageReceived);
    }

    @Test
    public void when_quorum_send_ack_then_receive_commit() throws UnknownHostException, InterruptedException {
        final Leader l = new Leader();
        final CountDownLatch count = new CountDownLatch(1);
        final AtomicInteger counter = new AtomicInteger(0);
        l.newFollower(new Peer(UUID.randomUUID(), "", 8080) {
            @Override
            void send(NetworkZabMessage networkZabMessage) {
                if (counter.get() == 0) {
                    // msg must be propose
                    if (!(networkZabMessage instanceof Propose)) {
                        fail("Not receive propose as first message");
                    }
                    Propose p = ((Propose) networkZabMessage);
                    l.receiveAck(new Ack(p.getEpoch(), p.getTxnId()));
                } else if (counter.get() == 1) {
                    // msg must be propose
                    if (!(networkZabMessage instanceof Commit)) {
                        fail("Not receive commit as second message");
                    }
                    count.countDown();
                }
                counter.incrementAndGet();
            }
        });
        l.receiveApplicationData(new ApplicationData());
        boolean messageReceived = count.await(1, TimeUnit.SECONDS);
        if (!messageReceived) {
            fail("Timeout");
        }
    }

    @Test
    public void when_with_two_follower_quorum_send_ack_then_receive_commit() throws UnknownHostException, InterruptedException {
        final Leader l = new Leader();
        final CountDownLatch count = new CountDownLatch(2);

        l.newFollower(new Peer(UUID.randomUUID(), "", 8080) {
            final AtomicInteger counter = new AtomicInteger(0);

            @Override
            void send(NetworkZabMessage networkZabMessage) {
                if (counter.get() == 0) {
                    // msg must be propose
                    if (!(networkZabMessage instanceof Propose)) {
                        fail("Not receive propose as first message");
                    }
                    Propose p = ((Propose) networkZabMessage);
                    l.receiveAck(new Ack(p.getEpoch(), p.getTxnId()));
                } else if (counter.get() == 1) {
                    // msg must be propose
                    if (!(networkZabMessage instanceof Commit)) {
                        fail("Not receive commit as second message");
                    }
                    count.countDown();
                }
                counter.incrementAndGet();
            }
        });
        l.newFollower(new Peer(UUID.randomUUID(), "", 8080) {
            final AtomicInteger counter = new AtomicInteger(0);

            @Override
            void send(NetworkZabMessage networkZabMessage) {
                if (counter.get() == 0) {
                    // msg must be propose
                    if (!(networkZabMessage instanceof Propose)) {
                        fail("Not receive propose as first message");
                    }
                    Propose p = ((Propose) networkZabMessage);
                    l.receiveAck(new Ack(p.getEpoch(), p.getTxnId()));
                } else if (counter.get() == 1) {
                    // msg must be propose
                    if (!(networkZabMessage instanceof Commit)) {
                        fail("Not receive commit as second message");
                    }
                    count.countDown();
                }
                counter.incrementAndGet();
            }
        });
        l.receiveApplicationData(new ApplicationData());
        boolean messageReceived = count.await(1, TimeUnit.SECONDS);
        if (!messageReceived) {
            fail("Timeout");
        }
    }

}
