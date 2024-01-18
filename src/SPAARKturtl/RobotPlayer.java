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

    protected final static int HEALER = 0;
    protected final static int ATTACKER = 1;
    protected final static int BUILDER = 2;

    protected static int PREPARE_ROUND;

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

        PREPARE_ROUND = GameConstants.SETUP_ROUNDS + 10 - Math.max(rc.getMapHeight(), rc.getMapWidth());

        while (true) {
            turnCount += 1;

            try {
                spawn: if (!rc.isSpawned()) {
                    if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS) {
                        MapLocation[] spawns = rc.getAllySpawnLocations();
                        int numSpawns = 0;
                        MapLocation spawnLoc = new MapLocation(-1, -1);
                        for (int i = 0; i < 27; i++) {
                            if (rc.canSpawn(spawns[i])) {
                                numSpawns++;
                                if (rng.nextDouble() < 1.0 / (double)numSpawns) {
                                    spawnLoc = spawns[i];
                                }
                            }
                        }
                        if (numSpawns > 0) {
                            rc.spawn(spawnLoc);
                        }
                    }
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
                    else if (mode == HEALER) {
                        Healer.jailed();
                    } else if (mode == ATTACKER) {
                        Attacker.jailed();
                    } else {
                        Builder.jailed();
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
                    else if (mode == HEALER) {
                        Healer.jailed();
                    } else if (mode == ATTACKER) {
                        Attacker.jailed();
                    } else {
                        Builder.jailed();
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
