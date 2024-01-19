package SPAARKturtl;

import java.util.Random;

import battlecode.common.*;

public class Healer {
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

    protected static void run() throws GameActionException {
        MapLocation flag = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_CUR_LOC + GlobalArray.flag));
        MapLocation me = rc.getLocation();
        if (me.distanceSquaredTo(flag) > 2) {
            int[] dangers = {
                rc.readSharedArray(GlobalArray.ALLY_FLAG_INFO),
                rc.readSharedArray(GlobalArray.ALLY_FLAG_INFO+1),
                rc.readSharedArray(GlobalArray.ALLY_FLAG_INFO+2)
            };
            int maxDanger = 0;
            int maxDangerIndex = -1;
            for (int i = 0; i < dangers.length; i++) {
                if (i == GlobalArray.flag) continue;
                if (dangers[i] > 997 + (GlobalArray.id % 16 - 10) && dangers[i] > dangers[GlobalArray.flag] && dangers[i] > maxDanger) {
                    maxDanger = dangers[i];
                    maxDangerIndex = i;
                }
            }
            if (maxDangerIndex != -1) {
                GlobalArray.flag = maxDangerIndex;
            }
        }
        Turtle.circleFlag(flag);
        rc.setIndicatorLine(me, flag, 0, 255, 255);
        Attack.heal();
        Attack.attack();
    }

    protected static void jailed() throws GameActionException {
        
    }
}
