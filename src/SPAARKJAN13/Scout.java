package SPAARKJAN13;

import battlecode.common.*;

import java.util.Random;

public class Scout {
    protected static RobotController rc;
    protected static StringBuilder indicatorString;

    protected static Random rng;

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
    protected static int flagIndex = -1;
    
    protected static int targetSector = -1;
    protected static int fullMappingSector = -1;
    
    protected static void run() throws GameActionException {
        MapLocation me = rc.getLocation();

        rc.setIndicatorDot(me, 255, 0, 255);

        // try to sneak flags back (call for help?)
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
        opponentFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());

        FlagInfo[] friendlyFlags = rc.senseNearbyFlags(-1, rc.getTeam());
        for (FlagInfo flag : friendlyFlags) {
            GlobalArray.writeFlag(flag);
        }
        for (FlagInfo flag : opponentFlags) {
            GlobalArray.writeFlag(flag);
        }

        String updatedSectors = GlobalArray.updateSector();

        if (flagIndex != -1) {
            // navigate back to spawn
            MapLocation[] spawnLocs = rc.getAllySpawnLocations();
            MapLocation bestLoc = Motion.getSafest(spawnLocs);
            rc.setIndicatorDot(bestLoc, 100, 100, 100);
            Motion.bugnavTowards(bestLoc, 1000);
            if (!rc.hasFlag()) {
                rc.writeSharedArray(GlobalArray.OPPO_FLAG_DEF_LOC + flagIndex, 0);
                rc.writeSharedArray(GlobalArray.OPPO_FLAG_CUR_LOC + flagIndex, 0);
                flagIndex = -1;
            }
        }
        else {
            Boolean action = false;

            // go to flag!
            if (!action) {
                if (closestFlag != null) {
                    Motion.bugnavTowards(closestFlag.getLocation(), Motion.DEFAULT_RETREAT_HP);
                    action = true;
                }
            }
            // crumb stuff if not already done
            if (!action) {
                MapInfo[] info = rc.senseNearbyMapInfos();
                for (MapInfo i : info) {
                    if (i.getCrumbs() > 0) {
                        Motion.bugnavTowards(i.getMapLocation(), Motion.DEFAULT_RETREAT_HP);
                        indicatorString.append("CRUMB("+i.getMapLocation().x+","+i.getMapLocation().y+");");
                        action = true;
                        break;
                    }
                }
            }
            if (!action) {
                // go to sectors that haven't been updated recently
                if (updatedSectors.contains(String.valueOf(targetSector) + "A")) {
                    targetSector = -1;
                }
                if (targetSector == -1) {
                    int currentSector = GlobalArray.locationToSector(me);
                    int x = currentSector % GlobalArray.SECTOR_SIZE;
                    int y = currentSector / GlobalArray.SECTOR_SIZE;
                    int maxTime = -1;
                    for (int i = Math.max(y - 1, 0); i <= Math.min(y + 1, 4); i++) {
                        for (int j = Math.max(x - 1, 0); j <= Math.min(x + 1, 4); j++) {
                            if (i == y && j == x) {
                                continue;
                            }
                            int time = GlobalArray.getTimeSinceLastExplored(rc.readSharedArray(GlobalArray.SECTOR_START + i * 5 + j));
                            if (time > maxTime) {
                                maxTime = time;
                                targetSector = i * 5 + j;
                            }
                        }
                    }
                }
                rc.setIndicatorLine(me, GlobalArray.sectorToLocation(targetSector), 0, 255, 0);
                Motion.bugnavTowards(GlobalArray.sectorToLocation(targetSector), Motion.DEFAULT_RETREAT_HP);
            }
        }


        opponentFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        friendlyFlags = rc.senseNearbyFlags(-1, rc.getTeam());
        for (FlagInfo flag : friendlyFlags) {
            GlobalArray.writeFlag(flag);
        }
        for (FlagInfo flag : opponentFlags) {
            GlobalArray.writeFlag(flag);
        }

        GlobalArray.updateSector();

        Attack.attack();
        Attack.heal();
    }
    protected static void jailed() throws GameActionException {
        if (flagIndex != -1) {
            rc.writeSharedArray(GlobalArray.OPPO_FLAG_CUR_LOC + flagIndex, rc.readSharedArray(GlobalArray.OPPO_FLAG_DEF_LOC + flagIndex));
            flagIndex = -1;
        }
        targetSector = -1;
    }
}
