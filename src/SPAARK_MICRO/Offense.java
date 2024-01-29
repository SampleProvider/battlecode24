package SPAARK_MICRO;

import battlecode.common.*;

import java.util.Random;

public class Offense {
    protected static RobotController rc;
    protected static StringBuilder indicatorString;

    protected static Random rng;

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
    protected static int flagIndex = -1;

    protected static int turnsFindingFlag = 0;
    protected static int reachedBroadcastTurn = 0;
    protected static MapLocation turnsFindingFlagTarget = new MapLocation(0, 0);
    
    protected static void run() throws GameActionException {
        // capturing opponent flags
        MapLocation me = rc.getLocation();
        tryPickupFlag();

        // writing flags to global array
        FlagInfo[] opponentFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        FlagInfo[] friendlyFlags = rc.senseNearbyFlags(-1, rc.getTeam());
        for (FlagInfo flag : Motion.flags) {
            Comms.writeFlag(flag);
        }
        Comms.checkFlags(friendlyFlags, opponentFlags);
        // GlobalArray.updateSector();
        Comms.updatePOI();
        Motion.updateBfsMap();

        indicatorString.append(flagIndex);

        // flagIndex: index of flag currently holding in global array
        run: {
            if (flagIndex != -1) {
                moveWithFlag();
                break run;
            }
            // crumb stuff if not already done
            MapInfo[] info = rc.senseNearbyMapInfos();
            if (Setup.getCrumbs(info)) {
                break run;
            }

            MapLocation best = Comms.getBestPOI();
            MapLocation target = null;
            if (best != null) {
                target = best;
                // target = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.POI + best));
            }

            FlagInfo closestFlag = Motion.getClosestFlag(opponentFlags, false);
            if (target == null && closestFlag != null) {
                target = closestFlag.getLocation();
            }
            if (Comms.id % 2 == 0) {
                // tested: (%3==0 (21/42), %5<2 (21/42), %10<3 (14/42), %10<4 (21/42), <30 (23/42))
                if (target == null) {
                    MapLocation closestStoredFlag = null;
                    for (int i = 3; --i >= 0;) {
                        int n = rc.readSharedArray(Comms.OPPO_FLAG_CUR_LOC + i);
                        // int n2 = rc.readSharedArray(GlobalArray.OPPO_FLAG_DEF_LOC + i);
                        if (Comms.hasLocation(n) && Comms.isFlagPickedUp(n)) {
                            MapLocation loc = Comms.parseLocation(n);
                            if (closestStoredFlag == null || me.distanceSquaredTo(closestStoredFlag) > me.distanceSquaredTo(loc)) {
                                closestStoredFlag = loc;
                            }
                        }
                    }
                    if (closestStoredFlag != null) {
                        Motion.bugnavAround(closestStoredFlag, 4, 10);
                        rc.setIndicatorLine(rc.getLocation(), closestStoredFlag, 200, 200, 200);
                        break run;
                    }
                }
            }
            if (target == null) {
                MapLocation closestStoredFlag = null;
                for (int i = 3; --i >= 0;) {
                    int n = rc.readSharedArray(Comms.OPPO_FLAG_CUR_LOC + i);
                    // int n2 = rc.readSharedArray(GlobalArray.OPPO_FLAG_DEF_LOC + i);
                    if (Comms.hasLocation(n) && !Comms.isFlagPickedUp(n)) {
                        MapLocation loc = Comms.parseLocation(n);
                        if (closestStoredFlag == null || me.distanceSquaredTo(closestStoredFlag) > me.distanceSquaredTo(loc)) {
                            closestStoredFlag = loc;
                        }
                    }
                }
                if (closestStoredFlag != null) {
                    Motion.bfsnav(closestStoredFlag, true);
                    rc.setIndicatorLine(rc.getLocation(), closestStoredFlag, 200, 200, 200);
                    break run;
                }
            }
            if (target == null) {
                MapLocation[] hiddenFlags = rc.senseBroadcastFlagLocations();
                if (hiddenFlags.length > 0) {
                    MapLocation closestHiddenFlag = Motion.getClosest(hiddenFlags);
                    if (me.distanceSquaredTo(closestHiddenFlag) < 4) {
                        reachedBroadcastTurn = rc.getRoundNum();
                    }
                    if (reachedBroadcastTurn / 100 == rc.getRoundNum() / 100) {
                        Motion.spreadRandomly();
                    }
                    target = closestHiddenFlag;
                }
            }
            if (target == null) {
                MapLocation closestStoredFlag = null;
                for (int i = 3; --i >= 0;) {
                    int n = rc.readSharedArray(Comms.OPPO_FLAG_CUR_LOC + i);
                    // int n2 = rc.readSharedArray(GlobalArray.OPPO_FLAG_DEF_LOC + i);
                    if (Comms.hasLocation(n) && Comms.isFlagPickedUp(n)) {
                        MapLocation loc = Comms.parseLocation(n);
                        if (closestStoredFlag == null || me.distanceSquaredTo(closestStoredFlag) > me.distanceSquaredTo(loc)) {
                            closestStoredFlag = loc;
                        }
                    }
                }
                if (closestStoredFlag != null) {
                    Motion.bugnavAround(closestStoredFlag, 4, 10);
                    rc.setIndicatorLine(rc.getLocation(), closestStoredFlag, 200, 200, 200);
                    break run;
                }
            }
        
            if (target != null) {
                Motion.bfsnav(target);
                rc.setIndicatorLine(rc.getLocation(), target, 255, 255, 255);
            }

            // indicatorString.append("GROUP:" + GlobalArray.groupId);
        }
        
        // opponentFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        // friendlyFlags = rc.senseNearbyFlags(-1, rc.getTeam());
        for (FlagInfo flag : Motion.flags) {
            Comms.writeFlag(flag);
        }
        // GlobalArray.checkFlags(opponentFlags);
        tryPickupFlag();

        Atk.attack();
        Atk.heal();
    }
    protected static void tryPickupFlag() throws GameActionException {
        FlagInfo[] opponentFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        FlagInfo closestFlag = Motion.getClosestFlag(opponentFlags, false);
        if (closestFlag != null && rc.canPickupFlag(closestFlag.getLocation())) {
            MapLocation[] spawnLocs = rc.getAllySpawnLocations();
            MapLocation bestLoc = Motion.getClosest(spawnLocs);
            RobotInfo[] closeFriendlyRobots = rc.senseNearbyRobots(closestFlag.getLocation(), 2, rc.getTeam());
            RobotInfo closestRobot = Motion.getClosestRobot(closeFriendlyRobots, bestLoc);
            if (closestRobot == null || closestRobot.getLocation().distanceSquaredTo(bestLoc) >= rc.getLocation().distanceSquaredTo(bestLoc) || rc.getLocation().equals(closestFlag.getLocation())) {
                rc.pickupFlag(closestFlag.getLocation());
                int flagId = closestFlag.getID();
                for (int i = 3; --i >= 0;) {
                    if (rc.readSharedArray(Comms.OPPO_FLAG_ID + i) == 0) {
                        flagIndex = i;
                        rc.writeSharedArray(Comms.OPPO_FLAG_ID + i, flagId);
                        break;
                    }
                    else if (rc.readSharedArray(Comms.OPPO_FLAG_ID + i) == flagId) {
                        flagIndex = i;
                        break;
                    }
                }
            }
        }
    }
    protected static void moveWithFlag() throws GameActionException {
        // navigate back to spawn
        MapLocation me = rc.getLocation();
        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
        MapLocation bestLoc = Motion.getClosest(spawnLocs);
        rc.setIndicatorDot(me, 255, 0, 0);
        rc.setIndicatorLine(me, bestLoc, 255, 0, 0);
        Motion.bfsnav(bestLoc);
        if (rc.canDropFlag(me)) {
            RobotInfo[] closeFriendlyRobots = rc.senseNearbyRobots(8, rc.getTeam());
            RobotInfo closestRobot = Motion.getClosestRobot(closeFriendlyRobots, bestLoc);
            if (closestRobot != null && closestRobot.getLocation().distanceSquaredTo(bestLoc) < rc.getLocation().distanceSquaredTo(bestLoc)) {
                if (rc.canDropFlag(rc.adjacentLocation(me.directionTo(closestRobot.getLocation())))) {
                    rc.dropFlag(rc.adjacentLocation(me.directionTo(closestRobot.getLocation())));
                    flagIndex = -1;
                    return;
                }
            }
        }
        // rc.writeSharedArray(GlobalArray.GROUP_INSTRUCTIONS + GlobalArray.groupId - GlobalArray.GROUP_OFFSET, GlobalArray.intifyTarget(GlobalArray.OPPO_FLAG_CUR_LOC + flagIndex));
        if (!rc.hasFlag()) {
            rc.writeSharedArray(Comms.OPPO_FLAG_DEF_LOC + flagIndex, 1);
            rc.writeSharedArray(Comms.OPPO_FLAG_CUR_LOC + flagIndex, 1);
            flagIndex = -1;
        }
        return;
    }
    protected static void jailed() throws GameActionException {
        if (flagIndex != -1) {
            // rc.writeSharedArray(GlobalArray.GROUP_INSTRUCTIONS + GlobalArray.groupId - GlobalArray.GROUP_OFFSET, 0);
            rc.writeSharedArray(Comms.OPPO_FLAG_CUR_LOC + flagIndex, rc.readSharedArray(Comms.OPPO_FLAG_DEF_LOC + flagIndex));
            flagIndex = -1;
        }
    }
}
