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
        FlagInfo[] flags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        FlagInfo nearestFlag = Motion.getNearestFlag(flags, false);
        if (nearestFlag != null && rc.canPickupFlag(nearestFlag.getLocation()) && flagIndex == -1) {
            boolean valid = true;
            for (int i = 6; i <= 8; i++) {
                int n = rc.readSharedArray(i);
                if (GlobalArray.hasLocation(n) && GlobalArray.parseLocation(n).equals(nearestFlag.getLocation()) && !GlobalArray.isFlagPlaced(n)) {
                    valid = false;
                    break;
                }
            }
            if (valid) {
                rc.pickupFlag(nearestFlag.getLocation());
                for (int i = 6; i <= 8; i++) {
                    if (GlobalArray.isFlagPlaced(rc.readSharedArray(i))) {
                        flagIndex = i;
                        indicatorString.append("WOW PICKED UP FLAG " + i);
                        break;
                    }
                }
                rc.writeSharedArray(flagIndex, GlobalArray.intifyLocation(rc.getLocation()));
            }
        }
        if (flagIndex != -1 && !rc.hasFlag() && (nearestFlag == null || !nearestFlag.getLocation().equals(rc.getLocation()))) {
            stopHoldingFlag();
            flagIndex = -1;
        }
        indicatorString.append(flagIndex);
        if (flagIndex != -1) {
            MapLocation[] spawnLocs = rc.getAllySpawnLocations();
            MapLocation bestLoc = Motion.getNearest(spawnLocs);
            rc.setIndicatorDot(bestLoc, 100, 100, 100);
            Motion.bug2Flag(bestLoc);
            // rc.writeSharedArray(flagIndex, (1 << 13) | GlobalArray.intifyLocation(rc.getLocation()));
            rc.writeSharedArray(flagIndex, GlobalArray.intifyLocation(rc.getLocation()));
        }
        else {
            for (int i = 6; i <= 8; i++) {
                int n = rc.readSharedArray(i);
                if (GlobalArray.hasLocation(n) && !GlobalArray.isFlagPlaced(n)) {
                    Motion.bugnavAround(GlobalArray.parseLocation(n), 8, 15, false);
                    break;
                }
            }
            if (nearestFlag != null) {
                Motion.bugnavTowards(nearestFlag.getLocation(), false);
            }
            else {
                MapLocation[] hiddenFlags = rc.senseBroadcastFlagLocations();
                if (hiddenFlags.length > 0) {
                    Motion.bugnavTowards(hiddenFlags[0], false);
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
            stopHoldingFlag();
        }
    }
    public static void stopHoldingFlag() throws GameActionException {
        rc.writeSharedArray(flagIndex, (1 << 13) ^ rc.readSharedArray(flagIndex));
        flagIndex = -1;
    }
}
