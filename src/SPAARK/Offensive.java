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

        FlagInfo closestFlag = Motion.getClosestFlag(opponentFlags, false);
        if (closestFlag != null && rc.canPickupFlag(closestFlag.getLocation())) {
            rc.pickupFlag(closestFlag.getLocation());
            int flagId = closestFlag.getID();
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
            MapLocation bestLoc = Motion.getClosest(spawnLocs);
            rc.setIndicatorDot(bestLoc, 100, 100, 100);
            Motion.bugnavTowards(bestLoc, 1000);
            if (!rc.hasFlag()) {
                rc.writeSharedArray(flagIndex + 3, 0);
                flagIndex = -1;
            }
        }
        else {
            Boolean action = false;

            MapLocation closestStolenFlag = null;
            for (int i = 6; i <= 8; i++) {
                int n = rc.readSharedArray(i);
                if (GlobalArray.isFlagPickedUp(n) && GlobalArray.hasLocation(n)) {
                    MapLocation loc = GlobalArray.parseLocation(n);
                    rc.setIndicatorDot(loc, 0, 255, 255);
                    if (rc.getLocation().distanceSquaredTo(loc) <= 4 && rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length == 0) {
                        boolean seesFlag = false;
                        for (FlagInfo flag : friendlyFlags) {
                            if (flag.isPickedUp() && flag.getID() == rc.readSharedArray(i - 6)) {
                                seesFlag = true;
                                break;
                            }
                        }
                        if (seesFlag == false) {
                            rc.writeSharedArray(i, 0);
                            continue;
                        }
                    }
                    if (closestStolenFlag == null || me.distanceSquaredTo(closestStolenFlag) > me.distanceSquaredTo(loc)) {
                        closestStolenFlag = loc;
                    }
                }
            }
            if (closestStolenFlag != null) {
                Motion.bugnavTowards(closestStolenFlag, 999);
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
                if (closestFlag != null) {
                    Motion.bugnavTowards(closestFlag.getLocation(), 999);
                    for (int j = 0; j < 8; j++) {
                        MapLocation buildLoc = rc.getLocation().add(DIRECTIONS[j]);
                        build: if (rc.canBuild(TrapType.STUN, buildLoc)) {
                            MapInfo[] mapInfo = rc.senseNearbyMapInfos(buildLoc, 2);
                            for (MapInfo m : mapInfo) {
                                if (m.getTrapType() != TrapType.NONE) {
                                    break build;
                                }
                            }
                            rc.build(TrapType.STUN, buildLoc);
                        }
                    }
                }
                else {
                    MapLocation[] hiddenFlags = rc.senseBroadcastFlagLocations();
                    if (hiddenFlags.length > 0) {
                        MapLocation closestHiddenFlag = Motion.getClosest(hiddenFlags);
                        Motion.bugnavTowards(closestHiddenFlag, 999);
                        if (rc.getLocation().distanceSquaredTo(closestHiddenFlag) < 400) {
                            for (int j = 0; j < 8; j++) {
                                MapLocation buildLoc = rc.getLocation().add(DIRECTIONS[j]);
                                build: if (rc.canBuild(TrapType.STUN, buildLoc)) {
                                    MapInfo[] mapInfo = rc.senseNearbyMapInfos(buildLoc, 2);
                                    for (MapInfo m : mapInfo) {
                                        if (m.getTrapType() != TrapType.NONE) {
                                            break build;
                                        }
                                    }
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
