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
        Turtle.rc = rc;
        Turtle.rng = rng;

        GlobalArray.init();

        Clock.yield();

        PREPARE_ROUND = GameConstants.SETUP_ROUNDS - Math.max(rc.getMapHeight(), rc.getMapWidth()) + 10;

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
                    } else {
                        MapLocation[] spawns = rc.getAllySpawnLocations();
                        MapLocation flag = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.SETUP_FLAG_TARGET));
                        for (MapLocation s : spawns) {
                            if (s.distanceSquaredTo(flag) < 10) {
                                if (rc.canSpawn(s)) {
                                    rc.spawn(s);
                                }
                            }
                        }
                    }
                }
                StringBuilder indicatorString = new StringBuilder();
                Motion.indicatorString = indicatorString;
                Attack.indicatorString = indicatorString;
                Attacker.indicatorString = indicatorString;
                Healer.indicatorString = indicatorString;
                Builder.indicatorString = indicatorString;
                Turtle.indicatorString = indicatorString;
                GlobalArray.indicatorString = indicatorString;
                Setup.indicatorString = indicatorString;
                if (!rc.isSpawned()) {
                    if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS) {
                        Setup.jailed();
                    }
                    else if (Turtle.isHealer()) {
                        Healer.jailed();
                    } else if (Turtle.isAttacker()) {
                        Attacker.jailed();
                    } else if (Turtle.isBuilder()) {
                        Builder.jailed();
                    } else {

                    }
                }
                else {
                    if (rc.canBuyGlobal(GlobalUpgrade.ATTACK)) {
                        rc.buyGlobal(GlobalUpgrade.ATTACK);
                    }
                    if (rc.canBuyGlobal(GlobalUpgrade.HEALING)) {
                        rc.buyGlobal(GlobalUpgrade.HEALING);
                    }
                    if (rc.canBuyGlobal(GlobalUpgrade.CAPTURING)) {
                        rc.buyGlobal(GlobalUpgrade.CAPTURING);
                    }
                    if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS) {
                        Setup.run();
                    }
                    else if (Turtle.isHealer()) {
                        Healer.run();
                    } else if (Turtle.isAttacker()) {
                        Attacker.run();
                    } else if (Turtle.isBuilder()) {
                        Builder.run();
                    } else {

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
