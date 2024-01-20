package TSPAARKJAN15;
import battlecode.common.*;

import java.util.Random;

public class Setup {
    protected static RobotController rc;
    protected static StringBuilder indicatorString;

    protected static Random rng;

    protected static int flagIndex = -1;
    protected static MapLocation[] placementLocationsOne = {
        new MapLocation(0, 6),
        new MapLocation(0, -6),
        new MapLocation(6, 6),
        new MapLocation(-6, -6),
    };
    protected static MapLocation[] placementLocationsTwo = {
        new MapLocation(6, 0),
        new MapLocation(-6, 0),
        new MapLocation(-6, 6),
        new MapLocation(6, -6),
    };
    protected static MapLocation flagOffset = new MapLocation(-100, -100);
    protected static int turnsPlacingFlag = 0;
    protected static MapLocation flagInit;

    protected static int[] damSpreadWeights = {0, 0, 0, 0, 0, 0, 0, 0, 0};
    protected static MapLocation damInit;

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

    protected static Boolean getCrumbs(MapInfo[] info) throws GameActionException {
        for (MapInfo i : info) {
            if (i.getCrumbs() > 0) {
                Motion.bugnavTowards(i.getMapLocation(), 500);
                indicatorString.append("CRUMB("+i.getMapLocation().x+","+i.getMapLocation().y+");");
                return true;
            }
        }
        return false;
    }

    protected static Boolean pickupFlag() throws GameActionException {
        FlagInfo[] flags = rc.senseNearbyFlags(-1, rc.getTeam());
        for (FlagInfo flag : flags) {
            GlobalArray.writeFlag(flag);
        }
        FlagInfo closestFlag = Motion.getClosestFlag(flags, false);
        int flagtarget = rc.readSharedArray(GlobalArray.SETUP_FLAG_TARGET);
        if (closestFlag != null && rc.canPickupFlag(closestFlag.getLocation())) {
            flagInit = closestFlag.getLocation();
            rc.pickupFlag(closestFlag.getLocation());
            boolean found = false;
            for (int i = 0; i <= 2; i++) {
                if (rc.readSharedArray(GlobalArray.ALLY_FLAG_ID + i) == closestFlag.getID()) {
                    flagIndex = i;
                    found = true;
                }
            }
            if (found) {
                rc.writeSharedArray(GlobalArray.SETUP_FLAG_TARGET, flagtarget);
                rc.writeSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC + flagIndex, GlobalArray.intifyLocation(flagInit));
            }
            return found;
        }
        return false;
    }

    protected static void moveFlag() throws GameActionException {
        MapLocation flagTarget = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.SETUP_FLAG_TARGET));
        MapLocation toPlace = new MapLocation(flagTarget.x+flagOffset.x, flagTarget.y+flagOffset.y);
        turnsPlacingFlag += 1;
        if (turnsPlacingFlag > 90) {
            toPlace = flagInit;
        }
        if (flagOffset.x == -100) {
            switch (flagIndex) {
                case 0:
                    flagOffset = new MapLocation(0, 0);
                    break;
                case 1:
                    for (MapLocation loc : placementLocationsOne) {
                        flagOffset = loc;
                        if (flagTarget.x < rc.getMapWidth() / 2) {
                            flagOffset = new MapLocation(flagOffset.x * -1, flagOffset.y);
                        }
                        if (flagTarget.y < rc.getMapHeight() / 2) {
                            flagOffset = new MapLocation(flagOffset.x, flagOffset.y * -1);
                        }
                        toPlace = new MapLocation(flagTarget.x+flagOffset.x, flagTarget.y+flagOffset.y);
                        if (toPlace.x>= 0 && toPlace.x <= rc.getMapWidth() && toPlace.y >= 0 && toPlace.y <= rc.getMapHeight()) {
                            break;
                        }
                    }
                    break;
                case 2:
                    for (MapLocation loc : placementLocationsTwo) {
                        flagOffset = loc;
                        if (flagTarget.x < rc.getMapWidth() / 2) {
                            flagOffset = new MapLocation(flagOffset.x * -1, flagOffset.y);
                        }
                        if (flagTarget.y < rc.getMapHeight() / 2) {
                            flagOffset = new MapLocation(flagOffset.x, flagOffset.y * -1);
                        }
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
            // testing if flag placement is valid because senseLegalStartingFlagPlacement is broken
            boolean valid = true;
            for (int i = 0; i <= 2; i++) {
                int n = rc.readSharedArray(GlobalArray.ALLY_FLAG_CUR_LOC + i);
                if (GlobalArray.hasLocation(n) && !GlobalArray.isFlagPickedUp(n)) {
                    if (toPlace.distanceSquaredTo(GlobalArray.parseLocation(n)) < 36) {
                        valid = false;
                    }
                }
            }
            if (!tile.isPassable() || !valid) {
                // System.out.println(flagIndex+" "+toPlace.x+","+toPlace.y+" "+rc.senseLegalStartingFlagPlacement(toPlace)+" "+tile.isPassable());
                indicatorString.append(flagIndex+" "+toPlace.x+","+toPlace.y+" "+valid+" "+tile.isPassable());
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
            rc.writeSharedArray(GlobalArray.ALLY_FLAG_CUR_LOC + flagIndex, GlobalArray.intifyLocation(toPlace));
            flagIndex = -1;
        }
        else {
            rc.setIndicatorLine(me, toPlace, 0, 255, 0);
            indicatorString.append("FLAG"+flagIndex+"->("+(flagTarget.x+flagOffset.x)+","+(flagTarget.y+flagOffset.y)+");");
            rc.writeSharedArray(GlobalArray.ALLY_FLAG_CUR_LOC + flagIndex, (7 << 13) | GlobalArray.intifyLocation(rc.getLocation()));
        }
    }
    
    protected static void run() throws GameActionException {
        if (rc.getRoundNum() < Math.max(rc.getMapHeight(), rc.getMapWidth())) {
            //exploration phase, lasts up to turn 60
            MapInfo[] infos = rc.senseNearbyMapInfos();
            if (!pickupFlag() && !getCrumbs(infos) && !rc.hasFlag()) { // try to get crumbs/flag
                Motion.spreadRandomly();
            }
        } else if (rc.getRoundNum() == Math.max(rc.getMapHeight(), rc.getMapWidth())) {
            //longest path
            if (!rc.hasFlag()) {
                MapInfo[] infos = rc.senseNearbyMapInfos();
                MapLocation me = rc.getLocation();
                for (MapInfo i : infos) {
                    if (i.isDam()) {
                        damInit = i.getMapLocation();
                        damSpreadWeights[me.directionTo(i.getMapLocation()).getDirectionOrderNum()] -= 100/me.distanceSquaredTo(i.getMapLocation());

                        int meet = rc.readSharedArray(GlobalArray.SETUP_GATHER_LOC);
                        if (!GlobalArray.hasLocation(meet)) {
                            rc.writeSharedArray(GlobalArray.SETUP_GATHER_LOC, GlobalArray.intifyLocation(damInit));
                        }
                    }
                }
            }
        } else if (rc.getRoundNum() < 2*Math.max(rc.getMapHeight(), rc.getMapWidth()) - Math.max(rc.getMapHeight(), rc.getMapWidth()) / 2) {
            if (damInit == null) {
                //not running longest path
                MapInfo[] infos = rc.senseNearbyMapInfos();
                if (!getCrumbs(infos) && !rc.hasFlag()) {
                    Motion.spreadRandomly();
                }
            } else {
                //running longest path
                //nav
                int dx=0;
                int dy=0;
                for (Direction i : Direction.allDirections()) {
                    dx += i.getDeltaX() * damSpreadWeights[i.getDirectionOrderNum()];
                    dy += i.getDeltaY() * damSpreadWeights[i.getDirectionOrderNum()];
                }
                Motion.bugnavTowards(new MapLocation(rc.getLocation().x + dx, rc.getLocation().y + dy), 500);
                rc.setIndicatorLine(rc.getLocation(), new MapLocation(rc.getLocation().x + dx, rc.getLocation().y + dy), 255, 255, 0);
                indicatorString.append("LONGPATH->("+(rc.getLocation().x+dx)+","+(rc.getLocation().y+dy)+");");
            }
        } else if (rc.getRoundNum() == 2*Math.max(rc.getMapHeight(), rc.getMapWidth()) - Math.max(rc.getMapHeight(), rc.getMapWidth()) / 2) {
            if (damInit == null) {
                //not running longest path
                MapInfo[] infos = rc.senseNearbyMapInfos();
                if (!getCrumbs(infos) && !rc.hasFlag()) {
                    Motion.spreadRandomly();
                }
            } else {
                //longest path
                //get closest spawn zone
                int closestSpawn = Integer.MAX_VALUE;
                MapLocation me = rc.getLocation();
                MapLocation[] spawns = rc.getAllySpawnLocations();
                for (MapLocation i : spawns) {
                    closestSpawn = Math.min(me.distanceSquaredTo(i), closestSpawn);
                }

                int weight = 32768 // 2^15
                + rc.getLocation().distanceSquaredTo(damInit) //distance to dam (farther is better)
                - 6*closestSpawn //distance to nearest spawn (closer is better)
                ;
                weight = Math.max(weight, 0);
                weight = Math.min(weight, 65535);

                //using setup flag target global array index
                int best = rc.readSharedArray(GlobalArray.SETUP_FLAG_WEIGHT);
                if (best < weight) {
                    rc.writeSharedArray(GlobalArray.SETUP_FLAG_WEIGHT, weight);
                    rc.writeSharedArray(GlobalArray.SETUP_FLAG_TARGET, GlobalArray.intifyLocation(rc.getLocation()));
                }
            }
        } else if (rc.getRoundNum() + Math.max(rc.getMapWidth(), rc.getMapHeight()) <= 210) {
            //move flag
            if (rc.hasFlag()) {
                moveFlag();
            } else if (GlobalArray.id < 6) {
                MapLocation flagCarrier = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_CUR_LOC + (GlobalArray.id % 3)));
                Motion.bugnavTowards(flagCarrier);
                rc.setIndicatorLine(rc.getLocation(), flagCarrier, 255, 0, 255);
                indicatorString.append("FOLLOW-FLAG;");
            } else {
                MapInfo[] infos = rc.senseNearbyMapInfos();
                if (!getCrumbs(infos)) {
                    Motion.spreadRandomly();
                }
            }
        } else {
            //line up
            if (rc.hasFlag()) {
                moveFlag();
            } else if (GlobalArray.id < 6) {
                MapLocation flagCarrier = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_CUR_LOC + (GlobalArray.id % 3)));
                Motion.bugnavTowards(flagCarrier);
                rc.setIndicatorLine(rc.getLocation(), flagCarrier, 255, 0, 255);
                indicatorString.append("FOLLOW-FLAG;" + GlobalArray.id);
            } else {
                MapInfo[] info = rc.senseNearbyMapInfos();
                int damLoc = rc.readSharedArray(GlobalArray.SETUP_GATHER_LOC);
                if (!GlobalArray.hasLocation(damLoc)) {
                    for (MapInfo i : info) {
                        if (i.isDam()) {
                            rc.writeSharedArray(GlobalArray.SETUP_GATHER_LOC, GlobalArray.intifyLocation(i.getMapLocation()) | 1 << 13);
                            break;
                        }
                    }
                }

                MapLocation me = rc.getLocation();
                Boolean nearDam = false;
                RobotInfo[] bots = rc.senseNearbyRobots();
                for (MapInfo i : info) {
                    if (i.isDam()) {
                        nearDam = true;
                    }
                }
                if (!nearDam) {
                    //if we aren't near the dam, then go to the meeting point
                    if (GlobalArray.hasLocation(damLoc)) {
                        Motion.bugnavTowards(GlobalArray.parseLocation(damLoc), 500);
                        indicatorString.append("MEET("+GlobalArray.parseLocation(damLoc).x+","+GlobalArray.parseLocation(damLoc).y+");");
                        rc.setIndicatorLine(me, GlobalArray.parseLocation(damLoc), 255, 100, 0);
                    } else {
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
                }
            }
        }
        if (GlobalArray.id < 6) {
            rc.setIndicatorDot(rc.getLocation(), 255, 0, 255);
        }
    }
    protected static void jailed() throws GameActionException {
        // how are you dying lol
    }
}
