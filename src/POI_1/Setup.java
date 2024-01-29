package POI_1;
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

    protected static MapLocation damInit;
    protected static MapLocation runTarget = new MapLocation(0, 0);

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

    protected static Boolean getCrumbs() throws GameActionException {
        return getCrumbs(rc.senseNearbyMapInfos());
    }

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
            flagIndex = Comms.id % 3;
            rc.writeSharedArray(Comms.SETUP_SYM_GUESS + flagIndex, Comms.intifyLocation(new MapLocation(rc.getMapWidth() - flagInit.x - 1, rc.getMapHeight() - flagInit.y - 1))); //rot
            rc.writeSharedArray(Comms.SETUP_SYM_GUESS + flagIndex + 3, Comms.intifyLocation(new MapLocation(rc.getMapWidth() - flagInit.x - 1, flagInit.y))); //vert
            rc.writeSharedArray(Comms.SETUP_SYM_GUESS + flagIndex + 6, Comms.intifyLocation(new MapLocation(flagInit.x, rc.getMapHeight() - flagInit.y - 1))); //horz
            rc.writeSharedArray(Comms.ALLY_FLAG_CUR_LOC, Comms.intifyLocation(rc.getLocation()));
            return true;
        }
        return false;
    }

    protected static void moveFlag() throws GameActionException {
        MapLocation[] allFlags = new MapLocation[] {
            Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_CUR_LOC)),
            Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_CUR_LOC+1)),
            Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_CUR_LOC+2)),
        };
        MapLocation me = rc.getLocation();
        int bestWeight = 0;
        Direction bestDir = Direction.CENTER;
        for (Direction d : DIRECTIONS) {
            int weight = 0;
            for (int i = 3; --i >= 0; ) {
                if (allFlags[i] == null) {
                    continue;
                }
                if (i == flagIndex) {
                    continue;
                }
                if (me.add(d).distanceSquaredTo(allFlags[i]) <= me.distanceSquaredTo(allFlags[i])) {
                    weight += 5;
                }
                if (me.distanceSquaredTo(allFlags[i]) <= 36) {
                    if (me.add(d).distanceSquaredTo(allFlags[i]) > me.distanceSquaredTo(allFlags[i])) {
                        weight += 100;
                    } else {
                        weight -= 100;
                    }
                } else if (me.distanceSquaredTo(allFlags[i]) <= 49) {
                    if (me.add(d).distanceSquaredTo(allFlags[i]) > me.distanceSquaredTo(allFlags[i])) {
                        weight += 2;
                    } else {
                        weight -= 2;
                    }
                }
                if (me.add(d).distanceSquaredTo(allFlags[i]) <= 36) {
                    weight -= 100;
                }
            }
            if (me.add(d).distanceSquaredTo(Motion.getMapCenter()) >= me.distanceSquaredTo(Motion.getMapCenter())) {
                //move away from center
                weight += 7;
            }
            if (weight > bestWeight) {
                bestWeight = weight;
                bestDir = d;
            }
        }
        // System.out.println(bestWeight + " " + bestDir);
        rc.setIndicatorLine(me, me.add(bestDir), 255, 255, 255);
        if (rc.onTheMap(me.add(bestDir))) {
            MapInfo info = rc.senseMapInfo(me.add(bestDir));
            if (info.isPassable() || info.isWater()) {
                if (info.isWater()) {
                    if (rc.canFill(info.getMapLocation())) {
                        rc.fill(info.getMapLocation());
                    }
                }
                Motion.bfsnav(me.add(bestDir));
            } else {

            }
        }
        rc.writeSharedArray(Comms.ALLY_FLAG_CUR_LOC + flagIndex, Comms.intifyLocation(rc.getLocation()));
    }

    protected static void followFlag() throws GameActionException {
        MapLocation flagCarrier = Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_CUR_LOC + (Comms.id % 3)));
        Motion.bfsnav(flagCarrier);
        rc.setIndicatorLine(rc.getLocation(), flagCarrier, 255, 0, 255);
        indicatorString.append("FOLLOW-FLAG" + (Comms.id % 3) + ";");
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
        for (int i = 9; --i >= 0;) {
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
        for (int i = 3; --i >= 0;) {
            if (i == spawnFlagIndex) continue;
            int flagLoc = rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC + i);
            if (Comms.hasLocation(flagLoc)) {
                MapLocation coord = Comms.parseLocation(flagLoc);
                if (rc.canSenseLocation(coord)) {
                    Motion.bfsnav(coord);
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
        Motion.updateBfsMap();
        if (rc.getRoundNum() + Math.max(rc.getMapWidth(), rc.getMapHeight()) <= 205) {
            //exploration phase, lasts up to turn 60
            MapInfo[] infos = rc.senseNearbyMapInfos();
            MapLocation me = rc.getLocation();
            if (spawnFlagIndex == -1) {
                //set spawnFlagIndex
                for (int i = 3; --i >= 0;) {
                    int flagLoc = rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC + i);
                    if (Comms.hasLocation(flagLoc) && me.distanceSquaredTo(Comms.parseLocation(flagLoc)) < 5) {
                        spawnFlagIndex = i;
                    }
                }
            }
            if (RobotPlayer.mode == RobotPlayer.DEFENSIVE) {
                if (!rc.hasFlag()) {
                    pickupFlag();
                } else {
                    moveFlag();
                }
            } else {
                if (!getCrumbs(infos) && !checkSpawnZoneConnected()) { // try to get crumbs
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
            }
            guessSymmetry();
        } else {
            //line up
            //clear array values
            if (rc.hasFlag()) {
                moveFlag();
            } else if (RobotPlayer.mode == RobotPlayer.SCOUT) {
                Motion.spreadRandomly();
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
                        Motion.bfsnav(Comms.parseLocation(damLoc));
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
        if (rc.hasFlag() && rc.getRoundNum() == GameConstants.SETUP_ROUNDS - 1) {
            rc.writeSharedArray(Comms.ALLY_FLAG_DEF_LOC + flagIndex, Comms.intifyLocation(rc.getLocation()));
        }
        if (Comms.id == 49 && rc.getRoundNum() == GameConstants.SETUP_ROUNDS - 1) {
            for (int i = 0; i < 9; i++) {
                rc.writeSharedArray(Comms.SETUP_SYM_GUESS + i, 0);
            }
        }
    }
    protected static void jailed() throws GameActionException {
        // how are you dying lol
    }
}