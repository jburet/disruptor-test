package jbu.zab;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.SingleThreadedClaimStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import jbu.zab.event.MsgEvent;
import jbu.zab.msg.ApplicationData;
import jbu.zab.transport.Peer;
import jbu.zab.transport.VmPeer;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public class OnePeerMemoryBench {
    @Test
    public void test() throws InterruptedException {
        final int NB_MESSAGE = 100000;
        final CountDownLatch countDown = new CountDownLatch(1);

        // callback of writed event
        Disruptor<MsgEvent<ApplicationData>> disruptor =
                new Disruptor<MsgEvent<ApplicationData>>(MsgEvent.NEWDATA_EVENT_FACTORY, Executors.newSingleThreadExecutor(),
                        new SingleThreadedClaimStrategy(64),
                        new SleepingWaitStrategy());
        disruptor.handleEventsWith(new EventHandler<MsgEvent<ApplicationData>>() {
            int count = 0;

            public void onEvent(MsgEvent<ApplicationData> event, long sequence, boolean endOfBatch) throws Exception {
                //System.out.println("receive : " + count);
                //countDown.countDown();
                count++;
                if(count % 1000 == 0){
                    System.out.println(count+" message received");
                }
                if (count == NB_MESSAGE) {
                    countDown.countDown();
                }
            }
        });
        // create leader
        Leader l = new Leader();
        // Create two peer
        // leader peer
        VmPeer leaderPeer = new VmPeer(UUID.randomUUID());
        // follower peer
        VmPeer followerPeer = new VmPeer(UUID.randomUUID());
        // create one follower
        Follower f = new Follower(leaderPeer, disruptor.start());
        l.newFollower(followerPeer);

        leaderPeer.setLeader(l);
        followerPeer.setFollower(f);

        // Send a lot of data
        long start = System.nanoTime();
        //ByteArrayApplicationData data = new ByteArrayApplicationData(new byte[1024]);
        for (int i = 0; i < NB_MESSAGE; i++) {
            l.receiveApplicationData(new ByteArrayApplicationData(new byte[1024]));
        }

        countDown.await();
        long time = System.nanoTime() - start;
        System.out.println("take " + time / 1000d / 1000d + " ms");
        System.out.println(NB_MESSAGE / (time / 1000d / 1000d / 1000d) + " req/s");


    }

}
