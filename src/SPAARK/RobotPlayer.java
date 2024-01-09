package SPAARK;

import battlecode.common.*;

import java.util.*;

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
        Motion.rng = new Random(rc.getID() + 2024);
        while (true) {
            turnCount += 1;

            try {
                if (!rc.isSpawned()) {
                    MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                    int index = rng.nextInt(27 * 3);
                    while (true) {
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
                    if (rc.canPickupFlag(rc.getLocation())) {
                        rc.pickupFlag(rc.getLocation());
                        rc.setIndicatorString("Holding a flag!");
                    }
                    if (rc.hasFlag() && rc.getRoundNum() >= GameConstants.SETUP_ROUNDS) {
                        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                        MapLocation firstLoc = spawnLocs[0];
                        Direction dir = rc.getLocation().directionTo(firstLoc);
                        if (rc.canMove(dir)) rc.move(dir);
                    }
                    Direction dir = directions[rng.nextInt(directions.length)];
                    MapLocation nextLoc = rc.getLocation().add(dir);
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                    }
                    else if (rc.canAttack(nextLoc)) {
                        rc.attack(nextLoc);
                        System.out.println("Take that! Damaged an enemy that was in our way!");
                    }

                    // Rarely attempt placing traps behind the robot.
                    MapLocation prevLoc = rc.getLocation().subtract(dir);
                    if (rc.canBuild(TrapType.EXPLOSIVE, prevLoc) && rng.nextInt() % 37 == 1) {
                        rc.build(TrapType.EXPLOSIVE, prevLoc);
                    }
                }

            }
            catch (GameActionException e) {
                System.out.println("GameActionException");
                e.printStackTrace();
            }
            catch (Exception e) {
                System.out.println("Exception");
                e.printStackTrace();
            }
            finally {
                Clock.yield();
            }
        }
    }
}
