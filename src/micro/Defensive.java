package micro;

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
            Motion.bugnavTowards(targetLoc, Motion.DEFAULT_RETREAT_HP);
            rc.setIndicatorLine(me, targetLoc, 255, 0, 255);
            if (me.distanceSquaredTo(targetLoc) <= 2) {
                hasFoundFlag = true;
                FlagInfo[] flags = rc.senseNearbyFlags(2, rc.getTeam());
                // if the flag is there the spawn point is valid
                if (flags.length > 0) {
                    for (FlagInfo flag : flags) {
                        if (flag.getLocation().equals(targetLoc)) {
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
                // TEMP CODE AAA
                // TEMP CODE AAA
                // TEMP CODE AAA
                // TEMP CODE AAA
                // TEMP CODE AAA
                // TEMP CODE AAA
                Motion.moveRandomly();
                for (int i = 0; i < 4; i++) {
                    MapLocation buildLoc = me.add(DIRECTIONS[rng.nextInt(8)]);
                    if (rng.nextBoolean()) {
                        if (rc.canBuild(TrapType.WATER, buildLoc)) {
                            rc.build(TrapType.WATER, buildLoc);
                        }
                    } else {
                        if (rc.canBuild(TrapType.STUN, buildLoc)) {
                            rc.build(TrapType.STUN, buildLoc);
                        }
                    }
                }
            } else {
                MapLocation targetLoc = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC + (GlobalArray.id % 3)));
                rc.setIndicatorLine(me, targetLoc, 255, 0, 255);
                if (GlobalArray.id < 3) {
                    Motion.bugnavTowards(targetLoc, Motion.DEFAULT_RETREAT_HP);
                    me = rc.getLocation();
                    if (me.equals(targetLoc)) {
                        // camping
                        for (int j = 0; j < 8; j++) {
                            MapLocation buildLoc = me.add(DIRECTIONS[j]);
                            if (j % 2 == 0) {
                                if (rc.canBuild(TrapType.WATER, buildLoc)) {
                                    rc.build(TrapType.WATER, buildLoc);
                                }
                            }
                            else {
                                // if (rc.canBuild(TrapType.STUN, buildLoc)) {
                                //     rc.build(TrapType.STUN, buildLoc);
                                // }
                                if (rc.canBuild(TrapType.EXPLOSIVE, buildLoc) && rc.getRoundNum() > 100) {
                                    rc.build(TrapType.EXPLOSIVE, buildLoc);
                                }
                            }
                        }
                    }
                } else {
                    // patrolling i guess
                    Motion.bugnavAround(targetLoc, 4, 25, Motion.DEFAULT_RETREAT_HP);
                    me = rc.getLocation();
                    MapLocation[] hiddenFlags = rc.senseBroadcastFlagLocations();
                    preemptiveTraps: for (MapLocation oppFlag : hiddenFlags) {
                        if (me.directionTo(oppFlag).equals(me.directionTo(targetLoc).opposite())) {
                            MapLocation buildLoc = me.add(DIRECTIONS[rng.nextInt(8)]);
                            for (Direction d : DIRECTIONS) {
                                // dont obstruct traps of camping duck
                                if (buildLoc.add(d).equals(targetLoc)) continue preemptiveTraps;
                            }
                            if (rng.nextBoolean()) {
                                if (rc.canBuild(TrapType.WATER, buildLoc)) {
                                    rc.build(TrapType.WATER, buildLoc);
                                }
                            } else {
                                if (rc.canBuild(TrapType.STUN, buildLoc)) {
                                    rc.build(TrapType.STUN, buildLoc);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Motion.spreadRandomly();
        }

        GlobalArray.updateSector();

        Attack.attack();
        Attack.heal();
    }
    protected static void jailed() throws GameActionException {

    }
}
