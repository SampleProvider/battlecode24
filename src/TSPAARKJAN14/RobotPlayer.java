package TSPAARKJAN14;

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
        Scout.rc = rc;
        Scout.rng = rng;
        Leader.rc = rc;
        Leader.rng = rng;
        Follower.rc = rc;
        Follower.rng = rng;

        GlobalArray.init();
        
        if (GlobalArray.groupId == 0) {
            mode = DEFENSIVE;
        } else if (GlobalArray.groupId == 1) {
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
                        if (GlobalArray.id < 3) {
                            if (!GlobalArray.hasLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC + GlobalArray.id))) {
                                break spawn;
                            }
                            MapLocation spawnLoc = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC + GlobalArray.id));
                            if (rc.canSpawn(spawnLoc)) {
                                rc.spawn(spawnLoc);
                                break spawn;
                            }
                            break spawn;
                        }
                    }
                    if (hiddenFlags.length == 0 || rc.getRoundNum() <= 20) {
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
                GlobalArray.indicatorString = indicatorString;
                Setup.indicatorString = indicatorString;
                Offensive.indicatorString = indicatorString;
                Defensive.indicatorString = indicatorString;
                Scout.indicatorString = indicatorString;
                Leader.indicatorString = indicatorString;
                Follower.indicatorString = indicatorString;
                if (GlobalArray.id == 0) {
                    GlobalArray.incrementSectorTime();
                    GlobalArray.allocateGroups();
                }
                if (!rc.isSpawned()) {
                    if (mode == DEFENSIVE) {
                        Defensive.jailed();
                    }
                    else if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS) {
                        Setup.jailed();
                    }
                    else if (mode == SCOUT) {
                        Scout.jailed();
                    }
                    else {
                        if (GlobalArray.groupLeader) {
                            Leader.jailed();
                        }
                        Follower.jailed();
                        Offensive.jailed();
                    }
                }
                else {
                    if (rc.getRoundNum() == 200) {
                        if (mode == DEFENSIVE && GlobalArray.id < 3) {
                            spawnLoc = rc.getLocation();
                        }
                    }
                    
                    if (rc.getRoundNum() >= 600 && rc.canBuyGlobal(GlobalUpgrade.ATTACK)) {
                        rc.buyGlobal(GlobalUpgrade.ATTACK);
                    }
                    if (rc.getRoundNum() >= 1200 && rc.canBuyGlobal(GlobalUpgrade.CAPTURING)) {
                        rc.buyGlobal(GlobalUpgrade.CAPTURING);
                    }
                    if (rc.getRoundNum() >= 1800 && rc.canBuyGlobal(GlobalUpgrade.HEALING)) {
                        rc.buyGlobal(GlobalUpgrade.HEALING);
                    }
                    if (mode == DEFENSIVE) {
                        Defensive.run();
                    }
                    else if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS) {
                        Setup.run();
                    }
                    else if (mode == SCOUT) {
                        Scout.run();
                    }
                    else {
                        if (GlobalArray.groupLeader) {
                            Leader.run();
                        }
                        Follower.run();
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
