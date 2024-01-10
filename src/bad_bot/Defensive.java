package bad_bot;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Defensive {
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
    
    public static void run() throws GameActionException {
        int bestFlag = 0;
        int bestDist = rc.getLocation().distanceSquaredTo(getFlag(0));

        for (int i = 1; i < 3; i++) {
            int currDist = rc.getLocation().distanceSquaredTo(getFlag(i));
            if (currDist < bestDist) {
                bestDist = currDist;
                bestFlag = i;
            }
        }
        

        MapLocation flagLoc = getFlag(bestFlag);
        travelTo(flagLoc);
        
    }

    protected static void travelTo(MapLocation dest) throws GameActionException {
        int symmetry = 0;
        Direction lastDirection = Direction.CENTER;
        boolean clockwiseRotation = true;

        while (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            if (me.equals(dest)) {
                return;
            }
            Direction direction = me.directionTo(dest);
            boolean moved = false;
            if (lastDirection != direction.opposite()) {
                if (rc.canMove(direction)) {
                    rc.move(direction);
                    boolean touchingTheWallBefore = false;
                    for (Direction d : DIRECTIONS) {
                        MapLocation translatedMapLocation = me.add(d);
                        if (rc.onTheMap(translatedMapLocation)) {
                            if (!rc.canMove(d)) {
                                touchingTheWallBefore = true;
                            }
                        }
                    }
                    lastDirection = direction;
                    if (touchingTheWallBefore) {
                        clockwiseRotation = !clockwiseRotation;
                    }
                    continue;
                }
            }

            int flagDist = rc.getLocation().distanceSquaredTo(dest);
            double placeRNG = Math.random();

            if (placeRNG > flagDist / 400.0) {
                if (rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation())) {
                    rc.build(TrapType.EXPLOSIVE, rc.getLocation());
                }
                if (rc.canBuild(TrapType.WATER, rc.getLocation())) {
                    rc.build(TrapType.WATER, rc.getLocation());
                }
            }
            else if (placeRNG > flagDist / 250.0) {
                if (rc.canDig(rc.getLocation())) {
                    rc.dig(rc.getLocation());
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
                    break;
                }
            } 
        }
    }

    protected static MapLocation getFlag(int index) throws GameActionException {
        int loc = rc.readSharedArray(index);
        return GlobalArray.parseLocation(loc);
    }
}
