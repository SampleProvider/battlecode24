package Defensive_SPAARK_OLD;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Defensive {
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
    
    public static void run() throws GameActionException {
        FlagInfo[] flags = rc.senseNearbyFlags(-1, rc.getTeam());
        if (flags.length > 0) {
            Motion.bug2(flags[0].getLocation());
        }
        else {
            Motion.moveRandomly();
        }
        MapLocation buildLoc = rc.getLocation().add(DIRECTIONS[rng.nextInt(8)]);
        if (rc.canBuild(TrapType.EXPLOSIVE, buildLoc) && rng.nextInt() % 37 == 1) {
            rc.build(TrapType.EXPLOSIVE, buildLoc);
        }
        else if (rc.canBuild(TrapType.WATER, buildLoc) && rng.nextInt() % 37 == 1) {
            rc.build(TrapType.WATER, buildLoc);
        }
        else if (rc.canBuild(TrapType.STUN, buildLoc) && rng.nextInt() % 37 == 1) {
            rc.build(TrapType.STUN, buildLoc);
        }
        Attack.attack();
        Attack.heal();
    }
}
