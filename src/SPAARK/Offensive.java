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
        // capturing opponentFlags
        MapLocation me = rc.getLocation();
        FlagInfo[] opponentFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());

        FlagInfo nearestFlag = Motion.getNearestFlag(opponentFlags, false);
        if (nearestFlag != null && rc.canPickupFlag(nearestFlag.getLocation())) {
            rc.pickupFlag(nearestFlag.getLocation());
            int flagId = nearestFlag.getID();
            for (int i = 9; i <= 11; i++) {
                if (rc.readSharedArray(i) == 0) {
                    flagIndex = i;
                    rc.writeSharedArray(i, flagId);
                    break;
                }
                else if (rc.readSharedArray(i) == flagId) {
                    flagIndex = i;
                    break;
                }
            }
        }
        opponentFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        FlagInfo[] friendlyFlags = rc.senseNearbyFlags(-1, rc.getTeam());
        for (FlagInfo flag : friendlyFlags) {
            GlobalArray.writeFlag(flag);
        }
        for (FlagInfo flag : opponentFlags) {
            GlobalArray.writeFlag(flag);
        }

        if (flagIndex != -1) {
            MapLocation[] spawnLocs = rc.getAllySpawnLocations();
            MapLocation bestLoc = Motion.getNearest(spawnLocs);
            rc.setIndicatorDot(bestLoc, 100, 100, 100);
            Motion.bugnavTowards(bestLoc, 1000);
            if (!rc.hasFlag()) {
                rc.writeSharedArray(flagIndex + 3, 0);
                flagIndex = -1;
            }
        }
        else {
            Boolean action = false;

            MapLocation nearestStolenFlag = null;
            for (int i = 6; i <= 8; i++) {
                int n = rc.readSharedArray(i);
                if (GlobalArray.isFlagPickedUp(n) && GlobalArray.hasLocation(n)) {
                    if (nearestStolenFlag == null || me.distanceSquaredTo(nearestStolenFlag) > me.distanceSquaredTo(GlobalArray.parseLocation(n))) {
                        nearestStolenFlag = GlobalArray.parseLocation(n);
                    }
                }
            }
            if (nearestStolenFlag != null) {
                Motion.bugnavTowards(nearestStolenFlag, 999);
                action = true;
            }

            if (!action) {
                MapInfo[] info = rc.senseNearbyMapInfos();
                for (MapInfo i : info) {
                    if (i.getCrumbs() > 0) {
                        Motion.bugnavTowards(i.getMapLocation(), 999);
                        indicatorString.append("CRUMB("+i.getMapLocation().x+","+i.getMapLocation().y+");");
                        action = true;
                        break;
                    }
                }
            }
            if (!action) {
                for (int i = 12; i <= 14; i++) {
                    int n = rc.readSharedArray(i);
                    if (GlobalArray.hasLocation(n) && GlobalArray.isFlagPickedUp(n)) {
                        Motion.bugnavAround(GlobalArray.parseLocation(n), 5, 20, 999);
                        break;
                    }
                }
                if (nearestFlag != null) {
                    Motion.bugnavTowards(nearestFlag.getLocation(), 999);
                    for (int j = 0; j < 8; j++) {
                        MapLocation buildLoc = rc.getLocation().add(DIRECTIONS[j]);
                        if (rc.canBuild(TrapType.STUN, buildLoc) && rng.nextInt() % 10 == 1) {
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
                                if (rc.canBuild(TrapType.STUN, buildLoc) && rng.nextInt() % 10 == 1) {
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
            rc.writeSharedArray(flagIndex + 3, 0);
            flagIndex = -1;
        }
    }
}
