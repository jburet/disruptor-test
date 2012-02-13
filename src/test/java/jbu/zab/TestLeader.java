package jbu.zab;

import jbu.zab.msg.*;
import jbu.zab.transport.Peer;
import org.junit.Test;

import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class TestLeader {

    @Test
    public void when_write_application_data_then_receive_propose() throws UnknownHostException, InterruptedException {
        Leader l = new Leader();
        final CountDownLatch count = new CountDownLatch(1);
        l.newFollower(new Peer(UUID.randomUUID()) {
            @Override
            public void send(NetworkZabMessage networkZabMessage) {
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
        l.newFollower(new Peer(UUID.randomUUID()) {
            @Override
            public void send(NetworkZabMessage networkZabMessage) {
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

        l.newFollower(new Peer(UUID.randomUUID()) {
            final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public void send(NetworkZabMessage networkZabMessage) {
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
        l.newFollower(new Peer(UUID.randomUUID()) {
            final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public void send(NetworkZabMessage networkZabMessage) {
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
    public void publish_many_data_then_processed_in_sequence_with_one_follower() throws UnknownHostException, InterruptedException {
        final Leader l = new Leader();
        final CountDownLatch count = new CountDownLatch(100);
        l.newFollower(new Peer(UUID.randomUUID()) {
            int lastCommit = 0;
            @Override
            public void send(NetworkZabMessage networkZabMessage) {
                if (networkZabMessage instanceof Propose) {
                    // ack
                    Propose p = (Propose) networkZabMessage;
                    l.receiveAck(new Ack(p.getEpoch(), p.getTxnId()));
                }

                if (networkZabMessage instanceof Commit) {
                    // Verify commit come in order with good payload
                    Commit c = (Commit) networkZabMessage;
                    assertEquals(lastCommit++, c.getTxnId());
                    System.out.println("Receive message : " + ((Commit) networkZabMessage).getTxnId());
                    count.countDown();
                }
            }
        });

        for (int i = 0; i < 100; i++) {
            l.receiveApplicationData(new TestApplicationData(i));
        }

        boolean messageReceived = count.await(2, TimeUnit.SECONDS);
        if (!messageReceived) {
            fail("Timeout");
        }
    }


    @Test
    public void publish_many_data_then_processed_in_sequence_with_two_follower() throws UnknownHostException, InterruptedException {
        final Leader l = new Leader();
        final CountDownLatch count = new CountDownLatch(200);
        l.newFollower(new Peer(UUID.randomUUID()) {
            int lastCommit = 0;
            @Override
            public void send(NetworkZabMessage networkZabMessage) {
                if (networkZabMessage instanceof Propose) {
                    // ack
                    Propose p = (Propose) networkZabMessage;
                    l.receiveAck(new Ack(p.getEpoch(), p.getTxnId()));
                }

                if (networkZabMessage instanceof Commit) {
                    // Verify commit come in order with good payload
                    Commit c = (Commit) networkZabMessage;
                    assertEquals(lastCommit++, c.getTxnId());
                    System.out.println("Receive message : " + ((Commit) networkZabMessage).getTxnId());
                    count.countDown();
                }
            }
        });

        l.newFollower(new Peer(UUID.randomUUID()) {
            int lastCommit = 0;
            @Override
            public void send(NetworkZabMessage networkZabMessage) {
                if (networkZabMessage instanceof Propose) {
                    // ack
                    Propose p = (Propose) networkZabMessage;
                    l.receiveAck(new Ack(p.getEpoch(), p.getTxnId()));
                }

                if (networkZabMessage instanceof Commit) {
                    // Verify commit come in order with good payload
                    Commit c = (Commit) networkZabMessage;
                    assertEquals(lastCommit++, c.getTxnId());
                    System.out.println("Receive message : " + ((Commit) networkZabMessage).getTxnId());
                    count.countDown();
                }
            }
        });

        for (int i = 0; i < 100; i++) {
            l.receiveApplicationData(new ApplicationData());
        }

        boolean messageReceived = count.await(4, TimeUnit.SECONDS);
        if (!messageReceived) {
            fail("Timeout");
        }
    }

    @Test
    public void publish_many_data_then_processed_in_sequence_with_three_follower() throws UnknownHostException, InterruptedException {
        final Leader l = new Leader();
        final CountDownLatch count = new CountDownLatch(300);
        l.newFollower(new Peer(UUID.randomUUID()) {
            int lastCommit = 0;
            @Override
            public void send(NetworkZabMessage networkZabMessage) {
                if (networkZabMessage instanceof Propose) {
                    // ack
                    Propose p = (Propose) networkZabMessage;
                    l.receiveAck(new Ack(p.getEpoch(), p.getTxnId()));
                }

                if (networkZabMessage instanceof Commit) {
                    // Verify commit come in order with good payload
                    Commit c = (Commit) networkZabMessage;
                    assertEquals(lastCommit++, c.getTxnId());
                    System.out.println("Receive message : " + ((Commit) networkZabMessage).getTxnId());
                    count.countDown();
                }
            }
        });
        l.newFollower(new Peer(UUID.randomUUID()) {
            int lastCommit = 0;
            @Override
            public void send(NetworkZabMessage networkZabMessage) {
                if (networkZabMessage instanceof Propose) {
                    // ack
                    Propose p = (Propose) networkZabMessage;
                    l.receiveAck(new Ack(p.getEpoch(), p.getTxnId()));
                }

                if (networkZabMessage instanceof Commit) {
                    // Verify commit come in order with good payload
                    Commit c = (Commit) networkZabMessage;
                    assertEquals(lastCommit++, c.getTxnId());
                    System.out.println("Receive message : " + ((Commit) networkZabMessage).getTxnId());
                    count.countDown();
                }
            }
        });

        l.newFollower(new Peer(UUID.randomUUID()) {
            int lastCommit = 0;
            @Override
            public void send(NetworkZabMessage networkZabMessage) {
                if (networkZabMessage instanceof Propose) {
                    // ack
                    Propose p = (Propose) networkZabMessage;
                    l.receiveAck(new Ack(p.getEpoch(), p.getTxnId()));
                }

                if (networkZabMessage instanceof Commit) {
                    // Verify commit come in order with good payload
                    Commit c = (Commit) networkZabMessage;
                    assertEquals(lastCommit++, c.getTxnId());
                    System.out.println("Receive message : " + ((Commit) networkZabMessage).getTxnId());
                    count.countDown();
                }
            }
        });

        for (int i = 0; i < 100; i++) {
            l.receiveApplicationData(new ApplicationData());
        }

        boolean messageReceived = count.await(2, TimeUnit.SECONDS);
        if (!messageReceived) {
            fail("Timeout");
        }
    }

}
