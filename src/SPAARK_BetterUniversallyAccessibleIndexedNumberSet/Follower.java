package SPAARK_BetterUniversallyAccessibleIndexedNumberSet;

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
        if (rc.readSharedArray(UniversallyAccessibleIndexedNumberSet.STAGING_TARGET) != 0) {
            if (rc.getRoundNum() % 2 == 0) {
                int instruction = rc.readSharedArray(UniversallyAccessibleIndexedNumberSet.GROUP_INSTRUCTIONS + UniversallyAccessibleIndexedNumberSet.groupId - UniversallyAccessibleIndexedNumberSet.GROUP_OFFSET);
                if (UniversallyAccessibleIndexedNumberSet.isGlobalArrayLoc(instruction)) {
                    int i = instruction & 0b111111;
                    if (i >= UniversallyAccessibleIndexedNumberSet.OPPO_FLAG_CUR_LOC && i <= UniversallyAccessibleIndexedNumberSet.OPPO_FLAG_CUR_LOC + 2) {
                        return;
                    }
                }
                int curr = rc.readSharedArray(UniversallyAccessibleIndexedNumberSet.STAGING_CURR);
                curr += Math.sqrt(rc.getLocation().distanceSquaredTo(UniversallyAccessibleIndexedNumberSet.parseLocation(rc.readSharedArray(UniversallyAccessibleIndexedNumberSet.STAGING_TARGET))));
                rc.writeSharedArray(UniversallyAccessibleIndexedNumberSet.STAGING_CURR, curr);
            }
        }
    }
    protected static void jailed() throws GameActionException {
    }
}
