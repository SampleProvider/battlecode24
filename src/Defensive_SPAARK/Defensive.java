package Defensive_SPAARK;

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
        for (int i = 0; i <= 2; i++) {
            int n = rc.readSharedArray(i);
            if (GlobalArray.hasLocation(n) && !GlobalArray.isFlagPlaced(n)) {
                MapLocation loc = GlobalArray.parseLocation(n);
                loc = new MapLocation(5, rc.getMapHeight() - 6);
                Motion.bugnavAround(loc, 5, 20, false);
                if (rc.getLocation().distanceSquaredTo(loc) < 20) {
                    for (int j = 0; j < 8; j++) {
                        MapLocation buildLoc = rc.getLocation().add(DIRECTIONS[j]);
                        if (rc.canBuild(TrapType.EXPLOSIVE, buildLoc) && rng.nextInt() % 5 == 1) {
                            rc.build(TrapType.EXPLOSIVE, buildLoc);
                        }
                        // else if (rc.canBuild(TrapType.WATER, buildLoc) && rng.nextInt() % 5 == 1) {
                        //     rc.build(TrapType.WATER, buildLoc);
                        // }
                        else if (rc.canBuild(TrapType.STUN, buildLoc) && rng.nextInt() % 5 == 1) {
                            rc.build(TrapType.STUN, buildLoc);
                        }
                    }
                }
                break;
            }
        }
        Attack.attack();
        Attack.heal();
    }
    public static void jailed() throws GameActionException {

    }
}
