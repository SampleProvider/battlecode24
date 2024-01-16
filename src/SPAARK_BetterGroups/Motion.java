package SPAARK_BetterGroups;

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

    protected static MapLocation getSafest(MapLocation[] a) throws GameActionException {
        MapLocation me = rc.getLocation();
        MapLocation safest = a[0];
        int robots = GlobalArray.getNumberOfOpponentRobots(rc.readSharedArray(GlobalArray.locationToSector(a[0])));
        for (MapLocation loc : a) {
            int r = GlobalArray.getNumberOfOpponentRobots(rc.readSharedArray(GlobalArray.locationToSector(loc)));
            if (r < robots && me.distanceSquaredTo(loc) < me.distanceSquaredTo(a[0]) * 2) {
                safest = loc;
                robots = r;
            }
        }
        return safest;
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
                if (d.equals(bugDir)) {
                    weight += 1;
                }
                if (d.equals(bugDir.rotateLeft()) || d.equals(bugDir.rotateRight())) {
                    weight += 1;
                }
                if (rc.hasFlag() && d.equals(bugDir.opposite()) || d.equals(bugDir.opposite().rotateLeft()) || d.equals(bugDir.opposite().rotateRight())) {
                    weight -= 2;
                }
                // really incentivize moving into spawn area
                if (rc.hasFlag()) {
                    if (rc.senseMapInfo(me.add(d)).getSpawnZoneTeam() == 1 && rc.getTeam() == Team.A) {
                        weight += 100;
                    }
                    if (rc.senseMapInfo(me.add(d)).getSpawnZoneTeam() == 2 && rc.getTeam() == Team.B) {
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
                        weight -= 3;
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
                        weight -= 16;
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
            }
            // trap micro
            if (bestDir != null) {
                if (rc.senseNearbyRobots(10, rc.getTeam().opponent()).length >= 3 && friendlyRobots.length >= 5) {
                    MapLocation buildLoc = rc.getLocation().add(bestDir);
                    build: if (rc.canBuild(TrapType.EXPLOSIVE, buildLoc)) {
                        MapInfo[] mapInfo = rc.senseNearbyMapInfos(buildLoc, 2);
                        for (MapInfo m : mapInfo) {
                            if (m.getTrapType() != TrapType.NONE) {
                                break build;
                            }
                        }
                        if ((rc.senseMapInfo(buildLoc).getTeamTerritory() != rc.getTeam() && rc.getCrumbs() >= 500) || rc.getCrumbs() >= 1000) {
                            rc.build(TrapType.EXPLOSIVE, buildLoc);
                        }
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
    protected static boolean[] bfsMap;
    protected static int[] bfsDir;
    protected static StringBuilder bfsQueue = new StringBuilder();
    protected static void bfsInit() {
        // bfsMap = new boolean[rc.getMapWidth() * rc.getMapHeight()];
        // bfsDir = new int[rc.getMapWidth() * rc.getMapHeight()];
    }

    private static MapLocation me;
    private static int width;
    private static int height;
    // private static int loc;
    private static boolean leftEdge;
    private static boolean rightEdge;
    private static boolean topEdge;
    private static boolean bottomEdge;
    private static int usedBytecodes = 0;
    // private static int bfsMod = 0;
    // private static int bfsDiv = 0;
    protected static void bfs(MapLocation dest) throws GameActionException {
        indicatorString.append(Clock.getBytecodesLeft() + " ");

        me = rc.getLocation();
        width = rc.getMapWidth();
        height = rc.getMapHeight();

        boolean newWall = false;

        MapInfo[] map = rc.senseNearbyMapInfos();
        for (MapInfo m : map) {
            if (m.isWall()) {
                int loc = m.getMapLocation().x + m.getMapLocation().y * width;
                if (bfsMap[loc] == false) {
                    bfsMap[loc] = true;
                    newWall = true;
                }
                // bfsDir[loc] = 0;
                // if (bfsQueue.toString().contains(10000 + loc + " ")) {
                //     bfsQueue.delete(bfsQueue.indexOf(10000 + loc + " "), bfsQueue.indexOf(10000 + loc + " ") + 6);
                // }
            }
        }
        if (newWall) {
            bfsDir = new int[rc.getMapWidth() * rc.getMapHeight()];
            bfsQueue = new StringBuilder();
            int loc = 10000 + dest.x + dest.y * width;
            bfsQueue.append(loc + " ");
            indicatorString.append(Clock.getBytecodesLeft() + " ");
            usedBytecodes = 0;
        }

        // if (!dest.equals(bfsDest)) {
        //     bfsDest = dest;
        //     // bfsDistance = new int[rc.getMapWidth() * rc.getMapHeight()];
        //     bfsDir = new int[rc.getMapWidth() * rc.getMapHeight()];
        //     bfsQueue = new StringBuilder();
        //     int loc = 10000 + dest.x + dest.y * width;
        //     bfsQueue.append(loc + " ");
        //     usedBytecodes = 0;
        // }

        int i = rc.getRoundNum() % width;
        for (int j = 0; j < height; j++) {
            // if (bfsMap[i + j * width]) {
            //     rc.setIndicatorDot(new MapLocation(i, j), 0, 0, 0);
            // }
            // else {
            //     rc.setIndicatorDot(new MapLocation(i, j), 255, 255, 255);
            // }
            switch (bfsDir[i + j * width]) {
                case 0:
                    rc.setIndicatorDot(new MapLocation(i, j), 0, 0, 0);
                    break;
                case 1:
                    rc.setIndicatorDot(new MapLocation(i, j), 255, 0, 0);
                    break;
                case 2:
                    rc.setIndicatorDot(new MapLocation(i, j), 255, 125, 0);
                    break;
                case 3:
                    rc.setIndicatorDot(new MapLocation(i, j), 255, 255, 0);
                    break;
                case 4:
                    rc.setIndicatorDot(new MapLocation(i, j), 0, 255, 0);
                    break;
                case 5:
                    rc.setIndicatorDot(new MapLocation(i, j), 0, 255, 255);
                    break;
                case 6:
                    rc.setIndicatorDot(new MapLocation(i, j), 0, 125, 255);
                    break;
                case 7:
                    rc.setIndicatorDot(new MapLocation(i, j), 0, 0, 255);
                    break;
                case 8:
                    rc.setIndicatorDot(new MapLocation(i, j), 255, 0, 255);
                    break;
            }
            // if (bfsMap[i + j * width]) {
            //     rc.setIndicatorDot(new MapLocation(i, j), 0, 0, 0);
            // }
            // else {
            //     rc.setIndicatorDot(new MapLocation(i, j), 255, 255, 255);
            // }
        }

        if (Clock.getBytecodesLeft() < 5000) {
            return;
        }

        while (bfsDir[me.x + me.y * width] == 0 && (Clock.getBytecodesLeft() > 5000) && bfsQueue.length() > 0) {
            int startBytecodes = Clock.getBytecodesLeft();
            int loc = Integer.parseInt(bfsQueue.substring(0, 5)) - 10000; // 7 bytecodes
            bfsQueue.delete(0, 6);

            // 35 bytecodes
            leftEdge = loc % width != 0;
            rightEdge = loc % width != width - 1;
            bottomEdge = loc / width != 0;
            topEdge = loc / width != height - 1;

            if (leftEdge && !bfsMap[loc - 1] && bfsDir[loc - 1] == 0) {
                bfsDir[loc - 1] = 1; // Direction.WEST
                bfsQueue.append(10000 + loc - 1 + " ");
            }
            if (rightEdge && !bfsMap[loc + 1] && bfsDir[loc + 1] == 0) {
                bfsDir[loc + 1] = 5; // Direction.EAST
                bfsQueue.append(10000 + loc + 1 + " ");
            }
            if (bottomEdge && !bfsMap[loc - width] && bfsDir[loc - width] == 0) {
                bfsDir[loc - width] = 7; // Direction.SOUTH
                bfsQueue.append(10000 + loc - width + " ");
            }
            if (topEdge && !bfsMap[loc + width] && bfsDir[loc + width] == 0) {
                bfsDir[loc + width] = 3; // Direction.NORTH
                bfsQueue.append(10000 + loc + width + " ");
            }
            if (topEdge && rightEdge && !bfsMap[loc + width + 1] && bfsDir[loc + width + 1] == 0) {
                bfsDir[loc + width + 1] = 4; // Direction.NORTHEAST
                bfsQueue.append(10000 + loc + width + 1 + " ");
            }
            if (bottomEdge && rightEdge && !bfsMap[loc - width + 1] && bfsDir[loc - width + 1] == 0) {
                bfsDir[loc - width + 1] = 6; // Direction.SOUTHEAST
                bfsQueue.append(10000 + loc - width + 1 + " ");
            }
            if (bottomEdge && leftEdge && !bfsMap[loc - width - 1] && bfsDir[loc - width - 1] == 0) {
                bfsDir[loc - width - 1] = 8; // Direction.SOUTHWEST
                bfsQueue.append(10000 + loc - width - 1 + " ");
            }
            if (topEdge && leftEdge && !bfsMap[loc + width - 1] && bfsDir[loc + width - 1] == 0) {
                bfsDir[loc + width - 1] = 2; // Direction.NORTHWEST
                bfsQueue.append(10000 + loc + width - 1 + " ");
            }
            int endBytecodes = Clock.getBytecodesLeft();
            usedBytecodes = startBytecodes - endBytecodes;
        }
        indicatorString.append("BYTECODES=" + usedBytecodes + " " + bfsDir[me.x + me.y * width] + " " + Direction.DIRECTION_ORDER[bfsDir[me.x + me.y * width]].opposite());

        Direction optimalDirection = Direction.CENTER;
        if (bfsDir[me.x + me.y * width] != 0) {
            optimalDirection = Direction.DIRECTION_ORDER[bfsDir[me.x + me.y * width]].opposite();
            if (!rc.canMove(optimalDirection) && !rc.canFill(me.add(optimalDirection))) {
                optimalDirection = optimalDirection.rotateLeft();
                if (!rc.canMove(optimalDirection) && !rc.canFill(me.add(optimalDirection))) {
                    optimalDirection = optimalDirection.rotateRight().rotateRight();
                    if (!rc.canMove(optimalDirection) && !rc.canFill(me.add(optimalDirection))) {
                        optimalDirection = bug2Helper(me, dest, TOWARDS, 0, 0);
                    }
                }
            }
        }
        else {
            // optimalDirection = bug2Helper(me, dest, TOWARDS, 0, 0);
        }
        if (rc.canMove(optimalDirection)) {
            rc.move(optimalDirection);
            lastDir = optimalDirection;
        }
        else if (rc.canFill(me.add(optimalDirection))) {
            rc.fill(me.add(optimalDirection));
        }
    }
}
