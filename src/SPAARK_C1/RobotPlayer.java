package SPAARK_C1;

import battlecode.common.*;

import java.util.Map;
import java.util.Random;

public strictfp class RobotPlayer {
    protected static Random rng;

    protected static Direction[] DIRECTIONS= {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };
    protected static final Direction[] ALL_DIRECTIONS = {
        Direction.SOUTHWEST,
        Direction.SOUTH,
        Direction.SOUTHEAST,
        Direction.WEST,
        Direction.EAST,
        Direction.NORTHWEST,
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.CENTER,
    };

    protected static int mode = -1;
    protected static MapLocation spawnLoc = new MapLocation(-1, -1);
    
    protected static MapLocation spawnLoc1 = new MapLocation(-1, -1);
    protected static MapLocation spawnLoc2 = new MapLocation(-1, -1);
    protected static MapLocation spawnLoc3 = new MapLocation(-1, -1);

    protected final static int DEFENSIVE = 0;
    protected final static int OFFENSIVE = 1;
    protected final static int SCOUT = 2;

    protected static int mapSizeFactor;

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

        mapSizeFactor = (rc.getMapHeight() + rc.getMapWidth()) / 20 - 2; // 1 to 4
        
        if (Comms.id < 3) {
            mode = DEFENSIVE;
        }
        else if (Comms.id < mapSizeFactor + 3) {
            //vary # of scouts based on map size
            mode = SCOUT;
            // tested: no scouts (22 out of 42)
        }

        if (!Comms.hasLocation(rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC))) {
            MapLocation[] spawns = rc.getAllySpawnLocations();
            int numFoundSoFar = 0;
            for (MapLocation m : spawns) {
                int numAdjSpawnZones = 0; // should be 8 if its the center
                for (MapLocation other : spawns) {
                    if (other.equals(m)) continue;
                    if (m.isAdjacentTo(other)) {
                        numAdjSpawnZones++;
                    }
                }
                if (numAdjSpawnZones == 8) {
                    rc.writeSharedArray(Comms.ALLY_FLAG_DEF_LOC + numFoundSoFar, Comms.intifyLocation(m));
                    numFoundSoFar++;
                }
            }
        }

        if (Motion.mapCenter.x < 0) {
            Motion.mapCenter = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
        }

        Clock.yield();

        while (true) {
            try {
                spawn: if (!rc.isSpawned()) {
                    MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                    MapLocation[] hiddenFlags = rc.senseBroadcastFlagLocations();
                    if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS) {
                        MapLocation loc1 = new MapLocation(-1, -1);
                        MapLocation loc2 = new MapLocation(-1, -1);
                        MapLocation loc3 = new MapLocation(-1, -1);
                        for (int i = 27; --i >= 0;) {
                            if (loc1.x == -1) {
                                loc1 = spawnLocs[i];
                            }
                            else if (loc1.distanceSquaredTo(spawnLocs[i]) >= 4 && loc1.distanceSquaredTo(spawnLocs[i]) <= 8) {
                                int x = loc1.x;
                                if (Math.abs(loc1.x - spawnLocs[i].x) == 2) {
                                    x = (loc1.x + spawnLocs[i].x) / 2;
                                }
                                int y = loc1.y;
                                if (Math.abs(loc1.y - spawnLocs[i].y) == 2) {
                                    y = (loc1.y + spawnLocs[i].y) / 2;
                                }
                                loc1 = new MapLocation(x, y);
                            }
                            else if (loc1.distanceSquaredTo(spawnLocs[i]) < 4) {
                            }
                            else if (loc2.x == -1) {
                                loc2 = spawnLocs[i];
                            }
                            else if (loc2.distanceSquaredTo(spawnLocs[i]) >= 4 && loc2.distanceSquaredTo(spawnLocs[i]) <= 8) {
                                int x = loc2.x;
                                if (Math.abs(loc2.x - spawnLocs[i].x) == 2) {
                                    x = (loc2.x + spawnLocs[i].x) / 2;
                                }
                                int y = loc2.y;
                                if (Math.abs(loc2.y - spawnLocs[i].y) == 2) {
                                    y = (loc2.y + spawnLocs[i].y) / 2;
                                }
                                loc2 = new MapLocation(x, y);
                            }
                            else if (loc2.distanceSquaredTo(spawnLocs[i]) < 4) {
                            }
                            else if (loc3.x == -1) {
                                loc3 = spawnLocs[i];
                            }
                            else if (loc3.distanceSquaredTo(spawnLocs[i]) >= 4 && loc3.distanceSquaredTo(spawnLocs[i]) <= 8) {
                                int x = loc3.x;
                                if (Math.abs(loc3.x - spawnLocs[i].x) == 2) {
                                    x = (loc3.x + spawnLocs[i].x) / 2;
                                }
                                int y = loc3.y;
                                if (Math.abs(loc3.y - spawnLocs[i].y) == 2) {
                                    y = (loc3.y + spawnLocs[i].y) / 2;
                                }
                                loc3 = new MapLocation(x, y);
                            }
                        }
                        spawnLoc1 = loc1;
                        spawnLoc2 = loc2;
                        spawnLoc3 = loc3;
                        if (mode == DEFENSIVE) {
                            //basically spawn next to your assigned flag lol
                            MapLocation target = Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC + Comms.id % 3));
                            for (int i = 27; --i >= 0;) {
                                if (rc.canSpawn(spawnLocs[i]) && spawnLocs[i].isAdjacentTo(target)) {
                                    rc.spawn(spawnLocs[i]);
                                    break spawn;
                                }
                            }
                        }
                        MapLocation[] spawns = {
                            Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC)),
                            Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC+1)),
                            Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC+2)),
                        };
                        MapLocation favoredSpawn = new MapLocation(0, 0);
                        if (Comms.id % 10 < 4) {
                            favoredSpawn = Motion.getClosest(spawns, Motion.mapCenter);
                        } else if (Comms.id % 10 < 7) {
                            //ends with 1, 5, 9
                            for (int i = 3; --i >= 0;) {
                                if (!spawns[i].equals(Motion.getFarthest(spawns, Motion.mapCenter)) && !spawns[i].equals(Motion.getClosest(spawns, Motion.mapCenter))) {
                                    favoredSpawn = spawns[i];
                                    break;
                                }
                            }
                        } else {
                            favoredSpawn = Motion.getFarthest(spawns, Motion.mapCenter);
                        }
                        for (Direction d : ALL_DIRECTIONS) {
                            if (rc.canSpawn(favoredSpawn.add(d))) {
                                rc.spawn(favoredSpawn.add(d));
                                break;
                            }
                        }

                    } else {
                        //game started
                        if (hiddenFlags.length == 0) {
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
                            MapLocation[] safeSpawnAreas = new MapLocation[] {
                                spawnLoc1,
                                spawnLoc2,
                                spawnLoc3,
                            };
                            for (int i = 0; i < 3; i++) {
                                if (rc.readSharedArray(Comms.SPAWN_SAFETY + i) > 10 && rc.getRoundNum() - rc.readSharedArray(Comms.SPAWN_SAFETY + i + 3) < 50) {
                                    safeSpawnAreas[i] = new MapLocation(-1000, -1000);
                                }
                            }
                            MapLocation[] safeSpawnLocs = new MapLocation[] {
                                new MapLocation(-1000, -1000),
                                new MapLocation(-1000, -1000),
                                new MapLocation(-1000, -1000),
                                new MapLocation(-1000, -1000),
                                new MapLocation(-1000, -1000),
                                new MapLocation(-1000, -1000),
                                new MapLocation(-1000, -1000),
                                new MapLocation(-1000, -1000),
                                new MapLocation(-1000, -1000),
                                new MapLocation(-1000, -1000),
                                new MapLocation(-1000, -1000),
                                new MapLocation(-1000, -1000),
                                new MapLocation(-1000, -1000),
                                new MapLocation(-1000, -1000),
                                new MapLocation(-1000, -1000),
                                new MapLocation(-1000, -1000),
                                new MapLocation(-1000, -1000),
                                new MapLocation(-1000, -1000),
                                new MapLocation(-1000, -1000),
                                new MapLocation(-1000, -1000),
                                new MapLocation(-1000, -1000),
                                new MapLocation(-1000, -1000),
                                new MapLocation(-1000, -1000),
                                new MapLocation(-1000, -1000),
                                new MapLocation(-1000, -1000),
                                new MapLocation(-1000, -1000),
                                new MapLocation(-1000, -1000),
                            };
                            for (int i = 9; --i >= 0;) {
                                safeSpawnLocs[i] = safeSpawnAreas[0].add(ALL_DIRECTIONS[i]);
                            }
                            for (int i = 9; --i >= 0;) {
                                safeSpawnLocs[i + 9] = safeSpawnAreas[1].add(ALL_DIRECTIONS[i]);
                            }
                            for (int i = 9; --i >= 0;) {
                                safeSpawnLocs[i + 18] = safeSpawnAreas[2].add(ALL_DIRECTIONS[i]);
                            }
                            for (int i = 27; --i >= 0;) {
                                if (!rc.canSpawn(safeSpawnLocs[i])) {
                                    safeSpawnLocs[i] = new MapLocation(-1000, -1000);
                                }
                            }
                            MapLocation bestSpawnLoc = Motion.getClosestPair(safeSpawnLocs, hiddenFlags);
                            for (int i = 3; --i >= 0;) {
                                if (Comms.isFlagInDanger(rc.readSharedArray(Comms.ALLY_FLAG_CUR_LOC + i))) {
                                    bestSpawnLoc = Motion.getClosest(safeSpawnLocs, Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_CUR_LOC + i)));
                                }
                            }
                            // MapLocation bestSpawnLoc = Motion.getClosest(spawnLocs, Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC + Comms.id)));
                            if (bestSpawnLoc != null && rc.canSpawn(bestSpawnLoc)) {
                                int index = -1;
                                for (int i = 3; --i >= 0;) {
                                    if (safeSpawnAreas[i].distanceSquaredTo(bestSpawnLoc) <= 2) {
                                        index = i;
                                        break;
                                    }
                                }
                                rc.spawn(bestSpawnLoc);
                                if (index != -1) {
                                    rc.writeSharedArray(Comms.SPAWN_SAFETY + index, Math.max(rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length - rc.senseNearbyRobots(-1, rc.getTeam()).length, 0));
                                    rc.writeSharedArray(Comms.SPAWN_SAFETY + index + 3, rc.getRoundNum());
                                }
                                else {
                                    System.out.println("BORKEN");
                                }
                            }
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
                    } else {
                        switch (mode) {
                            case DEFENSIVE:
                                Defense.jailed();
                                break;
                            case SCOUT:
                                Scout.jailed();
                                break;
                            default:
                                Offense.jailed();
                                break;
                        }
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
                    else {
                        switch (mode) {
                            case DEFENSIVE:
                                Defense.run();
                                break;
                            case SCOUT:
                                Scout.run();
                                break;
                            default:
                                Offense.run();
                                break;
                        }
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
