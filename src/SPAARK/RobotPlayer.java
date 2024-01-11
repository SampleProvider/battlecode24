package SPAARK;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public strictfp class RobotPlayer {
    static int turnCount = 0;

    static Random rng;

    static Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

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

        while (true) {
            turnCount += 1;

            try {
                if (!rc.isSpawned()) {
                    MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                    MapLocation[] hiddenFlags = rc.senseBroadcastFlagLocations();
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
                        MapLocation bestSpawnLoc = Motion.getNearest(spawnLocs, hiddenFlags[0]);
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
                    if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS) {
                        Setup.jailed();
                    }
                    else {
                        Offensive.jailed();
                    }
                }
                else {
                    if (rc.getRoundNum() == 750 && rc.canBuyGlobal(GlobalUpgrade.ACTION)) {
                        rc.buyGlobal(GlobalUpgrade.ACTION);
                    }
                    if (rc.getRoundNum() == 1500 && rc.canBuyGlobal(GlobalUpgrade.HEALING)) {
                        rc.buyGlobal(GlobalUpgrade.HEALING);
                    }
                    if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS) {
                        Setup.run();
                    }
                    else {
                        Offensive.run();
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
