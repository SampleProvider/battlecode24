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
     * 9-11: Ally flag number of robots and robot direction
     * 12-14: Opponent flag ids
     * 15-17: Opponent flag default locations
     * 18-20: Opponent flag current locations
     * 21-23: Opponent flag infos
     * 24: Flag target (setup only)
     * 25: Gathering point (setup only)
     * 24: Staging target
     * 25: Staging best
     * 26: Staging current
     * 27-30: Robot POI
     * rotating group staging?
     * rotates over 5 rounds - 12 indices (6 per group)
     * 1 index for location of one of the robots
     * other robots fill themselves in by distance in the same turn
      * 
     * use another 9 indices for teh actual targets of the group as dictated by leader (first robot in group)
     * 63: Global id counter (first round only), flag target heuristic (setup only)
     * 
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
     * Sectors:
     * bit 1-5: Number of opponent robots
     * bit 6-10: Number of friendly robots
     * bit 11-13: Number of assigned groups
     * bit 14-16: Time since last explored
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
    protected static final int SECTOR_START = 26;
    protected static final int SECTOR_SIZE = 5;
    protected static final int GROUP_INSTRUCTIONS = 51;
    protected static final int STAGING_TARGET = 59;
    protected static final int STAGING_BEST = 60;
    protected static final int STAGING_CURR = 61;
    protected static final int SECTOR_COUNT = SECTOR_SIZE * SECTOR_SIZE;
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

    protected static MapLocation sectorToLocation(int index) {
        int x = sectorX[index % SECTOR_SIZE] + sectorWidth[index % SECTOR_SIZE] / 2;
        int y = sectorY[index / SECTOR_SIZE] + sectorHeight[index / SECTOR_SIZE] / 2;
        return new MapLocation(x, y);
    }
    protected static int locationToSector(MapLocation loc) {
        return sectors[loc.x][loc.y];
    }
    protected static int locationToSectorInit(MapLocation loc) {
        int x = 4;
        for (int i = 0; i < SECTOR_SIZE; i++) {
            if (sectorX[i] > loc.x) {
                x = i - 1;
                break;
            }
        }
        int y = 4;
        for (int i = 0; i < SECTOR_SIZE; i++) {
            if (sectorY[i] > loc.y) {
                y = i - 1;
                break;
            }
        }
        return y * SECTOR_SIZE + x;
    }

    protected static final double MININUM_SENSED_TILES = 0.1;
    protected static String updateSector() throws GameActionException {
        // indicatorString.append(" " + Clock.getBytecodeNum() + " ");
        MapLocation[] locs = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 20);
        int[] sectors = new int[SECTOR_COUNT];
        int[] friendly = new int[SECTOR_COUNT];
        int[] opponents = new int[SECTOR_COUNT];
        // indicatorString.append(" " + Clock.getBytecodeNum() + " ");

        RobotInfo[] robots = rc.senseNearbyRobots(-1);

        for (MapLocation loc : locs) {
            int sector = locationToSector(loc);
            sectors[sector] += 1;
            // if (rc.isLocationOccupied(loc)) {
            //     RobotInfo robot = rc.senseRobotAtLocation(loc);
            //     if (robot.getTeam() == rc.getTeam()) {
            //         friendly[sector] += 1;
            //     }
            //     else {
            //         opponents[sector] += 1;
            //     }
            // }
        }
        // indicatorString.append(" " + Clock.getBytecodeNum() + " ");
        for (RobotInfo robot : robots) {
            int sector = locationToSector(robot.getLocation());
            if (robot.getTeam() == rc.getTeam()) {
                friendly[sector] += 1;
            }
            else {
                opponents[sector] += 1;
            }
        }
        // indicatorString.append(" " + Clock.getBytecodeNum() + " ");

        StringBuilder updatedSectors = new StringBuilder();

        for (int i = 0; i < SECTOR_COUNT; i++) {
            int size = sectorWidth[i % SECTOR_SIZE] * sectorHeight[i / SECTOR_SIZE];
            if (sectors[i] > (int) size * MININUM_SENSED_TILES) {
                int newFriendly = Math.min(friendly[i] * size / sectors[i], 31);
                int newOpponents = Math.min(opponents[i] * size / sectors[i], 31);
                int n = setTimeSinceLastExplored(setNumberOfFriendlyRobots(setNumberOfOpponentRobots(rc.readSharedArray(i), newOpponents), newFriendly), 0);
                rc.writeSharedArray(SECTOR_START + i, n);
                if (sectors[i] > (int) size * 0.4) {
                    updatedSectors.append(i + "A");
                }
            }
        }
        // indicatorString.append(" " + Clock.getBytecodeNum() + " ");
        return updatedSectors.toString();
    }

    protected static void incrementSectorTime() throws GameActionException {
        if (rc.getRoundNum() % 16 != 0) {
            return;
        }
        for (int i = 0; i < SECTOR_COUNT; i++) {
            int n = rc.readSharedArray(SECTOR_START + i);
            n = setTimeSinceLastExplored(n, Math.min(getTimeSinceLastExplored(n) + 1, 7));
            rc.writeSharedArray(SECTOR_START + i, n);
        }
    }

    // staging target
    protected static final int GROUP_OFFSET = 2;
    protected static boolean isGlobalArrayLoc(int n) {
        return ((n >> 13) & 0b1) == 1;
    }
    protected static int intifyTarget(int index) {
        return 0b11000000000000 | index;
    }
    protected static int getGroupData(int index) throws GameActionException {
        return rc.readSharedArray(GROUP_INSTRUCTIONS + index - GROUP_OFFSET) >> 13;
    }
    protected static MapLocation getGroupTarget(int index) throws GameActionException {
        int n = rc.readSharedArray(GROUP_INSTRUCTIONS + index - GROUP_OFFSET);
        if (n == 0) {
            return null;
        }
        if (GlobalArray.isGlobalArrayLoc(n)) {
            int i = n & 0b111111;
            int n2 = rc.readSharedArray(i);
            if (!GlobalArray.hasLocation(n2)) {
                rc.writeSharedArray(GROUP_INSTRUCTIONS + index - GROUP_OFFSET, 0);
                return null;
            }
            if (i >= ALLY_FLAG_CUR_LOC && i <= ALLY_FLAG_CUR_LOC + 2) {
                MapLocation defaultLoc = parseLocation(rc.readSharedArray(i - 3));
                if (parseLocation(n2).equals(defaultLoc)) {
                    rc.writeSharedArray(GROUP_INSTRUCTIONS + index - GROUP_OFFSET, 0);
                    return null;
                }
            }
            if (i >= OPPO_FLAG_DEF_LOC && i <= OPPO_FLAG_DEF_LOC + 2) {
                int currLoc = rc.readSharedArray(i + 3);
                if (isFlagPickedUp(currLoc)) {
                    rc.writeSharedArray(GROUP_INSTRUCTIONS + index - GROUP_OFFSET, 0);
                    return null;
                }
            }
            if (i >= OPPO_FLAG_CUR_LOC && i <= OPPO_FLAG_CUR_LOC + 2) {
                int info = rc.readSharedArray(i + 3);
                // just give up buh
                if (getNumberOfRobots(info) - getNumberOfFriendlyRobotsFlagInfo(info) >= 15) {
                    rc.writeSharedArray(GROUP_INSTRUCTIONS + index - GROUP_OFFSET, 0);
                    return null;
                }
            }
            return GlobalArray.parseLocation(n2);
        }
        else {
            return GlobalArray.parseLocation(n);
        }
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
    
    protected static StringBuilder[] sectorGroupsAssigned;
    protected static StringBuilder[] friendlyFlagGroupsAssigned = new StringBuilder[3];
    protected static StringBuilder[] opponentFlagGroupsAssigned = new StringBuilder[3];
    protected static void allocateGroups() throws GameActionException {
        if (rc.getRoundNum() % 2 == 0) {
            // find stolen flags to allocate
            // find sectors to allocate
            // if no sectors, find flags to allocate

            // no sectors right now because its kinda bad
            // for (int i = 0; i < SECTOR_COUNT; i++) {
            //     int n = rc.readSharedArray(SECTOR_START + i);
            //     if (getNumberOfOpponentRobots(n) - sectorGroupsAssigned[i].length() <= -5) {
            //         // System.out.println(sectorGroupsAssigned[i].length());
            //         // System.out.println(sectorGroupsAssigned[i].toString());
            //         String s = sectorGroupsAssigned[i].substring(sectorGroupsAssigned[i].length() - 5);
            //         int g = Integer.parseInt(s) - 10000;
            //         rc.writeSharedArray(GROUP_INSTRUCTIONS + g - GROUP_OFFSET, 0);
            //         sectorGroupsAssigned[i].delete(sectorGroupsAssigned[i].length() - 5, sectorGroupsAssigned[i].length());
            //     }
            // }
            // // rc.writeSharedArray(STAGING_TARGET, 0b11000000000000 | OPPO_FLAG_CUR_LOC);
            // // rc.writeSharedArray(STAGING_BEST, 0b111111111111);
            // for (int i = 0; i < SECTOR_COUNT; i++) {
            //     int n = rc.readSharedArray(SECTOR_START + i);
            //     if (getNumberOfOpponentRobots(n) - sectorGroupsAssigned[i].length() >= 5) {
            //         rc.writeSharedArray(STAGING_TARGET, intifyLocation(sectorToLocation(i)));
            //         rc.writeSharedArray(STAGING_BEST, 0b111111111111);
            //         // n = setGroupsAssigned(n, getGroupsAssigned(n) + 1);
            //         // rc.writeSharedArray(i, n);
            //         return;
            //     }
            // }
            for (int i = 0; i <= 2; i++) {
                int n = rc.readSharedArray(ALLY_FLAG_CUR_LOC + i);
                if (GlobalArray.isFlagInDanger(n) && GlobalArray.hasLocation(n)) {
                    int d = rc.readSharedArray(ALLY_FLAG_INFO + i);
                    // int number = GlobalArray.getNumberOfRobots(d) - friendlyFlagGroupsAssigned[i] * 5;
                    int number = GlobalArray.getNumberOfRobots(d);
                    for (int j = 0; j < 8; j++) {
                        if (rc.readSharedArray(GROUP_INSTRUCTIONS + j) == intifyTarget(ALLY_FLAG_CUR_LOC + i)) {
                            number -= 5;
                        }
                    }
                    if (number > 0) {
                        // rc.writeSharedArray(ALLY_FLAG_INFO + i, GlobalArray.setNumberOfRobots(d, Math.max(number - 5, 0)));
                        rc.writeSharedArray(STAGING_TARGET, intifyTarget(ALLY_FLAG_CUR_LOC + i));
                        rc.writeSharedArray(STAGING_BEST, 0b111111111111);
                        return;
                    }
                }
            }
            rc.writeSharedArray(STAGING_TARGET, 0);
        }
        else {
            // if there is a target:
            // if it is a sector
            int n = rc.readSharedArray(STAGING_TARGET);
            if (n != 0) {
                if (isGlobalArrayLoc(n)) {
                    int index = n & 0b111111;
                    if (index >= ALLY_FLAG_CUR_LOC && index <= ALLY_FLAG_CUR_LOC + 2) {
                        friendlyFlagGroupsAssigned[index - ALLY_FLAG_CUR_LOC].append(10000 + getGroupId(n) + "");
                    }
                    else {
                        opponentFlagGroupsAssigned[index - OPPO_FLAG_DEF_LOC].append(10000 + getGroupId(n) + "");
                    }
                }
                else {
                    sectorGroupsAssigned[locationToSector(parseLocation(n))].append(10000 + getGroupId(n) + ""); 
                }
            }
        }
    }

    protected static void init() throws GameActionException {
        // get own id (used for groups and staging)
        id = rc.readSharedArray(INIT_GLOBAL_ID_COUNTER);
        rc.writeSharedArray(INIT_GLOBAL_ID_COUNTER, id + 1);
        // divide the map into sectors
        sectorWidth = new int[SECTOR_SIZE];
        for (int i = 0; i < SECTOR_SIZE; i++) {
            sectorWidth[i] = rc.getMapWidth() / SECTOR_SIZE;
            if (rc.getMapWidth() % SECTOR_SIZE >= i) {
                sectorWidth[i] += 1;
            }
        }
        sectorHeight = new int[SECTOR_SIZE];
        for (int i = 0; i < SECTOR_SIZE; i++) {
            sectorHeight[i] = rc.getMapHeight() / SECTOR_SIZE;
            if (rc.getMapHeight() % SECTOR_SIZE >= i) {
                sectorHeight[i] += 1;
            }
        }
        sectorX = new int[SECTOR_SIZE];
        for (int i = 1; i < SECTOR_SIZE; i++) {
            sectorX[i] = sectorX[i - 1] + sectorWidth[i - 1];
        }
        sectorY = new int[SECTOR_SIZE];
        for (int i = 1; i < SECTOR_SIZE; i++) {
            sectorY[i] = sectorY[i - 1] + sectorHeight[i - 1];
        }
        sectors = new int[rc.getMapWidth()][rc.getMapHeight()];
        int x = 0;
        for (int i = 0; i < rc.getMapWidth(); i++) {
            if (x < 4 && i == sectorX[x + 1]) {
                x += 1;
            }
            int y = 0;
            for (int j = 0; j < rc.getMapHeight(); j++) {
                if (y < 4 && j == sectorY[y + 1]) {
                    y += 1;
                }
                sectors[i][j] = y * 5 + x;
            }
        }
        // set up groups here, leaders are just the first duck in the group
        groupId = id / 5;
        groupLeader = id % 5 == 0;
        sectorGroupsAssigned = new StringBuilder[SECTOR_COUNT];
        for (int i = 0; i < SECTOR_COUNT; i++) {
            sectorGroupsAssigned[i] = new StringBuilder();
        }
        for (int i = 0; i < 3; i++) {
            opponentFlagGroupsAssigned[i] = new StringBuilder();
            friendlyFlagGroupsAssigned[i] = new StringBuilder();
        }
        // Motion.bfsInit();
    }

}
