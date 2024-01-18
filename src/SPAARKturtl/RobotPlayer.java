package SPAARKturtl;

import battlecode.common.*;

import java.util.Random;

public strictfp class RobotPlayer {
    protected static int turnCount = 0;

    protected static Random rng;

    protected static Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    protected static int mode = -1;
    protected static MapLocation spawnLoc = new MapLocation(-1, -1);

    protected final static int DEFENSIVE = 0;
    protected final static int OFFENSIVE = 1;
    protected final static int SCOUT = 2;

    public static void run(RobotController rc) throws GameActionException {
        rng = new Random(rc.getID() + 2024);
        Motion.rc = rc;
        Motion.rng = rng;
        Attack.rc = rc;
        GlobalArray.rc = rc;
        Setup.rc = rc;
        Setup.rng = rng;
        Attacker.rc = rc;
        Attacker.rng = rng;
        Healer.rc = rc;
        Healer.rng = rng;
        Builder.rc = rc;
        Builder.rng = rng;

        GlobalArray.init();

        Clock.yield();

        while (true) {
            turnCount += 1;

            try {
                spawn: if (!rc.isSpawned()) {
                }
                StringBuilder indicatorString = new StringBuilder();
                Motion.indicatorString = indicatorString;
                Attack.indicatorString = indicatorString;
                GlobalArray.indicatorString = indicatorString;
                Setup.indicatorString = indicatorString;
                if (!rc.isSpawned()) {
                    if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS) {
                        Setup.jailed();
                    }
                    else {
                        Defensive.jailed();
                    }
                }
                else {
                    if (rc.getRoundNum() >= 600 && rc.canBuyGlobal(GlobalUpgrade.ATTACK)) {
                        rc.buyGlobal(GlobalUpgrade.ATTACK);
                    }
                    if (rc.getRoundNum() >= 1200 && rc.canBuyGlobal(GlobalUpgrade.HEALING)) {
                        rc.buyGlobal(GlobalUpgrade.HEALING);
                    }
                    if (rc.getRoundNum() >= 1800 && rc.canBuyGlobal(GlobalUpgrade.CAPTURING)) {
                        rc.buyGlobal(GlobalUpgrade.CAPTURING);
                    }
                    if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS) {
                        Setup.run();
                    }
                    else {
                        Defensive.run();
                    }
                }
                rc.setIndicatorString(indicatorString.toString());

            }
            catch (GameActionException e) {
                System.out.println("GameActionException");
                e.printStackTrace();
                // rc.resign();
            }
            catch (Exception e) {
                System.out.println("Exception");
                e.printStackTrace();
                // rc.resign();
            }
            finally {
                Clock.yield();
            }
        }
    }
}
