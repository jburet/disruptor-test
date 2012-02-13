package jbu.zab;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SingleThreadedClaimStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import jbu.zab.event.MsgEvent;
import jbu.zab.msg.ApplicationData;
import jbu.zab.transport.Peer;

import java.util.concurrent.Executors;

/**
 * Central class of application
 * Instanciate leader follower and connector
 */
public final class Znode {

    // never null after Znode is started
    private Follower follower;

    // Can be null if node is only a follower
    private Leader leader;

    // Buffer for managing received application data
    private RingBuffer<MsgEvent<ApplicationData>> applicationCallbackRingBuffer;
    private ApplicationCallback applicationCallback = null;

    public Znode(String netInterface, int port) {
        Disruptor<MsgEvent<ApplicationData>> disruptor =
                new Disruptor<MsgEvent<ApplicationData>>(MsgEvent.NEWDATA_EVENT_FACTORY, Executors.newSingleThreadExecutor(),
                        new SingleThreadedClaimStrategy(1),
                        new SleepingWaitStrategy());
        disruptor.handleEventsWith(new EventHandler<MsgEvent<ApplicationData>>() {
            public void onEvent(MsgEvent<ApplicationData> event, long sequence, boolean endOfBatch) throws Exception {
                if (applicationCallback != null) {
                    applicationCallback.newData(event.getMsg());
                }
            }
        });
        this.applicationCallbackRingBuffer = disruptor.start();
    }

    private void setNodeAsFollower(Peer leader) {
        // create a new follower
        this.follower = new Follower(leader, applicationCallbackRingBuffer);
    }

    private void setNodeAsLeader(){
        this.leader = new Leader();
        // FIXME implemeents peer and use them
        Peer me = null;
        this.follower = new Follower(me, applicationCallbackRingBuffer);
    }


}
