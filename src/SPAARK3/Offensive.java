package SPAARK3;

import battlecode.common.*;

import java.util.Random;

public class Offensive {
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
    protected static MapLocation turnsFindingFlagTarget = new MapLocation(0, 0);
    
    protected static void run() throws GameActionException {
        // capturing opponent flags
        MapLocation me = rc.getLocation();
        FlagInfo[] opponentFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        
        FlagInfo closestFlag = Motion.getClosestFlag(opponentFlags, false);
        if (closestFlag != null && rc.canPickupFlag(closestFlag.getLocation())) {
            rc.pickupFlag(closestFlag.getLocation());
            int flagId = closestFlag.getID();
            for (int i = 0; i <= 2; i++) {
                if (rc.readSharedArray(GlobalArray.OPPO_FLAG_ID + i) == 0) {
                    flagIndex = i;
                    rc.writeSharedArray(GlobalArray.OPPO_FLAG_ID + i, flagId);
                    break;
                }
                else if (rc.readSharedArray(GlobalArray.OPPO_FLAG_ID + i) == flagId) {
                    flagIndex = i;
                    break;
                }
            }
        }

        // writing flags to global array
        opponentFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        FlagInfo[] friendlyFlags = rc.senseNearbyFlags(-1, rc.getTeam());
        for (FlagInfo flag : friendlyFlags) {
            GlobalArray.writeFlag(flag);
        }
        for (FlagInfo flag : opponentFlags) {
            GlobalArray.writeFlag(flag);
        }
        GlobalArray.checkFlags(friendlyFlags, opponentFlags);
        // GlobalArray.updateSector();
        GlobalArray.updatePOI();
        Motion.updateBfsMap();

        // flagIndex: index of flag currently holding in global array
        if (flagIndex != -1) {
            // navigate back to spawn
            MapLocation[] spawnLocs = rc.getAllySpawnLocations();
            MapLocation bestLoc = Motion.getClosest(spawnLocs);
            rc.setIndicatorDot(me, 255, 0, 0);
            rc.setIndicatorLine(me, bestLoc, 255, 0, 0);
            Motion.bfsnav(bestLoc, 1000);
            // rc.writeSharedArray(GlobalArray.GROUP_INSTRUCTIONS + GlobalArray.groupId - GlobalArray.GROUP_OFFSET, GlobalArray.intifyTarget(GlobalArray.OPPO_FLAG_CUR_LOC + flagIndex));
            if (!rc.hasFlag()) {
                rc.writeSharedArray(GlobalArray.OPPO_FLAG_DEF_LOC + flagIndex, 1);
                rc.writeSharedArray(GlobalArray.OPPO_FLAG_CUR_LOC + flagIndex, 1);
                flagIndex = -1;
            }
        }
        else {
            // MapLocation target = GlobalArray.getGroupTarget(GlobalArray.groupId);
            boolean action = false;
            // crumb stuff if not already done
            if (!action) {
                MapInfo[] info = rc.senseNearbyMapInfos();
                for (MapInfo i : info) {
                    if (i.getCrumbs() > 0) {
                        Motion.bfsnav(i.getMapLocation());
                        indicatorString.append("CRUMB("+i.getMapLocation().x+","+i.getMapLocation().y+");");
                        action = true;
                        break;
                    }
                }
            }
            if (!action) {
                MapLocation best = GlobalArray.getBestPOI();
                MapLocation target = null;
                if (best != null) {
                    target = best;
                    // target = GlobalArray.parseLocation(rc.readSharedArray(GlobalArray.POI + best));
                }

                if (target == null && closestFlag != null) {
                    target = closestFlag.getLocation();
                }
                if (target == null) {
                    MapLocation closestStoredFlag = null;
                    for (int i = 0; i <= 2; i++) {
                        int n = rc.readSharedArray(GlobalArray.OPPO_FLAG_CUR_LOC + i);
                        // int n2 = rc.readSharedArray(GlobalArray.OPPO_FLAG_DEF_LOC + i);
                        if (GlobalArray.hasLocation(n)) {
                            MapLocation loc = GlobalArray.parseLocation(n);
                            if (closestStoredFlag == null || me.distanceSquaredTo(closestStoredFlag) > me.distanceSquaredTo(loc)) {
                                closestStoredFlag = loc;
                            }
                        }
                    }
                    if (closestStoredFlag != null) {
                        Motion.bugnavAround(closestStoredFlag, 4, 10);
                        rc.setIndicatorLine(rc.getLocation(), closestStoredFlag, 200, 200, 200);
                    }
                }
                if (target == null) {
                    MapLocation[] hiddenFlags = rc.senseBroadcastFlagLocations();
                    if (hiddenFlags.length > 0) {
                        MapLocation closestHiddenFlag = Motion.getClosest(hiddenFlags);
                        target = closestHiddenFlag;
                    }
                }
            
                if (target != null) {
                    Motion.bfsnav(target);
                    rc.setIndicatorLine(rc.getLocation(), target, 255, 255, 255);
                }
            }

            // indicatorString.append("GROUP:" + GlobalArray.groupId);
        }
        
        opponentFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        friendlyFlags = rc.senseNearbyFlags(-1, rc.getTeam());
        for (FlagInfo flag : friendlyFlags) {
            GlobalArray.writeFlag(flag);
        }
        for (FlagInfo flag : opponentFlags) {
            GlobalArray.writeFlag(flag);
        }
        // GlobalArray.checkFlags(opponentFlags);
        closestFlag = Motion.getClosestFlag(opponentFlags, false);
        if (closestFlag != null && rc.canPickupFlag(closestFlag.getLocation())) {
            rc.pickupFlag(closestFlag.getLocation());
            int flagId = closestFlag.getID();
            for (int i = 0; i <= 2; i++) {
                if (rc.readSharedArray(GlobalArray.OPPO_FLAG_ID + i) == 0) {
                    flagIndex = i;
                    rc.writeSharedArray(GlobalArray.OPPO_FLAG_ID + i, flagId);
                    break;
                }
                else if (rc.readSharedArray(GlobalArray.OPPO_FLAG_ID + i) == flagId) {
                    flagIndex = i;
                    break;
                }
            }
        }

        // GlobalArray.updateSector();
        // Attack.attack();
        Attack.heal();
    }
    protected static void jailed() throws GameActionException {
        if (flagIndex != -1) {
            // rc.writeSharedArray(GlobalArray.GROUP_INSTRUCTIONS + GlobalArray.groupId - GlobalArray.GROUP_OFFSET, 0);
            rc.writeSharedArray(GlobalArray.OPPO_FLAG_CUR_LOC + flagIndex, rc.readSharedArray(GlobalArray.OPPO_FLAG_DEF_LOC + flagIndex));
            flagIndex = -1;
        }
    }
}
