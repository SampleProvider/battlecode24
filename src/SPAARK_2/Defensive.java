package SPAARK_2;

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
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.NORTHWEST,
        Direction.NORTH,
    };
    
    public static void run() throws GameActionException {
        for (int j = 0; j < 8; j++) {
            MapLocation buildLoc = rc.getLocation().add(DIRECTIONS[j]);
            if (j % 2 == 0) {
                if (rc.canBuild(TrapType.WATER, buildLoc)) {
                    rc.build(TrapType.WATER, buildLoc);
                }
            }
            else {
                if (rc.canBuild(TrapType.STUN, buildLoc)) {
                    rc.build(TrapType.STUN, buildLoc);
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
        
        if (rc.getRoundNum() < 20) {
            FlagInfo closestFlag = Motion.getClosestFlag(friendlyFlags, false);
            if (closestFlag != null) {
                Motion.bugnavTowards(closestFlag.getLocation(), 999);
            }
        }

        Attack.attack();
        Attack.heal();
    }
    public static void jailed() throws GameActionException {

    }
}
