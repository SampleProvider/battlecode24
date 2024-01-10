package SPAARKJAN9;

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
    protected static int rotation = CLOCKWISE;

    protected static MapLocation getNearest(MapLocation[] a) throws GameActionException {
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
    protected static boolean bug2Helper(MapLocation me, MapLocation dest, int mode, int minRadiusSquared, int maxRadiusSquared) throws GameActionException {
        if (me.equals(dest)) {
            return false;
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
                rc.move(direction);
                return true;
            }
            else if (rc.canFill(me.add(direction))) {
                int water = 0;
                for (Direction d : DIRECTIONS) {
                    MapLocation translatedMapLocation = me.add(d);
                    if (rc.onTheMap(translatedMapLocation)) {
                        if (rc.canFill(translatedMapLocation)) {
                            water += 1;
                        }
                    }
                }
                if (water >= 3) {
                    rc.fill(me.add(direction));
                    return true;
                }
            }
        }
        
        if (rotation == NONE) {
            MapLocation clockwiseLoc = rc.getLocation();
            Direction clockwiseLastDir = lastDir;
            MapLocation counterClockwiseLoc = rc.getLocation();
            Direction counterClockwiseLastDir = lastDir;
            search: for (int t = 0; t < 100; t++) {
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
                            break search;
                        }
                    }
                }
                clockwiseLoc = clockwiseLoc.add(clockwiseDir);
                counterClockwiseLoc = counterClockwiseLoc.add(counterClockwiseDir);
            }
    
            int clockwiseDist = clockwiseLoc.distanceSquaredTo(dest);
            int counterClockwiseDist = counterClockwiseLoc.distanceSquaredTo(dest);
            
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
            if (tempMode == TOWARDS) {
                if (clockwiseDist > counterClockwiseDist) {
                    rotation = CLOCKWISE;
                }
                else {
                    rotation = COUNTER_CLOCKWISE;
                }
            }
            else if (tempMode == AWAY) {
                if (clockwiseDist > counterClockwiseDist) {
                    rotation = COUNTER_CLOCKWISE;
                }
                else {
                    rotation = CLOCKWISE;
                }
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
                rc.move(direction);
                lastDir = direction;
                return true;
            }
        }
        if (rc.canMove(lastDir.opposite())) {
            rc.move(lastDir.opposite());
            lastDir = lastDir.opposite();
        }
        return false;
    }
    protected static boolean bug2RetreatHelper(MapLocation me, Direction direction) throws GameActionException {
        if (lastDir != direction.opposite()) {
            if (rc.canMove(direction)) {
                rc.move(direction);
                boolean touchingTheWallBefore = false;
                for (Direction d : DIRECTIONS) {
                    MapLocation translatedMapLocation = me.add(d);
                    if (rc.onTheMap(translatedMapLocation)) {
                        if (!rc.senseMapInfo(translatedMapLocation).isPassable()) {
                            touchingTheWallBefore = true;
                        }
                    }
                }
                lastDir = direction;
                if (touchingTheWallBefore) {
                    rotation *= -1;
                }
                return true;
            }
            else if (rc.canFill(me.add(direction))) {
                int water = 0;
                for (Direction d : DIRECTIONS) {
                    MapLocation translatedMapLocation = me.add(d);
                    if (rc.onTheMap(translatedMapLocation)) {
                        if (rc.canFill(translatedMapLocation)) {
                            water += 1;
                        }
                    }
                }
                if (water >= 3) {
                    rc.fill(me.add(direction));
                    return true;
                }
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
                rc.move(direction);
                lastDir = direction;
                return true;
            }
        }
        if (rc.canMove(lastDir.opposite())) {
            rc.move(lastDir.opposite());
            lastDir = lastDir.opposite();
        }
        return false;
    }
    
    protected static void bug2towards(MapLocation dest) throws GameActionException {
        while (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            if (!bug2Helper(me, dest, TOWARDS, 0, 0)) {
                break;
            }
        }
        // indicatorString.append("BUG-LD=" + DIRABBREV[lastDir.getDirectionOrderNum()] + "; BUG-CW=" + rotation + "; ");
    }
    protected static void bug2away(MapLocation dest) throws GameActionException {
        while (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            if (!bug2Helper(me, dest, AWAY, 0, 0)) {
                break;
            }
        }
        // indicatorString.append("BUG-LD=" + DIRABBREV[lastDir.getDirectionOrderNum()] + "; BUG-CW=" + rotation + "; ");
    }
    protected static void bug2around(MapLocation dest, int minRadiusSquared, int maxRadiusSquared) throws GameActionException {
        while (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            if (!bug2Helper(me, dest, AROUND, minRadiusSquared, maxRadiusSquared)) {
                break;
            }
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
            if (!bug2RetreatHelper(me, direction)) {
                break;
            }
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
    
}
