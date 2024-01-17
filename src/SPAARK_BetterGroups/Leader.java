package SPAARK_BetterGroups;

import battlecode.common.*;

import java.util.Random;

public class Leader {
    protected static RobotController rc;
    protected static StringBuilder indicatorString;

    protected static Random rng;
    
    protected static void run() throws GameActionException {
        // this could be bytecode optimized but lol nope pretty useless right now
        // update own position for other robots
        rc.writeSharedArray(GlobalArray.GROUP_STAGING + GlobalArray.groupId, GlobalArray.updateLocation(rc.readSharedArray(GlobalArray.GROUP_STAGING + GlobalArray.groupId), rc.getLocation()));
        // remove group if there are no ducks in the group (delete own position)
        if (GlobalArray.getGroupRobotCount(rc.readSharedArray(GlobalArray.GROUP_STAGING + GlobalArray.groupId)) == 0) {
            rc.writeSharedArray(GlobalArray.GROUP_STAGING + GlobalArray.groupId, rc.readSharedArray(GlobalArray.GROUP_STAGING + GlobalArray.groupId) & 0b1110111111111111);
            GlobalArray.groupId = 0;
            GlobalArray.groupLeader = false;
        }
    }
    protected static void jailed() throws GameActionException {
        if (GlobalArray.groupId != -1) {
            GlobalArray.leaveGroup();
        }
    }
}
