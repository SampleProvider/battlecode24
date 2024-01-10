package SPAARK;

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

    protected static int symmetry = 0;
    protected static Direction lastDirection = Direction.CENTER;
    protected static boolean clockwiseRotation;

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

    protected static boolean circleAroundTarget(RobotController rc, MapLocation target, int distance, boolean clockwiseRotation, boolean avoidClouds, boolean avoidWells) throws GameActionException {
        boolean stuck = false;
        while (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            Direction direction = me.directionTo(target);
            robotInfo = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
            if (me.distanceSquaredTo(target) > (int) distance * 1.25) {
                if (clockwiseRotation) {
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
                if (clockwiseRotation) {
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
                    if (clockwiseRotation) {
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
                    if (clockwiseRotation) {
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
                clockwiseRotation = !clockwiseRotation;
                if (me.equals(rc.getLocation())) {
                    if (stuck == true) {
                        break;
                    }
                    stuck = true;
                }
            }
        }
        return clockwiseRotation;
    }
    */
    protected static boolean bugHelper(MapLocation me, Direction direction) throws GameActionException {
        if (lastDirection != direction.opposite()) {
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
                lastDirection = direction;
                if (touchingTheWallBefore) {
                    clockwiseRotation = !clockwiseRotation;
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
            if (clockwiseRotation) {
                direction = direction.rotateLeft();
            }
            else {
                direction = direction.rotateRight();
            }
            if (rc.canMove(direction) && lastDirection != direction.opposite()) {
                rc.move(direction);
                lastDirection = direction;
                // moved = true;
                // if (i >= 4) {
                //     clockwiseRotation = !clockwiseRotation;
                // }
                return true;
            }
        }
        if (rc.canMove(lastDirection.opposite())) {
            rc.move(lastDirection.opposite());
            lastDirection = lastDirection.opposite();
        }
        return false;
    }
    protected static void bug2(MapLocation dest) throws GameActionException {
        while (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            if (me.equals(dest)) {
                return;
            }
            Direction direction = me.directionTo(dest);
            if (!bugHelper(me, direction)) {
                break;
            }
        }
        // indicatorString.append("BUG-LD=" + DIRABBREV[lastDirection.getDirectionOrderNum()] + "; BUG-CW=" + clockwiseRotation + "; ");
    }
    protected static void bug2away(MapLocation dest) throws GameActionException {
        while (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            if (me.equals(dest)) {
                return;
            }
            Direction direction = me.directionTo(dest).opposite();
            if (!bugHelper(me, direction)) {
                break;
            }
        }
        // indicatorString.append("BUG-LD=" + DIRABBREV[lastDirection.getDirectionOrderNum()] + "; BUG-CW=" + clockwiseRotation + "; ");
    }
    protected static void bug2around(MapLocation dest, int radiusSquared) throws GameActionException {
        while (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            if (me.equals(dest)) {
                return;
            }
            Direction direction = me.directionTo(dest);
            if (me.distanceSquaredTo(dest) < radiusSquared) {
                direction = direction.opposite();
            }
            if (!bugHelper(me, direction)) {
                break;
            }
        }
        // indicatorString.append("BUG-LD=" + DIRABBREV[lastDirection.getDirectionOrderNum()] + "; BUG-CW=" + clockwiseRotation + "; ");
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
            if (!bugHelper(me, direction)) {
                break;
            }
        }
        indicatorString.append("BUG-LD=" + DIRABBREV[lastDirection.getDirectionOrderNum()] + "; BUG-CW=" + clockwiseRotation + "; ");
    }
    protected static void bugnav(MapLocation dest, boolean allowRetreat) throws GameActionException {
        if (rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length != 0 && allowRetreat) {
            bug2retreat();
        }
        else {
            bug2(dest);
        }
    }
    protected static void bugnavAround(MapLocation dest, int radiusSquared, boolean allowRetreat) throws GameActionException {
        if (rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length != 0 && allowRetreat) {
            bug2retreat();
        }
        else {
            bug2around(dest, radiusSquared);
        }
    }
    /*
    private static boolean canMove(RobotController rc, Direction direction, boolean avoidClouds, boolean avoidWells) throws GameActionException {
        MapLocation m = rc.getLocation().add(direction);
        if (rc.getType() == RobotType.LAUNCHER) {
            for (RobotInfo r : robotInfo) {
                if (r.getType() == RobotType.HEADQUARTERS) {
                    rc.setIndicatorLine(m, r.getLocation(), 255, 0, 0);
                    if (r.getLocation().distanceSquaredTo(m) <= RobotType.HEADQUARTERS.actionRadiusSquared) {
                        if (r.getLocation().distanceSquaredTo(rc.getLocation()) >= r.getLocation().distanceSquaredTo(m)) {
                            return false;
                        }
                    }
                }
            }
            for (MapLocation h : headquarters) {
                MapLocation rH = h;
                if (symmetry == 0) {
                    rH = new MapLocation(rc.getMapWidth() - 1 - h.x, h.y);
                }
                else if (symmetry == 1) {
                    rH = new MapLocation(h.x, rc.getMapHeight() - 1 - h.y);
                }
                else {
                    rH = new MapLocation(rc.getMapWidth() - 1 - h.x, rc.getMapHeight() - 1 - h.y);
                }
                if (rH.distanceSquaredTo(m) <= RobotType.HEADQUARTERS.actionRadiusSquared) {
                    if (rH.distanceSquaredTo(rc.getLocation()) >= rH.distanceSquaredTo(m)) {
                        return false;
                    }
                }
            }
        }
        if (rc.onTheMap(m)) {
            Direction currentDirection = rc.senseMapInfo(m).getCurrentDirection();
            if (rc.canMove(direction) && currentDirection != direction.opposite() && currentDirection != direction.opposite().rotateLeft() && currentDirection != direction.opposite().rotateRight() && !(rc.senseCloud(m) && avoidClouds)) {
                if (!avoidWells) {
                    return true;
                }
                for (Direction d : DIRECTIONS) {
                    MapLocation translatedMapLocation = m.add(d);
                    if (rc.onTheMap(translatedMapLocation) && rc.canSenseLocation(translatedMapLocation)) {
                        if (rc.senseWell(translatedMapLocation) != null) {
                            return false;
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }
    */
}
