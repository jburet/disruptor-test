package jbu.zab;

/**
 * Central class of application
 * Instanciate leader follower and connector
 */
public class Znode {

    // never null after Znode is started
    private Follower follower;

    // Can be null if node is only a follower
    private Leader leader;

}
