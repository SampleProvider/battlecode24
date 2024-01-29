package SPAARK;

import battlecode.common.*;

import java.util.Random;

public class Motion {
    protected static RobotController rc;
    protected static StringBuilder indicatorString;

    protected static Random rng;
    protected static final Direction[] DIRECTIONS = {
        Direction.SOUTHWEST,
        Direction.SOUTH,
        Direction.SOUTHEAST,
        Direction.WEST,
        Direction.EAST,
        Direction.NORTHWEST,
        Direction.NORTH,
        Direction.NORTHEAST,
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
    protected static final String[] DIRABBREV = {
        "C",
        "W",
        "NW",
        "N",
        "NE",
        "E",
        "SE",
        "S",
        "SW",
    };
    protected static final int TOWARDS = 0;
    protected static final int AWAY = 1;
    protected static final int AROUND = 2;
    protected static final int NONE = 0;
    protected static final int CLOCKWISE = 1;
    protected static final int COUNTER_CLOCKWISE = -1;

    protected static final int DEFAULT_RETREAT_HP = 999;

    protected static RobotInfo[] opponentRobots;
    protected static RobotInfo[] friendlyRobots;
    protected static FlagInfo[] flags;

    protected static int symmetry = 0;
    protected static Direction lastDir = Direction.CENTER;
    protected static Direction optimalDir = Direction.CENTER;
    protected static int rotation = NONE;
    protected static int circleDirection = CLOCKWISE;

    protected static Direction lastRandomDir = Direction.CENTER;
    protected static MapLocation lastRandomSpread;

    // common distance stuff
    protected static int getManhattanDistance(MapLocation a, MapLocation b) {
        return Math.abs(a.x-b.x)+Math.abs(a.y-b.y);
    }
    protected static int getChebyshevDistance(MapLocation a, MapLocation b) {
        return Math.max(Math.abs(a.x-b.x), Math.abs(a.y-b.y));
    }

    protected static MapLocation getClosest(MapLocation[] a) throws GameActionException {
        /* Get closest MapLocation to this robot (Euclidean) */
        return getClosest(a, rc.getLocation());
    }
    protected static MapLocation getClosest(MapLocation[] a, MapLocation me) throws GameActionException {
        /* Get closest MapLocation to me (Euclidean) */
        MapLocation closest = a[0];
        int distance = me.distanceSquaredTo(a[0]);
        for (MapLocation loc : a) {
            if (me.distanceSquaredTo(loc) < distance) {
                closest = loc;
                distance = me.distanceSquaredTo(loc);
            }
        }
        return closest;
    }
    protected static MapLocation getClosestPair(MapLocation[] a, MapLocation[] b) throws GameActionException {
        /* Get closest MapLocation to me (Euclidean) */
        MapLocation closest = a[0];
        int distance = b[0].distanceSquaredTo(a[0]);
        for (MapLocation loc : a) {
            for (MapLocation loc2 : b) {
                if (loc2.distanceSquaredTo(loc) < distance) {
                    closest = loc;
                    distance = loc2.distanceSquaredTo(loc);
                }
            }
        }
        return closest;
    }
    protected static FlagInfo getClosestFlag(FlagInfo[] a, boolean pickedUp) throws GameActionException {
        MapLocation me = rc.getLocation();
        FlagInfo closest = null;
        int distance = 0;
        for (FlagInfo loc : a) {
            if (loc.isPickedUp() != pickedUp) {
                continue;
            }
            if (closest == null || me.distanceSquaredTo(loc.getLocation()) < distance) {
                closest = loc;
                distance = me.distanceSquaredTo(loc.getLocation());
            }
        }
        return closest;
    }
    protected static RobotInfo getClosestRobot(RobotInfo[] a) throws GameActionException {
        MapLocation me = rc.getLocation();
        RobotInfo closest = null;
        int distance = 0;
        for (RobotInfo loc : a) {
            if (closest == null || me.distanceSquaredTo(loc.getLocation()) < distance) {
                closest = loc;
                distance = me.distanceSquaredTo(loc.getLocation());
            }
        }
        return closest;
    }
    protected static RobotInfo getClosestRobot(RobotInfo[] a, MapLocation b) throws GameActionException {
        RobotInfo closest = null;
        int distance = 0;
        for (RobotInfo loc : a) {
            if (closest == null || b.distanceSquaredTo(loc.getLocation()) < distance) {
                closest = loc;
                distance = b.distanceSquaredTo(loc.getLocation());
            }
        }
        return closest;
    }

    protected static MapLocation getFarthest(MapLocation[] a) throws GameActionException {
        /* Get farthest MapLocation to this robot (Euclidean) */
        return getFarthest(a, rc.getLocation());
    }
    protected static MapLocation getFarthest(MapLocation[] a, MapLocation me) throws GameActionException {
        /* Get farthest MapLocation to me (Euclidean) */
        MapLocation closest = a[0];
        int distance = me.distanceSquaredTo(a[0]);
        for (MapLocation loc : a) {
            if (me.distanceSquaredTo(loc) > distance) {
                closest = loc;
                distance = me.distanceSquaredTo(loc);
            }
        }
        return closest;
    }

    protected static MapLocation getMapCenter() throws GameActionException {
        return new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
    }

    // basic random movement
    protected static void moveRandomly() throws GameActionException {
        if (rc.isMovementReady()) {
            boolean stuck = true;
            for (Direction d : DIRECTIONS) {
                if (rc.canMove(d)) {
                    stuck = false;
                }
            }
            if (stuck) {
                return;
            }
            // move in a random direction but minimize making useless moves back to where you came from
            Direction direction = DIRECTIONS[rng.nextInt(DIRECTIONS.length)];
            if (direction == lastRandomDir.opposite() && rc.canMove(direction.opposite())) {
                direction = direction.opposite();
            }
            if (rc.canMove(direction)) {
                rc.move(direction);
                lastRandomDir = direction;
                updateInfo();
            }
        }
    }
    protected static void spreadRandomly() throws GameActionException {
        boolean stuck = true;
        for (Direction d : DIRECTIONS) {
            if (rc.canMove(d)) {
                stuck = false;
            }
        }
        if (stuck) {
            return;
        }
        if (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            MapLocation target = me;
            for (RobotInfo r : friendlyRobots) {
                target = target.add(me.directionTo(r.getLocation()).opposite());
            }
            if (target.equals(me)) {
                // just keep moving in the same direction as before if there's no robots nearby
                if (rc.getRoundNum() % 3 == 0 || lastRandomSpread == null) {
                    moveRandomly(); // occasionally move randomly to avoid getting stuck
                } else if (rng.nextInt(20) == 1) {
                    // don't get stuck in corners
                    lastRandomSpread = me.add(DIRECTIONS[rng.nextInt(DIRECTIONS.length)]);
                    moveRandomly();
                } else {
                    Direction direction = bug2Helper(me, lastRandomSpread, TOWARDS, 0, 0);
                    if (rc.canMove(direction)) {
                        rc.move(direction);
                        lastRandomSpread = lastRandomSpread.add(direction);
                        lastRandomDir = direction;
                        updateInfo();
                    } else {
                        moveRandomly();
                    }
                }
            } else {
                Direction direction = bug2Helper(me, target, TOWARDS, 0, 0);
                if (rc.canMove(direction)) {
                    rc.move(direction);
                    lastRandomSpread = target;
                    lastRandomDir = direction;
                    updateInfo();
                } else {
                    moveRandomly();
                }
            }
        }
    }
    
    // bugnav helpers
    protected static StringBuilder visitedList = new StringBuilder();
    protected static int[] simulateMovement(MapLocation me, MapLocation dest) throws GameActionException {
        MapLocation clockwiseLoc = rc.getLocation();
        Direction clockwiseLastDir = lastDir;
        int clockwiseStuck = 0;
        MapLocation counterClockwiseLoc = rc.getLocation();
        Direction counterClockwiseLastDir = lastDir;
        int counterClockwiseStuck = 0;
        search: for (int t = 0; t < 10; t++) {
            if (clockwiseLoc.equals(dest)) {
                break;
            }
            if (counterClockwiseLoc.equals(dest)) {
                break;
            }
            Direction clockwiseDir = clockwiseLoc.directionTo(dest);
            {
                for (int i = 9; --i >= 0;) {
                    MapLocation loc = clockwiseLoc.add(clockwiseDir);
                    if (rc.onTheMap(loc)) {
                        if (!rc.canSenseLocation(loc)) {
                            break search;
                        }
                        if (clockwiseDir != clockwiseLastDir.opposite() && rc.senseMapInfo(loc).isPassable() && rc.senseRobotAtLocation(loc) == null) {
                            clockwiseLastDir = clockwiseDir;
                            break;
                        }
                    }
                    clockwiseDir = clockwiseDir.rotateRight();
                    if (i == 7) {
                        clockwiseStuck = 1;
                        break search;
                    }
                }
            }
            Direction counterClockwiseDir = counterClockwiseLoc.directionTo(dest);
            {
                for (int i = 9; --i >= 0;) {
                    MapLocation loc = counterClockwiseLoc.add(counterClockwiseDir);
                    if (rc.onTheMap(loc)) {
                        if (!rc.canSenseLocation(loc)) {
                            break search;
                        }
                        if (counterClockwiseDir != counterClockwiseLastDir.opposite() && rc.senseMapInfo(loc).isPassable() && rc.senseRobotAtLocation(loc) == null) {
                            counterClockwiseLastDir = counterClockwiseDir;
                            break;
                        }
                    }
                    counterClockwiseDir = counterClockwiseDir.rotateLeft();
                    if (i == 7) {
                        counterClockwiseStuck = 1;
                        break search;
                    }
                }
            }
            clockwiseLoc = clockwiseLoc.add(clockwiseDir);
            counterClockwiseLoc = counterClockwiseLoc.add(counterClockwiseDir);
        }

        int clockwiseDist = clockwiseLoc.distanceSquaredTo(dest);
        int counterClockwiseDist = counterClockwiseLoc.distanceSquaredTo(dest);

        return new int[]{clockwiseDist, clockwiseStuck, counterClockwiseDist, counterClockwiseStuck};
    }
    protected static Direction bug2Helper(MapLocation me, MapLocation dest, int mode, int minRadiusSquared, int maxRadiusSquared) throws GameActionException {
        return bug2Helper(me, dest, mode, minRadiusSquared, maxRadiusSquared, true);
    }
    protected static Direction bug2Helper(MapLocation me, MapLocation dest, int mode, int minRadiusSquared, int maxRadiusSquared, boolean fillWater) throws GameActionException {
        Direction direction = me.directionTo(dest);
        if (me.equals(dest)) {
            if (mode == AROUND) {
                direction = Direction.EAST;
            }
            else {
                return Direction.CENTER;
            }
        }
        if (mode == AWAY) {
            direction = direction.opposite();
        }
        else if (mode == AROUND) {
            if (me.distanceSquaredTo(dest) < minRadiusSquared) {
                direction = direction.opposite();
            }
            else if (me.distanceSquaredTo(dest) <= maxRadiusSquared) {
                direction = direction.rotateLeft().rotateLeft();
                if (circleDirection == COUNTER_CLOCKWISE) {
                    direction = direction.opposite();
                }
            }
            lastDir = Direction.CENTER;
        }

        boolean stuck = true;
        for (int i = 4; --i >= 0;) {
            if (!visitedList.toString().contains(me + " " + i + " ")) {
                visitedList.append(me + " " + i + " ");
                stuck = false;
                break;
            }
        }
        if (stuck) {
            moveRandomly();
            visitedList = new StringBuilder();
            return Direction.CENTER;
        }

        indicatorString.append("DIR=" + direction + " ");
        if (optimalDir != Direction.CENTER && mode != AROUND) {
            if (rc.canMove(optimalDir) && lastDir != optimalDir.opposite()) {
                optimalDir = Direction.CENTER;
                rotation = NONE;
                visitedList = new StringBuilder();
            }
            else {
                direction = optimalDir;
            }
        }
        indicatorString.append("OPTIMAL=" + optimalDir + " ");

        // indicatorString.append("CIRCLE: " + circleDirection);
        // indicatorString.append("DIR: " + direction);
        // indicatorString.append("OFF: " + rc.onTheMap(me.add(direction)));
        
        if (lastDir != direction.opposite()) {
            if (rc.canMove(direction)) {
                // if (!lastBlocked) {
                //     rotation = NONE;
                // }
                // lastBlocked = false;
                // boolean touchingTheWallBefore = false;
                // for (Direction d : DIRECTIONS) {
                //     MapLocation translatedMapLocation = me.add(d);
                //     if (rc.onTheMap(translatedMapLocation)) {
                //         if (!rc.senseMapInfo(translatedMapLocation).isPassable()) {
                //             touchingTheWallBefore = true;
                //             break;
                //         }
                //     }
                // }
                // if (touchingTheWallBefore) {
                //     rotation = NONE;
                // }
                return direction;
            }
            else if (rc.canFill(me.add(direction)) && ((me.add(direction).x + me.add(direction).y) % 2 == 1 || fillWater)) {
                rc.fill(me.add(direction));
                return Direction.CENTER;
            }
        }
        else if (rc.canMove(direction)) {
            Direction dir;
            if (rotation == CLOCKWISE) {
                dir = direction.rotateRight();
            }
            else {
                dir = direction.rotateLeft();
            }
            if (!rc.onTheMap(me.add(dir))) {
                // boolean touchingTheWallBefore = false;
                // for (Direction d : DIRECTIONS) {
                //     MapLocation translatedMapLocation = me.add(d);
                //     if (rc.onTheMap(translatedMapLocation)) {
                //         if (!rc.senseMapInfo(translatedMapLocation).isPassable()) {
                //             touchingTheWallBefore = true;
                //             break;
                //         }
                //     }
                // }
                // if (touchingTheWallBefore) {
                //     rotation = NONE;
                // }
                rotation *= -1;
                return direction;
            }
        }
        if (!rc.onTheMap(me.add(direction))) {
            if (mode == AROUND) {
                circleDirection *= -1;
                direction = direction.opposite();
                indicatorString.append("FLIPPED");
            }
            else {
                direction = me.directionTo(dest);
            }
            if (rc.canMove(direction)) {
                return direction;
            }
            // else if (rc.canFill(me.add(direction))) {
            //     rc.fill(me.add(direction));
            //     return Direction.CENTER;
            // }
        }

        if (optimalDir == Direction.CENTER) {
            optimalDir = direction;
        }
        
        indicatorString.append("ROTATION=" + rotation + " ");
        if (rotation == NONE) {
            int[] simulated = simulateMovement(me, dest);
    
            int clockwiseDist = simulated[0];
            int counterClockwiseDist = simulated[2];
            boolean clockwiseStuck = simulated[1] == 1;
            boolean counterClockwiseStuck = simulated[3] == 1;
            
            indicatorString.append("DIST=" + clockwiseDist + " " + counterClockwiseDist);
            int tempMode = mode;
            if (mode == AROUND) {
                if (clockwiseDist < minRadiusSquared) {
                    if (counterClockwiseDist < minRadiusSquared) {
                        tempMode = AWAY;
                    }
                    else {
                        tempMode = AWAY;
                    }
                }
                else {
                    if (counterClockwiseDist < minRadiusSquared) {
                        tempMode = AWAY;
                    }
                    else {
                        tempMode = TOWARDS;
                    }
                }
            }
            if (clockwiseStuck) {
                rotation = COUNTER_CLOCKWISE;
            }
            else if (counterClockwiseStuck) {
                rotation = CLOCKWISE;
            }
            else if (tempMode == TOWARDS) {
                if (clockwiseDist < counterClockwiseDist) {
                    rotation = CLOCKWISE;
                }
                else {
                    rotation = COUNTER_CLOCKWISE;
                }
            }
            else if (tempMode == AWAY) {
                if (clockwiseDist < counterClockwiseDist) {
                    rotation = COUNTER_CLOCKWISE;
                }
                else {
                    rotation = CLOCKWISE;
                }
            }
        }

        for (int i = 8; --i >= 0;) {
            if (rotation == CLOCKWISE) {
                direction = direction.rotateRight();
            }
            else {
                direction = direction.rotateLeft();
            }
            // if (rc.onTheMap(me.add(direction)) && rc.senseMapInfo(me.add(direction)).isPassable() && lastDir != direction.opposite()) {
            //     if (rc.canMove(direction)) {
            //         return direction;
            //     }
            //     return Direction.CENTER;
            // }
            if (rc.canMove(direction) && lastDir != direction.opposite()) {
                if (rc.canMove(direction)) {
                    return direction;
                }
                return Direction.CENTER;
            }
            else if (rc.canFill(me.add(direction)) && ((me.add(direction).x + me.add(direction).y) % 2 == 1 || fillWater)) {
                int water = 0;
                for (Direction d : DIRECTIONS) {
                    MapLocation translatedMapLocation = me.add(d);
                    if (rc.onTheMap(translatedMapLocation)) {
                        if (!rc.senseMapInfo(translatedMapLocation).isPassable()) {
                            water += 1;
                        }
                    }
                }
                if (water >= 4) {
                    rc.fill(me.add(direction));
                    return Direction.CENTER;
                }
            }
        }
        if (rc.canMove(lastDir.opposite())) {
            return lastDir.opposite();
        }
        return Direction.CENTER;
    }
    
    // bugnav
    protected static void bugnavTowards(MapLocation dest) throws GameActionException {
        bugnavTowards(dest, true);
    }
    protected static void bugnavTowards(MapLocation dest, boolean fillWater) throws GameActionException {
        if (rc.isMovementReady()) {
            Direction d = bug2Helper(rc.getLocation(), dest, TOWARDS, 0, 0, fillWater);
            if (d == Direction.CENTER) {
                d = rc.getLocation().directionTo(dest);
            }
            micro(d, dest);
        }
    }
    protected static void bugnavAway(MapLocation dest) throws GameActionException {
        bugnavTowards(dest, true);
    }
    protected static void bugnavAway(MapLocation dest, boolean fillWater) throws GameActionException {
        if (rc.isMovementReady()) {
            Direction d = bug2Helper(rc.getLocation(), dest, AWAY, 0, 0, fillWater);
            if (d == Direction.CENTER) {
                d = rc.getLocation().directionTo(dest);
            }
            micro(d, dest);
        }
    }
    protected static void bugnavAround(MapLocation dest, int minRadiusSquared, int maxRadiusSquared) throws GameActionException {
        bugnavAround(dest, minRadiusSquared, maxRadiusSquared, true);
    }
    protected static void bugnavAround(MapLocation dest, int minRadiusSquared, int maxRadiusSquared, boolean fillWater) throws GameActionException {
        if (rc.isMovementReady()) {
            Direction d = bug2Helper(rc.getLocation(), dest, AROUND, minRadiusSquared, maxRadiusSquared, fillWater);
            if (d == Direction.CENTER) {
                d = rc.getLocation().directionTo(dest);
            }
            micro(d, dest);
        }
    }

    // micro strat used by bugnav
    protected static void micro(Direction optimalDir, MapLocation dest) throws GameActionException {
        MapLocation me = rc.getLocation();
        Direction bestDir = null;
        double bestWeight = 0;
        Direction bestFillDir = null;
        double bestFillWeight = 0;
        for (Direction d : ALL_DIRECTIONS) {
            if (!rc.canMove(d) && !rc.canFill(me.add(d))) {
                continue;
            }
            // incentivize moving towards target
            double weight = 0;
            if (rc.getHealth() > 500) { //tested: adv * 40
                if (d.equals(optimalDir)) {
                    weight += 1.6;
                }
                if (d.equals(optimalDir.rotateLeft()) || d.equals(optimalDir.rotateRight())) {
                    weight += 1.5;
                }
                if (rc.hasFlag() && d.equals(optimalDir.opposite()) || d.equals(optimalDir.opposite().rotateLeft()) || d.equals(optimalDir.opposite().rotateRight())) {
                    weight -= 2; //tested: 1.5, 2.5
                }
                //tested +0.5 for being closer to center
                //tested +0.5, +1 for being closer to axis of symmetry
            }
            else {
                if (d.equals(optimalDir)) {
                    weight += 0.6;
                }
                if (d.equals(optimalDir.rotateLeft()) || d.equals(optimalDir.rotateRight())) {
                    weight += 0.5;
                }
            }
            // really incentivize moving into spawn area
            if (rc.hasFlag() && rc.getRoundNum() > GameConstants.SETUP_ROUNDS) {
                if (rc.senseMapInfo(me.add(d)).getSpawnZoneTeamObject() == rc.getTeam()) {
                    weight += 100;
                }
            }
            int actions = rc.isActionReady() ? 1 : 0;
            int minHP = 1000;
            int adv = Comms.getFlagAdv();
            for (RobotInfo robot : opponentRobots) {
                MapLocation relativeLoc = robot.getLocation().add(d.opposite());
                rc.setIndicatorLine(rc.getLocation(), robot.getLocation(), 255, 255, 0);
                if (robot.getLocation().distanceSquaredTo(dest) > me.add(d).distanceSquaredTo(dest)) {
                    // weight -= 10 * (Math.sqrt(robot.getLocation().distanceSquaredTo(dest)) - Math.sqrt(me.add(d).distanceSquaredTo(dest)));
                    // weight -= squared * 100;
                }
                if (me.distanceSquaredTo(relativeLoc) <= 4) {
                    // attack micro - retreat when too close and move closer to attack
                    minHP = Math.min(minHP, robot.getHealth());
                    if (actions == 0 || rc.getHealth() < 500 + adv * 40) { //tested: adv * 30, adv * 50
                        weight -= 10; //tested: 8, 9, 11, 12 (med. difference)
                        //tested: +0.1adv, +0.2adv, +0.33adv (small difference)
                        // if (rc.getHealth() > 500 && friendlyRobots.length > 2) {
                        //     weight += 6;
                        // }
                    }
                    else {
                        actions -= 1;
                        weight += 5; //tested: 3, 3.5, 4, 4.5 (large difference)
                        weight -= adv * 0.33; //tested: 0.5
                    }
                    //suicide if you accidentally got heal specialization
                    if (rc.getExperience(SkillType.ATTACK) >= 70 && rc.getExperience(SkillType.ATTACK) < 75 && rc.getExperience(SkillType.HEAL) >= 100 && rc.getExperience(SkillType.HEAL) <= 105) {
                        // weight += 60 / RobotPlayer.mapSizeFactor;
                        weight += 20; //tested: 12, 16, 24, 28 (small difference)
                    }
                    if (rc.hasFlag()) {
                        weight -= 23; //tested: 20, 25, 27, 30 (med. difference)
                        if (opponentRobots.length > friendlyRobots.length) {
                            weight -= 10; //tested: 6, 8, 12, 14 (small difference)
                        }
                        weight -= adv * 12; //tested: 6, 8, 10, 15 (large difference)
                    }
                    else if (robot.hasFlag()) {
                        weight += 10; //tested: 6, 8, 12, 14 (large difference)
                        if (opponentRobots.length + 3 < friendlyRobots.length) { //tested: +adv (med. difference)
                            weight += 30; //tested: 20, 25, 35, 40 (small difference)
                        }
                        weight -= adv * 12; //tested: 6, 8, 10, 15 (large difference)
                    }
                    // stop moving into robots when you have the flag buh
                }
                else if (me.distanceSquaredTo(relativeLoc) <= 10) {
                    if (rc.getHealth() < 500 + adv * 40) { //tested: adv * 30, adv * 50
                        // weight -= 3;
                        weight -= 8; //tested: 7, 9 (small difference)
                    }
                }
                if (me.distanceSquaredTo(relativeLoc) <= 10) {
                    if (rc.hasFlag()) {
                        weight -= 20; //tested: 15, 25 (med. difference)
                        weight -= adv * 3; //tested: 2, 5 (small difference)
                    }
                    else if (robot.hasFlag()) {
                        weight += 20; //tested: 10, 15 (large difference)
                        if (opponentRobots.length + 3 < friendlyRobots.length) {
                            weight += 10;
                        }
                    }
                }
                // REALLY DONT BE THAT CLOSE
                if (me.distanceSquaredTo(relativeLoc) <= 2) {
                    // weight -= 16;
                    weight -= adv * 2; //tested: 3, 4 (med. difference)
                    if (robot.hasFlag()) {
                        weight += 20; //tested: 15, 25 (small difference)
                    }
                }
            }
            if (rc.getHealth() > minHP) {
                // weight += 20;
                weight += 3; //tested: 2
            }
            // maybe be closer to friendly robots
            if (opponentRobots.length > 0) {
                double friendlyWeight = 0;
                for (RobotInfo robot : friendlyRobots) {
                    MapLocation relativeLoc = robot.getLocation().add(d.opposite());
                    if (rc.canSenseLocation(relativeLoc)) {
                        friendlyWeight += 1.5; //tested: 0.5, 1 (small difference)
                    }
                    if (me.distanceSquaredTo(relativeLoc) <= 10) { //tested: 8, 9 (med. difference)
                        friendlyWeight += 1; //tested: 0.5, 1.5 (med. difference)
                    }
                    if (me.distanceSquaredTo(relativeLoc) <= 5) {
                        friendlyWeight += 1;
                    }
                    if (me.distanceSquaredTo(relativeLoc) < me.distanceSquaredTo(robot.getLocation())) {
                        friendlyWeight += 1; //tested: 0.5, 1.5 (small difference)
                    }
                    if (me.distanceSquaredTo(relativeLoc) <= 1) { //tested: 2 (large difference)
                        //prevent clogging
                        friendlyWeight -= 1; //tested: 0.7, 1.5, 2
                        // if (robot.hasFlag()) {
                        //     friendlyWeight -= 1;
                        // }
                    }
                }
                weight += Math.min(friendlyWeight, 4); //tested: 3, 6 (small difference)
            }
            // weight += friendlyWeight;
            // prefer not filling?

            if (rc.canFill(me.add(d))) {
                if (opponentRobots.length > 0) {
                    if (bestFillDir == null) {
                        bestFillDir = d;
                        bestFillWeight = weight;
                    }
                    else if (bestFillWeight < weight) {
                        bestFillDir = d;
                        bestFillWeight = weight;
                    }
                }
                else {
                    weight -= 0.1;
                    if (bestDir == null) {
                        bestDir = d;
                        bestWeight = weight;
                    }
                    else if (bestWeight < weight) {
                        bestDir = d;
                        bestWeight = weight;
                    }
                }
            }
            else {
                if (bestDir == null) {
                    bestDir = d;
                    bestWeight = weight;
                }
                else if (bestWeight < weight) {
                    bestDir = d;
                    bestWeight = weight;
                }
            }
        }
        // trap micro
        if (bestDir != null) {
            if (rc.senseNearbyRobots(10, rc.getTeam().opponent()).length >= 3 && friendlyRobots.length >= 5) {
                MapLocation buildLoc = rc.getLocation().add(bestDir);
                build: if (rc.canBuild(TrapType.STUN, buildLoc)) {
                    MapInfo[] mapInfo = rc.senseNearbyMapInfos(buildLoc, 2);
                    for (MapInfo m : mapInfo) {
                        if (m.getTrapType() != TrapType.NONE) {
                            break build;
                        }
                    }
                    // if ((rc.senseMapInfo(buildLoc).getTeamTerritory() != rc.getTeam() && rc.getCrumbs() >= 500) || rc.getCrumbs() >= 1000) {
                        rc.build(TrapType.STUN, buildLoc);
                    // }
                }
            }
            if (rc.canMove(bestDir)) {
                moveWithAction(bestDir);
            }
            else if (rc.canFill(me.add(bestDir))) {
                rc.fill(me.add(bestDir));
            }
            // if (bestDir != optimalDir) {
            //     lastDir = oldLastDir;
            // }
        }
        else if (bestFillDir != null) {
            rc.fill(me.add(bestFillDir));
        }
    }
    protected static void moveWithAction(Direction dir) throws GameActionException {
        if (rc.isActionReady()) {
            MapLocation me = rc.getLocation();
            MapLocation newMe = rc.getLocation().add(dir);
            
            FlagInfo[] opponentFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
            FlagInfo flag = null;

            for (FlagInfo f : opponentFlags) {
                if (me.distanceSquaredTo(f.getLocation()) > 2 && newMe.distanceSquaredTo(f.getLocation()) > 2) {
                    continue;
                }
                flag = f;
                break;
            }
            
            RobotInfo robot = null;
            if (flag == null) {
                for (RobotInfo r : opponentRobots) {
                    if (me.distanceSquaredTo(r.getLocation()) > 4 && newMe.distanceSquaredTo(r.getLocation()) > 4) {
                        continue;
                    }
                    if (robot == null) {
                        robot = r;
                    }
                    else if (robot.hasFlag()) {
                        if (!r.hasFlag()) {
                            robot = r;
                        }
                        else if (robot.getHealth() > r.getHealth()) {
                            robot = r;
                        }
                        else if (robot.getHealth() == r.getHealth() && robot.getID() > r.getID()) {
                            robot = r;
                        }
                    }
                    else if (robot.getHealth() > r.getHealth()) {
                        robot = r;
                    }
                    else if (robot.getHealth() == r.getHealth() && robot.getID() > r.getID()) {
                        robot = r;
                    }
                }
            }
            if (robot == null && flag == null) {
                for (RobotInfo r : friendlyRobots) {
                    if (me.distanceSquaredTo(r.getLocation()) > 4 && newMe.distanceSquaredTo(r.getLocation()) > 4) {
                        continue;
                    }
                    if (robot == null) {
                        robot = r;
                    }
                    else if (robot.hasFlag()) {
                        if (!r.hasFlag()) {
                            robot = r;
                        }
                        else if (robot.getHealth() > r.getHealth()) {
                            robot = r;
                        }
                        else if (robot.getHealth() == r.getHealth() && robot.getID() > r.getID()) {
                            robot = r;
                        }
                    }
                    else if (robot.getHealth() > r.getHealth()) {
                        robot = r;
                    }
                    else if (robot.getHealth() == r.getHealth() && robot.getID() > r.getID()) {
                        robot = r;
                    }
                }
            }

            if (flag != null) {
                Offense.tryPickupFlag();
            }
            if (robot != null) {
                if (robot.getTeam().equals(rc.getTeam())) {
                    while (rc.canHeal(robot.getLocation())) {
                        rc.heal(robot.getLocation());
                    }
                }
                else  {
                    while (rc.canAttack(robot.getLocation())) {
                        rc.attack(robot.getLocation());
                    }
                }
            }

            move(dir);

            if (flag != null) {
                Offense.tryPickupFlag();
            }
            if (robot != null) {
                if (robot.getTeam().equals(rc.getTeam())) {
                    while (rc.canHeal(robot.getLocation())) {
                        rc.heal(robot.getLocation());
                    }
                }
                else  {
                    while (rc.canAttack(robot.getLocation())) {
                        rc.attack(robot.getLocation());
                    }
                }
            }
        }
        else {
            move(dir);
        }
    }

    protected static MapLocation bfsDest;
    protected static long[] bfsMap;
    protected static long[] bfsDist;
    protected static long[] bfsCurr;
    protected static long bitmask;
    // protected static StringBuilder bfsQueue = new StringBuilder();
    protected static final int MAX_PATH_LENGTH = 100;
    protected static void bfsInit() {
        width = rc.getMapWidth();
        height = rc.getMapHeight();
        bfsMap = new long[height + 2];
        bfsCurr = new long[height + 2];
        bfsDist = new long[(height + 2) * MAX_PATH_LENGTH];
        bitmask = (long1 << width) - 1;
    }
    protected static int step = 1;
    protected static int stepOffset;
    protected static int width;
    protected static int height;
    protected static long long1 = 1;
    protected static int recalculationNeeded = MAX_PATH_LENGTH;
    protected static void updateBfsMap() throws GameActionException {
        MapInfo[] map = rc.senseNearbyMapInfos();
        for (MapInfo m : map) {
            if (m.isWall()) {
                int loc = m.getMapLocation().y + 1;
                int subloc = m.getMapLocation().x;
                if (((bfsMap[loc] >> subloc) & 1) == 0) {
                    bfsMap[loc] |= (long1 << subloc);
                    rc.setIndicatorDot(m.getMapLocation(), 255, 255, 255);
                    for (int i = step - 1; i >= 0; i--) {
                        if (((bfsDist[i * (height + 2) + loc] >> subloc) & 1) != 1) {
                            recalculationNeeded = Math.min(i, recalculationNeeded);
                            break;
                        }
                    }
                }
            }
        }
    }
    protected static void bfs() throws GameActionException {

        if (recalculationNeeded != MAX_PATH_LENGTH && recalculationNeeded < step) { 
            step = recalculationNeeded;
            for (int i = 1; i <= height; i++) {
                // bfsDist[i] = 0;
                // bfsCurr[i] = 0;
                bfsCurr[i] = bfsDist[step * (height + 2) + i];
            }
            step += 1;
            indicatorString.append("RECALCULATING;");
        }
        recalculationNeeded = MAX_PATH_LENGTH;
        
        while (step < MAX_PATH_LENGTH && Clock.getBytecodesLeft() > 5000) {
            stepOffset = step * (height + 2);
            switch (height) {
                case 30:
                MotionCodeGen.bfs30();
                break;
                case 31:
                MotionCodeGen.bfs31();
                break;
                case 32:
                MotionCodeGen.bfs32();
                break;
                case 33:
                MotionCodeGen.bfs33();
                break;
                case 34:
                MotionCodeGen.bfs34();
                break;
                case 35:
                MotionCodeGen.bfs35();
                break;
                case 36:
                MotionCodeGen.bfs36();
                break;
                case 37:
                MotionCodeGen.bfs37();
                break;
                case 38:
                MotionCodeGen.bfs38();
                break;
                case 39:
                MotionCodeGen.bfs39();
                break;
                case 40:
                MotionCodeGen.bfs40();
                break;
                case 41:
                MotionCodeGen.bfs41();
                break;
                case 42:
                MotionCodeGen.bfs42();
                break;
                case 43:
                MotionCodeGen.bfs43();
                break;
                case 44:
                MotionCodeGen.bfs44();
                break;
                case 45:
                MotionCodeGen.bfs45();
                break;
                case 46:
                MotionCodeGen.bfs46();
                break;
                case 47:
                MotionCodeGen.bfs47();
                break;
                case 48:
                MotionCodeGen.bfs48();
                break;
                case 49:
                MotionCodeGen.bfs49();
                break;
                case 50:
                MotionCodeGen.bfs50();
                break;
                case 51:
                MotionCodeGen.bfs51();
                break;
                case 52:
                MotionCodeGen.bfs52();
                break;
                case 53:
                MotionCodeGen.bfs53();
                break;
                case 54:
                MotionCodeGen.bfs54();
                break;
                case 55:
                MotionCodeGen.bfs55();
                break;
                case 56:
                MotionCodeGen.bfs56();
                break;
                case 57:
                MotionCodeGen.bfs57();
                break;
                case 58:
                MotionCodeGen.bfs58();
                break;
                case 59:
                MotionCodeGen.bfs59();
                break;
                case 60:
                MotionCodeGen.bfs60();
                break;
            }
            // var cod = "";
            // for (var i = 30; i <= 60; i++) {
            //     cod += "public static void bfs" + i + "() {\n";
            //     for (var j = 1; j <= i; j++) {
            //         cod += "Motion.bfsCurr[z] = Motion.bfsCurr[z] | (Motion.bfsCurr[z] >> 1) | (Motion.bfsCurr[z] << 1);\n".replaceAll("z", j);
            //     }
            //     for (var j = 1; j <= i; j++) {
            //         cod += "Motion.bfsDist[Motion.stepOffset + z] = (Motion.bfsCurr[z] | Motion.bfsCurr[y] | Motion.bfsCurr[x]) & (Motion.bitmask ^ Motion.bfsMap[z]);\n".replaceAll("z", j).replaceAll("y", j - 1).replaceAll("x", j + 1);
            //     }
            //     for (var j = 1; j <= i; j++) {
            //         //cod += "Motion.bfsDist[Motion.stepOffset + z] &= Motion.bitmask ^ Motion.bfsMap[z];\n".replaceAll("z", j);
            //     }
            //     for (var j = 1; j <= i; j++) {
            //         cod += "Motion.bfsCurr[z] = Motion.bfsDist[Motion.stepOffset + z];\n".replaceAll("z", j);
            //     }
            //     cod += "}\n";
            // }
            // console.log(cod);
            step += 1;
        }

        // int b = rc.getRoundNum() % width;
        // if (rc.getRoundNum() == 201) {
            // for (int i = 0; i < width; i++) {
            //     b = i;
            //     for (int j = 0; j < height; j++) {
            //         // if (((bfsDist[(rc.getRoundNum() % 100) * (height + 2) + j + 1] >> i) & 1) == 0) {
            //         if (((bfsDist[(rc.getRoundNum() % 100) * (height + 2) + j + 1] >> b) & 1) == 0) {
            //             if (((bfsMap[j + 1] >> b) & 1) == 0) {
            //                 rc.setIndicatorDot(new MapLocation(b, j), 255, 0, 0);
            //             }
            //             else {
            //                 rc.setIndicatorDot(new MapLocation(b, j), 0, 0, 0);
            //             }
            //         }
            //         else {
            //             if (((bfsMap[j + 1] >> b) & 1) == 0) {
            //                 rc.setIndicatorDot(new MapLocation(b, j), 255, 255, 255);
            //             }
            //             else {
            //                 rc.setIndicatorDot(new MapLocation(b, j), 0, 255, 0);
            //             }
            //         }
            //     }
            // }
        // }
        indicatorString.append("STEP=" + step);
    }
    protected static Direction getBfsDirection(MapLocation dest, boolean fillWater) throws GameActionException {
        MapLocation me = rc.getLocation();

        boolean[] directions = new boolean[9];
        for (int i = 1; i < step; i++) {
            if (((bfsDist[i * (height + 2) + 1 + me.y] >> me.x) & 1) == 1) {
                if (((bfsDist[(i - 1) * (height + 2) + 1 + me.y - 1] >> me.x) & 1) == 1) {
                    directions[7] = true;
                }
                if (((bfsDist[(i - 1) * (height + 2) + 1 + me.y + 1] >> me.x) & 1) == 1) {
                    directions[3] = true;
                }
                if (me.x > 0) {
                    if (((bfsDist[(i - 1) * (height + 2) + 1 + me.y] >> (me.x - 1)) & 1) == 1) {
                        directions[1] = true;
                    }
                    if (((bfsDist[(i - 1) * (height + 2) + 1 + me.y - 1] >> (me.x - 1)) & 1) == 1) {
                        directions[8] = true;
                    }
                    if (((bfsDist[(i - 1) * (height + 2) + 1 + me.y + 1] >> (me.x - 1)) & 1) == 1) {
                        directions[2] = true;
                    }
                }
                if (me.x < width - 1) {
                    if (((bfsDist[(i - 1) * (height + 2) + 1 + me.y] >> (me.x + 1)) & 1) == 1) {
                        directions[5] = true;
                    }
                    if (((bfsDist[(i - 1) * (height + 2) + 1 + me.y - 1] >> (me.x + 1)) & 1) == 1) {
                        directions[6] = true;
                    }
                    if (((bfsDist[(i - 1) * (height + 2) + 1 + me.y + 1] >> (me.x + 1)) & 1) == 1) {
                        directions[4] = true;
                    }
                }
                break;
            }
        }
        Direction optimalDirection = Direction.CENTER;
        Direction optimalFillDirection = Direction.CENTER;
        int minDist = Integer.MAX_VALUE;
        int minFillDist = Integer.MAX_VALUE;
        for (int i = 9; --i >= 0;) {
            if (directions[i]) {
                Direction dir = Direction.DIRECTION_ORDER[i];
                if (rc.canMove(dir)) {
                    if (me.add(dir).distanceSquaredTo(dest) < minDist) {
                        optimalDirection = dir;
                        minDist = me.add(dir).distanceSquaredTo(dest);
                    }
                }
                else if (rc.canFill(me.add(dir))) {
                    if (me.add(dir).distanceSquaredTo(dest) < minFillDist) {
                        optimalFillDirection = dir;
                        minFillDist = me.add(dir).distanceSquaredTo(dest);
                    }
                }
            }
        }
        if (optimalDirection != Direction.CENTER) {
            return optimalDirection;
        }
        if (optimalDirection == Direction.CENTER && optimalFillDirection == Direction.CENTER) {
            optimalDirection = bug2Helper(me, dest, TOWARDS, 0, 0, fillWater);
            indicatorString.append("BUGNAV");

            if (canMove(optimalDirection)) {
                return optimalDirection;
            }
        }
        if (canMove(optimalFillDirection)) {
            return optimalFillDirection;
        }
        return Direction.CENTER;
    }

    protected static void bfsnav(MapLocation dest) throws GameActionException {
        bfsnav(dest, true);
    }
    protected static void bfsnav(MapLocation dest, boolean fillWater) throws GameActionException {
        indicatorString.append(Clock.getBytecodesLeft() + " ");

        if (!dest.equals(bfsDest)) {
            bfsDest = dest;
            for (int i = 1; i <= height; i++) {
                bfsDist[i] = 0;
                bfsCurr[i] = 0;
            }
            bfsDist[dest.y + 1] = long1 << (dest.x);
            bfsCurr[dest.y + 1] = long1 << (dest.x);
            step = 1;
        }

        if (!rc.getLocation().equals(dest) && rc.isMovementReady()) {
            Direction d = getBfsDirection(dest, fillWater);
            if (d == Direction.CENTER) {
                d = rc.getLocation().directionTo(dest);
            }
            micro(d, dest);
        }
        else {
            Atk.attack();
            Atk.heal();
        }
        bfs();
        indicatorString.append(Clock.getBytecodesLeft() + " ");
    }

    protected static void move(Direction dir) throws GameActionException {
        if (rc.canMove(dir)) {
            rc.move(dir);
            lastDir = dir;
            updateInfo();
        }
        else if (rc.canFill(rc.adjacentLocation(dir))) {
            rc.fill(rc.adjacentLocation(dir));
        }
    }
    protected static boolean canMove(Direction dir) throws GameActionException {
        return rc.canMove(dir) || rc.canFill(rc.adjacentLocation(dir));
    }
    protected static void updateInfo() throws GameActionException {
        opponentRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        friendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
        flags = rc.senseNearbyFlags(-1);
    }
}
