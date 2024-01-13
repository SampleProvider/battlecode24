package BeforeGroupsSPAARK;

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
        if (rc.getRoundNum() <= 20) {
            return;
        }
        MapLocation targetLoc = new MapLocation(0, 0);
        switch (GlobalArray.id) {
            case 0:
                targetLoc = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_CUR_LOC));
                break;
            case 1:
                targetLoc = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_CUR_LOC + 1));
                break;
            case 2:
                targetLoc = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_CUR_LOC + 2));
                break;
            // case 0:
            //     targetLoc = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC)).add(Direction.NORTHEAST);
            //     break;
            // case 1:
            //     targetLoc = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC)).add(Direction.NORTHWEST);
            //     break;
            // case 2:
            //     targetLoc = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC)).add(Direction.SOUTHEAST);
            //     break;
            // case 3:
            //     targetLoc = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC)).add(Direction.SOUTHWEST);
            //     break;
            // case 4:
            //     targetLoc = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC + 1)).add(Direction.NORTHEAST);
            //     break;
            // case 5:
            //     targetLoc = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC + 1)).add(Direction.NORTHWEST);
            //     break;
            // case 6:
            //     targetLoc = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC + 1)).add(Direction.SOUTHEAST);
            //     break;
            // case 7:
            //     targetLoc = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC + 1)).add(Direction.SOUTHWEST);
            //     break;
            // case 8:
            //     targetLoc = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC + 2)).add(Direction.NORTHEAST);
            //     break;
            // case 9:
            //     targetLoc = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC + 2)).add(Direction.NORTHWEST);
            //     break;
            // case 10:
            //     targetLoc = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC + 2)).add(Direction.SOUTHEAST);
            //     break;
            // case 11:
            //     targetLoc = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC + 2)).add(Direction.SOUTHWEST);
            //     break;
        }
        
        RobotInfo[] opponentRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (opponentRobots.length != 0) {
            Motion.bugnavTowards(Attack.getPrioritizedOpponentRobot(opponentRobots).getLocation(), 0);
        }
        else {
            Motion.bugnavTowards(targetLoc, Motion.DEFAULT_RETREAT_HP);
            if (rc.getLocation().equals(targetLoc)) {
                rc.writeSharedArray(GlobalArray.id, GlobalArray.intifyLocation(targetLoc));
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
        }

        Attack.attack();
        Attack.heal();
    }
    protected static void jailed() throws GameActionException {

    }
}
