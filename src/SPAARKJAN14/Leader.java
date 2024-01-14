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
                else if (GlobalArray.isUnassigned(curr)) {
                    rc.writeSharedArray(GlobalArray.STAGING_BEST, curr);
                }
                else {
                    if (GlobalArray.getDistance(curr) < GlobalArray.getDistance(best)) {
                        rc.writeSharedArray(GlobalArray.STAGING_BEST, curr);
                    }
                }
                int instruction = rc.readSharedArray(GlobalArray.GROUP_INSTRUCTIONS + GlobalArray.groupId - 2);
                if (GlobalArray.isGlobalArrayLoc(instruction)) {
                    int i = instruction & 0b111111;
                    if (i >= GlobalArray.OPPO_FLAG_CUR_LOC && i <= GlobalArray.OPPO_FLAG_CUR_LOC + 2) {
                        return;
                    }
                }
                int n = GlobalArray.setGroupId(0, GlobalArray.groupId);
                if (instruction == 0) {
                    n += 1 << 15;
                }
                rc.writeSharedArray(GlobalArray.STAGING_CURR, n);
            }
            else {
                int n = rc.readSharedArray(GlobalArray.STAGING_BEST);
                if (GlobalArray.getGroupId(n) == GlobalArray.groupId) {
                    rc.writeSharedArray(GlobalArray.GROUP_INSTRUCTIONS + GlobalArray.groupId - 2, rc.readSharedArray(GlobalArray.STAGING_TARGET));
                }
            }
        }
    }
    protected static void jailed() throws GameActionException {
    }
}
