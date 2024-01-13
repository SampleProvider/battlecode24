package SPAARK_GROUPS_N_DEFENSE;

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
     * 0-2: ally flag ids
     * 3-5: ally flag default locations
     * 6-8: ally flag current locations
     * 9-11: ally flag number of robots and robot direction
     * 12-14: opponent flag ids
     * 15-17: opponent flag locations
     * 18: Flag target (setup only)
     * 19: Gathering point (setup only)
     * 20-44: Sectors
     * 45-52: Group instructions
     * 63: global id counter (first round only, can overwrite later)
     * 
     * Sectors:
     * 6: Number of opponent robots
     * 6: Number of friendly robots
     * 4: Turns since last explored
     * 
     * Groups:
     * 13: Location
     * 3: idk
     */
    protected static final int ALLY_FLAG_ID = 0;
    protected static final int ALLY_FLAG_DEF_LOC = 3;
    protected static final int ALLY_FLAG_CUR_LOC = 6;
    protected static final int ALLY_FLAG_INFO = 9;
    protected static final int OPPO_FLAG_ID = 12;
    protected static final int OPPO_FLAG_LOC = 15;
    protected static final int SETUP_FLAG_TARGET = 18;
    protected static final int SETUP_GATHER_LOC = 19;
    protected static final int SECTOR_START = 20;
    protected static final int SECTOR_SIZE = 5;
    protected static final int GROUP_INSTRUCTIONS = 45;
    protected static final int SECTOR_COUNT = SECTOR_SIZE * SECTOR_SIZE;

    protected static int[][] sectors;

    protected static boolean hasLocation(int n) {
        return (n >> 12 & 0b1) == 1;
    }
    protected static MapLocation parseLocation(int n) {
        return new MapLocation(n & 0b111111, (n >> 6) & 0b111111);
    }
    protected static int intifyLocation(MapLocation loc) {
        return 0b1000000000000 | (loc.y << 6) | loc.x;
    }
    protected static boolean isFlagPickedUp(int n) {
        return ((n >> 13) & 0b1) == 1;
    }
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

    protected static final double MININUM_SENSED_TILES = 0.4;
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
                int n = setNumberOfFriendlyRobots(setNumberOfOpponentRobots(0, newOpponents), newFriendly);
                rc.writeSharedArray(SECTOR_START + i, n);
                updatedSectors.append(i + "A");
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

    protected static void updateLocation(int index, MapLocation loc) throws GameActionException {
        int n = rc.readSharedArray(index);
        if (!hasLocation(n) || !parseLocation(n).equals(loc)) {
            rc.writeSharedArray(index, (n & 0b1110000000000000) | intifyLocation(loc));
        }
    }
    protected static void writeFlag(FlagInfo flag) throws GameActionException {
        int flagId = flag.getID();
        if (flag.getTeam().equals(rc.getTeam())) {
            for (int i = 0; i <= 2; i++) {
                if (rc.readSharedArray(ALLY_FLAG_ID + i) == 0 || rc.readSharedArray(ALLY_FLAG_ID + i) == flagId) {
                    if (rc.readSharedArray(ALLY_FLAG_ID + i) == 0) {
                        rc.writeSharedArray(ALLY_FLAG_ID + i, flagId);
                    }
                    if (flag.isPickedUp()) {
                        rc.writeSharedArray(ALLY_FLAG_CUR_LOC + i, (1 << 13) | intifyLocation(flag.getLocation()));
                    } else {
                        if (rc.getRoundNum() < 200) {
                            rc.writeSharedArray(ALLY_FLAG_DEF_LOC + i, intifyLocation(flag.getLocation()));
                        }
                        rc.writeSharedArray(ALLY_FLAG_CUR_LOC + i, intifyLocation(flag.getLocation()));
                    }
                    break;
                }
            }
        }
        else {
            for (int i = 0; i <= 2; i++) {
                if (rc.readSharedArray(OPPO_FLAG_ID + i) == 0 || rc.readSharedArray(OPPO_FLAG_ID + i) == flagId) {
                    if (rc.readSharedArray(OPPO_FLAG_ID + i) == 0) {
                        rc.writeSharedArray(OPPO_FLAG_ID + i, flagId);
                    }
                    if (flag.isPickedUp()) {
                        rc.writeSharedArray(OPPO_FLAG_LOC + i, (1 << 13) | intifyLocation(flag.getLocation()));
                    } else {
                        rc.writeSharedArray(OPPO_FLAG_LOC + i, intifyLocation(flag.getLocation()));
                    }
                    break;
                }
            }
        }
    }

    protected static MapLocation groupTarget(int index) throws GameActionException {
        return parseLocation(rc.readSharedArray(GROUP_INSTRUCTIONS + index - 2));
    }
    protected static int groupData(int index) throws GameActionException {
        return rc.readSharedArray(GROUP_INSTRUCTIONS + index - 2) >> 13;
    }

    protected static void init() throws GameActionException {
        // get own id (used for groups and staging)
        id = rc.readSharedArray(63);
        rc.writeSharedArray(63, id + 1);
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
        for (int i = 0; i < rc.getMapWidth(); i++) {
            for (int j = 0; j < rc.getMapHeight(); j++) {
                sectors[i][j] = locationToSectorInit(new MapLocation(i, j));
            }
        }
        // set up groups here, leaders are just the first duck in the group
        groupId = id / 5;
        groupLeader = id % 5 == 0;
    }
}
