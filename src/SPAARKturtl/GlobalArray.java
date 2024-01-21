package SPAARKturtl;

import battlecode.common.*;

public class GlobalArray {
    protected static RobotController rc;
    protected static StringBuilder indicatorString;

    protected static int id;
    protected static int flag;
    protected static int type;
    /*
     * 0: builder
     * 1: attacker
     * 2: healer
     */

    /*
     * 0-2: Ally flag ids
     * 3-5: Ally flag default locations
     * 6-8: Ally flag current locations
     * 9-11: Ally flag info
     * 12-14: Ally flag distance from dam (setup only)
     * 15-17: symmetry ROT flag 0, ROT flag 1, ROT flag 2 (setup only)
     * 18-20: symmetry VERT flag 0, VERT flag 1, VERT flag 2 (setup only)
     * 21-23: symmetry HORZ flag 0, HORZ flag 1, HORZ flag 2 (setup only)
     * 24: Flag target
     * 62: symmetry (0b110=6:ROT, 0b101=5:VERT, 0b011=3:HORZ, else unknown)
     * 63: Global id counter (first round only)
     * 63: Flag target heuristic (setup only)
    */
    /*
     * Formatting:
     * 
     * Location:
     * bit 1-6: X
     * bit 7-12: Y
     * bit 13: Presence indicator
     * 
     * Flags:
     * bit 14: Flag picked up
     * 
     * Flag Info:
     * bit 1-6: Number of opponent robots
     * bit 7-10: X of robot direction (which direction the robot with the flag is going)
     * bit 11-14: Y of robot direction
     * 
     * 
     */
    protected static final int ALLY_FLAG_ID = 0;
    protected static final int ALLY_FLAG_DEF_LOC = 3;
    protected static final int ALLY_FLAG_CUR_LOC = 6;
    protected static final int ALLY_FLAG_INFO = 9;
    protected static final int SETUP_FLAG_DISTANCE = 12;
    protected static final int SETUP_SYM_GUESS = 15;
    protected static final int SETUP_FLAG_TARGET = 24;
    protected static final int SETUP_DAM_LOC = 25;
    protected static final int SYM = 62;
    protected static final int SETUP_FLAG_WEIGHT = 63;
    protected static final int INIT_GLOBAL_ID_COUNTER = 63;

    protected static int[][] sectors;

    protected static void write(int index, int bits, int n) throws GameActionException {
        int r = rc.readSharedArray(index);
        for (int i = index / 16; i < (index + bits) / 16; i++) {
            for (int bit = Math.max(index - i * 16, 0); bit < Math.min(index + bits - i * 16, 16); bit++) {
                if (((r >> bit) | 1) != (n >> (i*16 + bit - index))) {
                    r ^= (int) Math.pow(2, bit);
                }
            }
        }
        rc.writeSharedArray(index, r);
    }

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
    protected static int updateLocation(int n, MapLocation loc) {
        return (n & 0b1110000000000000) | intifyLocation(loc);
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
                        // MapLocation me = rc.getLocation();
                        if (rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length > 0) {
                            rc.writeSharedArray(ALLY_FLAG_CUR_LOC + i, (1 << 14) | intifyLocation(flag.getLocation()));
                            rc.writeSharedArray(ALLY_FLAG_INFO + i, Math.min(rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length * 2 + 5, 64));
                        }
                        else {
                            rc.writeSharedArray(ALLY_FLAG_CUR_LOC + i, intifyLocation(flag.getLocation()));
                        }
                    }
                    break;
                }
            }
        }
    }

    // sector info
    protected static int getNumberOfOpponentRobots(int n) {
        return (n & 0b11111);
    }
    protected static int setNumberOfOpponentRobots(int n, int v) {
        return (n & 0b1111111111100000) | v;
    }
    protected static int getNumberOfFriendlyRobots(int n) {
        return ((n >> 5) & 0b11111);
    }
    protected static int setNumberOfFriendlyRobots(int n, int v) {
        return (n & 0b1111110000011111) | (v << 5);
    }
    protected static int getGroupsAssigned(int n) {
        return ((n >> 10) & 0b111);
    }
    protected static int setGroupsAssigned(int n, int v) {
        return (n & 0b1110001111111111) | (v << 10);
    }
    protected static int getTimeSinceLastExplored(int n) {
        return ((n >> 13) & 0b111);
    }
    protected static int setTimeSinceLastExplored(int n, int v) {
        return (n & 0b0001111111111111) | (v << 13);
    }

    // staging target
    protected static final int GROUP_OFFSET = 2;
    protected static boolean isGlobalArrayLoc(int n) {
        return ((n >> 13) & 0b1) == 1;
    }
    protected static int intifyTarget(int index) {
        return 0b11000000000000 | index;
    }

    // group staging
    protected static int getDistance(int n) {
        return n & 0b111111111111;
    }
    protected static int setDistance(int n, int v) {
        return (n & 0b1111000000000000) | v;
    }
    protected static int getGroupId(int n) {
        return ((n >> 12) & 0b111) + GROUP_OFFSET;
    }
    protected static int setGroupId(int n, int v) {
        return (n & 0b1000111111111111) | ((v - GROUP_OFFSET) << 12);
    }
    protected static boolean isUnassigned(int n) {
        return ((n >> 15) & 0b1) == 1;
    }

    protected static void init() throws GameActionException {
        // get own id (used for groups and staging)
        id = rc.readSharedArray(INIT_GLOBAL_ID_COUNTER);
        rc.writeSharedArray(INIT_GLOBAL_ID_COUNTER, id + 1);
        Motion.bfsInit();
        if (id % 16 < 1 && id < 48) {
            type = 0;
        } else if (id % 16 < 9 && id < 48) {
            type = 1;
        } else {
            type = 2;
        }
        flag = Math.min(id / 16, 2);
    }
}
