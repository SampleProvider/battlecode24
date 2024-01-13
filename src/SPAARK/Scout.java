package SPAARK;

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
    
    protected static int run() throws GameActionException {

        Attack.attack();
        Attack.heal();
        return RobotPlayer.SCOUT;
    }
    protected static void jailed() throws GameActionException {

    }
}
