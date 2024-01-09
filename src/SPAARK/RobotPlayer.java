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
        Setup.rc = rc;
        Setup.rng = rng;
        Offensive.rc = rc;
        Offensive.rng = rng;
        Defensive.rc = rc;
        Defensive.rng = rng;
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
                    StringBuilder indicatorString = new StringBuilder();
                    Motion.indicatorString = indicatorString;
                    Attack.indicatorString = indicatorString;
                    Setup.indicatorString = indicatorString;
                    Offensive.indicatorString = indicatorString;
                    Defensive.indicatorString = indicatorString;
                    Attack.opponentRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                    Attack.friendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
                    if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS) {
                        Setup.run();
                    }
                    else {
                        Offensive.run();
                    }
                    // Direction dir = directions[rng.nextInt(directions.length)];
                    // MapLocation nextLoc = rc.getLocation().add(dir);
                    // if (rc.canMove(dir)) {
                    //     rc.move(dir);
                    // }
                    // else if (rc.canAttack(nextLoc)) {
                    //     rc.attack(nextLoc);
                    //     System.out.println("Take that! Damaged an enemy that was in our way!");
                    // }

                    // // Rarely attempt placing traps behind the robot.
                    // MapLocation prevLoc = rc.getLocation().subtract(dir);
                    // if (rc.canBuild(TrapType.EXPLOSIVE, prevLoc) && rng.nextInt() % 37 == 1) {
                    //     rc.build(TrapType.EXPLOSIVE, prevLoc);
                    // }
                    rc.setIndicatorString(indicatorString.toString());
                }

            }
            catch (GameActionException e) {
                System.out.println("GameActionException");
                e.printStackTrace();
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
