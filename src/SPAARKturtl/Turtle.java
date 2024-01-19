package SPAARKturtl;

import java.util.Random;

import battlecode.common.*;

public class Turtle {
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

    protected static Boolean isBuilder() throws GameActionException {
        return GlobalArray.type == 0;
    }

    protected static Boolean isAttacker() throws GameActionException {
        return GlobalArray.type == 1;
    }

    protected static Boolean isHealer() throws GameActionException {
        return GlobalArray.type == 2;
    }

    protected static int getFlagIndex() throws GameActionException {
        return GlobalArray.flag;
    }

    protected static void circleFlag(MapLocation flag) throws GameActionException {
        MapLocation me = rc.getLocation();
        int flagIndex = getFlagIndex();
        if (flagIndex != 0) {
            if (me.distanceSquaredTo(flag) <= 2) {
                if (rc.canMove(me.directionTo(flag))) {
                    rc.move(me.directionTo(flag));
                }
            } else {
                Motion.bfsnav(flag);
            }
        } else {
            //flagIndex == 0 (don't block spawn)
            if (me.distanceSquaredTo(flag) <= 2 && me.distanceSquaredTo(flag) > 0) {
                if (rc.canMove(me.directionTo(flag))) {
                    rc.move(me.directionTo(flag));
                } else {
                    Motion.moveRandomly();
                }
            } else {
                Motion.bfsnav(flag);
            }
        }
        if (me.distanceSquaredTo(flag) == 0) {
            RobotInfo[] bots = rc.senseNearbyRobots();
            int ctr = 1000;
            for (RobotInfo bot : bots) {
                if (bot.getTeam() != rc.getTeam()) {
                    ctr++;
                } else {
                    ctr--;
                }
            }
            System.out.println(flagIndex + " " + ctr);
            rc.writeSharedArray(GlobalArray.ALLY_FLAG_INFO+flagIndex, ctr);
        }
    }
}
