package SPAARK_POIComms;

import battlecode.common.*;

public class GlobalArray {
    protected static RobotController rc;
    protected static StringBuilder indicatorString;

    protected static int id;

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
     * REPLACE WITH POI SYSTEM AHND DUCKS INDIVIDUALLY PICK TARGETS
     * LOCATION/INDEX, DUCK ALLOCATION COUNTER
     * SWARMING DONE IN MICRO
     * 63: Global id counter (first round only)
     * 63: Flag target heuristic (setup only)
    */
    /**
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
    protected static final int POI = 26;
    protected static final int SETUP_FLAG_WEIGHT = 63;
    protected static final int INIT_GLOBAL_ID_COUNTER = 63;

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
                        writePOI(flag.getLocation(), rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length);
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
    protected static void checkFlags(FlagInfo[] friendlyFlags, FlagInfo[] opponentFlags) throws GameActionException {
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

    
    // new POI stuff
    public static int getOpponentRobots(int n) {
        return (n & 0b111111);
    }
    public static int setOpponentRobots(int n, int v) {
        return (n & 0b1111111111000000) | v;
    }
    public static int getFriendlyRobots(int n) {
        return ((n >> 6) & 0b111111);
    }
    public static int setFriendlyRobots(int n, int v) {
        return (n & 0b1111000000111111) | (v << 6);
    }

    protected static void writePOI(MapLocation loc, int robots) throws GameActionException {
        for (int i = 0; i < 10; i += 2) {
            if (rc.readSharedArray(POI + i) == 0 || parseLocation(rc.readSharedArray(POI + i)).distanceSquaredTo(loc) <= 4) {
                rc.writeSharedArray(POI + i, intifyLocation(loc));
                rc.writeSharedArray(POI + i + 1, setOpponentRobots(rc.readSharedArray(POI + i + 1), robots));
                indicatorString.append("WRITTEN POI " + rc.readSharedArray(POI + i + 1));
                break;
            }
        }
    }
    protected static void updatePOI() throws GameActionException {
        RobotInfo[] friendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
        RobotInfo[] opponentRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (opponentRobots.length - friendlyRobots.length > 0) {
            int x = 0;
            int y = 0;
            for (RobotInfo robot : opponentRobots) {
                x += robot.getLocation().x;
                y += robot.getLocation().y;
            }
            x /= opponentRobots.length;
            y /= opponentRobots.length;
            // writePOI(new MapLocation(x, y), Math.max(opponentRobots.length - friendlyRobots.length, 0));
            writePOI(new MapLocation(x, y), opponentRobots.length);
        }
        else {
            for (int i = 0; i < 10; i += 2) {
                if (rc.readSharedArray(POI + i) != 0 && parseLocation(rc.readSharedArray(POI + i)).distanceSquaredTo(rc.getLocation()) <= 2) {
                    rc.writeSharedArray(POI + i, 0);
                    rc.writeSharedArray(POI + i + 1, 0);
                    break;
                }
            }
        }
    }
    protected static int lastPOI = -1;
    protected static int getBestPOI() throws GameActionException {
        MapLocation me = rc.getLocation();
        if (lastPOI != -1) {
            int lastN = rc.readSharedArray(POI + lastPOI + 1);
            if (getFriendlyRobots(lastN) > 0) {
                rc.writeSharedArray(POI + lastPOI + 1, setFriendlyRobots(lastN, getFriendlyRobots(lastN) - 1));
            }
        }
        MapLocation flag1 = parseLocation(rc.readSharedArray(ALLY_FLAG_CUR_LOC));
        MapLocation flag2 = parseLocation(rc.readSharedArray(ALLY_FLAG_CUR_LOC + 1));
        MapLocation flag3 = parseLocation(rc.readSharedArray(ALLY_FLAG_CUR_LOC + 2));
        int closestDist = -1;
        int closestIndex = -1;
        for (int i = 0; i < 10; i += 2) {
            int n = rc.readSharedArray(POI + i);
            int n2 = rc.readSharedArray(POI + i + 1);
            if (n != 0 && getOpponentRobots(n2) - getFriendlyRobots(n2) > 0) {
                MapLocation loc = parseLocation(n);
                if (closestDist == -1 || flag1.distanceSquaredTo(loc) < closestDist || flag2.distanceSquaredTo(loc) < closestDist || flag3.distanceSquaredTo(loc) < closestDist) {
                    closestIndex = i;
                    closestDist = Math.min(Math.min(flag1.distanceSquaredTo(loc), flag2.distanceSquaredTo(loc)), flag3.distanceSquaredTo(loc));
                }
                break;
            }
        }
        if (closestIndex != -1) {
            lastPOI = closestIndex;
            int n = rc.readSharedArray(POI + lastPOI + 1);
            rc.writeSharedArray(POI + lastPOI + 1, setFriendlyRobots(n, getFriendlyRobots(n) + 1));
            indicatorString.append(getFriendlyRobots(n) + " " + lastPOI + " " + n + " ");
            if (rc.getRoundNum() < 220) {
                // System.out.println(n + 1);
            }
        }
        return closestIndex;
    }

    protected static void init() throws GameActionException {
        // get own id (used for groups and staging)
        id = rc.readSharedArray(INIT_GLOBAL_ID_COUNTER);
        rc.writeSharedArray(INIT_GLOBAL_ID_COUNTER, id + 1);
        Motion.bfsInit();
    }
}
