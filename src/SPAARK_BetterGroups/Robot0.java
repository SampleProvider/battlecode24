package SPAARK_BetterGroups;

import battlecode.common.*;

import java.util.Random;

public strictfp class Robot0 {
    protected static RobotController rc;
    protected static StringBuilder indicatorString;

    protected static Random rng;

    protected static MapLocation[] leaderLocations = new MapLocation[9];
    
    protected static void run() throws GameActionException {
        for (int i = 0; i < 9; i++) {
            int n = rc.readSharedArray(GlobalArray.GROUP_STAGING + i);
            if (GlobalArray.hasLocation(n)) {
                leaderLocations[i] = GlobalArray.parseLocation(n);
            } else {
                leaderLocations[i] = null;
            }
        }
        // stage any POI using the locations and write to group targets
        // if there's no POI then send the groups ot capture flags
    }
}
