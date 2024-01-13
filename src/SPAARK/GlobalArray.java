package SPAARK;

import battlecode.common.*;

public class GlobalArray {
    protected static RobotController rc;
    protected static StringBuilder indicatorString;

    protected static int id;

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
     * 63: global id counter (first round only, can overwrite later)
     * 
     * Sectors:
     * Number of opponent robots (64 - 6)
     * Number of friendly robots (64 - 6)
     * Turns since last explored (16 - 4)
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
    protected static final int SECTOR_COUNT = GlobalArray.SECTOR_SIZE * GlobalArray.SECTOR_SIZE;

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
        for (int i = index / 16; i < (index + bits) / 16; i++) {
            int r = rc.readSharedArray(index);
            for (int bit = Math.max(index - i * 16, 0); bit < Math.min(index + bits - i * 16, 16); bit++) {
                if (((r >> bit) | 1) != (n >> (i*16 + bit - index))) {
                    r ^= (int) Math.pow(2, bit);
                }
            }
            rc.writeSharedArray(index, r);
        }
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
    protected static int getTimeSinceLastExplored(int n) {
        return ((n >> 10) & 0b111111);
    }
    protected static int setTimeSinceLastExplored(int n, int v) {
        return (n & 0b0000001111111111) | (v << 10);
    }

    protected static MapLocation sectorToLocation(int index) {
        int x = sectorX[index % GlobalArray.SECTOR_SIZE] + sectorWidth[index % GlobalArray.SECTOR_SIZE] / 2;
        int y = sectorY[index / GlobalArray.SECTOR_SIZE] + sectorHeight[index / GlobalArray.SECTOR_SIZE] / 2;
        return new MapLocation(x, y);
    }
    protected static int locationToSector(MapLocation loc) {
        int x = 4;
        for (int i = 0; i < GlobalArray.SECTOR_SIZE; i++) {
            if (sectorX[i] > loc.x) {
                x = i - 1;
                break;
            }
        }
        int y = 4;
        for (int i = 0; i < GlobalArray.SECTOR_SIZE; i++) {
            if (sectorY[i] > loc.y) {
                y = i - 1;
                break;
            }
        }
        return y * GlobalArray.SECTOR_SIZE + x;
    }

    protected static double MININUM_SENSED_TILES = 0.5;
    protected static void updateSector() throws GameActionException {
        MapLocation[] locs = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 20);
        int[] sectors = new int[GlobalArray.SECTOR_COUNT];
        int[] friendly = new int[GlobalArray.SECTOR_COUNT];
        int[] opponents = new int[GlobalArray.SECTOR_COUNT];

        for (MapLocation loc : locs) {
            int sector = locationToSector(loc);
            sectors[sector] += 1;
            if (rc.canSenseRobotAtLocation(loc)) {
                RobotInfo robot = rc.senseRobotAtLocation(loc);
                if (robot.getTeam() == rc.getTeam()) {
                    friendly[sector] += 1;
                }
                else {
                    opponents[sector] += 1;
                }
            }
        }

        for (int i = 0; i < GlobalArray.SECTOR_COUNT; i++) {
            int size = sectorWidth[i % GlobalArray.SECTOR_SIZE] * sectorHeight[i / GlobalArray.SECTOR_SIZE];
            if (sectors[i] > (int) size * MININUM_SENSED_TILES) {
                int newFriendly = Math.min(friendly[i] * size / sectors[i], 31);
                int newOpponents = Math.min(opponents[i] * size / sectors[i], 31);
                int n = setNumberOfFriendlyRobots(setNumberOfOpponentRobots(0, newOpponents), newFriendly);
                rc.writeSharedArray(SECTOR_START + i, n);
            }
        }
    }

    protected static void incrementSectorTime() throws GameActionException {
        if (rc.getRoundNum() % 4 != 0) {
            return;
        }
        for (int i = 0; i < GlobalArray.SECTOR_COUNT; i++) {
            int n = rc.readSharedArray(SECTOR_START + i);
            n = setTimeSinceLastExplored(n, Math.min(getTimeSinceLastExplored(n) + 1, 63));
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
                if (rc.readSharedArray(ALLY_FLAG_ID + i) == 0) {
                    rc.writeSharedArray(ALLY_FLAG_ID + i, flagId);
                    if (flag.isPickedUp()) {
                        rc.writeSharedArray(ALLY_FLAG_CUR_LOC + i, (1 << 13) | GlobalArray.intifyLocation(flag.getLocation()));
                    }
                    else {
                        if (rc.getRoundNum() < 200) {
                            rc.writeSharedArray(ALLY_FLAG_DEF_LOC + i, GlobalArray.intifyLocation(flag.getLocation()));
                        }
                        rc.writeSharedArray(ALLY_FLAG_CUR_LOC + i, GlobalArray.intifyLocation(flag.getLocation()));
                    }
                    break;
                }
                else if (rc.readSharedArray(ALLY_FLAG_ID + i) == flagId) {
                    if (flag.isPickedUp()) {
                        rc.writeSharedArray(ALLY_FLAG_CUR_LOC + i, (1 << 13) | GlobalArray.intifyLocation(flag.getLocation()));
                    }
                    else {
                        if (rc.getRoundNum() < 200) {
                            rc.writeSharedArray(ALLY_FLAG_DEF_LOC + i, GlobalArray.intifyLocation(flag.getLocation()));
                        }
                        rc.writeSharedArray(ALLY_FLAG_CUR_LOC + i, GlobalArray.intifyLocation(flag.getLocation()));
                    }
                    break;
                }
            }
        }
        else {
            for (int i = 0; i <= 2; i++) {
                if (rc.readSharedArray(OPPO_FLAG_ID + i) == 0) {
                    rc.writeSharedArray(OPPO_FLAG_ID + i, flagId);
                    if (flag.isPickedUp()) {
                        rc.writeSharedArray(OPPO_FLAG_LOC + i, (1 << 13) | GlobalArray.intifyLocation(flag.getLocation()));
                    }
                    else {
                        rc.writeSharedArray(OPPO_FLAG_LOC + i, GlobalArray.intifyLocation(flag.getLocation()));
                    }
                    break;
                }
                else if (rc.readSharedArray(OPPO_FLAG_ID + i) == flagId) {
                    if (flag.isPickedUp()) {
                        rc.writeSharedArray(OPPO_FLAG_LOC + i, (1 << 13) | GlobalArray.intifyLocation(flag.getLocation()));
                    }
                    else {
                        rc.writeSharedArray(OPPO_FLAG_LOC + i, GlobalArray.intifyLocation(flag.getLocation()));
                    }
                    break;
                }
            }
        }
    }

    protected static void init() throws GameActionException {
        id = rc.readSharedArray(63);
        rc.writeSharedArray(63, id + 1);
        sectorWidth = new int[GlobalArray.SECTOR_SIZE];
        for (int i = 0; i < GlobalArray.SECTOR_SIZE; i++) {
            sectorWidth[i] = rc.getMapWidth() / GlobalArray.SECTOR_SIZE;
            if (rc.getMapWidth() % GlobalArray.SECTOR_SIZE >= i) {
                sectorWidth[i] += 1;
            }
        }
        sectorHeight = new int[GlobalArray.SECTOR_SIZE];
        for (int i = 0; i < GlobalArray.SECTOR_SIZE; i++) {
            sectorHeight[i] = rc.getMapHeight() / GlobalArray.SECTOR_SIZE;
            if (rc.getMapHeight() % GlobalArray.SECTOR_SIZE >= i) {
                sectorHeight[i] += 1;
            }
        }
        sectorX = new int[GlobalArray.SECTOR_SIZE];
        for (int i = 1; i < GlobalArray.SECTOR_SIZE; i++) {
            sectorX[i] = sectorX[i - 1] + sectorWidth[i - 1];
        }
        sectorY = new int[GlobalArray.SECTOR_SIZE];
        for (int i = 1; i < GlobalArray.SECTOR_SIZE; i++) {
            sectorY[i] = sectorY[i - 1] + sectorHeight[i - 1];
        }
    }
}
