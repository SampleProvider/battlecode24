package SPAARK_GROUPS_N_DEFENSE;

import battlecode.common.*;

import java.util.Random;

public class Leader {
    protected static RobotController rc;
    protected static StringBuilder indicatorString;

    protected static Random rng;

    protected static final Direction[] DIRECTIONS = {
        Direction.SOUTHWEST,
        Direction.SOUTH,
        Direction.SOUTHEAST,
        Direction.WEST,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.NORTHWEST,
        Direction.NORTH,
    };
    
    protected static void run() throws GameActionException {
        if (rc.readSharedArray(GlobalArray.STAGING_TARGET) != 0) {
            if (rc.getRoundNum() % 2 == Math.max(1 - GlobalArray.groupId, 0)) {
                int curr = rc.readSharedArray(GlobalArray.STAGING_CURR);
                int best = rc.readSharedArray(GlobalArray.STAGING_BEST);
                if (GlobalArray.isUnassigned(best)) {
                    if (GlobalArray.isUnassigned(curr)) {
                        if (GlobalArray.getDistance(curr) < GlobalArray.getDistance(best)) {
                            rc.writeSharedArray(GlobalArray.STAGING_BEST, curr);
                        }
                    }
                }
                else {
                    if (GlobalArray.getDistance(curr) < GlobalArray.getDistance(best)) {
                        rc.writeSharedArray(GlobalArray.STAGING_BEST, curr);
                    }
                }
                int n = 0;
                if (rc.readSharedArray(GlobalArray.GROUP_INSTRUCTIONS + GlobalArray.groupId) == 0) {
                    n = 1 << 15;
                }
                n += GlobalArray.groupId << 11;
                rc.writeSharedArray(GlobalArray.STAGING_CURR, n);
            }
            else {
                int n = rc.readSharedArray(GlobalArray.STAGING_BEST);
                if (((n >> 11) & 0b1111) == GlobalArray.groupId) {
                    rc.writeSharedArray(GlobalArray.GROUP_INSTRUCTIONS + GlobalArray.groupId, rc.readSharedArray(GlobalArray.STAGING_TARGET));
                    System.out.println(rc.readSharedArray(GlobalArray.STAGING_TARGET));
                }
            }
        }
    }
    protected static void jailed() throws GameActionException {
    }
}
