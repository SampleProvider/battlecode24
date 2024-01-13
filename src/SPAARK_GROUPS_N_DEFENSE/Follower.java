package SPAARK_GROUPS_N_DEFENSE;

import battlecode.common.*;

import java.util.Random;

public class Follower {
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
            if (rc.getRoundNum() % 2 == 0) {
                int curr = rc.readSharedArray(GlobalArray.STAGING_CURR);
                curr += Math.sqrt(rc.getLocation().distanceSquaredTo(GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.STAGING_TARGET))));
                rc.writeSharedArray(GlobalArray.STAGING_CURR, curr);
            }
        }
    }
    protected static void jailed() throws GameActionException {
    }
}
