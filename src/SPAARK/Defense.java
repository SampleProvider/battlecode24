package SPAARK;

import battlecode.common.*;

import java.util.Random;

public class Defense {
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
            MapLocation targetLoc = Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_CUR_LOC + (Comms.id % 3)));
            Motion.bfsnav(targetLoc);
            rc.setIndicatorLine(me, targetLoc, 255, 0, 255);
            if (me.distanceSquaredTo(targetLoc) <= 2) {
                hasFoundFlag = true;
                FlagInfo[] flags = rc.senseNearbyFlags(2, rc.getTeam());
                // if the flag is there the spawn point is valid
                if (flags.length > 0) {
                    for (FlagInfo flag : flags) {
                        if (flag.getLocation().equals(targetLoc) && flag.getID() == rc.readSharedArray(Comms.ALLY_FLAG_ID + (Comms.id % 3))) {
                            rc.writeSharedArray(Comms.ALLY_FLAG_DEF_LOC + (Comms.id % 3), rc.readSharedArray(Comms.ALLY_FLAG_CUR_LOC + (Comms.id % 3)));
                            break;
                        }
                    }
                }
            }
        }
        FlagInfo[] opponentFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        FlagInfo[] friendlyFlags = rc.senseNearbyFlags(-1, rc.getTeam());
        for (FlagInfo flag : Motion.flags) {
            Comms.writeFlag(flag);
        }
        Comms.checkFlags(friendlyFlags, opponentFlags);
        Comms.updatePOI();
        if (Comms.hasLocation(rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC + Comms.id))) {
            RobotInfo[] opponentRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            if (opponentRobots.length > 0) {
                // spam traps between enemy and flag
                RobotInfo closestRobot = Motion.getClosestRobot(opponentRobots);
                // Motion.moveRandomly();
                for (int i = 3; --i >= 0;) {
                    // MapLocation buildLoc = me.add(DIRECTIONS[rng.nextInt(8)]);
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
                            // rc.build(TrapType.EXPLOSIVE, buildLoc);
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
                                if (info.getTrapType() == TrapType.STUN) break placeTrap; 
                            }
                            if (rc.canBuild(TrapType.STUN, buildLoc)) {
                                rc.build(TrapType.STUN, buildLoc);
                            }
                        }
                    }
                }
            } else {
                MapLocation targetLoc = Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC + (Comms.id % 3)));
                rc.setIndicatorLine(me, targetLoc, 255, 0, 255);
                if (Comms.id < 3) {
                    Motion.bfsnav(targetLoc);
                    me = rc.getLocation();
                    if (me.equals(targetLoc)) {
                        // dont bother placing traps around nothing
                        FlagInfo[] flags = rc.senseNearbyFlags(2, rc.getTeam());
                        if (flags.length > 0) {
                            // camping
                            for (int j = 8; --j >= 0;) {
                                MapLocation buildLoc = me.add(DIRECTIONS[j]);
                                if (rc.canFill(buildLoc)) {
                                    rc.fill(buildLoc);
                                }
                                if (j % 2 == 0) {
                                if (rc.canBuild(TrapType.WATER, buildLoc)) {
                                    // rc.build(TrapType.WATER, buildLoc);
                                }
                                }
                                // else {
                                //     if (rc.canBuild(TrapType.EXPLOSIVE, buildLoc) && rc.getRoundNum() > 100) {
                                //         rc.build(TrapType.EXPLOSIVE, buildLoc);
                                //     }
                                // }
                            }
                        }
                    }
                } else {
                    // patrolling i guess
                    Motion.bugnavAround(targetLoc, 9, 25);
                    me = rc.getLocation();
                    FlagInfo[] flags = rc.senseNearbyFlags(-1, rc.getTeam());
                    // also dont place traps around nothing
                    if (flags.length > 0) {
                        MapLocation[] hiddenFlags = rc.senseBroadcastFlagLocations();
                        preemptiveTraps: for (MapLocation oppFlag : hiddenFlags) {
                            if (me.directionTo(oppFlag).equals(me.directionTo(targetLoc).opposite())) {
                                MapLocation buildLoc = me.add(DIRECTIONS[rng.nextInt(8)]);
                                for (Direction d : DIRECTIONS) {
                                    // dont obstruct traps of camping duck
                                    if (buildLoc.add(d).equals(targetLoc)) continue preemptiveTraps;
                                }
                                MapInfo[] nearbyTraps = rc.senseNearbyMapInfos(buildLoc, 4);
                                if (rng.nextBoolean()) {
                                    placeTrap: {
                                        for (MapInfo info : nearbyTraps) {
                                            if (info.getTrapType() == TrapType.WATER) break placeTrap; 
                                        }
                                        if (rc.canBuild(TrapType.WATER, buildLoc)) {
                                            rc.build(TrapType.WATER, buildLoc);
                                        }
                                    }
                                } else {
                                    placeTrap: {
                                        for (MapInfo info : nearbyTraps) {
                                            if (info.getTrapType() == TrapType.STUN) break placeTrap; 
                                        }
                                        if (rc.canBuild(TrapType.STUN, buildLoc)) {
                                            rc.build(TrapType.STUN, buildLoc);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Motion.spreadRandomly();
            RobotPlayer.mode = RobotPlayer.OFFENSIVE;
        }

        // GlobalArray.updateSector();

        Atk.attack();
        Atk.heal();
    }
    protected static void jailed() throws GameActionException {

    }
}
