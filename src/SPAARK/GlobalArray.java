package SPAARK;

import battlecode.common.*;

public class GlobalArray {
    public static RobotController rc;
    public static StringBuilder indicatorString;

    public static int id;

    public static int[] sectorWidth;
    public static int[] sectorHeight;
    public static int[] sectorX;
    public static int[] sectorY;

    /*
     * Array Indices 1-3:
     *  Default Flag Locations
     * Array Indices 4-6:
     *  Stolen Flag Locations
     * Array Indices 7-9:
     *  Opponent Flag Locations
     * Array Index 10:
     *  0-11: Flag target location during setup
     * 
     * danger levels for flag carriers
     * 
     * 
     * Sectors:
     * Number of opponent robots (64 - 6)
     * Number of friendly robots (64 - 6)
     * Turns since last explored (16 - 4)
     * 
     */

    //  public static final int ROBOT_IDS = 0;
    //  public static final int ALLY_FLAG_IDS = 8;
    //  public static final int ALLY_FLAG_ID_STEP = 16;
    //  public static final int ALLY_FLAG_DEFAULT_LOCATIONS = 56;
    //  public static final int ALLY_FLAG_DEFAULT_LOCATIONS = 56;

    public static boolean hasLocation(int n) {
        return (n >> 12 & 0b1) == 1;
    }
    public static MapLocation parseLocation(int n) {
        return new MapLocation(n & 0b111111, (n >> 6) & 0b111111);
    }
    public static int intifyLocation(MapLocation loc) {
        return 0b1000000000000 | (loc.y << 6) | loc.x;
    }
    public static boolean isFlagPickedUp(int n) {
        return ((n >> 13) & 0b1) == 1;
    }
    public static void write(int index, int bits, int n) throws GameActionException {
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

    public static int getNumberOfOpponentRobots(int n) {
        return (n & 0b11111);
    }
    public static int setNumberOfOpponentRobots(int n, int v) {
        return (n | 0b1111111111100000) | v;
    }
    public static int getNumberOfFriendlyRobots(int n) {
        return ((n >> 5) & 0b11111);
    }
    public static int setNumberOfFriendlyRobots(int n, int v) {
        return (n | 0b1111110000011111) | (v << 5);
    }
    public static int getTimeSinceLastExplored(int n) {
        return ((n >> 10) & 0b111111);
    }
    public static int setTimeSinceLastExplored(int n, int v) {
        return (n | 0b0000001111111111) | (v << 10);
    }

    public static MapLocation sectorToLocation(int index) {
        int x = sectorX[index % 5] + sectorWidth[index % 5] / 2;
        int y = sectorY[index / 5] + sectorHeight[index / 5] / 2;
        return new MapLocation(x, y);
    }
    public static int locationToSector(MapLocation loc) {
        int x = 4;
        for (int i = 0; i < 5; i++) {
            if (sectorX[i] > loc.x) {
                x = i - 1;
                break;
            }
        }
        int y = 4;
        for (int i = 0; i < 5; i++) {
            if (sectorY[i] > loc.y) {
                y = i - 1;
                break;
            }
        }
        return y * 5 + x;
    }

    public static void updateSector() throws GameActionException {
        MapLocation[] locs = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 20);
    }

    public static void updateLocation(int index, MapLocation loc) throws GameActionException {
        int n = rc.readSharedArray(index);
        if (!hasLocation(n) || !parseLocation(n).equals(loc)) {
            rc.writeSharedArray(index, (n & 0b1110000000000000) | intifyLocation(loc));
        }
    }
    public static void writeFlag(FlagInfo flag) throws GameActionException {
        int flagId = flag.getID();
        if (flag.getTeam().equals(rc.getTeam())) {
            for (int i = 0; i <= 2; i++) {
                if (rc.readSharedArray(i) == 0) {
                    rc.writeSharedArray(i, flagId);
                    if (flag.isPickedUp()) {
                        rc.writeSharedArray(i + 6, (1 << 13) | GlobalArray.intifyLocation(flag.getLocation()));
                    }
                    else {
                        if (rc.getRoundNum() < 200) {
                            rc.writeSharedArray(i + 3, GlobalArray.intifyLocation(flag.getLocation()));
                        }
                        rc.writeSharedArray(i + 6, GlobalArray.intifyLocation(flag.getLocation()));
                    }
                    break;
                }
                else if (rc.readSharedArray(i) == flagId) {
                    if (flag.isPickedUp()) {
                        rc.writeSharedArray(i + 6, (1 << 13) | GlobalArray.intifyLocation(flag.getLocation()));
                    }
                    else {
                        if (rc.getRoundNum() < 200) {
                            rc.writeSharedArray(i + 3, GlobalArray.intifyLocation(flag.getLocation()));
                        }
                        rc.writeSharedArray(i + 6, GlobalArray.intifyLocation(flag.getLocation()));
                    }
                    break;
                }
            }
        }
        else {
            for (int i = 9; i <= 11; i++) {
                if (rc.readSharedArray(i) == 0) {
                    rc.writeSharedArray(i, flagId);
                    if (flag.isPickedUp()) {
                        rc.writeSharedArray(i + 3, (1 << 13) | GlobalArray.intifyLocation(flag.getLocation()));
                    }
                    else {
                        rc.writeSharedArray(i + 3, GlobalArray.intifyLocation(flag.getLocation()));
                    }
                    break;
                }
                else if (rc.readSharedArray(i) == flagId) {
                    if (flag.isPickedUp()) {
                        rc.writeSharedArray(i + 3, (1 << 13) | GlobalArray.intifyLocation(flag.getLocation()));
                    }
                    else {
                        rc.writeSharedArray(i + 3, GlobalArray.intifyLocation(flag.getLocation()));
                    }
                    break;
                }
            }
        }
    }

    public static void init() throws GameActionException {
        id = rc.readSharedArray(63);
        rc.writeSharedArray(63, id + 1);
        sectorWidth = new int[5];
        for (int i = 0; i < 5; i++) {
            sectorWidth[i] = rc.getMapWidth() / 5;
            if (rc.getMapWidth() % 5 >= i) {
                sectorWidth[i] += 1;
            }
        }
        sectorHeight = new int[5];
        for (int i = 0; i < 5; i++) {
            sectorHeight[i] = rc.getMapHeight() / 5;
            if (rc.getMapHeight() % 5 >= i) {
                sectorHeight[i] += 1;
            }
        }
        sectorX = new int[5];
        for (int i = 1; i < 5; i++) {
            sectorX[i] = sectorX[i - 1] + sectorWidth[i - 1];
        }
        sectorY = new int[5];
        for (int i = 0; i < 5; i++) {
            sectorY[i] = sectorY[i - 1] + sectorHeight[i - 1];
        }
    }
}
