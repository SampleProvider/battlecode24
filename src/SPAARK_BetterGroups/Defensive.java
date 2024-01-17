package SPAARK_BetterGroups;

import battlecode.common.*;

import java.util.Random;

public class Defensive {
    protected static RobotController rc;
    protected static StringBuilder indicatorString;

    protected static Random rng;

    protected static boolean hasFoundFlag = false;

    protected static final Direction[] DIRECTIONS = {
        Direction.SOUTHWEST,
        Direction.SOUTH,
        Direction.SOUTHEAST,
        Direction.WEST,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.NORTHWEST,
        Direction.NORTH,
    };

    protected static void run() throws GameActionException {
        MapLocation me = rc.getLocation();
        rc.setIndicatorDot(me, 255, 0, 255);
        if (!hasFoundFlag) {
            MapLocation targetLoc = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_CUR_LOC + (GlobalArray.id % 3)));
            Motion.bugnavTowards(targetLoc);
            rc.setIndicatorLine(me, targetLoc, 255, 0, 255);
            if (me.distanceSquaredTo(targetLoc) <= 2) {
                hasFoundFlag = true;
                FlagInfo[] flags = rc.senseNearbyFlags(2, rc.getTeam());
                // if the flag is there the spawn point is valid
                if (flags.length > 0) {
                    for (FlagInfo flag : flags) {
                        if (flag.getLocation().equals(targetLoc) && flag.getID() == rc.readSharedArray(GlobalArray.ALLY_FLAG_ID + (GlobalArray.id % 3))) {
                            rc.writeSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC + (GlobalArray.id % 3), rc.readSharedArray(GlobalArray.ALLY_FLAG_CUR_LOC + (GlobalArray.id % 3)));
                            break;
                        }
                    }
                }
            }
        }
        FlagInfo[] opponentFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        FlagInfo[] friendlyFlags = rc.senseNearbyFlags(-1, rc.getTeam());
        for (FlagInfo flag : friendlyFlags) {
            GlobalArray.writeFlag(flag);
        }
        for (FlagInfo flag : opponentFlags) {
            GlobalArray.writeFlag(flag);
        }
        if (GlobalArray.hasLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC + GlobalArray.id))) {
            RobotInfo[] opponentRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            if (opponentRobots.length > 0) {
                // spam traps between enemy and flag
                RobotInfo closestRobot = Motion.getClosestRobot(opponentRobots);
                // Motion.moveRandomly();
                for (int i = 0; i < 2; i++) {
                    Direction buildDir = me.directionTo(closestRobot.getLocation());
                    if (rng.nextInt(3) == 0) {
                        buildDir = buildDir.rotateLeft();
                    }
                    if (rng.nextInt(3) == 0) {
                        buildDir = buildDir.rotateRight();
                    }
                    MapLocation buildLoc = me.add(buildDir);
                    if (rng.nextInt(3) == 0) {
                        if (rc.canBuild(TrapType.EXPLOSIVE, buildLoc)) {
                            rc.build(TrapType.EXPLOSIVE, buildLoc);
                        }
                        // MapInfo[] nearbyTraps = rc.senseNearbyMapInfos(buildLoc, 4);
                        // placeTrap: {
                        //     for (MapInfo info : nearbyTraps) {
                        //         if (info.getTrapType() == TrapType.STUN) break placeTrap; 
                        //     }
                        //     if (rc.canBuild(TrapType.STUN, buildLoc)) {
                        //         rc.build(TrapType.STUN, buildLoc);
                        //     }
                        // }
                    } else {
                        MapInfo[] nearbyTraps = rc.senseNearbyMapInfos(buildLoc, 4);
                        placeTrap: {
                            for (MapInfo info : nearbyTraps) {
                                if (info.getTrapType() == TrapType.WATER) break placeTrap; 
                            }
                            if (rc.canBuild(TrapType.WATER, buildLoc)) {
                                rc.build(TrapType.WATER, buildLoc);
                            }
                        }
                    }
                }
            } else {
                MapLocation targetLoc = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC + (GlobalArray.id % 3)));
                rc.setIndicatorLine(me, targetLoc, 255, 0, 255);
                Motion.bugnavTowards(targetLoc, 0);
                me = rc.getLocation();
                if (me.equals(targetLoc)) {
                    // dont bother placing traps around nothing
                    FlagInfo[] flags = rc.senseNearbyFlags(2, rc.getTeam());
                    if (flags.length > 0) {
                        // camping
                        for (int j = 0; j < 8; j++) {
                            MapLocation buildLoc = me.add(DIRECTIONS[j]);
                            if (rc.canFill(buildLoc)) {
                                rc.fill(buildLoc);
                            }
                            if (rc.canBuild(TrapType.WATER, buildLoc)) {
                                rc.build(TrapType.WATER, buildLoc);
                            }
                        }
                    }
                }
            }
        } else {
            Motion.spreadRandomly();
        }

        Attack.attack();
        Attack.heal();
    }
    protected static void jailed() throws GameActionException {
        // probably should start spamming stuff into POI
    }
}