package SPAARKbuild;

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

    protected static int mapSizeFactor;

    protected final static int DEFENSIVE = 0;
    protected final static int OFFENSIVE = 1;
    protected final static int SCOUT = 2;
    protected final static int BUILD = 3;

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
        Build.rc = rc;
        Build.rng = rng;

        Comms.init();

        mapSizeFactor = (rc.getMapHeight() + rc.getMapWidth()) / 20 - 2;
        
        if (Comms.id < 3) {
            mode = DEFENSIVE;
        }
        else if (Comms.id < mapSizeFactor + 3) {
            //vary # of scouts based on map size
            mode = SCOUT;
        } else if (Comms.id >= 47) {
            mode = BUILD;
        }

        Clock.yield();

        for (int i = 50; --i >= 0;) {
            Comms.commsIdToGameId[i] = rc.readSharedArray(i);
            Comms.gameIdToCommsId[Comms.commsIdToGameId[i]-10000] = i;
        }
        if (Comms.id == 49) {
            //clear arr
            for (int i = 50; --i >= 0;) {
                rc.writeSharedArray(i, 0);
            }
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
                            for (int i = 27; --i >= 0;) {
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
                        for (int i = 27; --i >= 0;) {
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
                        for (int i = 27; --i >= 0;) {
                            if (!rc.canSpawn(spawnLocs[i])) {
                                spawnLocs[i] = new MapLocation(-1000, -1000);
                            }
                        }
                        MapLocation bestSpawnLoc = Motion.getClosestPair(spawnLocs, hiddenFlags);
                        for (int i = 3; --i >= 0;) {
                            if (Comms.isFlagInDanger(rc.readSharedArray(Comms.ALLY_FLAG_CUR_LOC + i))) {
                                bestSpawnLoc = Motion.getClosest(spawnLocs, Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_CUR_LOC + i)));
                            }
                        }
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
                Build.indicatorString = indicatorString;
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
                    else if (mode == BUILD) {
                        Build.jailed();
                    }
                    else {
                        Offense.jailed();
                    }
                }
                else {
                    Motion.opponentRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                    Motion.friendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
                    Motion.flags = rc.senseNearbyFlags(-1);
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
                    else if (mode == BUILD) {
                        Build.run();
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
