package TSPAARKJAN20;

import battlecode.common.*;

import java.util.Random;

public class Scout {
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
    protected static int flagIndex = -1;
    
    protected static MapLocation target = null;
    protected static int targetTurns = 0;
    
    protected static void run() throws GameActionException {
        MapLocation me = rc.getLocation();

        rc.setIndicatorDot(me, 0, 255, 0);

        // try to sneak flags back (call for help?)
        FlagInfo[] opponentFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());

        FlagInfo closestFlag = Motion.getClosestFlag(opponentFlags, false);
        if (closestFlag != null && rc.canPickupFlag(closestFlag.getLocation())) {
            // rc.pickupFlag(closestFlag.getLocation());
            // int flagId = closestFlag.getID();
            // for (int i = 0; i <= 2; i++) {
            //     if (rc.readSharedArray(GlobalArray.OPPO_FLAG_ID + i) == 0) {
            //         flagIndex = i;
            //         rc.writeSharedArray(GlobalArray.OPPO_FLAG_ID + i, flagId);
            //         break;
            //     }
            //     else if (rc.readSharedArray(GlobalArray.OPPO_FLAG_ID + i) == flagId) {
            //         flagIndex = i;
            //         break;
            //     }
            // }
        }
        opponentFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());

        FlagInfo[] friendlyFlags = rc.senseNearbyFlags(-1, rc.getTeam());
        for (FlagInfo flag : friendlyFlags) {
            GlobalArray.writeFlag(flag);
        }
        for (FlagInfo flag : opponentFlags) {
            GlobalArray.writeFlag(flag);
        }

        GlobalArray.updatePOI();
        Motion.updateBfsMap();

        if (target == null || rc.getLocation().distanceSquaredTo(target) <= 4 || targetTurns >= 50) {
            targetTurns = 0;
            target = new MapLocation(rng.nextInt(rc.getMapWidth()), rng.nextInt(rc.getMapHeight()));
        }
        targetTurns += 1;

        rc.setIndicatorLine(me, target, 0, 255, 0);
        Motion.bfsnav(target);

        Attack.attack();
        Attack.heal();
    }
    protected static void jailed() throws GameActionException {
        if (flagIndex != -1) {
            rc.writeSharedArray(GlobalArray.OPPO_FLAG_CUR_LOC + flagIndex, rc.readSharedArray(GlobalArray.OPPO_FLAG_DEF_LOC + flagIndex));
            flagIndex = -1;
        }
    }
}
