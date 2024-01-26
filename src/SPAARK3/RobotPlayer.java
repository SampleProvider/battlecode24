package SPAARK3;

import battlecode.common.*;

import java.util.Random;

import SPAARK2.GlobalArray;

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
        Atk.rc = rc;
        Comms.rc = rc;
        Setup.rc = rc;
        Setup.rng = rng;
        Offense.rc = rc;
        Offense.rng = rng;
        Defense.rc = rc;
        Defense.rng = rng;
        Scout.rc = rc;
        Scout.rng = rng;

        Comms.init();
        
        if (Comms.id < 3) {
            mode = DEFENSIVE;
        }
        else if (Comms.id < 6) {
            mode = SCOUT;
        }

        Clock.yield();

        while (true) {
            turnCount += 1;

            try {
                spawn: if (!rc.isSpawned()) {
                    MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                    MapLocation[] hiddenFlags = rc.senseBroadcastFlagLocations();
                    if (mode == DEFENSIVE) {
                        if (Comms.id < 6) {
                            if (!Comms.hasLocation(rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC + (Comms.id % 3)))) {
                                break spawn; // labels moment
                            }
                            for (int i = 0; i < 27; i++) {
                                if (!rc.canSpawn(spawnLocs[i])) {
                                    spawnLocs[i] = new MapLocation(-1000, -1000);
                                }
                            }
                            MapLocation bestSpawnLoc = Motion.getClosest(spawnLocs, Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC + Comms.id)));
                            if (bestSpawnLoc != null && rc.canSpawn(bestSpawnLoc)) {
                                rc.spawn(bestSpawnLoc);
                                break spawn;
                            }
                        }
                    }
                    if (hiddenFlags.length == 0 || rc.getRoundNum() <= 20) {
                        int index = rng.nextInt(27 * 3);
                        for (int i = 0; i < 27; i++) {
                            MapLocation randomLoc = spawnLocs[index % spawnLocs.length];
                            if (rc.canSpawn(randomLoc)) {
                                rc.spawn(randomLoc);
                                break spawn;
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
                        MapLocation bestSpawnLoc = Motion.getClosestPair(spawnLocs, hiddenFlags);
                        // for (int i = 0; i < 3; i++) {
                        //     if (Comms.isFlagInDanger(rc.readSharedArray(Comms.ALLY_FLAG_CUR_LOC + i))) {
                                
                        //     }
                        // }
                        // MapLocation bestSpawnLoc = Motion.getClosest(spawnLocs, Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC + Comms.id)));
                        if (bestSpawnLoc != null && rc.canSpawn(bestSpawnLoc)) {
                            rc.spawn(bestSpawnLoc);
                        }
                    }
                }
                StringBuilder indicatorString = new StringBuilder();
                Motion.indicatorString = indicatorString;
                Atk.indicatorString = indicatorString;
                Comms.indicatorString = indicatorString;
                Setup.indicatorString = indicatorString;
                Offense.indicatorString = indicatorString;
                Defense.indicatorString = indicatorString;
                Scout.indicatorString = indicatorString;
                if (!rc.isSpawned()) {
                    if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS) {
                        Setup.jailed();
                    }
                    else if (mode == DEFENSIVE) {
                        Defense.jailed();
                    }
                    else if (mode == SCOUT) {
                        Scout.jailed();
                    }
                    else {
                        Offense.jailed();
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
                    else if (mode == DEFENSIVE) {
                        Defense.run();
                    }
                    else if (mode == SCOUT) {
                        Scout.run();
                    }
                    else {
                        Offense.run();
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