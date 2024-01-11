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

    protected static final Direction[] DIRECTIONS = {
        Direction.SOUTHWEST,
        Direction.SOUTH,
        Direction.SOUTHEAST,
        Direction.WEST,
        Direction.EAST,
        Direction.NORTHWEST,
        Direction.NORTH,
        Direction.NORTHEAST,
    };

    public static int flagIndex = -1;
    
    public static void run() throws GameActionException {
        // capturing flags
        Attack.attack();
        Attack.heal();
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
                if (flagIndex != -1) {
                    rc.writeSharedArray(flagIndex, GlobalArray.intifyLocation(rc.getLocation()));
                }
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
            MapInfo[] info = rc.senseNearbyMapInfos();
            Boolean action = false;
            for (MapInfo i : info) {
                if (i.getCrumbs() > 0) {
                    Motion.bugnavTowards(i.getMapLocation(), 999);
                    indicatorString.append("CRUMB("+i.getMapLocation().x+","+i.getMapLocation().y+");");
                    action = true;
                    break;
                }
            }
            if (!action) {
                for (int i = 6; i <= 8; i++) {
                    int n = rc.readSharedArray(i);
                    if (GlobalArray.hasLocation(n) && !GlobalArray.isFlagPlaced(n)) {
                        Motion.bugnavAround(GlobalArray.parseLocation(n), 8, 15, 999);
                        break;
                    }
                }
                if (nearestFlag != null) {
                    Motion.bugnavTowards(nearestFlag.getLocation(), 999);
                    for (int j = 0; j < 8; j++) {
                        MapLocation buildLoc = rc.getLocation().add(DIRECTIONS[j]);
                        if (rc.canBuild(TrapType.EXPLOSIVE, buildLoc) && rng.nextInt() % 3 == 1) {
                            rc.build(TrapType.EXPLOSIVE, buildLoc);
                        }
                        else if (rc.canBuild(TrapType.STUN, buildLoc)) {
                            rc.build(TrapType.STUN, buildLoc);
                        }
                    }
                }
                else {
                    MapLocation[] hiddenFlags = rc.senseBroadcastFlagLocations();
                    if (hiddenFlags.length > 0) {
                        Motion.bugnavTowards(hiddenFlags[0], 999);
                        if (rc.getLocation().distanceSquaredTo(hiddenFlags[0]) < 100) {
                            for (int j = 0; j < 8; j++) {
                                MapLocation buildLoc = rc.getLocation().add(DIRECTIONS[j]);
                                if (rc.canBuild(TrapType.EXPLOSIVE, buildLoc) && rng.nextInt() % 3 == 1) {
                                    rc.build(TrapType.EXPLOSIVE, buildLoc);
                                }
                                else if (rc.canBuild(TrapType.STUN, buildLoc)) {
                                    rc.build(TrapType.STUN, buildLoc);
                                }
                            }
                        }
                    }
                    else {
                        Motion.moveRandomly();
                    }
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
