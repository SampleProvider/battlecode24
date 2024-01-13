package SPAARK;

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
        Offensive.rc = rc;
        Offensive.rng = rng;
        Defensive.rc = rc;
        Defensive.rng = rng;

        GlobalArray.init();

        if (GlobalArray.id < 3) {
            mode = DEFENSIVE;
        }

        while (true) {
            turnCount += 1;

            try {
                spawn: if (!rc.isSpawned()) {
                    MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                    MapLocation[] hiddenFlags = rc.senseBroadcastFlagLocations();
                    if (mode == DEFENSIVE && spawnLoc.x != -1) {
                        if (rc.canSpawn(spawnLoc)) {
                            rc.spawn(spawnLoc);
                            break spawn;
                        }
                    }
                    if (hiddenFlags.length == 0 || rc.getRoundNum() == 1) {
                        int index = rng.nextInt(27 * 3);
                        for (int i = 0; i < 27; i++) {
                            MapLocation randomLoc = spawnLocs[index % spawnLocs.length];
                            if (rc.canSpawn(randomLoc)) {
                                rc.spawn(randomLoc);
                                break;
                            }
                            else {
                                index++;
                            }
                        }
                    }
                    else {
                        for (int i = 0; i < 27; i++) {
                            if (!rc.canSpawn(spawnLocs[i])) {
                                spawnLocs[i] = new MapLocation(-1000, -1000);
                            }
                        }
                        MapLocation bestSpawnLoc = Motion.getClosest(spawnLocs, hiddenFlags[0]);
                        if (bestSpawnLoc != null && rc.canSpawn(bestSpawnLoc)) {
                            rc.spawn(bestSpawnLoc);
                        }
                    }
                }
                StringBuilder indicatorString = new StringBuilder();
                Motion.indicatorString = indicatorString;
                Attack.indicatorString = indicatorString;
                Setup.indicatorString = indicatorString;
                Offensive.indicatorString = indicatorString;
                Defensive.indicatorString = indicatorString;
                if (!rc.isSpawned()) {
                    if (mode == DEFENSIVE) {
                        Defensive.jailed();
                    }
                    else if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS) {
                        Setup.jailed();
                    }
                    else {
                        Offensive.jailed();
                    }
                }
                else {
                    if (rc.getRoundNum() == 200) {
                        if (mode == DEFENSIVE) {
                            spawnLoc = rc.getLocation();
                        }
                    }
                    
                    if (rc.getRoundNum() == 750 && rc.canBuyGlobal(GlobalUpgrade.ACTION)) {
                        rc.buyGlobal(GlobalUpgrade.ACTION);
                    }
                    if (rc.getRoundNum() == 1500 && rc.canBuyGlobal(GlobalUpgrade.HEALING)) {
                        rc.buyGlobal(GlobalUpgrade.HEALING);
                    }
                    if (mode == DEFENSIVE) {
                        mode = Defensive.run();
                    }
                    else if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS) {
                        Setup.run();
                    }
                    else {
                        mode = Offensive.run();
                    }
                }
                rc.setIndicatorString(indicatorString.toString());

            }
            catch (GameActionException e) {
                System.out.println("GameActionException");
                e.printStackTrace();
                rc.resign();
            }
            catch (Exception e) {
                System.out.println("Exception");
                e.printStackTrace();
                rc.resign();
            }
            finally {
                Clock.yield();
            }
        }
    }
}
