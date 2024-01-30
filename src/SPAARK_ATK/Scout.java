package SPAARK_ATK;

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
    
    protected static MapLocation target = null;
    protected static int targetTurns = 0;
    
    protected static void run() throws GameActionException {
        if (Comms.getFlagAdv() < 0) { //tested: -1 (small difference)
            Offense.run();
            return;
        }
        MapLocation me = rc.getLocation();

        rc.setIndicatorDot(me, 0, 255, 0);

        // try to sneak flags back (call for help?)
        Offense.tryPickupFlag();
        FlagInfo[] opponentFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());

        FlagInfo[] friendlyFlags = rc.senseNearbyFlags(-1, rc.getTeam());
        for (FlagInfo flag : friendlyFlags) {
            Comms.writeFlag(flag);
        }
        for (FlagInfo flag : opponentFlags) {
            Comms.writeFlag(flag);
        }

        Comms.updatePOI();
        Motion.updateBfsMap();

        run: {
            if (Offense.flagIndex != -1) {
                Offense.moveWithFlag();
                break run;
            }
            if (!Setup.getCrumbs(rc.senseNearbyMapInfos())) {
                if (target == null || rc.getLocation().distanceSquaredTo(target) <= 4 || targetTurns >= 50) {
                    targetTurns = 0;
                    target = new MapLocation(rng.nextInt(rc.getMapWidth()), rng.nextInt(rc.getMapHeight()));
                }
                targetTurns += 1;
                rc.setIndicatorLine(me, target, 0, 255, 0);
                Motion.bfsnav(target);
            }
        }

        
        Offense.tryPickupFlag();

        Atk.attack();
        Atk.heal();
    }
    protected static void jailed() throws GameActionException {
        Offense.jailed();
    }
}
