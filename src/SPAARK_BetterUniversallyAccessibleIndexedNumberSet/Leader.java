package SPAARK_BetterUniversallyAccessibleIndexedNumberSet;

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
        if (rc.readSharedArray(UniversallyAccessibleIndexedNumberSet.STAGING_TARGET) != 0) {
            if (rc.getRoundNum() % 2 == Math.max(1 - UniversallyAccessibleIndexedNumberSet.groupId, 0)) {
                int curr = rc.readSharedArray(UniversallyAccessibleIndexedNumberSet.STAGING_CURR);
                int best = rc.readSharedArray(UniversallyAccessibleIndexedNumberSet.STAGING_BEST);
                if (UniversallyAccessibleIndexedNumberSet.isUnassigned(best)) {
                    if (UniversallyAccessibleIndexedNumberSet.isUnassigned(curr)) {
                        if (UniversallyAccessibleIndexedNumberSet.getDistance(curr) < UniversallyAccessibleIndexedNumberSet.getDistance(best)) {
                            rc.writeSharedArray(UniversallyAccessibleIndexedNumberSet.STAGING_BEST, curr);
                        }
                    }
                }
                else if (UniversallyAccessibleIndexedNumberSet.isUnassigned(curr)) {
                    rc.writeSharedArray(UniversallyAccessibleIndexedNumberSet.STAGING_BEST, curr);
                }
                else {
                    if (UniversallyAccessibleIndexedNumberSet.getDistance(curr) < UniversallyAccessibleIndexedNumberSet.getDistance(best)) {
                        rc.writeSharedArray(UniversallyAccessibleIndexedNumberSet.STAGING_BEST, curr);
                    }
                }
                int instruction = rc.readSharedArray(UniversallyAccessibleIndexedNumberSet.GROUP_INSTRUCTIONS + UniversallyAccessibleIndexedNumberSet.groupId - UniversallyAccessibleIndexedNumberSet.GROUP_OFFSET);
                if (instruction == rc.readSharedArray(UniversallyAccessibleIndexedNumberSet.STAGING_TARGET)) {
                    return;
                }
                // if (GlobalArray.isGlobalArrayLoc(instruction)) {
                //     int i = instruction & 0b111111;
                //     if (i >= GlobalArray.ALLY_FLAG_CUR_LOC && i <= GlobalArray.ALLY_FLAG_CUR_LOC + 2) {
                //         return;
                //     }
                // }
                int n = UniversallyAccessibleIndexedNumberSet.setGroupId(0, UniversallyAccessibleIndexedNumberSet.groupId);
                if (instruction == 0) {
                    n += 1 << 15;
                }
                rc.writeSharedArray(UniversallyAccessibleIndexedNumberSet.STAGING_CURR, n);
            }
            else {
                int n = rc.readSharedArray(UniversallyAccessibleIndexedNumberSet.STAGING_BEST);
                if (UniversallyAccessibleIndexedNumberSet.getGroupId(n) == UniversallyAccessibleIndexedNumberSet.groupId) {
                    rc.writeSharedArray(UniversallyAccessibleIndexedNumberSet.GROUP_INSTRUCTIONS + UniversallyAccessibleIndexedNumberSet.groupId - UniversallyAccessibleIndexedNumberSet.GROUP_OFFSET, rc.readSharedArray(UniversallyAccessibleIndexedNumberSet.STAGING_TARGET));
                }
            }
        }
    }
    protected static void jailed() throws GameActionException {
    }
}
