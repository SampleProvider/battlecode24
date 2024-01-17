package SPAARK_BetterGroups;

import battlecode.common.*;

import java.util.Random;

public class Follower {
    protected static RobotController rc;
    protected static StringBuilder indicatorString;

    protected static Random rng;

    protected static MapLocation lastLeaderLocation;
    
    protected static void run() throws GameActionException {
        int[] joinWeights = new int[9];
        for (int i = 0; i < 9; i++) {

        }
        // join nearest group (probably can leave current group whenever since nearest group is often also current group, unless new group was made)
        // if group size 0-4, join without other calculations
        // if group size 5-8, use heuristic based on distance and group size (larger groups and further groups are bad)
        // creating a new group is also possible - create group when other ducks nearby (dont want leader alone)
        // should help balance group sizes
    }
    protected static void jailed() throws GameActionException {
        if (GlobalArray.groupId != -1) {
            GlobalArray.leaveGroup();
        }
    }
}
