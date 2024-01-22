package SPAARKwater;

import battlecode.common.*;

import java.util.Random;

public class Motion1 {
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

    // basic random movement
    protected static void moveRandomly() throws GameActionException {
        boolean stuck = true;
        for (Direction d : DIRECTIONS) {
            if (rc.canMove(d)) {
                stuck = false;
            }
        }
        if (stuck) {
            return;
        }
        while (rc.isMovementReady()) {
            // move in a random direction but minimize making useless moves back to where you came from
            Direction direction = DIRECTIONS[rng.nextInt(DIRECTIONS.length)];
            if (direction == lastRandomDir.opposite() && rc.canMove(direction.opposite())) {
                direction = direction.opposite();
            }
            if (rc.canMove(direction)) {
                rc.move(direction);
                lastRandomDir = direction;
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
            RobotInfo[] robotInfo = rc.senseNearbyRobots(20, rc.getTeam());
            MapLocation target = me;
            for (RobotInfo r : robotInfo) {
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
                } else {
                    moveRandomly();
                }
            }
        }
    }
    
    // bugnav helpers
    protected static boolean lastBlocked = false;
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
                for (int i = 0; i < 8; i++) {
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
                for (int i = 0; i < 8; i++) {
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
        for (int i = 0; i < 3; i++) {
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
            else if (rc.canFill(me.add(direction))) {
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
            else if (rc.canFill(me.add(direction))) {
                rc.fill(me.add(direction));
                return Direction.CENTER;
            }
        }

        if (optimalDir == Direction.CENTER) {
            optimalDir = direction;
        }
        
        indicatorString.append("ROTATION=" + rotation + " ");
        indicatorString.append("OPTIMAL=" + optimalDir + " ");
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
        lastBlocked = true;

        for (int i = 0; i < 7; i++) {
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
            else if (rc.canFill(me.add(direction))) {
                int water = 0;
                for (Direction d : DIRECTIONS) {
                    MapLocation translatedMapLocation = me.add(d);
                    if (rc.onTheMap(translatedMapLocation)) {
                        if (!rc.senseMapInfo(translatedMapLocation).isPassable()) {
                            water += 1;
                        }
                    }
                }
                if (water >= 3) {
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
    protected static Direction bug2RetreatHelper(MapLocation me, Direction direction) throws GameActionException {
        if (lastDir != direction.opposite()) {
            if (rc.canMove(direction)) {
                boolean touchingTheWallBefore = false;
                for (Direction d : DIRECTIONS) {
                    MapLocation translatedMapLocation = me.add(d);
                    if (rc.onTheMap(translatedMapLocation)) {
                        if (!rc.senseMapInfo(translatedMapLocation).isPassable()) {
                            touchingTheWallBefore = true;
                            break;
                        }
                    }
                }
                if (touchingTheWallBefore) {
                    rotation = NONE;
                }
                return direction;
            }
            else if (rc.canFill(me.add(direction))) {
                int water = 0;
                for (Direction d : DIRECTIONS) {
                    MapLocation translatedMapLocation = me.add(d);
                    if (rc.onTheMap(translatedMapLocation)) {
                        // if (rc.canFill(translatedMapLocation)) {
                        if (!rc.senseMapInfo(translatedMapLocation).isPassable()) {
                            water += 1;
                        }
                    }
                }
                if (water >= 3) {
                    rc.fill(me.add(direction));
                    return Direction.CENTER;
                }
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
                boolean touchingTheWallBefore = false;
                for (Direction d : DIRECTIONS) {
                    MapLocation translatedMapLocation = me.add(d);
                    if (rc.onTheMap(translatedMapLocation)) {
                        if (!rc.senseMapInfo(translatedMapLocation).isPassable()) {
                            touchingTheWallBefore = true;
                            break;
                        }
                    }
                }
                if (touchingTheWallBefore) {
                    rotation = NONE;
                }
                return direction;
            }
        }

        if (rotation == NONE) {
            rotation = CLOCKWISE;
            if (rng.nextBoolean()) {
                rotation *= -1;
            }
        }
        
        for (int i = 0; i < 7; i++) {
            if (rotation == CLOCKWISE) {
                direction = direction.rotateRight();
            }
            else {
                direction = direction.rotateLeft();
            }
            if (rc.canMove(direction) && lastDir != direction.opposite()) {
                return direction;
            }
        }
        if (rc.canMove(lastDir.opposite())) {
            return lastDir.opposite();
        }
        return Direction.CENTER;
    }

    // bugnav
    protected static Direction bug2towards(MapLocation dest) throws GameActionException {
        while (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            Direction d = bug2Helper(me, dest, TOWARDS, 0, 0);
            if (d == Direction.CENTER) {
                break;
            }
            return d;
        }
        return Direction.CENTER;
        // indicatorString.append("BUG-LD=" + DIRABBREV[lastDir.getDirectionOrderNum()] + "; BUG-CW=" + rotation + "; ");
    }
    protected static Direction bug2away(MapLocation dest) throws GameActionException {
        while (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            Direction d = bug2Helper(me, dest, AWAY, 0, 0);
            if (d == Direction.CENTER) {
                break;
            }
            return d;
        }
        return Direction.CENTER;
        // indicatorString.append("BUG-LD=" + DIRABBREV[lastDir.getDirectionOrderNum()] + "; BUG-CW=" + rotation + "; ");
    }
    protected static Direction bug2around(MapLocation dest, int minRadiusSquared, int maxRadiusSquared) throws GameActionException {
        while (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            Direction d = bug2Helper(me, dest, AROUND, minRadiusSquared, maxRadiusSquared);
            if (d == Direction.CENTER) {
                break;
            }
            return d;
        }
        return Direction.CENTER;
        // indicatorString.append("BUG-LD=" + DIRABBREV[lastDir.getDirectionOrderNum()] + "; BUG-CW=" + rotation + "; ");
    }
    protected static void bug2retreat() throws GameActionException {
        while (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            Direction direction = null;
            int bestWeight = 0;
            RobotInfo[] opponentRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            RobotInfo[] friendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
            for (Direction d : ALL_DIRECTIONS) {
                if (!rc.canMove(d)) {
                    continue;
                }
                int weight = 0;
                int friendlyWeight = 0;
                for (RobotInfo robot : opponentRobots) {
                    MapLocation relativeLoc = robot.getLocation().add(d.opposite());
                    // if (rc.canSenseLocation(relativeLoc)) {
                    //     weight -= 4;
                    // }
                    if (me.distanceSquaredTo(relativeLoc) <= 4) {
                        weight -= 4;
                    }
                    if (me.distanceSquaredTo(relativeLoc) <= 2) {
                        weight -= 16;
                    }
                }
                for (RobotInfo robot : friendlyRobots) {
                    MapLocation relativeLoc = robot.getLocation().add(d.opposite());
                    if (rc.canSenseLocation(relativeLoc)) {
                        friendlyWeight += 1;
                    }
                    if (me.distanceSquaredTo(relativeLoc) < me.distanceSquaredTo(robot.getLocation())) {
                        friendlyWeight += 1;
                    }
                }
                weight += Math.min(friendlyWeight, 4);
                if (direction == null) {
                    direction = d;
                    bestWeight = weight;
                }
                else if (bestWeight < weight) {
                    direction = d;
                    bestWeight = weight;
                }
            }
            if (direction == null) {
                break;
            }
            Direction d = bug2RetreatHelper(me, direction);
            if (d == Direction.CENTER) {
                break;
            }
            rc.move(d);
            lastDir = d;
        }
        indicatorString.append("BUG-LD=" + DIRABBREV[lastDir.getDirectionOrderNum()] + "; BUG-CW=" + rotation + "; ");
    }
    
    // actual bugnav
    protected static void bugnavTowards(MapLocation dest) throws GameActionException {
        bugnavTowards(dest, DEFAULT_RETREAT_HP);
    }
    protected static void bugnavTowards(MapLocation dest, int retreatHP) throws GameActionException {
        // RobotInfo[] nearbyRobots = rc.senseNearbyRobots(10, rc.getTeam().opponent());
        // if ((nearbyRobots.length != 0 && rc.getHealth() <= retreatHP) || nearbyRobots.length >= 3 || rc.senseNearbyRobots(4, rc.getTeam().opponent()).length > 0) {
        //     bug2retreat();
        if (rc.hasFlag() && rc.getLocation().distanceSquaredTo(dest) <= 36) {
            retreatHP = 0;
        }
        if (rc.isMovementReady()) {
            Direction d = bug2towards(dest);
            if (d == Direction.CENTER) {
                d = rc.getLocation().directionTo(dest);
            }
            if (rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length != 0 && rc.getHealth() <= retreatHP) {
                micro(d);
            }
            else if (rc.canMove(d)) {
                rc.move(d);
                lastDir = d;
            }
        }
    }
    protected static void bugnavAway(MapLocation dest) throws GameActionException {
        bugnavAway(dest, DEFAULT_RETREAT_HP);
    }
    protected static void bugnavAway(MapLocation dest, int retreatHP) throws GameActionException {
        if (rc.isMovementReady()) {
            Direction d = bug2away(dest);
            if (d == Direction.CENTER) {
                d = rc.getLocation().directionTo(dest);
            }
            if (rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length != 0 && rc.getHealth() <= retreatHP) {
                micro(d);
            }
            else if (rc.canMove(d)) {
                rc.move(d);
                lastDir = d;
            }
        }
    }
    protected static void bugnavAround(MapLocation dest, int minRadiusSquared, int maxRadiusSquared) throws GameActionException {
        bugnavAround(dest, minRadiusSquared, maxRadiusSquared, DEFAULT_RETREAT_HP);
    }
    protected static void bugnavAround(MapLocation dest, int minRadiusSquared, int maxRadiusSquared, int retreatHP) throws GameActionException {
        if (rc.isMovementReady()) {
            Direction d = bug2around(dest, minRadiusSquared, maxRadiusSquared);
            if (d == Direction.CENTER) {
                d = rc.getLocation().directionTo(dest);
            }
            if (rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length != 0 && rc.getHealth() <= retreatHP) {
                micro(d);
            }
            else if (rc.canMove(d)) {
                rc.move(d);
                lastDir = d;
            }
        }
    }

    // micro strat used by bugnav
    protected static void micro(Direction bugDir) throws GameActionException {
        if (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            Direction bestDir = null;
            int bestWeight = 0;
            Direction bestFillDir = null;
            int bestFillWeight = 0;
            RobotInfo[] opponentRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            RobotInfo[] friendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
            for (Direction d : ALL_DIRECTIONS) {
                if (!rc.canMove(d) && !rc.canFill(me.add(d))) {
                    continue;
                }
                // incentivize moving towards target
                int weight = 0;
                if (opponentRobots.length > 0) {
                    if (d.equals(bugDir)) {
                        weight += 1;
                    }
                    if (d.equals(bugDir.rotateLeft()) || d.equals(bugDir.rotateRight())) {
                        weight += 1;
                    }
                    if (rc.hasFlag() && d.equals(bugDir.opposite()) || d.equals(bugDir.opposite().rotateLeft()) || d.equals(bugDir.opposite().rotateRight())) {
                        weight -= 2;
                    }
                }
                // really incentivize moving into spawn area
                if (rc.hasFlag()) {
                    if (rc.senseMapInfo(me.add(d)).getSpawnZoneTeamObject() == rc.getTeam()) {
                        weight += 100;
                    }
                }
                int actions = rc.isActionReady() ? 1 : 0;
                for (RobotInfo robot : opponentRobots) {
                    MapLocation relativeLoc = robot.getLocation().add(d.opposite());
                    if (me.distanceSquaredTo(relativeLoc) <= 4) {
                        // attack micro - retreat when too close and move closer to attack
                        if (actions == 0 || rc.getHealth() < 500) {
                            weight -= 10;
                            // if (rc.getHealth() > 500 && friendlyRobots.length > 2) {
                            //     weight += 6;
                            // }
                        }
                        else {
                            actions -= 1;
                            weight += 4;
                        }
                        if (rc.hasFlag()) {
                            weight -= 30;
                        }
                        else if (robot.hasFlag()) {
                            weight += 10;
                        }
                        // stop moving into robots when you have the flag buh
                    }
                    else if (me.distanceSquaredTo(relativeLoc) <= 10) {
                        // weight -= 3;
                    }
                    if (me.distanceSquaredTo(relativeLoc) <= 10) {
                        if (rc.hasFlag()) {
                            weight -= 20;
                        }
                        else if (robot.hasFlag()) {
                            weight += 20;
                        }
                    }
                    // REALLY DONT BE THAT CLOSE
                    if (me.distanceSquaredTo(relativeLoc) <= 2) {
                        // weight -= 16;
                        if (robot.hasFlag()) {
                            weight += 20;
                        }
                    }
                }
                // maybe be closer to friendly robots
                int friendlyWeight = 0;
                for (RobotInfo robot : friendlyRobots) {
                    MapLocation relativeLoc = robot.getLocation().add(d.opposite());
                    if (rc.canSenseLocation(relativeLoc)) {
                        friendlyWeight += 1;
                    }
                    if (me.distanceSquaredTo(relativeLoc) < me.distanceSquaredTo(robot.getLocation())) {
                        friendlyWeight += 1;
                    }
                }
                weight += Math.min(friendlyWeight, 4);
                // weight += friendlyWeight;
                // prefer not filling?
                if (rc.canFill(me.add(d))) {
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
                    if (bestDir == null) {
                        bestDir = d;
                        bestWeight = weight;
                    }
                    else if (bestWeight < weight) {
                        bestDir = d;
                        bestWeight = weight;
                    }
                }
                if (Clock.getBytecodesLeft() < 5000) {
                    break;
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
                moveWithAction(bestDir);
                lastDir = bestDir;
            }
            else if (bestFillDir != null) {
                rc.fill(me.add(bestFillDir));
            }
        }
    }
    protected static void moveWithAction(Direction dir) throws GameActionException {
        if (rc.isActionReady()) {
            MapLocation me = rc.getLocation();
            MapLocation newMe = rc.getLocation().add(dir);
            
            RobotInfo[] opponentRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            RobotInfo robot = null;
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

            if (robot == null) {
                RobotInfo[] friendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
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

            rc.move(dir);
            lastDir = dir;

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
            rc.move(dir);
            lastDir = dir;
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
                    rc.setIndicatorDot(m.getMapLocation(), 0, 255, 255);
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
    protected static Direction bfs(MapLocation dest) throws GameActionException {
        indicatorString.append(Clock.getBytecodesLeft() + " ");

        MapLocation me = rc.getLocation();

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
        
        while (step < MAX_PATH_LENGTH && Clock.getBytecodesLeft() > 15000) {
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
        //     for (int i = 0; i < width; i++) {
        //         b = i;
        //         for (int j = 0; j < height; j++) {
        //             // if (((bfsDist[(rc.getRoundNum() % 100) * (height + 2) + j + 1] >> i) & 1) == 0) {
        //             if (((bfsDist[(99) * (height + 2) + j + 1] >> b) & 1) == 0) {
        //                 if (((bfsMap[j + 1] >> b) & 1) == 0) {
        //                     rc.setIndicatorDot(new MapLocation(b, j), 255, 0, 0);
        //                 }
        //                 else {
        //                     rc.setIndicatorDot(new MapLocation(b, j), 0, 0, 0);
        //                 }
        //             }
        //             else {
        //                 if (((bfsMap[j + 1] >> b) & 1) == 0) {
        //                     rc.setIndicatorDot(new MapLocation(b, j), 255, 255, 255);
        //                 }
        //                 else {
        //                     rc.setIndicatorDot(new MapLocation(b, j), 0, 255, 0);
        //                 }
        //             }
        //         }
        //     }
        // }
        indicatorString.append("STEP=" + step);

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
        for (int i = 1; i <= 8; i++) {
            if (directions[i]) {
                Direction optimalDirection = Direction.DIRECTION_ORDER[i];
                if (rc.canMove(optimalDirection)) {
                    return optimalDirection;
                }
                else if (rc.canFill(me.add(optimalDirection))) {
                    rc.fill(me.add(optimalDirection));
                    return Direction.CENTER;
                }
            }
        }
        Direction optimalDirection = bug2Helper(me, dest, TOWARDS, 0, 0);
        if (rc.canMove(optimalDirection)) {
            return optimalDirection;
        }
        else if (rc.canFill(me.add(optimalDirection))) {
            rc.fill(me.add(optimalDirection));
            return Direction.CENTER;
        }
        return Direction.CENTER;
    }

    protected static void bfsnav(MapLocation dest) throws GameActionException {
        bfsnav(dest, DEFAULT_RETREAT_HP);
    }
    protected static void bfsnav(MapLocation dest, int retreatHP) throws GameActionException {
        if (rc.isMovementReady()) {
            Direction d = bfs(dest);
            if (d == Direction.CENTER) {
                d = rc.getLocation().directionTo(dest);
            }
            if (rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length != 0 && rc.getHealth() <= retreatHP) {
                micro(d);
            }
            else if (rc.canMove(d)) {
                rc.move(d);
                lastDir = d;
            }
        }
    }
}
