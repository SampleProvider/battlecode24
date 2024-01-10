package bad_bot;


import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Offensive {
    public static RobotController rc;
    public static StringBuilder indicatorString;

    public static Random rng;
    
    public static void run() throws GameActionException {
        // capturing flags
        if (rc.canPickupFlag(rc.getLocation())) {
            rc.pickupFlag(rc.getLocation());
        }
        if (rc.hasFlag()) {
            MapLocation[] spawnLocs = rc.getAllySpawnLocations();
            MapLocation firstLoc = spawnLocs[0];
            Motion.bug2(firstLoc);
        }
        else {
            FlagInfo[] flags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
            if (flags.length > 0) {
                Motion.bug2(flags[0].getLocation());
            }
            else {
                MapLocation[] hiddenFlags = rc.senseBroadcastFlagLocations();
                if (hiddenFlags.length > 0) {
                    Motion.bug2(hiddenFlags[0]);
                }
                else {
                    Motion.moveRandomly();
                }
            }
        }
        Attack.attack();
        Attack.heal();
    }
}
