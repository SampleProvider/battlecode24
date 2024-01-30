package SPAARK;
import battlecode.common.*;

import java.util.Random;

public class Setup {
    protected static RobotController rc;
    protected static StringBuilder indicatorString;

    protected static Random rng;

    protected static int flagIndex = -1;

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
        MapLocation[] spawns = new MapLocation[] {
            Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC)),
            Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC+1)),
            Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC+2)),
        };
        MapLocation me = rc.getLocation();
        double bestWeight = 0;
        MapLocation best = me;
        MapInfo[] mapInfos = rc.senseNearbyMapInfos();
        for (int j = 3; --j >= 0;) {
            if (allFlags[j] == null) {
                continue;
            }
            if (j == flagIndex) {
                continue;
            }
            if (me.distanceSquaredTo(allFlags[j]) <= 36) {
                Motion.bugnavAway(allFlags[j], true);
            }
        }
        t: for (MapInfo i : mapInfos) {
            double weight = 0;
            if (i.isWall() || i.getTeamTerritory() != rc.getTeam()) {
                continue;
            }
            for (int j = 3; --j >= 0;) {
                if (allFlags[j] == null) {
                    continue;
                }
                if (j == flagIndex) {
                    continue;
                }
                if (i.getMapLocation().distanceSquaredTo(allFlags[j]) <= 36) {
                    continue t;
                }
                if (me.add(me.directionTo(i.getMapLocation())).distanceSquaredTo(allFlags[j]) <= 36) {
                    continue t;
                }
                if (i.getMapLocation().distanceSquaredTo(allFlags[j]) <= 49) {
                    weight -= 3;
                }
                //closer to other flags
                weight += 2*(Math.sqrt(me.distanceSquaredTo(allFlags[j])) - Math.sqrt(i.getMapLocation().distanceSquaredTo(allFlags[j])));

                //closer to spawns
                weight += (Math.sqrt(me.distanceSquaredTo(spawns[j])) - Math.sqrt(i.getMapLocation().distanceSquaredTo(spawns[j])));
            }
            //farther from center
            weight += 4*(Math.sqrt(i.getMapLocation().distanceSquaredTo(Motion.getMapCenter())) - Math.sqrt(me.distanceSquaredTo(Motion.getMapCenter())));
            if (weight > bestWeight) {
                bestWeight = weight;
                best = i.getMapLocation();
            }
        }
        rc.setIndicatorLine(me, best, 255, 255, 255);
        if (!me.isAdjacentTo(best) && !me.equals(best)) {
            for (int j = 3; --j >= 0;) {
                if (j == flagIndex) {
                    continue;
                }
                if (allFlags[j] == null) {
                    continue;
                }
                if (me.add(Motion.getBfsDirection(best, true, true)).distanceSquaredTo(allFlags[j]) > 36) {
                    Motion.bfsnav(best, true);
                }
            }
        } else {
            MapInfo info = rc.senseMapInfo(best);
            if (info.isWater()) {
                if (rc.canFill(info.getMapLocation())) {
                    rc.fill(info.getMapLocation());
                }
            }
            if (rc.canMove(me.directionTo(best))) {
                rc.move(me.directionTo(best));
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
        if (flagIndex == -1) return false;
        if (flagIndex == 1) {
            if ((rc.readSharedArray(Comms.SPAWN_CONNECTED) & 0b101000) > 0) {
                return false;
            }
        } else if (flagIndex == 2) {
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
            if (i == flagIndex) continue;
            int flagLoc = rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC + i);
            if (Comms.hasLocation(flagLoc)) {
                MapLocation coord = Comms.parseLocation(flagLoc);
                if (rc.canSenseLocation(coord)) {
                    Motion.bfsnav(coord);
                    if (rc.getLocation().distanceSquaredTo(coord) <= 2) {
                        //connected!
                        if (flagIndex == 1) {
                            if (i == 2) {
                                rc.writeSharedArray(Comms.SPAWN_CONNECTED, rc.readSharedArray(Comms.SPAWN_CONNECTED) | 1 << 3);
                            } else {
                                //i == 3
                                rc.writeSharedArray(Comms.SPAWN_CONNECTED, rc.readSharedArray(Comms.SPAWN_CONNECTED) | 1 << 5);
                            }
                        } else if (flagIndex == 2) {
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
            if (flagIndex == -1) {
                //set flagIndex
                for (int i = 3; --i >= 0;) {
                    int flagLoc = rc.readSharedArray(Comms.ALLY_FLAG_DEF_LOC + i);
                    if (Comms.hasLocation(flagLoc) && me.distanceSquaredTo(Comms.parseLocation(flagLoc)) < 5) {
                        flagIndex = i;
                        break;
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
                    Motion.spreadRandomly(false);
                    // if (rc.getRoundNum() < 20) {
                    //     Motion.spreadRandomly(true);
                    // } else {
                    //     Motion.spreadRandomly(false);
                    // }
                }
            }
            guessSymmetry();
        } else {
            //line up
            //clear array values
            guessSymmetry();
            if (rc.hasFlag()) {
                moveFlag();
            // } else if (RobotPlayer.mode == RobotPlayer.SCOUT) {
            //     Motion.spreadRandomly();
            } else {
                MapInfo[] infos = rc.senseNearbyMapInfos();
                int damLoc = rc.readSharedArray(Comms.SETUP_GATHER_LOC+flagIndex*2);
                if (!Comms.hasLocation(damLoc)) {
                    for (MapInfo i : infos) {
                        if (i.isDam()) {
                            rc.writeSharedArray(Comms.SETUP_GATHER_LOC+flagIndex*2, Comms.intifyLocation(i.getMapLocation()) | 1 << 13);
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
                    if (Comms.hasLocation(damLoc) && !getCrumbs()) {
                        Motion.bfsnav(Comms.parseLocation(damLoc));
                        indicatorString.append("MEET("+Comms.parseLocation(damLoc).x+","+Comms.parseLocation(damLoc).y+");");
                        rc.setIndicatorLine(me, Comms.parseLocation(damLoc), 255, 100, 0);
                    } else {
                        Motion.spreadRandomly();
                        indicatorString.append("RANDOM;");
                    }
                } else {
                    //Nearby dam, try to spread out
                    if (!getCrumbs()) {
                        int botWeight = 32767;
                        double weights[] = {0, 0, 0, 0, 0, 0, 0, 0, 0}; //one for each direction
                        for (RobotInfo i : bots) {
                            if (i.team == rc.getTeam()) {
                                botWeight -= 3;
                                weights[me.directionTo(i.getLocation()).getDirectionOrderNum()] -= 10/me.distanceSquaredTo(i.getLocation());
                            } else {
                                botWeight += 9;
                                weights[me.directionTo(i.getLocation()).getDirectionOrderNum()] += 60/me.distanceSquaredTo(i.getLocation());
                            }
                        }
                        MapLocation[] flags = {
                            Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_CUR_LOC)),
                            Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_CUR_LOC+1)),
                            Comms.parseLocation(rc.readSharedArray(Comms.ALLY_FLAG_CUR_LOC+2)),
                        };
                        botWeight -= Math.sqrt(me.distanceSquaredTo(Motion.getClosest(flags)));
                        int storedBotWeight = rc.readSharedArray(Comms.SETUP_GATHER_LOC+flagIndex*2+1);
                        if (!Comms.hasLocation(damLoc) || botWeight > storedBotWeight) {
                            //Changing meeting point
                            for (MapInfo i : infos) {
                                if (i.isDam()) {
                                    rc.writeSharedArray(Comms.SETUP_GATHER_LOC+flagIndex*2, Comms.intifyLocation(i.getMapLocation()));
                                    rc.writeSharedArray(Comms.SETUP_GATHER_LOC+flagIndex*2+1, botWeight);
                                    // if (flagIndex == 1) {
                                    //     if (((rc.readSharedArray(Comms.SPAWN_CONNECTED) >> 3) & 1) > 0) {
                                    //         rc.writeSharedArray(Comms.SETUP_GATHER_LOC, Comms.intifyLocation(i.getMapLocation()));
                                    //         rc.writeSharedArray(Comms.SETUP_GATHER_LOC+1, botWeight);
                                    //     }
                                    //     if (((rc.readSharedArray(Comms.SPAWN_CONNECTED) >> 4) & 1) > 0) {
                                    //         rc.writeSharedArray(Comms.SETUP_GATHER_LOC+4, Comms.intifyLocation(i.getMapLocation()));
                                    //         rc.writeSharedArray(Comms.SETUP_GATHER_LOC+5, botWeight);
                                    //     }
                                    // } else if (flagIndex == 2) {
                                    //     if (((rc.readSharedArray(Comms.SPAWN_CONNECTED) >> 5) & 1) > 0) {
                                    //         rc.writeSharedArray(Comms.SETUP_GATHER_LOC, Comms.intifyLocation(i.getMapLocation()));
                                    //         rc.writeSharedArray(Comms.SETUP_GATHER_LOC+1, botWeight);
                                    //     }
                                    //     if (((rc.readSharedArray(Comms.SPAWN_CONNECTED) >> 4) & 1) > 0) {
                                    //         rc.writeSharedArray(Comms.SETUP_GATHER_LOC+2, Comms.intifyLocation(i.getMapLocation()));
                                    //         rc.writeSharedArray(Comms.SETUP_GATHER_LOC+3, botWeight);
                                    //     }
                                    // } else {
                                    //     if (((rc.readSharedArray(Comms.SPAWN_CONNECTED) >> 5) & 1) > 0) {
                                    //         rc.writeSharedArray(Comms.SETUP_GATHER_LOC+4, Comms.intifyLocation(i.getMapLocation()));
                                    //         rc.writeSharedArray(Comms.SETUP_GATHER_LOC+5, botWeight);
                                    //     }
                                    //     if (((rc.readSharedArray(Comms.SPAWN_CONNECTED) >> 3) & 1) > 0) {
                                    //         rc.writeSharedArray(Comms.SETUP_GATHER_LOC+2, Comms.intifyLocation(i.getMapLocation()));
                                    //         rc.writeSharedArray(Comms.SETUP_GATHER_LOC+3, botWeight);
                                    //     }
                                    // }
                                    break;
                                }
                            }
                        }
                        if (me.distanceSquaredTo(Comms.parseLocation(damLoc)) <= 4) {
                            //Update weight at this location
                            rc.writeSharedArray(Comms.SETUP_GATHER_LOC+flagIndex*2+1, botWeight);
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