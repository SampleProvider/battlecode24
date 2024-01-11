package Defensive_SPAARK;

import battlecode.common.*;

import java.util.Random;

public class Motion {
    public static RobotController rc;
    public static StringBuilder indicatorString;

    public static Random rng;
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

    protected static int symmetry = 0;
    protected static Direction lastDir = Direction.CENTER;
    protected static int rotation = NONE;

    protected static int getManhattanDistance(MapLocation a, MapLocation b) {
        return Math.abs(a.x-b.x)+Math.abs(a.y-b.y);
    }

    protected static int getChebyshevDistance(MapLocation a, MapLocation b) {
        return Math.max(Math.abs(a.x-b.x), Math.abs(a.y-b.y));
    }

    protected static MapLocation getNearest(MapLocation[] a) throws GameActionException {
        /* Get nearest MapLocation to this robot (Euclidean) */
        MapLocation me = rc.getLocation();
        MapLocation nearest = a[0];
        int distance = me.distanceSquaredTo(a[0]);
        for (MapLocation loc : a) {
            if (me.distanceSquaredTo(loc) < distance) {
                nearest = loc;
                distance = me.distanceSquaredTo(loc);
            }
        }
        return nearest;
    }
    protected static FlagInfo getNearestFlag(FlagInfo[] a, boolean pickedUp) throws GameActionException {
        MapLocation me = rc.getLocation();
        FlagInfo nearest = null;
        int distance = 0;
        for (FlagInfo loc : a) {
            if (loc.isPickedUp() != pickedUp) {
                continue;
            }
            if (nearest == null || me.distanceSquaredTo(loc.getLocation()) < distance) {
                nearest = loc;
                distance = me.distanceSquaredTo(loc.getLocation());
            }
        }
        return nearest;
    }

    protected static MapLocation getFarthest(MapLocation[] a) throws GameActionException {
        /* Get farthest MapLocation to this robot (Euclidean) */
        MapLocation me = rc.getLocation();
        MapLocation nearest = a[0];
        int distance = me.distanceSquaredTo(a[0]);
        for (MapLocation loc : a) {
            if (me.distanceSquaredTo(loc) > distance) {
                nearest = loc;
                distance = me.distanceSquaredTo(loc);
            }
        }
        return nearest;
    }

    protected static MapLocation getNearest(MapLocation[] a, MapLocation me) throws GameActionException {
        /* Get nearest MapLocation to me (Euclidean) */
        MapLocation nearest = a[0];
        int distance = me.distanceSquaredTo(a[0]);
        for (MapLocation loc : a) {
            if (me.distanceSquaredTo(loc) < distance) {
                nearest = loc;
                distance = me.distanceSquaredTo(loc);
            }
        }
        return nearest;
    }

    protected static MapLocation getFarthest(MapLocation[] a, MapLocation me) throws GameActionException {
        /* Get farthest MapLocation to me (Euclidean) */
        MapLocation nearest = a[0];
        int distance = me.distanceSquaredTo(a[0]);
        for (MapLocation loc : a) {
            if (me.distanceSquaredTo(loc) > distance) {
                nearest = loc;
                distance = me.distanceSquaredTo(loc);
            }
        }
        return nearest;
    }

    protected static void moveRandomly() throws GameActionException {
        while (rc.isMovementReady()) {
            Direction direction = DIRECTIONS[rng.nextInt(DIRECTIONS.length)];
            if (rc.canMove(direction)) {
                rc.move(direction);
            }
            boolean stuck = true;
            for (Direction d : DIRECTIONS) {
                if (rc.canMove(d)) {
                    stuck = false;
                }
            }
            if (stuck) {
                break;
            }
        }
    }
    /*
    protected static void spreadRandomly(RobotController rc, MapLocation me) throws GameActionException {
        RobotInfo[] robotInfo = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam());
        if (robotInfo.length > 0) {
            RobotInfo prioritizedRobotInfo = null;
            for (RobotInfo w : robotInfo) {
                if (w.getType() != rc.getType()) {
                    continue;
                }
                if (me.distanceSquaredTo(w.getLocation()) > 9) {
                    continue;
                }
                if (prioritizedRobotInfo == null) {
                    prioritizedRobotInfo = w;
                    continue;
                }
                if (me.distanceSquaredTo(prioritizedRobotInfo.getLocation()) > me.distanceSquaredTo(w.getLocation())) {
                    prioritizedRobotInfo = w;
                }
            }
            Direction direction = null;
            if (prioritizedRobotInfo != null) {
                direction = me.directionTo(prioritizedRobotInfo.getLocation()).opposite();
                while (rc.isMovementReady()) {
                    if (rc.canMove(direction)) {
                        rc.move(direction);
                        continue;
                    }
                    if (rc.canMove(direction.rotateLeft())) {
                        rc.move(direction.rotateLeft());
                        continue;
                    }
                    if (rc.canMove(direction.rotateLeft().rotateLeft())) {
                        rc.move(direction.rotateLeft().rotateLeft());
                        continue;
                    }
                    if (rc.canMove(direction.rotateRight())) {
                        rc.move(direction.rotateRight());
                        continue;
                    }
                    if (rc.canMove(direction.rotateRight().rotateRight())) {
                        rc.move(direction.rotateRight().rotateRight());
                        continue;
                    }
                    break;
                }
            } else {
                moveRandomly(rc);
            }
        } else {
            moveRandomly(rc);
        }
    }

    protected static void spreadRandomly(RobotController rc, MapLocation me, MapLocation target) throws GameActionException {
        Direction direction = me.directionTo(target).opposite();
        while (rc.isMovementReady()) {
            boolean moved = false;
            int random = rng.nextInt(6);
            if (random == 0) {
                if (rc.canMove(direction.rotateLeft())) {
                    rc.move(direction.rotateLeft());
                    moved = true;
                }
            } else if (random == 1) {
                if (rc.canMove(direction.rotateLeft().rotateLeft())) {
                    rc.move(direction.rotateLeft().rotateLeft());
                    moved = true;
                }
            } else if (random == 2) {
                if (rc.canMove(direction.rotateRight())) {
                    rc.move(direction.rotateRight());
                    moved = true;
                }
            } else if (random == 3) {
                if (rc.canMove(direction.rotateRight().rotateRight())) {
                    rc.move(direction.rotateRight().rotateRight());
                    moved = true;
                }
            } else {
                if (rc.canMove(direction)) {
                    rc.move(direction);
                    moved = true;
                }
            }
            if (moved == false) {
                break;
            }
        }
    }

    protected static void circleAroundTarget(RobotController rc, MapLocation me, MapLocation target) throws GameActionException {
        Direction direction = me.directionTo(target).rotateLeft();
        if (direction.ordinal() % 2 == 1) {
            direction = direction.rotateLeft();
        }
        if (rc.canMove(direction)) {
            rc.move(direction);
        }
    }

    protected static boolean circleAroundTarget(RobotController rc, MapLocation target, int distance, boolean rotation, boolean avoidClouds, boolean avoidWells) throws GameActionException {
        boolean stuck = false;
        while (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            Direction direction = me.directionTo(target);
            robotInfo = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
            if (me.distanceSquaredTo(target) > (int) distance * 1.25) {
                if (rotation) {
                    for (int i = 0; i < 2; i++) {
                        direction = direction.rotateLeft();
                        if (canMove(rc, direction, avoidClouds, avoidWells)) {
                            rc.move(direction);
                            stuck = false;
                            break;
                        }
                    }
                } else {
                    for (int i = 0; i < 2; i++) {
                        direction = direction.rotateRight();
                        if (canMove(rc, direction, avoidClouds, avoidWells)) {
                            rc.move(direction);
                            stuck = false;
                            break;
                        }
                    }
                }
            } else if (me.distanceSquaredTo(target) < (int) distance * 0.75) {
                direction = direction.opposite();
                if (canMove(rc, direction, avoidClouds, avoidWells)) {
                    rc.move(direction);
                    stuck = false;
                    continue;
                }
                direction = direction.rotateLeft();
                if (canMove(rc, direction, avoidClouds, avoidWells)) {
                    rc.move(direction);
                    stuck = false;
                    continue;
                }
                direction = direction.rotateRight().rotateRight();
                if (canMove(rc, direction, avoidClouds, avoidWells)) {
                    rc.move(direction);
                    stuck = false;
                    continue;
                }
            } else {
                if (rotation) {
                    direction = direction.rotateLeft();
                    for (int i = 0; i < 2; i++) {
                        direction = direction.rotateLeft();
                        if (canMove(rc, direction, avoidClouds, avoidWells)) {
                            rc.move(direction);
                            stuck = false;
                            break;
                        }
                    }
                } else {
                    direction = direction.rotateRight();
                    for (int i = 0; i < 2; i++) {
                        direction = direction.rotateRight();
                        if (canMove(rc, direction, avoidClouds, avoidWells)) {
                            rc.move(direction);
                            stuck = false;
                            break;
                        }
                    }
                }
            }
            if (me.equals(rc.getLocation())) {
                direction = me.directionTo(target);
                if (me.distanceSquaredTo(target) > (int) distance * 1.25) {
                    if (rotation) {
                        for (int i = 0; i < 2; i++) {
                            direction = direction.rotateLeft();
                            if (canMove(rc, direction, false, false)) {
                                rc.move(direction);
                                stuck = false;
                                break;
                            }
                        }
                    } else {
                        for (int i = 0; i < 2; i++) {
                            direction = direction.rotateRight();
                            if (canMove(rc, direction, false, false)) {
                                rc.move(direction);
                                stuck = false;
                                break;
                            }
                        }
                    }
                } else if (me.distanceSquaredTo(target) < (int) distance * 0.75) {
                    direction = direction.opposite();
                    if (canMove(rc, direction, false, false)) {
                        rc.move(direction);
                        stuck = false;
                        continue;
                    }
                    direction = direction.rotateLeft();
                    if (canMove(rc, direction, false, false)) {
                        rc.move(direction);
                        stuck = false;
                        continue;
                    }
                    direction = direction.rotateRight().rotateRight();
                    if (canMove(rc, direction, false, false)) {
                        rc.move(direction);
                        stuck = false;
                        continue;
                    }
                } else {
                    if (rotation) {
                        direction = direction.rotateLeft();
                        for (int i = 0; i < 2; i++) {
                            direction = direction.rotateLeft();
                            if (canMove(rc, direction, false, false)) {
                                rc.move(direction);
                                stuck = false;
                                break;
                            }
                        }
                    } else {
                        direction = direction.rotateRight();
                        for (int i = 0; i < 2; i++) {
                            direction = direction.rotateRight();
                            if (canMove(rc, direction, false, false)) {
                                rc.move(direction);
                                stuck = false;
                                break;
                            }
                        }
                    }
                }
                rotation = !rotation;
                if (me.equals(rc.getLocation())) {
                    if (stuck == true) {
                        break;
                    }
                    stuck = true;
                }
            }
        }
        return rotation;
    }
    */
    
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
                        if (clockwiseDir != clockwiseLastDir.opposite() && rc.senseMapInfo(loc).isPassable()) {
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
                        if (counterClockwiseDir != counterClockwiseLastDir.opposite() && rc.senseMapInfo(loc).isPassable()) {
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
        if (me.equals(dest)) {
            return Direction.CENTER;
        }
        Direction direction = me.directionTo(dest);
        if (mode == AWAY) {
            direction = direction.opposite();
        }
        else if (mode == AROUND) {
            if (me.distanceSquaredTo(dest) < minRadiusSquared) {
                direction = direction.opposite();
            }
            else if (me.distanceSquaredTo(dest) <= maxRadiusSquared) {
                direction = direction.rotateLeft().rotateLeft();
            }
        }
        
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
                rc.fill(me.add(direction));
                return Direction.CENTER;
                // int[] simulated = simulateMovement(me, dest);
        
                // int clockwiseDist = simulated[0];
                // int counterClockwiseDist = simulated[2];
                // boolean clockwiseStuck = simulated[1] == 1;
                // boolean counterClockwiseStuck = simulated[3] == 1;

                // if ((clockwiseStuck || clockwiseDist > me.distanceSquaredTo(dest)) && (counterClockwiseStuck || counterClockwiseDist > me.distanceSquaredTo(dest))) {

                //     int water = 0;
                //     for (Direction d : DIRECTIONS) {
                //         MapLocation translatedMapLocation = me.add(d);
                //         if (rc.onTheMap(translatedMapLocation)) {
                //             // if (rc.canFill(translatedMapLocation)) {
                //             if (!rc.senseMapInfo(translatedMapLocation).isPassable()) {
                //                 water += 1;
                //             }
                //         }
                //     }
                //     if (water >= 2) {
                //         rc.fill(me.add(direction));
                //         return Direction.CENTER;
                //     }
                // }
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
            int[] simulated = simulateMovement(me, dest);
    
            int clockwiseDist = simulated[0];
            int counterClockwiseDist = simulated[2];
            boolean clockwiseStuck = simulated[1] == 1;
            boolean counterClockwiseStuck = simulated[3] == 1;
            
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
            rc.setIndicatorString(clockwiseDist + " " + counterClockwiseDist);
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
    
    protected static void bug2towards(MapLocation dest) throws GameActionException {
        while (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            Direction d = bug2Helper(me, dest, TOWARDS, 0, 0);
            if (d == Direction.CENTER) {
                break;
            }
            rc.move(d);
            lastDir = d;
        }
        // indicatorString.append("BUG-LD=" + DIRABBREV[lastDir.getDirectionOrderNum()] + "; BUG-CW=" + rotation + "; ");
    }
    protected static void bug2away(MapLocation dest) throws GameActionException {
        while (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            Direction d = bug2Helper(me, dest, AWAY, 0, 0);
            if (d == Direction.CENTER) {
                break;
            }
            rc.move(d);
            lastDir = d;
        }
        // indicatorString.append("BUG-LD=" + DIRABBREV[lastDir.getDirectionOrderNum()] + "; BUG-CW=" + rotation + "; ");
    }
    protected static void bug2around(MapLocation dest, int minRadiusSquared, int maxRadiusSquared) throws GameActionException {
        while (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            Direction d = bug2Helper(me, dest, AROUND, minRadiusSquared, maxRadiusSquared);
            if (d == Direction.CENTER) {
                break;
            }
            rc.move(d);
            lastDir = d;
        }
        // indicatorString.append("BUG-LD=" + DIRABBREV[lastDir.getDirectionOrderNum()] + "; BUG-CW=" + rotation + "; ");
    }
    protected static void bug2retreat() throws GameActionException {
        while (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            Direction direction = null;
            int bestWeight = 0;
            RobotInfo[] opponentRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            RobotInfo[] friendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            for (Direction d : DIRECTIONS) {
                if (!rc.canMove(d)) {
                    continue;
                }
                int weight = 0;
                for (RobotInfo robot : opponentRobots) {
                    MapLocation relativeLoc = robot.getLocation().add(d.opposite());
                    if (rc.canSenseLocation(relativeLoc)) {
                        weight -= 1;
                    }
                    if (me.distanceSquaredTo(relativeLoc) <= 4) {
                        weight -= 1;
                    }
                }
                for (RobotInfo robot : friendlyRobots) {
                    MapLocation relativeLoc = robot.getLocation().add(d.opposite());
                    if (rc.canSenseLocation(relativeLoc)) {
                        weight += 1;
                    }
                    if (me.distanceSquaredTo(relativeLoc) < me.distanceSquaredTo(robot.getLocation())) {
                        weight += 1;
                    }
                }
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
    protected static void bugnavTowards(MapLocation dest, boolean allowRetreat) throws GameActionException {
        if (rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length != 0 && allowRetreat) {
            bug2retreat();
        }
        else {
            bug2towards(dest);
        }
    }
    protected static void bugnavAway(MapLocation dest, boolean allowRetreat) throws GameActionException {
        if (rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length != 0 && allowRetreat) {
            bug2retreat();
        }
        else {
            bug2away(dest);
        }
    }
    protected static void bugnavAround(MapLocation dest, int minRadiusSquared, int maxRadiusSquared, boolean allowRetreat) throws GameActionException {
        if (rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length != 0 && allowRetreat) {
            bug2retreat();
        }
        else {
            bug2around(dest, minRadiusSquared, maxRadiusSquared);
        }
    }
    protected static void bug2Flag(MapLocation dest) throws GameActionException {
        while (rc.isMovementReady() && rc.isActionReady()) {
            MapLocation me = rc.getLocation();
            Direction d = bug2Helper(me, dest, TOWARDS, 0, 0);
            if (d == Direction.CENTER) {
                if (!rc.hasFlag() && rc.canPickupFlag(me)) {
                    rc.pickupFlag(me);
                }
                break;
            }
            if (rc.hasFlag() && rc.canDropFlag(rc.getLocation().add(d))) {
                rc.dropFlag(rc.getLocation().add(d));
            }
            rc.move(d);
            if (!rc.hasFlag() && rc.canPickupFlag(me)) {
                rc.pickupFlag(me);
            }
            lastDir = d;
        }
    }
}
