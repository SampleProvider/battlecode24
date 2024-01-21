package SPAARKturtl;
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
                rc.writeSharedArray(GlobalArray.SETUP_FLAG_DISTANCE + flagIndex, 0);
                rc.writeSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC + flagIndex, GlobalArray.intifyLocation(flagInit));
                rc.writeSharedArray(GlobalArray.SETUP_SYM_GUESS + flagIndex, GlobalArray.intifyLocation(new MapLocation(rc.getMapWidth() - flagInit.x - 1, rc.getMapHeight() - flagInit.y - 1))); //rot
                rc.writeSharedArray(GlobalArray.SETUP_SYM_GUESS + flagIndex + 3, GlobalArray.intifyLocation(new MapLocation(rc.getMapWidth() - flagInit.x - 1, flagInit.y))); //vert
                rc.writeSharedArray(GlobalArray.SETUP_SYM_GUESS + flagIndex + 6, GlobalArray.intifyLocation(new MapLocation(flagInit.x, rc.getMapHeight() - flagInit.y - 1))); //horz
            }
            return found;
        }
        return false;
    }

    protected static void moveFlag() throws GameActionException {
        MapLocation flagTarget = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.SETUP_FLAG_TARGET));
        MapLocation toPlace = new MapLocation(flagTarget.x+flagOffset.x, flagTarget.y+flagOffset.y);
        turnsPlacingFlag++;
        // if (turnsPlacingFlag > 90) {
        //     toPlace = flagInit;
        // }
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
        Motion.bugnavTowards(toPlace);
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
            if (Turtle.isBuilder()) {
                if (!getCrumbs(infos)) {
                    Motion.spreadRandomlyWaterWall();
                }
            }
            if (!pickupFlag() && !getCrumbs(infos) && !rc.hasFlag()) { // try to get crumbs/flag
                if (GlobalArray.id >= 3 && GlobalArray.id < 12) {
                    //symmetry detection
                    MapLocation guess = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.SETUP_SYM_GUESS + GlobalArray.id - 3));
                    Motion.bfsnav(guess);
                    if (rc.canSenseLocation(guess)) {
                        MapInfo info = rc.senseMapInfo(guess);
                        if (info.getSpawnZoneTeamObject() != rc.getTeam().opponent()) {
                            //not this symmetry!
                            rc.writeSharedArray(GlobalArray.SYM, rc.readSharedArray(GlobalArray.SYM) | 1 << ((GlobalArray.id - 3) / 3));
                        }
                    }
                } else {
                    Motion.spreadRandomlyWaterWall();
                }
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
                        int damLoc = rc.readSharedArray(GlobalArray.SETUP_DAM_LOC);
                        if (!GlobalArray.hasLocation(damLoc)) {
                            rc.writeSharedArray(GlobalArray.SETUP_DAM_LOC, GlobalArray.intifyLocation(i.getMapLocation()));
                        }
                    }
                }
            }
        } else if (rc.getRoundNum() < 5*Math.max(rc.getMapHeight(), rc.getMapWidth())/3) {
            if (damInit == null) {
                //not running longest path
                MapInfo[] infos = rc.senseNearbyMapInfos();
                if (!getCrumbs(infos) && !rc.hasFlag()) {
                    Motion.spreadRandomlyWaterWall();
                    indicatorString.append("RANDOM;");
                }
            } else {
                //running longest path
                MapInfo[] infos = rc.senseNearbyMapInfos();
                MapLocation me = rc.getLocation();
                MapLocation[] defaultFlags = {
                    GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC)),
                    GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC+1)),
                    GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC+2))
                };
                for (int i = 0; i < defaultFlags.length; i++) {
                    if (rc.canSenseLocation(defaultFlags[i])) {
                        int dist = defaultFlags[i].distanceSquaredTo(damInit);
                        int best = rc.readSharedArray(GlobalArray.SETUP_FLAG_DISTANCE+i);
                        if (dist < best || best == 0) {
                            rc.writeSharedArray(GlobalArray.SETUP_FLAG_DISTANCE+i, dist);
                            damInit = null;
                        }
                    }
                }
                //nav
                int dx=0;
                int dy=0;
                for (Direction i : Direction.allDirections()) {
                    dx += i.getDeltaX() * damSpreadWeights[i.getDirectionOrderNum()];
                    dy += i.getDeltaY() * damSpreadWeights[i.getDirectionOrderNum()];
                }
                Motion.bugnavTowards(new MapLocation(rc.getLocation().x + dx, rc.getLocation().y + dy));
                rc.setIndicatorLine(rc.getLocation(), new MapLocation(rc.getLocation().x + dx, rc.getLocation().y + dy), 255, 255, 0);
                indicatorString.append("LONGPATH->("+(rc.getLocation().x+dx)+","+(rc.getLocation().y+dy)+");");
            }
        } else if (rc.getRoundNum() == 5*Math.max(rc.getMapHeight(), rc.getMapWidth())/3) {
            if (damInit == null) {
                //not running longest path
                MapInfo[] infos = rc.senseNearbyMapInfos();
                if (!getCrumbs(infos) && !rc.hasFlag()) {
                    Motion.moveRandomly();
                    indicatorString.append("RANDOM;");
                }
            } else {
                //longest path
                //get closest spawn zone
                int flagTarget = rc.readSharedArray(GlobalArray.SETUP_FLAG_TARGET);
                if (!GlobalArray.hasLocation(flagTarget)) {
                    int[] dists = {
                        rc.readSharedArray(GlobalArray.SETUP_FLAG_DISTANCE),
                        rc.readSharedArray(GlobalArray.SETUP_FLAG_DISTANCE+1),
                        rc.readSharedArray(GlobalArray.SETUP_FLAG_DISTANCE+2)
                    };
                    int max = Math.max(dists[0], Math.max(dists[1], dists[2]));
                    if (max == dists[0]) {
                        rc.writeSharedArray(GlobalArray.SETUP_FLAG_TARGET,rc.readSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC));
                    } else if (max == dists[1]) {
                        rc.writeSharedArray(GlobalArray.SETUP_FLAG_TARGET,rc.readSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC+1));
                    } else {
                        //max == dists[2]
                        rc.writeSharedArray(GlobalArray.SETUP_FLAG_TARGET,rc.readSharedArray(GlobalArray.ALLY_FLAG_DEF_LOC+2));
                    }
                }
            }
        } else if (rc.getRoundNum() < RobotPlayer.PREPARE_ROUND) {
            //move flag
            if (rc.hasFlag()) {
                moveFlag();
            } else if (Turtle.isBuilder()) {
                //spam water
                MapInfo[] info = rc.senseNearbyMapInfos();

                Boolean nearDam = false;
                for (MapInfo i : info) {
                    if (i.isDam()) {
                        nearDam = true;
                    }
                }
                if (nearDam) {
                    Motion.moveRandomly();
                    for (Direction d : DIRECTIONS) {
                        MapLocation loc = rc.adjacentLocation(d);
                        if (rc.canDig(loc) && (loc.x+loc.y)%2 == 0 && rc.getLevel(SkillType.BUILD) < 6) {
                            rc.dig(loc);
                        }
                    }
                } else {
                    int damLoc = rc.readSharedArray(GlobalArray.SETUP_DAM_LOC);
                    if (GlobalArray.hasLocation(damLoc)) {
                        Motion.bugnavTowards(GlobalArray.parseLocation(damLoc), false);
                    } else {
                        Motion.moveRandomly();
                    }
                    for (Direction d : DIRECTIONS) {
                        MapLocation loc = rc.adjacentLocation(d);
                        if (rc.canDig(loc) && (loc.x+loc.y)%2 == 0 && rc.getLevel(SkillType.BUILD) < 6) {
                            rc.dig(loc);
                        }
                    }
                }
            } else {
                Motion.moveRandomly();
            }
        } else {
            //Preparation
            if (rc.hasFlag()) {
                moveFlag();
            } else {
                // MapLocation[] flags = {
                //     GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_CUR_LOC)),
                //     GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_CUR_LOC+1)),
                //     GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_CUR_LOC+2))
                // };
                // MapLocation avg = new MapLocation((flags[0].x+flags[1].x+flags[2].x)/3, (flags[0].y+flags[1].y+flags[2].y)/3);
                // Motion.bfsnav(avg);
                Motion.bugnavTowards(GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.ALLY_FLAG_CUR_LOC+GlobalArray.flag)), false);
            }
            if (Turtle.isBuilder()) {
                for (Direction d : DIRECTIONS) {
                    MapLocation loc = rc.adjacentLocation(d);
                    if (rc.canDig(loc) && (loc.x+loc.y)%2 == 0 && rc.getLevel(SkillType.BUILD) < 6) {
                        rc.dig(loc);
                    }
                }
            }
        }
    }
    protected static void jailed() throws GameActionException {
        // how are you dying lol
    }
}
