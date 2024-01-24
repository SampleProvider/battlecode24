package SPAARK;
import battlecode.common.*;

import java.util.Random;

public class Setup {
    protected static RobotController rc;
    protected static StringBuilder indicatorString;

    protected static Random rng;

    protected static int spawnFlagIndex = -1;

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
            if (i.getCrumbs() > 0 && !i.isWall() && !i.isDam()) {
                Motion.bfsnav(i.getMapLocation());
                indicatorString.append("CRUMB("+i.getMapLocation().x+","+i.getMapLocation().y+");");
                return true;
            }
        }
        return false;
    }

    protected static Boolean pickupFlag() throws GameActionException {
        FlagInfo[] flags = rc.senseNearbyFlags(-1, rc.getTeam());
        for (FlagInfo flag : flags) {
            Comms.writeFlag(flag);
        }
        FlagInfo closestFlag = Motion.getClosestFlag(flags, false);
        if (closestFlag != null && rc.canPickupFlag(closestFlag.getLocation())) {
            MapLocation flagInit = closestFlag.getLocation();
            rc.pickupFlag(closestFlag.getLocation());
            boolean found = false;
            for (int i = 0; i <= 2; i++) {
                if (rc.readSharedArray(Comms.ALLY_FLAG_ID + i) == closestFlag.getID()) {
                    flagIndex = i;
                    found = true;
                }
            }
            if (found) {
                rc.writeSharedArray(Comms.SETUP_FLAG_DIST + flagIndex, 65535);
                rc.writeSharedArray(Comms.ALLY_FLAG_DEF_LOC + flagIndex, Comms.intifyLocation(flagInit));
                rc.writeSharedArray(Comms.SETUP_SYM_GUESS + flagIndex, Comms.intifyLocation(new MapLocation(rc.getMapWidth() - flagInit.x - 1, rc.getMapHeight() - flagInit.y - 1))); //rot
                rc.writeSharedArray(Comms.SETUP_SYM_GUESS + flagIndex + 3, Comms.intifyLocation(new MapLocation(rc.getMapWidth() - flagInit.x - 1, flagInit.y))); //vert
                rc.writeSharedArray(Comms.SETUP_SYM_GUESS + flagIndex + 6, Comms.intifyLocation(new MapLocation(flagInit.x, rc.getMapHeight() - flagInit.y - 1))); //horz
            }
            return found;
        }
        return false;
    }

    protected static void moveFlag() throws GameActionException {
        MapLocation flagTarget = Comms.parseLocation(rc.readSharedArray(Comms.SETUP_FLAG_TARGET));
        MapLocation toPlace = new MapLocation(flagTarget.x+flagOffset.x, flagTarget.y+flagOffset.y);
        turnsPlacingFlag += 1;
        if (turnsPlacingFlag > 90) {
            //taking too long
            MapLocation me = rc.getLocation();
            MapLocation closestConnectedSpawnLoc = Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC + flagIndex));
            switch (flagIndex) {
                case 0:
                    if (((rc.readSharedArray(Comms.SPAWN_CONNECTED) >> 3) & 0b1) == 1) {
                        MapLocation loc = Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC + 1));
                        if (closestConnectedSpawnLoc == null || me.distanceSquaredTo(closestConnectedSpawnLoc) < me.distanceSquaredTo(loc)) {
                            closestConnectedSpawnLoc = loc;
                        }
                    }
                    if (((rc.readSharedArray(Comms.SPAWN_CONNECTED) >> 5) & 0b1) == 1) {
                        MapLocation loc = Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC + 2));
                        if (closestConnectedSpawnLoc == null || me.distanceSquaredTo(closestConnectedSpawnLoc) < me.distanceSquaredTo(loc)) {
                            closestConnectedSpawnLoc = loc;
                        }
                    }
                    break;
                case 1:
                    if (((rc.readSharedArray(Comms.SPAWN_CONNECTED) >> 3) & 0b1) == 1) {
                        MapLocation loc = Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC + 0));
                        if (closestConnectedSpawnLoc == null || me.distanceSquaredTo(closestConnectedSpawnLoc) < me.distanceSquaredTo(loc)) {
                            closestConnectedSpawnLoc = loc;
                        }
                    }
                    if (((rc.readSharedArray(Comms.SPAWN_CONNECTED) >> 4) & 0b1) == 1) {
                        MapLocation loc = Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC + 2));
                        if (closestConnectedSpawnLoc == null || me.distanceSquaredTo(closestConnectedSpawnLoc) < me.distanceSquaredTo(loc)) {
                            closestConnectedSpawnLoc = loc;
                        }
                    }
                    break;
                case 2:
                    if (((rc.readSharedArray(Comms.SPAWN_CONNECTED) >> 4) & 0b1) == 1) {
                        MapLocation loc = Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC + 1));
                        if (closestConnectedSpawnLoc == null || me.distanceSquaredTo(closestConnectedSpawnLoc) < me.distanceSquaredTo(loc)) {
                            closestConnectedSpawnLoc = loc;
                        }
                    }
                    if (((rc.readSharedArray(Comms.SPAWN_CONNECTED) >> 5) & 0b1) == 1) {
                        MapLocation loc = Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC + 0));
                        if (closestConnectedSpawnLoc == null || me.distanceSquaredTo(closestConnectedSpawnLoc) < me.distanceSquaredTo(loc)) {
                            closestConnectedSpawnLoc = loc;
                        }
                    }
                    break;
                default:
                    break;
            }
            flagTarget = closestConnectedSpawnLoc;
            turnsPlacingFlag = 0;
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
        Motion.bugnavTowards(toPlace);
        MapLocation me = rc.getLocation();
        if (rc.canSenseLocation(toPlace)) {
            MapInfo tile = rc.senseMapInfo(toPlace);
            // testing if flag placement is valid because senseLegalStartingFlagPlacement is broken
            // well this is broken now too
            // boolean valid = true;
            // for (int i = 0; i <= 2; i++) {
            //     int n = rc.readSharedArray(Comms.ALLY_FLAG_CUR_LOC + i);
            //     if (Comms.hasLocation(n) && !Comms.isFlagPickedUp(n)) {
            //         if (toPlace.distanceSquaredTo(Comms.parseLocation(n)) < 36) {
            //             valid = false;
            //         }
            //     }
            // }
            if (!tile.isPassable()) {
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
            rc.writeSharedArray(Comms.ALLY_FLAG_CUR_LOC + flagIndex, Comms.intifyLocation(toPlace));
            rc.writeSharedArray(Comms.ALLY_FLAG_DEF_LOC + flagIndex, Comms.intifyLocation(toPlace));
            flagIndex = -1;
        }
        else {
            rc.setIndicatorLine(me, toPlace, 0, 255, 0);
            indicatorString.append("FLAG"+flagIndex+"->("+(flagTarget.x+flagOffset.x)+","+(flagTarget.y+flagOffset.y)+");");
            rc.writeSharedArray(Comms.ALLY_FLAG_CUR_LOC + flagIndex, Comms.intifyLocation(rc.getLocation()));
        }
    }

    protected static void guessSymmetry() throws GameActionException {
        int curSymmetry = rc.readSharedArray(Comms.SYM) & 0b111;
        if (curSymmetry == 0b110 || curSymmetry == 0b101 || curSymmetry == 0b011) {
            //already done
            return;
        }
        MapLocation[] guesses = {
            Comms.parseLocation(rc.readSharedArray(Comms.SETUP_SYM_GUESS)),
            Comms.parseLocation(rc.readSharedArray(Comms.SETUP_SYM_GUESS+1)),
            Comms.parseLocation(rc.readSharedArray(Comms.SETUP_SYM_GUESS+2)),
            Comms.parseLocation(rc.readSharedArray(Comms.SETUP_SYM_GUESS+3)),
            Comms.parseLocation(rc.readSharedArray(Comms.SETUP_SYM_GUESS+4)),
            Comms.parseLocation(rc.readSharedArray(Comms.SETUP_SYM_GUESS+5)),
            Comms.parseLocation(rc.readSharedArray(Comms.SETUP_SYM_GUESS+6)),
            Comms.parseLocation(rc.readSharedArray(Comms.SETUP_SYM_GUESS+7)),
            Comms.parseLocation(rc.readSharedArray(Comms.SETUP_SYM_GUESS+8)),
        };
        for (int i = 0; i < guesses.length; i++) {
            if (rc.canSenseLocation(guesses[i])) {
                MapInfo info = rc.senseMapInfo(guesses[i]);
                if (info.getSpawnZoneTeamObject() != rc.getTeam().opponent()) {
                    //wrong symmetry buh
                    rc.writeSharedArray(Comms.SYM, rc.readSharedArray(Comms.SYM) | 1 << (i / 3));
                }
            }
        }
    }

    protected static int turnsCheckingSpawnZoneConnected = 0;
    protected static boolean checkSpawnZoneConnectedCooldown = false;

    protected static Boolean checkSpawnZoneConnected() throws GameActionException {
        if (spawnFlagIndex == -1) return false;
        if (spawnFlagIndex == 1) {
            if ((rc.readSharedArray(Comms.SPAWN_CONNECTED) & 0b101000) > 0) {
                return false;
            }
        } else if (spawnFlagIndex == 2) {
            if ((rc.readSharedArray(Comms.SPAWN_CONNECTED) & 0b011000) > 0) {
                return false;
            }
        } else {
            if ((rc.readSharedArray(Comms.SPAWN_CONNECTED) & 0b110000) > 0) {
                return false;
            }
        }
        turnsCheckingSpawnZoneConnected++;
        if (turnsCheckingSpawnZoneConnected > 10 || checkSpawnZoneConnectedCooldown) {
            checkSpawnZoneConnectedCooldown = true;
            turnsCheckingSpawnZoneConnected -= 2;
            if (turnsCheckingSpawnZoneConnected <= 0) {
                checkSpawnZoneConnectedCooldown = false;
            }
            return false;
        }
        for (int i = 0; i < 3; i++) {
            if (i == spawnFlagIndex) continue;
            int flagLoc = rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC + i);
            if (Comms.hasLocation(flagLoc)) {
                MapLocation coord = Comms.parseLocation(flagLoc);
                if (rc.canSenseLocation(coord)) {
                    Motion.bugnavTowards(coord);
                    if (rc.getLocation().distanceSquaredTo(coord) <= 2) {
                        //connected!
                        if (spawnFlagIndex == 1) {
                            if (i == 2) {
                                rc.writeSharedArray(Comms.SPAWN_CONNECTED, rc.readSharedArray(Comms.SPAWN_CONNECTED) | 1 << 3);
                            } else {
                                //i == 3
                                rc.writeSharedArray(Comms.SPAWN_CONNECTED, rc.readSharedArray(Comms.SPAWN_CONNECTED) | 1 << 5);
                            }
                        } else if (spawnFlagIndex == 2) {
                            if (i == 3) {
                                rc.writeSharedArray(Comms.SPAWN_CONNECTED, rc.readSharedArray(Comms.SPAWN_CONNECTED) | 1 << 4);
                            } else {
                                //i == 1
                                rc.writeSharedArray(Comms.SPAWN_CONNECTED, rc.readSharedArray(Comms.SPAWN_CONNECTED) | 1 << 3);
                            }
                        } else {
                            if (i == 1) {
                                rc.writeSharedArray(Comms.SPAWN_CONNECTED, rc.readSharedArray(Comms.SPAWN_CONNECTED) | 1 << 5);
                            } else {
                                //i == 2
                                rc.writeSharedArray(Comms.SPAWN_CONNECTED, rc.readSharedArray(Comms.SPAWN_CONNECTED) | 1 << 4);
                            }
                        }
                        turnsCheckingSpawnZoneConnected = 0;
                    }
                    return true;
                }
            }
        }
        return false;
    }
    
    protected static void run() throws GameActionException {
        if (rc.getRoundNum() < Math.max(rc.getMapHeight(), rc.getMapWidth())) {
            //exploration phase, lasts up to turn 60
            MapInfo[] infos = rc.senseNearbyMapInfos();
            MapLocation me = rc.getLocation();
            if (spawnFlagIndex == -1) {
                //set spawnFlagIndex
                for (int i = 0; i < 3; i++) {
                    int flagLoc = rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC + i);
                    if (Comms.hasLocation(flagLoc) && me.distanceSquaredTo(Comms.parseLocation(flagLoc)) < 5) {
                        spawnFlagIndex = i;
                    }
                }
            }
            if (!pickupFlag() && !getCrumbs(infos) && !checkSpawnZoneConnected() && !rc.hasFlag()) { // try to get crumbs/flag
                guessSymmetry();
                Boolean nearDam = false;
                MapLocation damTarget = new MapLocation(-1, -1);
                for (MapInfo i : infos) {
                    if (i.isDam()) {
                        nearDam = true;
                        damTarget = i.getMapLocation();
                    }
                }
                if (nearDam) {
                    Motion.bfsnav(damTarget);
                } else {
                    Motion.spreadRandomly();
                }
            }
        } else if (rc.getRoundNum() == Math.max(rc.getMapHeight(), rc.getMapWidth())) {
            //longest path
            if (!rc.hasFlag()) {
                MapInfo[] infos = rc.senseNearbyMapInfos();
                MapLocation me = rc.getLocation();
                for (MapInfo i : infos) {
                    if (i.getTeamTerritory() == rc.getTeam().opponent()) {
                        damInit = i.getMapLocation();
                        damSpreadWeights[me.directionTo(i.getMapLocation()).getDirectionOrderNum()] -= 100/me.distanceSquaredTo(i.getMapLocation());
                    }
                    if (i.isDam()) {
                        if (damInit == null) {
                            damInit = i.getMapLocation();
                        }
                        damSpreadWeights[me.directionTo(i.getMapLocation()).getDirectionOrderNum()] -= 10/me.distanceSquaredTo(i.getMapLocation());
                        int meet = rc.readSharedArray(Comms.SETUP_GATHER_LOC);
                        if (!Comms.hasLocation(meet)) {
                            rc.writeSharedArray(Comms.SETUP_GATHER_LOC, Comms.intifyLocation(damInit));
                        }
                    }
                }
            }
        } else if (rc.getRoundNum() <= 5*Math.max(rc.getMapHeight(), rc.getMapWidth())/3) {
            if (damInit == null) {
                //not running longest path
                MapInfo[] infos = rc.senseNearbyMapInfos();
                if (!getCrumbs(infos) && !checkSpawnZoneConnected() && !rc.hasFlag()) {
                    guessSymmetry();
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
                MapLocation me = rc.getLocation();
                MapLocation[] spawns = {
                    Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC)),
                    Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC+1)),
                    Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC+2)),
                };
                // MapLocation closestSpawn = Motion.getClosest(spawns);
                Motion.bugnavTowards(new MapLocation(me.x+dx, me.y+dy));
                rc.setIndicatorLine(rc.getLocation(), new MapLocation(me.x+dx, me.y+dy), 255, 255, 0);
                indicatorString.append("LONGPATH->("+(me.x+dx)+","+(me.y+dy)+");");

                for (int i = 0; i < spawns.length; i++) {
                    if (rc.canSenseLocation(spawns[i])) {
                        int dist = spawns[i].distanceSquaredTo(damInit);
                        int best = rc.readSharedArray(Comms.SETUP_FLAG_DIST+i);
                        if (dist < best) {
                            rc.writeSharedArray(Comms.SETUP_FLAG_DIST+i, dist);
                        }
                    }
                }
            }
        // } else if (rc.getRoundNum() == 5*Math.max(rc.getMapHeight(), rc.getMapWidth())/3) {
        //     if (damInit == null) {
        //         //not running longest path
        //         MapInfo[] infos = rc.senseNearbyMapInfos();
        //         if (!getCrumbs(infos) && !rc.hasFlag()) {
        //             guessSymmetry();
        //             Motion.spreadRandomly();
        //         }
        //     } else {
        //         //longest path
        //         //get closest spawn zone
        //         int closestSpawn = Integer.MAX_VALUE;
        //         MapLocation me = rc.getLocation();
        //         MapLocation[] spawns = rc.getAllySpawnLocations();
        //         for (MapLocation i : spawns) {
        //             closestSpawn = Math.min(me.distanceSquaredTo(i), closestSpawn);
        //         }

        //         int weight = 4096 // 2^12
        //         + rc.getLocation().distanceSquaredTo(damInit) //distance to dam (farther is better)
        //         - 6*closestSpawn //distance to nearest spawn (closer is better)
        //         ;
        //         weight = Math.max(weight, 0);
        //         weight = Math.min(weight, 8192);

        //         //using setup flag target global array index
        //         int best = rc.readSharedArray(Comms.SETUP_FLAG_WEIGHT);
        //         if (best < weight) {
        //             rc.writeSharedArray(Comms.SETUP_FLAG_WEIGHT, weight);
        //             rc.writeSharedArray(Comms.SETUP_FLAG_TARGET, Comms.intifyLocation(rc.getLocation()));
        //         }
        //     }
        } else if (rc.getRoundNum() + Math.max(rc.getMapWidth(), rc.getMapHeight()) <= 205) {
            //move flag
            if (!Comms.hasLocation(rc.readSharedArray(Comms.SETUP_FLAG_TARGET))) {
                int[] dists = {
                    rc.readSharedArray(Comms.SETUP_FLAG_DIST),
                    rc.readSharedArray(Comms.SETUP_FLAG_DIST+1),
                    rc.readSharedArray(Comms.SETUP_FLAG_DIST+2),
                };
                // for (int i : dists)System.out.println(i);
                int best = 0;
                int max = 0;
                for (int i= 0; i < dists.length; i++) {
                    if (dists[i] > max) {
                        max = dists[i];
                        best = i;
                    }
                }
                System.out.println(max + " " + best);
                rc.writeSharedArray(Comms.SETUP_FLAG_TARGET, rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC+best) & 0b1111111111111);
            }
            if (rc.hasFlag()) {
                moveFlag();
            } else if (Comms.id < 6) {
                MapLocation flagCarrier = Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_CUR_LOC + (Comms.id % 3)));
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
            } else if (Comms.id < 6) {
                MapLocation flagCarrier = Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_CUR_LOC + (Comms.id % 3)));
                Motion.bugnavTowards(flagCarrier);
                rc.setIndicatorLine(rc.getLocation(), flagCarrier, 255, 0, 255);
                indicatorString.append("FOLLOW-FLAG;" + Comms.id);
            } else {
                guessSymmetry();
                MapInfo[] infos = rc.senseNearbyMapInfos();
                int damLoc = rc.readSharedArray(Comms.SETUP_GATHER_LOC);
                if (!Comms.hasLocation(damLoc)) {
                    for (MapInfo i : infos) {
                        if (i.isDam()) {
                            rc.writeSharedArray(Comms.SETUP_GATHER_LOC, Comms.intifyLocation(i.getMapLocation()) | 1 << 13);
                            break;
                        }
                    }
                }

                MapLocation me = rc.getLocation();
                Boolean nearDam = false;
                RobotInfo[] bots = rc.senseNearbyRobots();
                for (MapInfo i : infos) {
                    if (i.isDam()) {
                        nearDam = true;
                    }
                }
                if (!nearDam) {
                    //if we aren't near the dam, then go to the meeting point
                    if (Comms.hasLocation(damLoc)) {
                        Motion.bugnavTowards(Comms.parseLocation(damLoc), 500);
                        indicatorString.append("MEET("+Comms.parseLocation(damLoc).x+","+Comms.parseLocation(damLoc).y+");");
                        rc.setIndicatorLine(me, Comms.parseLocation(damLoc), 255, 100, 0);
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
                    if (!Comms.hasLocation(damLoc) || botWeight > storedBotWeight) {
                        //Changing meeting point if there are lots of enemy bots/not many friendly bots
                        for (MapInfo i : infos) {
                            if (i.isDam()) {
                                rc.writeSharedArray(Comms.SETUP_GATHER_LOC, Comms.intifyLocation(i.getMapLocation()) | botWeight << 13);
                                break;
                            }
                        }
                    }
                    if (me.distanceSquaredTo(Comms.parseLocation(damLoc)) < 5) {
                        rc.writeSharedArray(Comms.SETUP_GATHER_LOC, Comms.intifyLocation(Comms.parseLocation(damLoc)) | botWeight << 13);
                    }
                    for (MapInfo i : infos) {
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
        if (Comms.id < 6) {
            rc.setIndicatorDot(rc.getLocation(), 255, 0, 255);
        }
    }
    protected static void jailed() throws GameActionException {
        // how are you dying lol
    }
}
