package SPAARK_BetterGroups;

import battlecode.common.*;

import java.util.Random;

public class Leader {
    protected static RobotController rc;
    protected static StringBuilder indicatorString;

    protected static Random rng;
    
    protected static void run() throws GameActionException {
        // update own position
        // remove group if there are no ducks in the group (delete own position)
    }
    protected static void jailed() throws GameActionException {
        // remove own group id and become follower (also delete position)
    }
}
