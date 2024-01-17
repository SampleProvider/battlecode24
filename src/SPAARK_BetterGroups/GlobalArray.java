package SPAARK_BetterGroups;

import battlecode.common.*;

public class GlobalArray {
    protected static RobotController rc;
    protected static StringBuilder indicatorString;

    protected static int id;
    protected static int groupId;
    protected static boolean groupLeader;

    protected static int[] sectorWidth;
    protected static int[] sectorHeight;
    protected static int[] sectorX;
    protected static int[] sectorY;

    /*
     * 0-2: Ally flag ids
     * 3-5: Ally flag default locations
     * 6-8: Ally flag current locations
     * 9-11: Ally flag info
     * 12-14: Opponent flag ids
     * 15-17: Opponent flag default locations
     * 18-20: Opponent flag current locations
     * 21-23: Opponent flag info
     * 24: Flag target (setup only)
     * 25: Gathering point (setup only)
     * 24: Staging target
     * 25: Staging best
     * 26: Staging current
     * 27-30: Robot POI
     * 31-39: Group staging
     * 40-48: Group targets
     * 63: Global id counter (first round only)
     * 63: Flag target heuristic (setup only)
     * 
     * Formatting:
     * 
     * Location:
     * bit 1-6: X
     * bit 7-12: Y
     * bit 13: Presence indicator
     * 
     * Flags:
     * bits 1-13: Flag location
     * bit 14: Flag picked up
     * 
     * Flag Info:
     * bit 1-6: Number of opponent robots
     * bit 7-10: X of robot direction (which direction the robot with the flag is going)
     * bit 11-14: Y of robot direction
     * 
     * Staging Target:
     * bit 13: Presence indicator
     * 
     * If bit 14 is 1:
     *   bits 1-6: Array index of target
     * If bit 14 is 0:
     *   bits 1-12: Location of target
     * 
     * Staging curr/best group:
     * bits 1-12: Distance from target
     * bits 13-15: Group id
     * bit 16: if the target is NOT assigned
     * 
     * Group staging area:
     * bits 1-13: Leader location
     * bits 14-16: Robot count
     */
    protected static final int ALLY_FLAG_ID = 0;
    protected static final int ALLY_FLAG_DEF_LOC = 3;
    protected static final int ALLY_FLAG_CUR_LOC = 6;
    protected static final int ALLY_FLAG_INFO = 9;
    protected static final int OPPO_FLAG_ID = 12;
    protected static final int OPPO_FLAG_DEF_LOC = 15;
    protected static final int OPPO_FLAG_CUR_LOC = 18;
    protected static final int OPPO_FLAG_INFO = 21;
    protected static final int SETUP_FLAG_TARGET = 24;
    protected static final int SETUP_GATHER_LOC = 25;
    protected static final int SETUP_FLAG_WEIGHT = 63;
    protected static final int POINTS_OF_INTEREST = 27;
    protected static final int POI_LENGTH = 4;
    protected static final int GROUP_STAGING = 31;
    protected static final int GROUP_INSTRUCTIONS = 40;
    protected static final int STAGING_TARGET = 59;
    protected static final int STAGING_BEST = 60;
    protected static final int STAGING_CURR = 61;
    protected static final int INIT_GLOBAL_ID_COUNTER = 63;

    protected static int[][] sectors;

    // location
    protected static boolean hasLocation(int n) {
        return (n >> 12 & 0b1) == 1;
    }
    protected static MapLocation parseLocation(int n) {
        return new MapLocation(n & 0b111111, (n >> 6) & 0b111111);
    }
    protected static int intifyLocation(MapLocation loc) {
        return 0b1000000000000 | (loc.y << 6) | loc.x;
    }
    protected static void updateLocation(int index, MapLocation loc) throws GameActionException {
        int n = rc.readSharedArray(index);
        if (!hasLocation(n) || !parseLocation(n).equals(loc)) {
            rc.writeSharedArray(index, (n & 0b1110000000000000) | intifyLocation(loc));
        }
    }

    // flags
    protected static boolean isFlagPickedUp(int n) {
        return ((n >> 13) & 0b1) == 1;
    }
    protected static boolean isFlagInDanger(int n) {
        return ((n >> 14) & 0b1) == 1;
    }

    // flaginfos
    public static int getNumberOfRobots(int n) {
        return (n & 0b111111);
    }
    public static int setNumberOfRobots(int n, int v) {
        return (n & 0b1111111111000000) | v;
    }
    public static int getNumberOfFriendlyRobotsFlagInfo(int n) {
        return ((n >> 6) & 0b111111);
    }
    public static MapLocation getRobotDirection(int n) {
        return new MapLocation(((n >> 6) & 0b1111) - 8, ((n >> 10) & 0b1111) - 8);
    }

    // write flag
    protected static void writeFlag(FlagInfo flag) throws GameActionException {
        int flagId = flag.getID();
        if (flag.getTeam().equals(rc.getTeam())) {
            for (int i = 0; i <= 2; i++) {
                if (rc.readSharedArray(ALLY_FLAG_ID + i) == 0 || rc.readSharedArray(ALLY_FLAG_ID + i) == flagId) {
                    if (rc.readSharedArray(ALLY_FLAG_ID + i) == 0) {
                        rc.writeSharedArray(ALLY_FLAG_ID + i, flagId);
                    }
                    if (flag.isPickedUp()) {
                        MapLocation me = rc.getLocation();
                        rc.writeSharedArray(ALLY_FLAG_CUR_LOC + i, (1 << 14) | (1 << 13) | intifyLocation(flag.getLocation()));
                        // bug bug fix now
                        if (rc.getRoundNum() >= GameConstants.SETUP_ROUNDS) {
                            rc.writeSharedArray(ALLY_FLAG_INFO + i, ((flag.getLocation().y - me.y + 8) << 10) | ((flag.getLocation().x - me.x + 8) << 6) | Math.min(rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length * 2 + 5, 64));
                        }
                    } else {
                        // if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS) {
                        //     rc.writeSharedArray(ALLY_FLAG_DEF_LOC + i, intifyLocation(flag.getLocation()));
                        // }
                        MapLocation me = rc.getLocation();
                        if (rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length > 0) {
                            rc.writeSharedArray(ALLY_FLAG_CUR_LOC + i, (1 << 14) | intifyLocation(flag.getLocation()));
                            rc.writeSharedArray(ALLY_FLAG_INFO + i, ((flag.getLocation().y - me.y + 8) << 10) | ((flag.getLocation().x - me.x + 8) << 6) | Math.min(rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length * 2 + 5, 64));
                        }
                        else {
                            rc.writeSharedArray(ALLY_FLAG_CUR_LOC + i, intifyLocation(flag.getLocation()));
                        }
                    }
                    break;
                }
            }
        }
        else {
            FlagInfo[] flags = rc.senseNearbyFlags(0, rc.getTeam());
            for (int i = 0; i <= 2; i++) {
                if (rc.readSharedArray(OPPO_FLAG_ID + i) == 0 || rc.readSharedArray(OPPO_FLAG_ID + i) == flagId) {
                    if (rc.readSharedArray(OPPO_FLAG_ID + i) == 0) {
                        rc.writeSharedArray(OPPO_FLAG_ID + i, flagId);
                        rc.writeSharedArray(OPPO_FLAG_DEF_LOC + i, intifyLocation(flag.getLocation()));
                    }
                    if (flag.isPickedUp()) {
                        rc.writeSharedArray(OPPO_FLAG_CUR_LOC + i, (1 << 13) | intifyLocation(flag.getLocation()));
                    } else {
                        rc.writeSharedArray(OPPO_FLAG_CUR_LOC + i, intifyLocation(flag.getLocation()));
                    }
                    if (flags.length > 0 && flags[0].getID() == flagId) {
                        rc.writeSharedArray(OPPO_FLAG_INFO + i, (rc.senseNearbyRobots(-1, rc.getTeam()).length << 6) | rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length);
                    }
                    break;
                }
            }
        }
    }
    protected static void checkFlags(FlagInfo[] opponentFlags) throws GameActionException {
        // detect if flags were reset
        for (int i = 0; i <= 2; i++) {
            int n = rc.readSharedArray(OPPO_FLAG_CUR_LOC + i);
            if (hasLocation(n)) {
                if (rc.canSenseLocation(parseLocation(n))) {
                    boolean found = false;
                    for (FlagInfo flag : opponentFlags) {
                        if (flag.getLocation().equals(parseLocation(n)) && flag.getID() == rc.readSharedArray(OPPO_FLAG_ID + i)) {
                            found = true;
                        }
                    }
                    if (!found) {
                        rc.writeSharedArray(OPPO_FLAG_CUR_LOC + i, rc.readSharedArray(OPPO_FLAG_DEF_LOC + i));
                    }
                }
            }
        }
    }

    // staging target
    // REWRITE A BIT TO REMOVE GROUP OFFSET
    // protected static final int GROUP_OFFSET = 1;
    // protected static boolean isGlobalArrayLoc(int n) {
    //     return ((n >> 13) & 0b1) == 1;
    // }
    // protected static int intifyTarget(int index) {
    //     return 0b11000000000000 | index;
    // }
    // protected static int getGroupData(int index) throws GameActionException {
    //     return rc.readSharedArray(GROUP_INSTRUCTIONS + index - GROUP_OFFSET) >> 13;
    // }
    // protected static MapLocation getGroupTarget(int index) throws GameActionException {
    //     int n = rc.readSharedArray(GROUP_INSTRUCTIONS + index - GROUP_OFFSET);
    //     if (n == 0) {
    //         return null;
    //     }
    //     if (GlobalArray.isGlobalArrayLoc(n)) {
    //         int i = n & 0b111111;
    //         int n2 = rc.readSharedArray(i);
    //         if (!GlobalArray.hasLocation(n2)) {
    //             rc.writeSharedArray(GROUP_INSTRUCTIONS + index - GROUP_OFFSET, 0);
    //             return null;
    //         }
    //         if (i >= ALLY_FLAG_CUR_LOC && i <= ALLY_FLAG_CUR_LOC + 2) {
    //             MapLocation defaultLoc = parseLocation(rc.readSharedArray(i - 3));
    //             if (parseLocation(n2).equals(defaultLoc)) {
    //                 rc.writeSharedArray(GROUP_INSTRUCTIONS + index - GROUP_OFFSET, 0);
    //                 return null;
    //             }
    //         }
    //         if (i >= OPPO_FLAG_DEF_LOC && i <= OPPO_FLAG_DEF_LOC + 2) {
    //             int currLoc = rc.readSharedArray(i + 3);
    //             if (isFlagPickedUp(currLoc)) {
    //                 rc.writeSharedArray(GROUP_INSTRUCTIONS + index - GROUP_OFFSET, 0);
    //                 return null;
    //             }
    //         }
    //         if (i >= OPPO_FLAG_CUR_LOC && i <= OPPO_FLAG_CUR_LOC + 2) {
    //             int info = rc.readSharedArray(i + 3);
    //             // just give up buh
    //             if (getNumberOfRobots(info) - getNumberOfFriendlyRobotsFlagInfo(info) >= 15) {
    //                 rc.writeSharedArray(GROUP_INSTRUCTIONS + index - GROUP_OFFSET, 0);
    //                 return null;
    //             }
    //         }
    //         return GlobalArray.parseLocation(n2);
    //     }
    //     else {
    //         return GlobalArray.parseLocation(n);
    //     }
    // }

    // group staging
    // protected static int getDistance(int n) {
    //     return n & 0b111111111111;
    // }
    // protected static int setDistance(int n, int v) {
    //     return (n & 0b1111000000000000) | v;
    // }
    // protected static int getGroupId(int n) {
    //     return ((n >> 12) & 0b111) + GROUP_OFFSET;
    // }
    // protected static int setGroupId(int n, int v) {
    //     return (n & 0b1000111111111111) | ((v - GROUP_OFFSET) << 12);
    // }
    // protected static boolean isUnassigned(int n) {
    //     return ((n >> 15) & 0b1) == 1;
    // }

    // new group stuff
    protected static int getGroupRobotCount(int n) {
        return n >> 13;
    }
    protected static void joinGroup(int id) throws GameActionException {
        groupId = id;
        rc.writeSharedArray(GROUP_STAGING + groupId, incrementGroupRobotCount(rc.readSharedArray(GROUP_STAGING + groupId)));
    }
    protected static void leaveGroup() throws GameActionException {
        groupId = -1;
        groupLeader = false;
        rc.writeSharedArray(GROUP_STAGING + groupId, decrementGroupRobotCount(rc.readSharedArray(GROUP_STAGING + groupId)));
    }
    protected static int incrementGroupRobotCount(int n) {
        return (Math.min(8, (n >> 13) + 1) << 13) | (n & 0b1111111111111);
    }
    protected static int decrementGroupRobotCount(int n) {
        return (Math.max(0, (n >> 13) - 1) << 13) | (n & 0b1111111111111);
    }

    protected static void init() throws GameActionException {
        // get own id (used for groups and staging)
        id = rc.readSharedArray(INIT_GLOBAL_ID_COUNTER);
        rc.writeSharedArray(INIT_GLOBAL_ID_COUNTER, id + 1);
        // Motion.bfsInit();
    }
}
