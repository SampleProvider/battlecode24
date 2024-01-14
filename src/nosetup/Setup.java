package nosetup;
import battlecode.common.*;

import java.util.Random;

public class Setup {
    protected static RobotController rc;
    protected static StringBuilder indicatorString;

    protected static Random rng;

    protected static int flagIndex = -1;
    protected static MapLocation[] placementLocationsOne = {
        new MapLocation(5, 5),
        new MapLocation(-5, -5),
        new MapLocation(0, 10),
        new MapLocation(0, -10),
    };
    protected static MapLocation[] placementLocationsTwo = {
        new MapLocation(-5, 5),
        new MapLocation(5, -5),
        new MapLocation(10, 0),
        new MapLocation(-10, 0),
    };
    protected static MapLocation flagOffset = new MapLocation(-100, -100);

    protected static final Direction[] DIRECTIONS = {
        Direction.SOUTHWEST,
        Direction.SOUTH,
        Direction.SOUTHEAST,
        Direction.WEST,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.NORTHWEST,
        Direction.NORTH,
    };
    
    protected static void run() throws GameActionException {
        FlagInfo[] flags = rc.senseNearbyFlags(-1, rc.getTeam());
        for (FlagInfo flag : flags) {
            GlobalArray.writeFlag(flag);
        }
        FlagInfo closestFlag = Motion.getClosestFlag(flags, false);
        int flagtarget = rc.readSharedArray(GlobalArray.SETUP_FLAG_TARGET);
        if (closestFlag != null && rc.canPickupFlag(closestFlag.getLocation()) && flagtarget < 0b110000000000000) {
            // rc.pickupFlag(closestFlag.getLocation());
            // rc.writeSharedArray(GlobalArray.SETUP_FLAG_TARGET, flagtarget + 0b10000000000000);
        }
        if (rc.hasFlag()) {
            //ignore this because we aren't moving flags rn
            if (flagIndex == -1) {
                int flagId = rc.senseNearbyFlags(0, rc.getTeam())[0].getID();
                for (int i = 0; i <= 2; i++) {
                    if (rc.readSharedArray(GlobalArray.ALLY_FLAG_ID + i) == 0) {
                        rc.writeSharedArray(i, flagId);
                        flagIndex = i;
                        break;
                    }
                    else if (rc.readSharedArray(GlobalArray.ALLY_FLAG_ID + i) == flagId) {
                        flagIndex = i;
                        break;
                    }
                }
            }
            if (!GlobalArray.hasLocation(rc.readSharedArray(GlobalArray.SETUP_FLAG_TARGET))) {
                //set flag target
                MapLocation[] spawns = rc.getAllySpawnLocations();
                rc.writeSharedArray(GlobalArray.SETUP_FLAG_TARGET, GlobalArray.intifyLocation(Motion.getFarthest(spawns, new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2))));
            }
            MapLocation flagTarget = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.SETUP_FLAG_TARGET));
            MapLocation toPlace = new MapLocation(flagTarget.x+flagOffset.x, flagTarget.y+flagOffset.y);
            if (flagOffset.x == -100) {
                switch (flagIndex) {
                    case 0:
                        flagOffset = new MapLocation(0, 0);
                        break;
                    case 1:
                        for (MapLocation loc : placementLocationsOne) {
                            flagOffset = loc;
                            toPlace = new MapLocation(flagTarget.x+flagOffset.x, flagTarget.y+flagOffset.y);
                            if (toPlace.x>= 0 && toPlace.x <= rc.getMapWidth() && toPlace.y >= 0 && toPlace.y <= rc.getMapHeight()) {
                                break;
                            }
                        }
                        break;
                    case 2:
                        for (MapLocation loc : placementLocationsTwo) {
                            flagOffset = loc;
                            toPlace = new MapLocation(flagTarget.x+flagOffset.x, flagTarget.y+flagOffset.y);
                            if (toPlace.x >= 0 && toPlace.x <= rc.getMapWidth() && toPlace.y >= 0 && toPlace.y <= rc.getMapHeight()) {
                                break;
                            }
                        }
                        break;
                    default:
                }
            }
            Motion.bugnavTowards(toPlace, 500);
            MapLocation me = rc.getLocation();
            if (rc.canSenseLocation(toPlace)) {
                MapInfo tile = rc.senseMapInfo(toPlace);
                if (!tile.isPassable() && !tile.isWater()) {
                    System.out.println(flagIndex+" "+toPlace.x+","+toPlace.y+" "+rc.senseLegalStartingFlagPlacement(toPlace)+" "+tile.isPassable());
                    indicatorString.append("FLAGINVALID;");
                    if (flagOffset.x < 0) {
                        flagOffset = new MapLocation(flagOffset.x - 1, flagOffset.y);
                    } else
                    if (flagOffset.x > 0) {
                        flagOffset = new MapLocation(flagOffset.x + 1, flagOffset.y);
                    } else
                    if (flagOffset.y < 0) {
                        flagOffset = new MapLocation(flagOffset.x, flagOffset.y - 1);
                    } else
                    if (flagOffset.y > 0) {
                        flagOffset = new MapLocation(flagOffset.x, flagOffset.y + 1);
                    }
                    toPlace = new MapLocation(flagTarget.x+flagOffset.x, flagTarget.y+flagOffset.y);
                }
            }
            if (rc.canFill(toPlace)) {
                rc.fill(toPlace);
            }
            if (rc.canDropFlag(toPlace)) {
                rc.dropFlag(toPlace);
                rc.writeSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC + flagIndex, GlobalArray.intifyLocation(toPlace));
                rc.writeSharedArray(GlobalArray.ALLY_FLAG_CUR_LOC + flagIndex, GlobalArray.intifyLocation(toPlace));
                flagIndex = -1;
            }
            else {
                // rc.setIndicatorLine(me, toPlace, 255, 255, 255);
                indicatorString.append("FLAG"+flagIndex+"->("+(flagTarget.x+flagOffset.x)+","+(flagTarget.y+flagOffset.y)+");");
                rc.writeSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC + flagIndex, (1 << 13) | GlobalArray.intifyLocation(rc.getLocation()));
            }
        }
        else {
            //grab any crumb we see
            MapInfo[] info = rc.senseNearbyMapInfos();
            RobotInfo[] bots = rc.senseNearbyRobots();
            MapLocation me = rc.getLocation();
            Boolean action = false;
            for (MapInfo i : info) {
                if (i.getCrumbs() > 0) {
                    Motion.bugnavTowards(i.getMapLocation(), 500);
                    indicatorString.append("CRUMB("+i.getMapLocation().x+","+i.getMapLocation().y+");");
                    action = true;
                    break;
                }
            }
            Boolean nearDam = false;
            for (MapInfo i : info) {
                if (i.isDam()) {
                    nearDam = true;
                }
            }
            int damLoc = rc.readSharedArray(GlobalArray.SETUP_GATHER_LOC);
            if (!GlobalArray.hasLocation(damLoc)) {
                for (MapInfo i : info) {
                    if (i.isDam()) {
                        rc.writeSharedArray(GlobalArray.SETUP_GATHER_LOC, GlobalArray.intifyLocation(i.getMapLocation()) | 1 << 13);
                        break;
                    }
                }
            }
            // System.out.println(GlobalArray.parseLocation(damLoc).x + " " + GlobalArray.parseLocation(damLoc).y + " " + damLoc);
            if (rc.getRoundNum() + rc.getMapHeight() + rc.getMapWidth() > 240) {
                //Almost done with setup rounds, go and line up at the wall
                if (!action) {
                    if (!nearDam) {
                        //if we aren't near the dam, then go to the meeting point
                        if (GlobalArray.hasLocation(damLoc)) {
                            Motion.bugnavTowards(GlobalArray.parseLocation(damLoc), 500);
                            indicatorString.append("MEET("+GlobalArray.parseLocation(damLoc).x+","+GlobalArray.parseLocation(damLoc).y+");");
                            // rc.setIndicatorLine(me, GlobalArray.parseLocation(damLoc), 0, 255, 0);
                            action = true;
                        }
                        if (!action) {
                            Motion.moveRandomly();
                            indicatorString.append("RANDOM;");
                        }
                    } else {
                        //Nearby dam, try to spread out
                        int botWeight = 16;
                        double weights[] = {0, 0, 0, 0, 0, 0, 0, 0, 0}; //one for each direction
                        for (RobotInfo i : bots) {
                            if (i.team == rc.getTeam()) {
                                botWeight -= 1;
                                weights[me.directionTo(i.getLocation()).getDirectionOrderNum()] -= 10/me.distanceSquaredTo(i.getLocation());
                            } else {
                                botWeight += 3;
                                weights[me.directionTo(i.getLocation()).getDirectionOrderNum()] += 60/me.distanceSquaredTo(i.getLocation());
                            }
                        }
                        botWeight /= 4;
                        botWeight = Math.max(botWeight, 0);
                        botWeight = Math.min(botWeight, 7);
                        int storedBotWeight = damLoc >> 13 & 0b111;
                        if (!GlobalArray.hasLocation(damLoc) || botWeight > storedBotWeight) {
                            //Changing meeting point if there are lots of enemy bots/not many friendly bots
                            for (MapInfo i : info) {
                                if (i.isDam()) {
                                    rc.writeSharedArray(GlobalArray.SETUP_GATHER_LOC, GlobalArray.intifyLocation(i.getMapLocation()) | botWeight << 13);
                                    break;
                                }
                            }
                        }
                        if (me.distanceSquaredTo(GlobalArray.parseLocation(damLoc)) < 5) {
                            rc.writeSharedArray(GlobalArray.SETUP_GATHER_LOC, GlobalArray.intifyLocation(GlobalArray.parseLocation(damLoc)) | botWeight << 13);
                        }
                        for (MapInfo i : info) {
                            if (i.isDam()) {
                                weights[me.directionTo(i.getMapLocation()).getDirectionOrderNum()] += 20;
                            }
                        }
                        double dx=0;
                        double dy=0;
                        for (Direction i : Direction.allDirections()) {
                            dx += i.getDeltaX() * weights[i.getDirectionOrderNum()];
                            dy += i.getDeltaY() * weights[i.getDirectionOrderNum()];
                        }
                        Direction dir = me.directionTo(new MapLocation((int)(me.x+dx), (int)(me.y+dy)));
                        if (!rc.canMove(dir)) {
                            dir = dir.rotateRight();
                            if (!rc.canMove(dir)) {
                                dir = dir.rotateLeft().rotateLeft();
                            }
                        }
                        if (rc.canMove(dir) && rc.isMovementReady()) {
                            rc.move(dir);
                        }
                        indicatorString.append("DAMLINE,dx="+(int)dx+",dy="+(int)dy+");");
                        action = true;
                    }
                }
            } else {
                if (!action)
                Motion.spreadRandomly();
            }
        }
    }
    protected static void jailed() throws GameActionException {
    }
}
