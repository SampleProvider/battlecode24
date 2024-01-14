package SPAARK;

import battlecode.common.*;

import java.util.Random;

public class Offensive {
    protected static RobotController rc;
    protected static StringBuilder indicatorString;

    protected static Random rng;

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
    protected static int flagIndex = -1;
    
    protected static void run() throws GameActionException {
        // capturing opponentFlags
        MapLocation me = rc.getLocation();
        FlagInfo[] opponentFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());

        FlagInfo closestFlag = Motion.getClosestFlag(opponentFlags, false);
        if (closestFlag != null && rc.canPickupFlag(closestFlag.getLocation())) {
            rc.pickupFlag(closestFlag.getLocation());
            int flagId = closestFlag.getID();
            for (int i = 0; i <= 2; i++) {
                if (rc.readSharedArray(GlobalArray.OPPO_FLAG_ID + i) == 0) {
                    flagIndex = i;
                    rc.writeSharedArray(GlobalArray.OPPO_FLAG_ID + i, flagId);
                    break;
                }
                else if (rc.readSharedArray(GlobalArray.OPPO_FLAG_ID + i) == flagId) {
                    flagIndex = i;
                    break;
                }
            }
        }

        // writing flags to global array
        opponentFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        FlagInfo[] friendlyFlags = rc.senseNearbyFlags(-1, rc.getTeam());
        for (FlagInfo flag : friendlyFlags) {
            GlobalArray.writeFlag(flag);
        }
        for (FlagInfo flag : opponentFlags) {
            GlobalArray.writeFlag(flag);
        }

        GlobalArray.updateSector();

        // flagIndex: index of flag currently holding in global array
        if (flagIndex != -1) {
            // navigate back to spawn
            MapLocation[] spawnLocs = rc.getAllySpawnLocations();
            MapLocation bestLoc = Motion.getClosest(spawnLocs);
            rc.setIndicatorDot(bestLoc, 100, 100, 100);
            Motion.bugnavTowards(bestLoc, 1000);
            if (!rc.hasFlag()) {
                rc.writeSharedArray(GlobalArray.OPPO_FLAG_LOC + flagIndex, 0);
                flagIndex = -1;
            }
        }
        else {
            Boolean action = false;

            MapLocation closestStolenFlag = null;
            for (int i = 0; i <= 2; i++) {
                int n = rc.readSharedArray(GlobalArray.ALLY_FLAG_CUR_LOC + i);
                // if (GlobalArray.isFlagPickedUp(n) && GlobalArray.hasLocation(n)) {
                if (GlobalArray.hasLocation(n)) {
                    MapLocation loc = GlobalArray.parseLocation(n);
                    if (!loc.equals(GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC + i)))) {
                        rc.setIndicatorDot(loc, 0, 255, 255);
                        if (rc.getLocation().distanceSquaredTo(loc)  <= 4 && rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length == 0) {
                            boolean seesFlag = false;
                            for (FlagInfo flag : friendlyFlags) {
                                if (flag.isPickedUp() && flag.getID() == rc.readSharedArray(i - 6)) {
                                    seesFlag = true;
                                    break;
                                }
                            }
                            if (seesFlag == false) {
                                rc.writeSharedArray(GlobalArray.ALLY_FLAG_CUR_LOC + i, 0);
                                continue;
                            }
                        }
                        if (closestStolenFlag == null || me.distanceSquaredTo(closestStolenFlag) > me.distanceSquaredTo(loc)) {
                            closestStolenFlag = loc;
                        }
                    }
                }
            }
            if (closestStolenFlag != null) {
                Motion.bugnavTowards(closestStolenFlag, Motion.DEFAULT_RETREAT_HP);
                action = true;
            }

            if (!action) {
                MapInfo[] info = rc.senseNearbyMapInfos();
                for (MapInfo i : info) {
                    if (i.getCrumbs() > 0) {
                        Motion.bugnavTowards(i.getMapLocation(), Motion.DEFAULT_RETREAT_HP);
                        indicatorString.append("CRUMB("+i.getMapLocation().x+","+i.getMapLocation().y+");");
                        action = true;
                        break;
                    }
                }
            }
            if (!action) {
                for (int i = 0; i <= 2; i++) {
                    int n = rc.readSharedArray(GlobalArray.OPPO_FLAG_LOC + i);
                    if (GlobalArray.hasLocation(n) && GlobalArray.isFlagPickedUp(n)) {
                        Motion.bugnavAround(GlobalArray.parseLocation(n), 5, 20, Motion.DEFAULT_RETREAT_HP);
                        break;
                    }
                }
                if (closestFlag != null) {
                    Motion.bugnavTowards(closestFlag.getLocation(), Motion.DEFAULT_RETREAT_HP);
                }
                else {
                    MapLocation[] hiddenFlags = rc.senseBroadcastFlagLocations();
                    if (hiddenFlags.length > 0) {
                        MapLocation closestHiddenFlag = Motion.getClosest(hiddenFlags);
                        Motion.bugnavTowards(closestHiddenFlag, Motion.DEFAULT_RETREAT_HP);
                    }
                    else {
                        Motion.moveRandomly();
                        // Motion.groupRandomly();
                    }
                }
            }
        }
        for (int j = 0; j < 8; j++) {
            MapLocation buildLoc = rc.getLocation().add(DIRECTIONS[j]);
            build: if (rc.canBuild(TrapType.EXPLOSIVE, buildLoc)) {
                MapInfo[] mapInfo = rc.senseNearbyMapInfos(buildLoc, 10);
                for (MapInfo m : mapInfo) {
                    if (m.getTrapType() != TrapType.NONE) {
                        break build;
                    }
                }
                rc.build(TrapType.EXPLOSIVE, buildLoc);
            }
        }
        Attack.attack();
        Attack.heal();
        
        opponentFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        friendlyFlags = rc.senseNearbyFlags(-1, rc.getTeam());
        for (FlagInfo flag : friendlyFlags) {
            GlobalArray.writeFlag(flag);
        }
        for (FlagInfo flag : opponentFlags) {
            GlobalArray.writeFlag(flag);
        }

        GlobalArray.updateSector();
    }
    protected static void jailed() throws GameActionException {
        if (flagIndex != -1) {
            rc.writeSharedArray(GlobalArray.OPPO_FLAG_LOC + flagIndex, 0);
            flagIndex = -1;
        }
    }
}
