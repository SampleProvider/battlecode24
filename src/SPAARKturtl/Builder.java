package SPAARKturtl;

import java.util.Random;

import battlecode.common.*;

public class Builder {
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
        Turtle.circleFlag(flag);
        for (Direction d : DIRECTIONS) {
            MapLocation loc = rc.adjacentLocation(d);
            if (rc.canBuild(TrapType.STUN, loc) && (loc.x+loc.y)%2 == 1) {
                rc.build(TrapType.STUN, loc);
            }
        }
        rc.setIndicatorLine(me, flag, 128, 0, 128);
        Attack.attack();
        Attack.heal();
    }

    protected static void jailed() throws GameActionException {

    }
}
