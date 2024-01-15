package SPAARKSPRINT1;

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
        
        // indicator thingy
        rc.setIndicatorDot(me, 0, 255, Math.min((GlobalArray.groupId - 2) * 32, 255));

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
        GlobalArray.checkFlags(opponentFlags);
        GlobalArray.updateSector();

        // flagIndex: index of flag currently holding in global array
        if (flagIndex != -1) {
            // navigate back to spawn
            MapLocation[] spawnLocs = rc.getAllySpawnLocations();
            MapLocation bestLoc = Motion.getSafest(spawnLocs);
            rc.setIndicatorDot(me, 255, 0, 0);
            rc.setIndicatorLine(me, bestLoc, 255, 0, 0);
            Motion.bugnavTowards(bestLoc, 1000);
            rc.writeSharedArray(GlobalArray.GROUP_INSTRUCTIONS + GlobalArray.groupId - GlobalArray.GROUP_OFFSET, GlobalArray.intifyTarget(GlobalArray.OPPO_FLAG_CUR_LOC + flagIndex));
            if (!rc.hasFlag()) {
                rc.writeSharedArray(GlobalArray.OPPO_FLAG_DEF_LOC + flagIndex, 1);
                rc.writeSharedArray(GlobalArray.OPPO_FLAG_CUR_LOC + flagIndex, 1);
                flagIndex = -1;
            }
        }
        else {
            MapLocation target = GlobalArray.getGroupTarget(GlobalArray.groupId);

            boolean findingTarget = false;

            if (target != null) {
                int n = rc.readSharedArray(GlobalArray.GROUP_INSTRUCTIONS + GlobalArray.groupId - GlobalArray.GROUP_OFFSET);
                if (GlobalArray.isGlobalArrayLoc(n)) {
                    int i = n & 0b111111;
                    indicatorString.append("INDEX=" + i + " ");
                    if (i >= GlobalArray.ALLY_FLAG_CUR_LOC && i <= GlobalArray.ALLY_FLAG_CUR_LOC + 2) {
                        if (rc.getLocation().distanceSquaredTo(target) <= 2 && rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length == 0) {
                            boolean seesFlag = false;
                            for (FlagInfo flag : friendlyFlags) {
                                if (flag.getID() == rc.readSharedArray(i - 6)) {
                                    seesFlag = true;
                                    break;
                                }
                            }
                            if (seesFlag == false) {
                                findingTarget = true;
                                if (target.distanceSquaredTo(turnsFindingFlagTarget) > 2) {
                                    turnsFindingFlag = 0;
                                }
                                
                                turnsFindingFlag += 1;

                                // MapLocation offset = GlobalArray.getRobotDirection(rc.readSharedArray(i));
                                // indicatorString.append(offset);
                                // Motion.bugnavTowards(target.translate(offset.x, offset.y));
                                // target = target.add(me.directionTo(rc.getLocation()));
                                turnsFindingFlagTarget = target;

                                if (!rc.onTheMap(target) || turnsFindingFlag >= 10) {
                                    rc.writeSharedArray(i, 0);
                                    target = null;
                                }
                                // else {
                                //     rc.writeSharedArray(GlobalArray.GROUP_INSTRUCTIONS + GlobalArray.groupId - GlobalArray.GROUP_OFFSET, GlobalArray.intifyLocation(target));
                                // }
                            }
                        }
                    }
                }
            }

            if (!findingTarget) {
                turnsFindingFlag = 0;
            }
            
            if (target != null) {
                if (rc.getLocation().equals(target)) {
                    rc.writeSharedArray(GlobalArray.GROUP_INSTRUCTIONS + GlobalArray.groupId - GlobalArray.GROUP_OFFSET, 0);
                    return;
                }
                int n = rc.readSharedArray(GlobalArray.GROUP_INSTRUCTIONS + GlobalArray.groupId - GlobalArray.GROUP_OFFSET);
                if (GlobalArray.isGlobalArrayLoc(n) && (n & 0b111111) >= GlobalArray.OPPO_FLAG_CUR_LOC && (n & 0b111111) <= GlobalArray.OPPO_FLAG_CUR_LOC + 2 && GlobalArray.isFlagPickedUp(rc.readSharedArray(n & 0b111111))) {
                    Motion.bugnavAround(target, 4, 20);
                }
                else {
                    Motion.bugnavTowards(target);
                }
                rc.setIndicatorLine(rc.getLocation(), target, 255, 255, 255);
                indicatorString.append(GlobalArray.groupId);
            }
            else {
                boolean action = false;
                // crumb stuff if not already done
                if (!action) {
                    MapInfo[] info = rc.senseNearbyMapInfos();
                    for (MapInfo i : info) {
                        if (i.getCrumbs() > 0) {
                            Motion.bugnavTowards(i.getMapLocation());
                            indicatorString.append("CRUMB("+i.getMapLocation().x+","+i.getMapLocation().y+");");
                            action = true;
                            break;
                        }
                    }
                }
                
                if (!action) {
                    if (closestFlag != null) {
                        // Motion.bugnavTowards(closestFlag.getLocation());
                        rc.writeSharedArray(GlobalArray.GROUP_INSTRUCTIONS + GlobalArray.groupId - GlobalArray.GROUP_OFFSET, GlobalArray.intifyLocation(closestFlag.getLocation()));
                    }
                    else {
                        int closestStoredFlagIndex = -1;
                        MapLocation closestStoredFlag = null;
                        for (int i = 0; i <= 2; i++) {
                            int n = rc.readSharedArray(GlobalArray.OPPO_FLAG_CUR_LOC + i);
                            int n2 = rc.readSharedArray(GlobalArray.OPPO_FLAG_DEF_LOC + i);
                            // if (GlobalArray.hasLocation(n) && !GlobalArray.isFlagPickedUp(n)) {
                            if (GlobalArray.hasLocation(n)) {
                                MapLocation loc = GlobalArray.parseLocation(n2);
                                if (closestStoredFlag == null || me.distanceSquaredTo(closestStoredFlag) > me.distanceSquaredTo(loc)) {
                                    closestStoredFlag = loc;
                                    closestStoredFlagIndex = GlobalArray.OPPO_FLAG_CUR_LOC + i;
                                }
                            }
                        }
                        if (closestStoredFlag != null) {
                            rc.writeSharedArray(GlobalArray.GROUP_INSTRUCTIONS + GlobalArray.groupId - GlobalArray.GROUP_OFFSET, GlobalArray.intifyTarget(closestStoredFlagIndex));
                            // rc.writeSharedArray(GlobalArray.GROUP_INSTRUCTIONS + GlobalArray.groupId - GlobalArray.GROUP_OFFSET, GlobalArray.intifyLocation(closestStoredFlag));
                        }
                        else {
                            MapLocation[] hiddenFlags = rc.senseBroadcastFlagLocations();
                            if (hiddenFlags.length > 0) {
                                MapLocation closestHiddenFlag = Motion.getClosest(hiddenFlags);
                                rc.writeSharedArray(GlobalArray.GROUP_INSTRUCTIONS + GlobalArray.groupId - GlobalArray.GROUP_OFFSET, GlobalArray.intifyLocation(closestHiddenFlag));
                                // Motion.bugnavTowards(closestHiddenFlag);
                            }
                            else {
                                closestStoredFlag = null;
                                for (int i = 0; i <= 2; i++) {
                                    int n = rc.readSharedArray(GlobalArray.OPPO_FLAG_CUR_LOC + i);
                                    if (GlobalArray.hasLocation(n) && !GlobalArray.isFlagPickedUp(n)) {
                                        MapLocation loc = GlobalArray.parseLocation(n);
                                        if (closestStoredFlag == null || me.distanceSquaredTo(closestStoredFlag) > me.distanceSquaredTo(loc)) {
                                            closestStoredFlag = loc;
                                            closestStoredFlagIndex = GlobalArray.OPPO_FLAG_CUR_LOC + i;
                                        }
                                    }
                                }
                                if (closestStoredFlag != null) {
                                    rc.writeSharedArray(GlobalArray.GROUP_INSTRUCTIONS + GlobalArray.groupId - GlobalArray.GROUP_OFFSET, GlobalArray.intifyTarget(closestStoredFlagIndex));
                                }
                                else {
                                    MapLocation closestStolenFlag = null;
                                    int closestStolenFlagIndex = -1;
                                    for (int i = 0; i <= 2; i++) {
                                        int n = rc.readSharedArray(GlobalArray.ALLY_FLAG_CUR_LOC + i);
                                        if (GlobalArray.hasLocation(n) && GlobalArray.isFlagInDanger(n)) {
                                            MapLocation loc = GlobalArray.parseLocation(n);
                                            if (closestStolenFlag == null || me.distanceSquaredTo(closestStolenFlag) > me.distanceSquaredTo(loc)) {
                                                closestStolenFlag = loc;
                                                closestStolenFlagIndex = GlobalArray.ALLY_FLAG_CUR_LOC + i;
                                            }
                                        }
                                    }
                                    if (closestStolenFlag != null) {
                                        rc.writeSharedArray(GlobalArray.GROUP_INSTRUCTIONS + GlobalArray.groupId - GlobalArray.GROUP_OFFSET, GlobalArray.intifyTarget(closestStolenFlagIndex));
                                    }
                                    else {
                                        Motion.bugnavTowards(new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2));
                                    }
                                }
                                // Motion.moveRandomly();
                            }
                        }
                    }
                }
            }

            indicatorString.append("GROUP:" + GlobalArray.groupId);
        }
        
        opponentFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        friendlyFlags = rc.senseNearbyFlags(-1, rc.getTeam());
        for (FlagInfo flag : friendlyFlags) {
            GlobalArray.writeFlag(flag);
        }
        for (FlagInfo flag : opponentFlags) {
            GlobalArray.writeFlag(flag);
        }
        GlobalArray.checkFlags(opponentFlags);
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

        GlobalArray.updateSector();
        Attack.attack();
        Attack.heal();
    }
    protected static void jailed() throws GameActionException {
        if (flagIndex != -1) {
            rc.writeSharedArray(GlobalArray.GROUP_INSTRUCTIONS + GlobalArray.groupId - GlobalArray.GROUP_OFFSET, 0);
            rc.writeSharedArray(GlobalArray.OPPO_FLAG_CUR_LOC + flagIndex, rc.readSharedArray(GlobalArray.OPPO_FLAG_DEF_LOC + flagIndex));
            flagIndex = -1;
        }
    }
}
