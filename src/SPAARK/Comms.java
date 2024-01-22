package SPAARK;

import battlecode.common.*;

public class Comms {
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
     * 26-28: symmetry ROT flag 0, ROT flag 1, ROT flag 2 (setup only)
     * 29-31: symmetry VERT flag 0, VERT flag 1, VERT flag 2 (setup only)
     * 32-34: symmetry HORZ flag 0, HORZ flag 1, HORZ flag 2 (setup only)
     * 26-55: Points of Interest
     * 62: spawn zone connectedness
     * 62: symmetry (0b110=6:ROT, 0b101=5:VERT, 0b011=3:HORZ)
     * 63: Global id counter (first round only)
     * 63: Flag target heuristic (setup only)
    */
    /**
     * Formatting:
     * 
     * Location:
     *   bit 1-6: X
     *   bit 7-12: Y
     *   bit 13: Presence indicator
     * 
     * Flags:
     *   bit 14: Flag picked up
     * 
     * Flag Info:
     *   bit 1-6: Number of opponent robots
     *   bit 7-10: X of robot direction (which direction the robot with the flag is going)
     *   bit 11-14: Y of robot direction
     * 
     * POI:
     * Pairs of indices
     * 1:
     *   bit 1-6: Number of opponent robots
     *   bit 7-12: Number of friendly robots
     *   bit 13-15: Flag index
     * 2:
     *   maitian go write this or soemthing
     * 
     * Bit 62:
     *   bit 1-3: symmetry
     *   bit 4-6: connectedness of spawn zones
     *      bit 4 means spawn zones 1,2
     *      bit 5 means spawn zones 2,3
     *      bit 6 means spawn zones 3,1
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
    protected static final int SETUP_SYM_GUESS = 26;
    protected static final int POI = 26;
    protected static final int SPAWN_CONNECTED = 62;
    protected static final int SYM = 62;
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
                    if (rc.getRoundNum() > GameConstants.SETUP_ROUNDS) {
                        if (flag.isPickedUp() || !flag.getLocation().equals(parseLocation(rc.readSharedArray(ALLY_FLAG_DEF_LOC + i)))) {
                            writePOI(flag.getLocation(), rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length, i + 1);
                            // writePOI(flag.getLocation(), 50);
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
            int n = rc.readSharedArray(ALLY_FLAG_CUR_LOC + i);
            if (hasLocation(n)) {
                if (rc.canSenseLocation(parseLocation(n))) {
                    boolean found = false;
                    for (FlagInfo flag : friendlyFlags) {
                        if (flag.getLocation().equals(parseLocation(n)) && flag.getID() == rc.readSharedArray(OPPO_FLAG_ID + i)) {
                            found = true;
                        }
                    }
                    if (!found) {
                        if (parseLocation(rc.readSharedArray(ALLY_FLAG_CUR_LOC + i)).equals(parseLocation(rc.readSharedArray(ALLY_FLAG_DEF_LOC + i)))) {
                            rc.writeSharedArray(ALLY_FLAG_CUR_LOC + i, 0);
                        }
                        else {
                            rc.writeSharedArray(ALLY_FLAG_CUR_LOC + i, rc.readSharedArray(ALLY_FLAG_DEF_LOC + i));
                        }
                    }
                }
            }
        }
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
    public static boolean isFlag(int n) {
        return ((n >> 12) & 0b11) > 0;
    }
    public static int getFlag(int n) {
        return ((n >> 12) & 0b11);
    }

    protected static void writePOI(MapLocation loc, int robots, int flag) throws GameActionException {
        for (int i = 0; i < 30; i += 2) {
            boolean empty = (!hasLocation(rc.readSharedArray(POI + i)) && rc.getRoundNum() >= rc.readSharedArray(POI + i));
            if (empty || parseLocation(rc.readSharedArray(POI + i)).distanceSquaredTo(loc) <= 8) {
                // if (rc.getRoundNum() == 470) {
                    // System.out.println("WRITE " + i + " " + rc.readSharedArray(POI + i + 1) + " " + parseLocation(rc.readSharedArray(POI + i)) + " " + loc);
                // }
                if (!empty && getFlag(rc.readSharedArray(POI + i + 1)) != flag) {
                    continue;
                }
                rc.setIndicatorLine(rc.getLocation(), loc, 0, 255, 255);
                rc.writeSharedArray(POI + i, intifyLocation(loc));
                rc.writeSharedArray(POI + i + 1, (flag << 12) | setOpponentRobots(rc.readSharedArray(POI + i + 1), robots));
                for (int j = 0; j < 30; j += 2) {
                    if (j == i) {
                        continue;
                    }
                    if (hasLocation(rc.readSharedArray(POI + j)) && parseLocation(rc.readSharedArray(POI + j)).distanceSquaredTo(loc) <= 8 && getFlag(rc.readSharedArray(POI + j + 1)) == flag) {
                        rc.writeSharedArray(POI + j + 1, 0);
                        if (rc.getRoundNum() == 470) {
                            // System.out.println("RESET " + j);
                        }
                    }
                }
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
            writePOI(new MapLocation(x, y), opponentRobots.length, 0);
        }
        else if (opponentRobots.length - friendlyRobots.length <= 0) {
            for (int i = 0; i < 30; i += 2) {
                if (hasLocation(rc.readSharedArray(POI + i)) && parseLocation(rc.readSharedArray(POI + i)).distanceSquaredTo(rc.getLocation()) <= 8) {
                    if (isFlag(rc.readSharedArray(POI + i + 1))) {
                        int index = getFlag(rc.readSharedArray(POI + i + 1)) - 1;
                        if (!hasLocation(rc.readSharedArray(ALLY_FLAG_CUR_LOC + index)) || (parseLocation(rc.readSharedArray(ALLY_FLAG_CUR_LOC + index)).equals(parseLocation(rc.readSharedArray(ALLY_FLAG_DEF_LOC + index))) && !isFlagInDanger(rc.readSharedArray(ALLY_FLAG_CUR_LOC + index)))) {
                            rc.writeSharedArray(POI + i, rc.getRoundNum() + 2);
                            rc.writeSharedArray(POI + i + 1, 0);
                        }
                    }
                    else {
                        rc.writeSharedArray(POI + i, rc.getRoundNum() + 2);
                        rc.writeSharedArray(POI + i + 1, 0);
                    }
                    // System.out.println("DELETED " + i);
                }
            }
        }
    }
    protected static int lastPOI = -1;
    protected static MapLocation getBestPOI() throws GameActionException {
        // MapLocation me = rc.getLocation();
        if (lastPOI != -1 && hasLocation(rc.readSharedArray(POI + lastPOI))) {
            int lastN = rc.readSharedArray(POI + lastPOI + 1);
            if (getFriendlyRobots(lastN) > 0) {
                rc.writeSharedArray(POI + lastPOI + 1, setFriendlyRobots(lastN, getFriendlyRobots(lastN) - 1));
                // if (rc.getRoundNum() == 470) {
                    // System.out.println("LAST " + lastPOI + " " + rc.readSharedArray(POI + lastPOI + 1) + " " + parseLocation(rc.readSharedArray(POI + lastPOI)));
                // }
            }
        }
        MapLocation flag1 = parseLocation(rc.readSharedArray(ALLY_FLAG_CUR_LOC));
        MapLocation flag2 = parseLocation(rc.readSharedArray(ALLY_FLAG_CUR_LOC + 1));
        MapLocation flag3 = parseLocation(rc.readSharedArray(ALLY_FLAG_CUR_LOC + 2));
        int closestDist = -1;
        int closestIndex = -1;
        boolean closestIsFlag = false;
        int closestNeededRobots = 0;
        for (int i = 0; i < 30; i += 2) {
            int n = rc.readSharedArray(POI + i);
            int n2 = rc.readSharedArray(POI + i + 1);
            if (hasLocation(n) && getOpponentRobots(n2) - getFriendlyRobots(n2) > 0) {
                if (rc.getRoundNum() == 235) {
                    // System.out.println(i);
                }
                MapLocation loc = parseLocation(n);
                if (closestIsFlag) {
                    if (isFlag(n2)) {
                        if (closestDist == -1) {
                            int dist = Math.min(Math.min(flag1.distanceSquaredTo(loc), flag2.distanceSquaredTo(loc)), flag3.distanceSquaredTo(loc));
                            if (dist > closestDist) {
                                closestIndex = i;
                                closestDist = Math.min(Math.min(flag1.distanceSquaredTo(loc), flag2.distanceSquaredTo(loc)), flag3.distanceSquaredTo(loc));
                                closestNeededRobots = getOpponentRobots(n2) - getFriendlyRobots(n2);
                            }
                        }
                    }
                }
                else {
                    if (isFlag(n2)) {
                        closestIndex = i;
                        closestDist = Math.min(Math.min(flag1.distanceSquaredTo(loc), flag2.distanceSquaredTo(loc)), flag3.distanceSquaredTo(loc));
                        closestNeededRobots = getOpponentRobots(n2) - getFriendlyRobots(n2);
                        closestIsFlag = true;
                    }
                    else {
                        int min = Math.min(Math.min(flag1.distanceSquaredTo(loc), flag2.distanceSquaredTo(loc)), flag3.distanceSquaredTo(loc));;
                        if (closestDist == -1 || (min - closestDist + (-(getOpponentRobots(n2) - getFriendlyRobots(n2)) + closestNeededRobots) * 4) < 0) {
                            closestIndex = i;
                            closestDist = 
                            closestNeededRobots = getOpponentRobots(n2) - getFriendlyRobots(n2);
                        }
                    }
                }
                break;
            }
        }
        lastPOI = closestIndex;
        if (closestIndex != -1) {
            int n = rc.readSharedArray(POI + lastPOI + 1);
            rc.writeSharedArray(POI + lastPOI + 1, setFriendlyRobots(n, getFriendlyRobots(n) + 1));
            // if (rc.getRoundNum() == 470) {
                // System.out.println("CURR " + lastPOI + " " + rc.readSharedArray(POI + lastPOI + 1));
            // }
            // if (rc.getRoundNum() > 300) {
            //     rc.resign();
            // }
            if (closestIsFlag) {
                MapLocation loc = parseLocation(rc.readSharedArray(POI + lastPOI));
                MapLocation startLoc = parseLocation(rc.readSharedArray(ALLY_FLAG_DEF_LOC + getFlag(n)));
                if (startLoc.distanceSquaredTo(loc) > 8) {
                    Direction d = startLoc.directionTo(loc);
                    loc = loc.add(d).add(d).add(d);
                    if (loc.x < 0) {
                        loc = new MapLocation(0, loc.y);
                    }
                    if (loc.x >= rc.getMapWidth()) {
                        loc = new MapLocation(rc.getMapWidth() - 1, loc.y);
                    }
                    if (loc.y < 0) {
                        loc = new MapLocation(loc.x, 0);
                    }
                    if (loc.y >= rc.getMapHeight()) {
                        loc = new MapLocation(loc.x, rc.getMapHeight() - 1);
                    }
                }
                return loc;
            }
            else {
                return parseLocation(rc.readSharedArray(POI + lastPOI));
            }
        }
        // if (id == 45 && rc.getRoundNum() >= 290 && rc.getRoundNum() < 300) {
        //     for (int i = 0; i < 30; i += 2) {
        //         int n = rc.readSharedArray(POI + i);
        //         int n2 = rc.readSharedArray(POI + i + 1);
        //         System.out.println(i + " " + parseLocation(n) + " " + getFriendlyRobots(n2) + " " + getOpponentRobots(n2));
        //     }
        // }
        return null;
    }

    protected static void init() throws GameActionException {
        // get own id (used for groups and staging)
        id = rc.readSharedArray(INIT_GLOBAL_ID_COUNTER);
        rc.writeSharedArray(INIT_GLOBAL_ID_COUNTER, id + 1);
        Motion.bfsInit();
    }
}
