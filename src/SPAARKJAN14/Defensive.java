package SPAARK_GROUPS_N_DEFENSE;

import battlecode.common.*;

import java.util.Random;

public class Defensive {
    protected static RobotController rc;
    protected static StringBuilder indicatorString;

    protected static Random rng;

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

    protected static MapLocation targetFlag = new MapLocation(-1, -1);
    
    protected static void run() throws GameActionException {
        FlagInfo[] opponentFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        FlagInfo[] friendlyFlags = rc.senseNearbyFlags(-1, rc.getTeam());
        for (FlagInfo flag : friendlyFlags) {
            GlobalArray.writeFlag(flag);
        }
        for (FlagInfo flag : opponentFlags) {
            GlobalArray.writeFlag(flag);
        }
        if (GlobalArray.id < 3 && GlobalArray.hasLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_CUR_LOC + GlobalArray.id))) {
            MapLocation targetLoc = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_CUR_LOC + GlobalArray.id));
            // RobotInfo[] opponentRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            // if (opponentRobots.length != 0) {
            //     Motion.bugnavTowards(Attack.getPrioritizedOpponentRobot(opponentRobots).getLocation(), 0);
            // } else {
            Motion.bugnavTowards(targetLoc, Motion.DEFAULT_RETREAT_HP);
            if (rc.getLocation().equals(targetLoc)) {
                // rc.writeSharedArray(GlobalArray.id, GlobalArray.intifyLocation(targetLoc));
                for (int j = 0; j < 8; j++) {
                    MapLocation buildLoc = rc.getLocation().add(DIRECTIONS[j]);
                    // if (rc.canBuild(TrapType.EXPLOSIVE, buildLoc) && rc.getRoundNum() > 100) {
                    //     rc.build(TrapType.EXPLOSIVE, buildLoc);
                    //     break;
                    // }
                    if (j % 2 == 0) {
                        if (rc.canBuild(TrapType.EXPLOSIVE, buildLoc) && rc.getRoundNum() > 100) {
                            rc.build(TrapType.EXPLOSIVE, buildLoc);
                        }
                    }
                    else {
                        if (rc.canBuild(TrapType.STUN, buildLoc)) {
                            rc.build(TrapType.STUN, buildLoc);
                        }
                    }
                }
            }
            // }
        } else {
            // spam traps around other flags
            if (targetFlag.x == -1 || rng.nextInt(30) == 1) {
                int flag = 0;
                for (int i = 0; i < 10 && !GlobalArray.hasLocation(flag); i++) {
                    flag = rc.readSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC + rng.nextInt(3));
                }
                if (GlobalArray.hasLocation(flag)){
                    targetFlag = GlobalArray.parseLocation(flag);
                }
            }
            if (targetFlag.x != -1) {
                Motion.bugnavAround(targetFlag, 16, 36, Motion.DEFAULT_RETREAT_HP);
                for (int i = 0; i < 4; i++) {
                    MapLocation buildLoc = rc.getLocation().add(DIRECTIONS[rng.nextInt(8)]);
                    if (targetFlag.distanceSquaredTo(buildLoc) <= 2) {
                        continue;
                    }
                    // MapInfo[] mapInfo = rc.senseNearbyMapInfos(buildLoc, 4);
                    // for (MapInfo m : mapInfo) {
                    //     if (m.getTrapType() != TrapType.NONE) {
                    //         continue;
                    //     }
                    // }
                    // if (i % 2 == 0) {
                    //     if (rc.canBuild(TrapType.WATER, buildLoc)) {
                    //         rc.build(TrapType.WATER, buildLoc);
                    //     }
                    // } else {
                    //     // if (buildLoc.translate(i, i))
                    //     if (rc.canBuild(TrapType.STUN, buildLoc)) {
                    //         rc.build(TrapType.STUN, buildLoc);
                    //     }
                    // }
                }
            }
        }

        GlobalArray.updateSector();

        Attack.attack();
        Attack.heal();
    }
    protected static void jailed() throws GameActionException {

    }
}
