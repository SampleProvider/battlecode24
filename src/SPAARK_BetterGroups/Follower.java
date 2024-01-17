package SPAARK_BetterGroups;

import battlecode.common.*;

import java.util.Random;

public class Follower {
    protected static RobotController rc;
    protected static StringBuilder indicatorString;

    protected static Random rng;

    protected static MapLocation lastLeaderLocation;
    
    protected static void run() throws GameActionException {
        // join the nearest group, but also balancing group size if there's a lot of groups
        int lowestWeight = Integer.MAX_VALUE;
        int lowestIndex = -1;
        for (int i = 0; i < 9; i++) {
            int n = rc.readSharedArray(GlobalArray.GROUP_STAGING + i);
            if (GlobalArray.hasLocation(n)) {
                int size = GlobalArray.getGroupRobotCount(n);
                if (size < 5) {
                    GlobalArray.joinGroup(i);
                    lowestIndex = -1;
                    break;
                } else if (size <= 8) {
                    // weight for group size has higher order to really decentivize large groups
                    int weight = rc.getLocation().distanceSquaredTo(GlobalArray.parseLocation(n)) + size * size * size;
                    if (weight < lowestWeight) {
                        lowestIndex = i;
                        lowestWeight = weight;
                    }
                }
            }
        }
        if (lowestIndex != -1) {
            GlobalArray.joinGroup(lowestIndex);
        }
        // create a group if there's ducks nearby
        if (GlobalArray.groupId == -1 && rc.senseNearbyRobots(-1, rc.getTeam()).length > 3) {
            GlobalArray.createGroup();
        }
    }
    protected static void jailed() throws GameActionException {
        if (GlobalArray.groupId != -1) {
            GlobalArray.leaveGroup();
        }
    }
}
