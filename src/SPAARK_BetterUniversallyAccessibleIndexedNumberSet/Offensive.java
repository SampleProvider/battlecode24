package SPAARK_BetterUniversallyAccessibleIndexedNumberSet;

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
        rc.setIndicatorDot(me, 0, 255, Math.min((UniversallyAccessibleIndexedNumberSet.groupId - 2) * 32, 255));

        FlagInfo closestFlag = Motion.getClosestFlag(opponentFlags, false);
        if (closestFlag != null && rc.canPickupFlag(closestFlag.getLocation())) {
            rc.pickupFlag(closestFlag.getLocation());
            int flagId = closestFlag.getID();
            for (int i = 0; i <= 2; i++) {
                if (rc.readSharedArray(UniversallyAccessibleIndexedNumberSet.OPPO_FLAG_ID + i) == 0) {
                    flagIndex = i;
                    rc.writeSharedArray(UniversallyAccessibleIndexedNumberSet.OPPO_FLAG_ID + i, flagId);
                    break;
                }
                else if (rc.readSharedArray(UniversallyAccessibleIndexedNumberSet.OPPO_FLAG_ID + i) == flagId) {
                    flagIndex = i;
                    break;
                }
            }
        }

        // writing flags to global array
        opponentFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        FlagInfo[] friendlyFlags = rc.senseNearbyFlags(-1, rc.getTeam());
        for (FlagInfo flag : friendlyFlags) {
            UniversallyAccessibleIndexedNumberSet.writeFlag(flag);
        }
        for (FlagInfo flag : opponentFlags) {
            UniversallyAccessibleIndexedNumberSet.writeFlag(flag);
        }
        UniversallyAccessibleIndexedNumberSet.checkFlags(opponentFlags);
        // GlobalArray.updateSector();
        Motion.updateBfsMap();

        // flagIndex: index of flag currently holding in global array
        if (flagIndex != -1) {
            // navigate back to spawn
            MapLocation[] spawnLocs = rc.getAllySpawnLocations();
            MapLocation bestLoc = Motion.getSafest(spawnLocs);
            rc.setIndicatorDot(me, 255, 0, 0);
            rc.setIndicatorLine(me, bestLoc, 255, 0, 0);
            Motion.bfsnav(bestLoc, 1000);
            rc.writeSharedArray(UniversallyAccessibleIndexedNumberSet.GROUP_INSTRUCTIONS + UniversallyAccessibleIndexedNumberSet.groupId - UniversallyAccessibleIndexedNumberSet.GROUP_OFFSET, UniversallyAccessibleIndexedNumberSet.intifyTarget(UniversallyAccessibleIndexedNumberSet.OPPO_FLAG_CUR_LOC + flagIndex));
            if (!rc.hasFlag()) {
                rc.writeSharedArray(UniversallyAccessibleIndexedNumberSet.OPPO_FLAG_DEF_LOC + flagIndex, 1);
                rc.writeSharedArray(UniversallyAccessibleIndexedNumberSet.OPPO_FLAG_CUR_LOC + flagIndex, 1);
                flagIndex = -1;
            }
        }
        else {
            MapLocation target = UniversallyAccessibleIndexedNumberSet.getGroupTarget(UniversallyAccessibleIndexedNumberSet.groupId);

            boolean findingTarget = false;

            if (target != null) {
                int n = rc.readSharedArray(UniversallyAccessibleIndexedNumberSet.GROUP_INSTRUCTIONS + UniversallyAccessibleIndexedNumberSet.groupId - UniversallyAccessibleIndexedNumberSet.GROUP_OFFSET);
                if (UniversallyAccessibleIndexedNumberSet.isGlobalArrayLoc(n)) {
                    int i = n & 0b111111;
                    indicatorString.append("INDEX=" + i + " ");
                    if (i >= UniversallyAccessibleIndexedNumberSet.ALLY_FLAG_CUR_LOC && i <= UniversallyAccessibleIndexedNumberSet.ALLY_FLAG_CUR_LOC + 2) {
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
                    rc.writeSharedArray(UniversallyAccessibleIndexedNumberSet.GROUP_INSTRUCTIONS + UniversallyAccessibleIndexedNumberSet.groupId - UniversallyAccessibleIndexedNumberSet.GROUP_OFFSET, 0);
                    return;
                }
                int n = rc.readSharedArray(UniversallyAccessibleIndexedNumberSet.GROUP_INSTRUCTIONS + UniversallyAccessibleIndexedNumberSet.groupId - UniversallyAccessibleIndexedNumberSet.GROUP_OFFSET);
                if (UniversallyAccessibleIndexedNumberSet.isGlobalArrayLoc(n) && (n & 0b111111) >= UniversallyAccessibleIndexedNumberSet.OPPO_FLAG_CUR_LOC && (n & 0b111111) <= UniversallyAccessibleIndexedNumberSet.OPPO_FLAG_CUR_LOC + 2 && UniversallyAccessibleIndexedNumberSet.isFlagPickedUp(rc.readSharedArray(n & 0b111111))) {
                    Motion.bugnavAround(target, 4, 20);
                }
                else {
                    Motion.bfsnav(target);
                }
                rc.setIndicatorLine(rc.getLocation(), target, 255, 255, 255);
            }
            else {
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
                    if (closestFlag != null) {
                        // Motion.bugnavTowards(closestFlag.getLocation());
                        rc.writeSharedArray(UniversallyAccessibleIndexedNumberSet.GROUP_INSTRUCTIONS + UniversallyAccessibleIndexedNumberSet.groupId - UniversallyAccessibleIndexedNumberSet.GROUP_OFFSET, UniversallyAccessibleIndexedNumberSet.intifyLocation(closestFlag.getLocation()));
                    }
                    else {
                        int closestStoredFlagIndex = -1;
                        MapLocation closestStoredFlag = null;
                        for (int i = 0; i <= 2; i++) {
                            int n = rc.readSharedArray(UniversallyAccessibleIndexedNumberSet.OPPO_FLAG_CUR_LOC + i);
                            int n2 = rc.readSharedArray(UniversallyAccessibleIndexedNumberSet.OPPO_FLAG_DEF_LOC + i);
                            // if (GlobalArray.hasLocation(n) && !GlobalArray.isFlagPickedUp(n)) {
                            if (UniversallyAccessibleIndexedNumberSet.hasLocation(n)) {
                                MapLocation loc = UniversallyAccessibleIndexedNumberSet.parseLocation(n2);
                                if (closestStoredFlag == null || me.distanceSquaredTo(closestStoredFlag) > me.distanceSquaredTo(loc)) {
                                    closestStoredFlag = loc;
                                    closestStoredFlagIndex = UniversallyAccessibleIndexedNumberSet.OPPO_FLAG_CUR_LOC + i;
                                }
                            }
                        }
                        if (closestStoredFlag != null) {
                            rc.writeSharedArray(UniversallyAccessibleIndexedNumberSet.GROUP_INSTRUCTIONS + UniversallyAccessibleIndexedNumberSet.groupId - UniversallyAccessibleIndexedNumberSet.GROUP_OFFSET, UniversallyAccessibleIndexedNumberSet.intifyTarget(closestStoredFlagIndex));
                            // rc.writeSharedArray(GlobalArray.GROUP_INSTRUCTIONS + GlobalArray.groupId - GlobalArray.GROUP_OFFSET, GlobalArray.intifyLocation(closestStoredFlag));
                        }
                        else {
                            MapLocation[] hiddenFlags = rc.senseBroadcastFlagLocations();
                            if (hiddenFlags.length > 0) {
                                MapLocation closestHiddenFlag = Motion.getClosest(hiddenFlags);
                                rc.writeSharedArray(UniversallyAccessibleIndexedNumberSet.GROUP_INSTRUCTIONS + UniversallyAccessibleIndexedNumberSet.groupId - UniversallyAccessibleIndexedNumberSet.GROUP_OFFSET, UniversallyAccessibleIndexedNumberSet.intifyLocation(closestHiddenFlag));
                                // Motion.bugnavTowards(closestHiddenFlag);
                            }
                            else {
                                closestStoredFlag = null;
                                for (int i = 0; i <= 2; i++) {
                                    int n = rc.readSharedArray(UniversallyAccessibleIndexedNumberSet.OPPO_FLAG_CUR_LOC + i);
                                    if (UniversallyAccessibleIndexedNumberSet.hasLocation(n) && !UniversallyAccessibleIndexedNumberSet.isFlagPickedUp(n)) {
                                        MapLocation loc = UniversallyAccessibleIndexedNumberSet.parseLocation(n);
                                        if (closestStoredFlag == null || me.distanceSquaredTo(closestStoredFlag) > me.distanceSquaredTo(loc)) {
                                            closestStoredFlag = loc;
                                            closestStoredFlagIndex = UniversallyAccessibleIndexedNumberSet.OPPO_FLAG_CUR_LOC + i;
                                        }
                                    }
                                }
                                if (closestStoredFlag != null) {
                                    rc.writeSharedArray(UniversallyAccessibleIndexedNumberSet.GROUP_INSTRUCTIONS + UniversallyAccessibleIndexedNumberSet.groupId - UniversallyAccessibleIndexedNumberSet.GROUP_OFFSET, UniversallyAccessibleIndexedNumberSet.intifyTarget(closestStoredFlagIndex));
                                }
                                else {
                                    MapLocation closestStolenFlag = null;
                                    int closestStolenFlagIndex = -1;
                                    for (int i = 0; i <= 2; i++) {
                                        int n = rc.readSharedArray(UniversallyAccessibleIndexedNumberSet.ALLY_FLAG_CUR_LOC + i);
                                        if (UniversallyAccessibleIndexedNumberSet.hasLocation(n) && UniversallyAccessibleIndexedNumberSet.isFlagInDanger(n)) {
                                            MapLocation loc = UniversallyAccessibleIndexedNumberSet.parseLocation(n);
                                            if (closestStolenFlag == null || me.distanceSquaredTo(closestStolenFlag) > me.distanceSquaredTo(loc)) {
                                                closestStolenFlag = loc;
                                                closestStolenFlagIndex = UniversallyAccessibleIndexedNumberSet.ALLY_FLAG_CUR_LOC + i;
                                            }
                                        }
                                    }
                                    if (closestStolenFlag != null) {
                                        rc.writeSharedArray(UniversallyAccessibleIndexedNumberSet.GROUP_INSTRUCTIONS + UniversallyAccessibleIndexedNumberSet.groupId - UniversallyAccessibleIndexedNumberSet.GROUP_OFFSET, UniversallyAccessibleIndexedNumberSet.intifyTarget(closestStolenFlagIndex));
                                    }
                                    else {
                                        Motion.bfsnav(new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2));
                                    }
                                }
                                // Motion.moveRandomly();
                            }
                        }
                    }
                }
            }

            indicatorString.append("GROUP:" + UniversallyAccessibleIndexedNumberSet.groupId);
        }
        
        opponentFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        friendlyFlags = rc.senseNearbyFlags(-1, rc.getTeam());
        for (FlagInfo flag : friendlyFlags) {
            UniversallyAccessibleIndexedNumberSet.writeFlag(flag);
        }
        for (FlagInfo flag : opponentFlags) {
            UniversallyAccessibleIndexedNumberSet.writeFlag(flag);
        }
        UniversallyAccessibleIndexedNumberSet.checkFlags(opponentFlags);
        closestFlag = Motion.getClosestFlag(opponentFlags, false);
        if (closestFlag != null && rc.canPickupFlag(closestFlag.getLocation())) {
            rc.pickupFlag(closestFlag.getLocation());
            int flagId = closestFlag.getID();
            for (int i = 0; i <= 2; i++) {
                if (rc.readSharedArray(UniversallyAccessibleIndexedNumberSet.OPPO_FLAG_ID + i) == 0) {
                    flagIndex = i;
                    rc.writeSharedArray(UniversallyAccessibleIndexedNumberSet.OPPO_FLAG_ID + i, flagId);
                    break;
                }
                else if (rc.readSharedArray(UniversallyAccessibleIndexedNumberSet.OPPO_FLAG_ID + i) == flagId) {
                    flagIndex = i;
                    break;
                }
            }
        }

        // GlobalArray.updateSector();
        Attack.attack();
        Attack.heal();
    }
    protected static void jailed() throws GameActionException {
        if (flagIndex != -1) {
            rc.writeSharedArray(UniversallyAccessibleIndexedNumberSet.GROUP_INSTRUCTIONS + UniversallyAccessibleIndexedNumberSet.groupId - UniversallyAccessibleIndexedNumberSet.GROUP_OFFSET, 0);
            rc.writeSharedArray(UniversallyAccessibleIndexedNumberSet.OPPO_FLAG_CUR_LOC + flagIndex, rc.readSharedArray(UniversallyAccessibleIndexedNumberSet.OPPO_FLAG_DEF_LOC + flagIndex));
            flagIndex = -1;
        }
    }
}
