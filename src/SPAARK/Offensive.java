package SPAARK;

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

    public static int flagIndex = -1;
    
    public static void run() throws GameActionException {
        // capturing flags
        if (rc.canPickupFlag(rc.getLocation())) {
            rc.pickupFlag(rc.getLocation());
        }
        if (rc.hasFlag() && rc.getRoundNum() != GameConstants.SETUP_ROUNDS) {
            MapLocation[] spawnLocs = rc.getAllySpawnLocations();
            MapLocation firstLoc = spawnLocs[0];
            Motion.bugnav(firstLoc, false);
            if (flagIndex == -1) {
                for (int i = 7; i <= 9; i++) {
                    if (!GlobalArray.hasLocation(rc.readSharedArray(i))) {
                        flagIndex = i;
                        break;
                    }
                }
            }
            rc.writeSharedArray(flagIndex, GlobalArray.intifyLocation(rc.getLocation()));
        }
        else {
            if (flagIndex != -1) {
                rc.writeSharedArray(flagIndex, 0);
                flagIndex = -1;
            }
            FlagInfo[] flags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
            if (flags.length > 0) {
                Motion.bugnavAround(flags[0].getLocation(), 5, false);
            }
            else {
                for (int i = 7; i <= 9; i++) {
                    if (GlobalArray.hasLocation(rc.readSharedArray(i))) {
                        Motion.bugnavAround(GlobalArray.parseLocation(rc.readSharedArray(i)), 5, false);
                        break;
                    }
                }
                MapLocation[] hiddenFlags = rc.senseBroadcastFlagLocations();
                if (hiddenFlags.length > 0) {
                    Motion.bugnav(hiddenFlags[0], false);
                }
                else {
                    Motion.moveRandomly();
                }
            }
        }
        Attack.attack();
        Attack.heal();
    }
    public static void jailed() throws GameActionException {
        if (flagIndex != -1) {
            rc.writeSharedArray(flagIndex, 0);
            flagIndex = -1;
        }
    }
}
