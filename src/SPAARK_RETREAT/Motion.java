package SPAARK_RETREAT;

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

    protected static final int DEFAULT_RETREAT_HP = 1000;

    protected static int symmetry = 0;
    protected static Direction lastDir = Direction.CENTER;
    protected static int rotation = NONE;

    protected static Direction lastRandomDir = Direction.CENTER;
    protected static MapLocation lastRandomSpread;

    protected static int getManhattanDistance(MapLocation a, MapLocation b) {
        return Math.abs(a.x-b.x)+Math.abs(a.y-b.y);
    }

    protected static int getChebyshevDistance(MapLocation a, MapLocation b) {
        return Math.max(Math.abs(a.x-b.x), Math.abs(a.y-b.y));
    }

    protected static MapLocation getClosest(MapLocation[] a) throws GameActionException {
        /* Get closest MapLocation to this robot (Euclidean) */
        MapLocation me = rc.getLocation();
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

    protected static MapLocation getFarthest(MapLocation[] a) throws GameActionException {
        /* Get farthest MapLocation to this robot (Euclidean) */
        MapLocation me = rc.getLocation();
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
                // just keep moving
                if (rc.getRoundNum() % 3 == 0 || lastRandomSpread == null) {
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
    protected static void groupRandomly() throws GameActionException {
        // moves randomly but tries to stick to robots in small groups
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

                // if ((clockwiseStuck || clockwiseDist > me.add(direction).distanceSquaredTo(dest)) && (counterClockwiseStuck || counterClockwiseDist > me.add(direction).distanceSquaredTo(dest))) {

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
                // else {
                //     int tempMode = mode;
                //     if (mode == AROUND) {
                //         if (clockwiseDist < minRadiusSquared) {
                //             if (counterClockwiseDist < minRadiusSquared) {
                //                 tempMode = AWAY;
                //             }
                //             else {
                //                 tempMode = AWAY;
                //             }
                //         }
                //         else {
                //             if (counterClockwiseDist < minRadiusSquared) {
                //                 tempMode = AWAY;
                //             }
                //             else {
                //                 tempMode = TOWARDS;
                //             }
                //         }
                //     }
                //     if (clockwiseStuck) {
                //         rotation = COUNTER_CLOCKWISE;
                //     }
                //     else if (counterClockwiseStuck) {
                //         rotation = CLOCKWISE;
                //     }
                //     else if (tempMode == TOWARDS) {
                //         if (clockwiseDist < counterClockwiseDist) {
                //             rotation = CLOCKWISE;
                //         }
                //         else {
                //             rotation = COUNTER_CLOCKWISE;
                //         }
                //     }
                //     else if (tempMode == AWAY) {
                //         if (clockwiseDist < counterClockwiseDist) {
                //             rotation = COUNTER_CLOCKWISE;
                //         }
                //         else {
                //             rotation = CLOCKWISE;
                //         }
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
    protected static void bugnavTowards(MapLocation dest, int retreatHP) throws GameActionException {
        // RobotInfo[] nearbyRobots = rc.senseNearbyRobots(10, rc.getTeam().opponent());
        // if ((nearbyRobots.length != 0 && rc.getHealth() <= retreatHP) || nearbyRobots.length >= 3 || rc.senseNearbyRobots(4, rc.getTeam().opponent()).length > 0) {
        //     bug2retreat();
        if (rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length != 0 && rc.getHealth() <= retreatHP) {
            // bug2retreat();
            micro(dest);
        }
        else {
            if (rc.isMovementReady()) {
                // MapLocation me = rc.getLocation();
                Direction d = bug2towards(dest);
                if (d == Direction.CENTER) {
                    return;
                }
                // for (RobotInfo robot : nearbyRobots) {
                //     MapLocation relativeLoc = robot.getLocation().add(d.opposite());
                //     // if (rc.canSenseLocation(relativeLoc)) {
                //     //     weight -= 4;
                //     // }
                //     if (me.distanceSquaredTo(relativeLoc) <= 2) {
                //         return;
                //     }
                // }
                // if (friendlyRobots.length < 6 || rc.getHealth() <= retreatHP) {
                // }
                rc.move(d);
                lastDir = d;
            }
        }
    }
    protected static void bugnavAway(MapLocation dest, int retreatHP) throws GameActionException {
        if (rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length != 0 && rc.getHealth() <= retreatHP) {
            // bug2retreat();
            micro(dest);
        }
        else {
            Direction d = bug2away(dest);
            if (d == Direction.CENTER) {
                return;
            }
            rc.move(d);
            lastDir = d;
        }
    }
    protected static void bugnavAround(MapLocation dest, int minRadiusSquared, int maxRadiusSquared, int retreatHP) throws GameActionException {
        if (rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length != 0 && rc.getHealth() <= retreatHP) {
            // bug2retreat();
            micro(dest);
        }
        else {
            Direction d = bug2around(dest, minRadiusSquared, maxRadiusSquared);
            if (d == Direction.CENTER) {
                return;
            }
            rc.move(d);
            lastDir = d;
        }
    }

    protected static void micro(MapLocation dest) throws GameActionException {
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
                int weight = 0;
                if (d.equals(me.directionTo(dest))) {
                    weight += 1;
                }
                int actions = rc.isActionReady() ? 1 : 0;
                for (RobotInfo robot : opponentRobots) {
                    MapLocation relativeLoc = robot.getLocation().add(d.opposite());
                    if (me.distanceSquaredTo(relativeLoc) <= 4) {
                        if (actions == 0 || rc.getHealth() < 500) {
                            weight -= 10;
                        }
                        else {
                            actions -= 1;
                            weight += 4;
                        }
                    }
                    if (me.distanceSquaredTo(relativeLoc) <= 2) {
                        weight -= 16;
                    }
                }
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
            if (bestDir != null) {
                rc.move(bestDir);
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
}
