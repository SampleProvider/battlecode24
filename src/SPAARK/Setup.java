package SPAARK;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Setup {
    public static RobotController rc;
    public static StringBuilder indicatorString;

    public static Random rng;

    public static int flagIndex = -1;
    
    public static void run() throws GameActionException {
        if (rc.canPickupFlag(rc.getLocation())) {
            rc.pickupFlag(rc.getLocation());
        }
        if (rc.hasFlag()) {
            if (flagIndex == -1) {
                for (int i = 0; i <= 2; i++) {
                    if (!GlobalArray.hasLocation(rc.readSharedArray(i))) {
                        flagIndex = i;
                        break;
                    }
                }
            }
            Motion.moveRandomly();
            rc.writeSharedArray(flagIndex, GlobalArray.intifyLocation(rc.getLocation()));
        }
        else {
            MapLocation[] crumbs = rc.senseNearbyCrumbs(-1);
            if (crumbs.length > 0) {
                Motion.bugnavTowards(crumbs[0], true);
            }
            else {
                Motion.moveRandomly();
            }
        }
    }
    public static void jailed() throws GameActionException {
        if (flagIndex != -1) {
            rc.writeSharedArray(flagIndex, 0);
            flagIndex = -1;
        }
    }
}
