package SPAARK;

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
    
    protected static void run() throws GameActionException {
        FlagInfo[] opponentFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        FlagInfo[] friendlyFlags = rc.senseNearbyFlags(-1, rc.getTeam());
        for (FlagInfo flag : friendlyFlags) {
            GlobalArray.writeFlag(flag);
        }
        for (FlagInfo flag : opponentFlags) {
            GlobalArray.writeFlag(flag);
        }
        if (rc.getRoundNum() == 1) {
            return;
        }
        MapLocation targetLoc = new MapLocation(0, 0);
        switch (GlobalArray.id) {
            case 0:
                targetLoc = GlobalArray.parseLocation(rc.readSharedArray(3));
                break;
            case 1:
                targetLoc = GlobalArray.parseLocation(rc.readSharedArray(4));
                break;
            case 2:
                targetLoc = GlobalArray.parseLocation(rc.readSharedArray(5));
                break;
            // case 0:
            //     targetLoc = GlobalArray.parseLocation(rc.readSharedArray(3)).add(Direction.NORTHEAST);
            //     break;
            // case 1:
            //     targetLoc = GlobalArray.parseLocation(rc.readSharedArray(3)).add(Direction.NORTHWEST);
            //     break;
            // case 2:
            //     targetLoc = GlobalArray.parseLocation(rc.readSharedArray(3)).add(Direction.SOUTHEAST);
            //     break;
            // case 3:
            //     targetLoc = GlobalArray.parseLocation(rc.readSharedArray(3)).add(Direction.SOUTHWEST);
            //     break;
            // case 4:
            //     targetLoc = GlobalArray.parseLocation(rc.readSharedArray(4)).add(Direction.NORTHEAST);
            //     break;
            // case 5:
            //     targetLoc = GlobalArray.parseLocation(rc.readSharedArray(4)).add(Direction.NORTHWEST);
            //     break;
            // case 6:
            //     targetLoc = GlobalArray.parseLocation(rc.readSharedArray(4)).add(Direction.SOUTHEAST);
            //     break;
            // case 7:
            //     targetLoc = GlobalArray.parseLocation(rc.readSharedArray(4)).add(Direction.SOUTHWEST);
            //     break;
            // case 8:
            //     targetLoc = GlobalArray.parseLocation(rc.readSharedArray(5)).add(Direction.NORTHEAST);
            //     break;
            // case 9:
            //     targetLoc = GlobalArray.parseLocation(rc.readSharedArray(5)).add(Direction.NORTHWEST);
            //     break;
            // case 10:
            //     targetLoc = GlobalArray.parseLocation(rc.readSharedArray(5)).add(Direction.SOUTHEAST);
            //     break;
            // case 11:
            //     targetLoc = GlobalArray.parseLocation(rc.readSharedArray(5)).add(Direction.SOUTHWEST);
            //     break;
        }
        
        RobotInfo[] opponentRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (opponentRobots.length != 0) {
            Motion.bugnavTowards(Attack.getPrioritizedOpponentRobot(opponentRobots).getLocation(), 0);
        }
        else {
            Motion.bugnavTowards(targetLoc, 999);
            if (rc.getLocation().equals(targetLoc)) {
                for (int j = 0; j < 8; j++) {
                    MapLocation buildLoc = rc.getLocation().add(DIRECTIONS[j]);
                    if (rc.canBuild(TrapType.EXPLOSIVE, buildLoc) && rc.getRoundNum() > 100) {
                        rc.build(TrapType.EXPLOSIVE, buildLoc);
                        break;
                    }
                    // if (j % 2 == 0) {
                    //     if (rc.canBuild(TrapType.STUN, buildLoc)) {
                    //         rc.build(TrapType.STUN, buildLoc);
                    //     }
                    // }
                    // else {
                    //     // if (rc.canBuild(TrapType.WATER, buildLoc)) {
                    //     //     rc.build(TrapType.WATER, buildLoc);
                    //     // }
                    // }
                }
            }
        }

        Attack.attack();
        Attack.heal();
    }
    protected static void jailed() throws GameActionException {

    }
}
