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
                    int index = rc.getID();
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
                    // If we are holding an enemy flag, singularly focus on moving towards
                    // an ally spawn zone to capture it! We use the check roundNum >= SETUP_ROUNDS
                    // to make sure setup phase has ended.
                    if (rc.hasFlag() && rc.getRoundNum() >= GameConstants.SETUP_ROUNDS) {
                        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                        MapLocation firstLoc = spawnLocs[0];
                        Direction dir = rc.getLocation().directionTo(firstLoc);
                        if (rc.canMove(dir)) rc.move(dir);
                    }
                    // Move and attack randomly if no objective.
                    Direction dir = directions[rng.nextInt(directions.length)];
                    MapLocation nextLoc = rc.getLocation().add(dir);
                    if (rc.canMove(dir)){
                        rc.move(dir);
                    }
                    else if (rc.canAttack(nextLoc)){
                        rc.attack(nextLoc);
                        System.out.println("Take that! Damaged an enemy that was in our way!");
                    }

                    // Rarely attempt placing traps behind the robot.
                    MapLocation prevLoc = rc.getLocation().subtract(dir);
                    if (rc.canBuild(TrapType.EXPLOSIVE, prevLoc) && rng.nextInt() % 37 == 1)
                        rc.build(TrapType.EXPLOSIVE, prevLoc);
                    // We can also move our code into different methods or classes to better organize it!
                    updateEnemyRobots(rc);
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
    public static void updateEnemyRobots(RobotController rc) throws GameActionException {
        // Sensing methods can be passed in a radius of -1 to automatically 
        // use the largest possible value.
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (enemyRobots.length != 0){
            rc.setIndicatorString("There are nearby enemy robots! Scary!");
            // Save an array of locations with enemy robots in them for future use.
            MapLocation[] enemyLocations = new MapLocation[enemyRobots.length];
            for (int i = 0; i < enemyRobots.length; i++){
                enemyLocations[i] = enemyRobots[i].getLocation();
            }
            // Let the rest of our team know how many enemy robots we see!
            if (rc.canWriteSharedArray(0, enemyRobots.length)){
                rc.writeSharedArray(0, enemyRobots.length);
                int numEnemies = rc.readSharedArray(0);
            }
        }
    }
}
