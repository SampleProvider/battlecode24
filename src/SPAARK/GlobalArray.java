package SPAARK;

import battlecode.common.*;

public class GlobalArray {
    public static RobotController rc;
    public static StringBuilder indicatorString;

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
     */

    public static boolean hasLocation(int n) {
        return (n >> 12 & 0b1) == 1;
    }
    public static MapLocation parseLocation(int n) {
        return new MapLocation(n & 0b111111, (n >> 6) & 0b111111);
    }
    public static int intifyLocation(MapLocation loc) {
        return 0b1000000000000 | (loc.y << 6) | loc.x;
    }
    public static boolean isFlagPlaced(int n) {
        return ((n >> 13) & 0b1) == 1;
    }

    public static void updateLocation(int index, MapLocation loc) throws GameActionException {
        int n = rc.readSharedArray(index);
        if (!hasLocation(n) || !parseLocation(n).equals(loc)) {
            rc.writeSharedArray(index, (n & 0b1110000000000000) | intifyLocation(loc));
        }
    }

    public static void init() throws GameActionException {
        rc.writeSharedArray(6, 1 << 13);
        rc.writeSharedArray(7, 1 << 13);
        rc.writeSharedArray(8, 1 << 13);
    }
}