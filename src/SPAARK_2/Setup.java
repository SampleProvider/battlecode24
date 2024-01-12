package SPAARK_2;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Setup {
    public static RobotController rc;
    public static StringBuilder indicatorString;

    public static Random rng;

    public static int flagIndex = -1;
    public static MapLocation[] placementLocationsOne = {
        new MapLocation(5, 5),
        new MapLocation(-5, -5),
        new MapLocation(0, 10),
        new MapLocation(0, -10),
    };
    public static MapLocation[] placementLocationsTwo = {
        new MapLocation(-5, 5),
        new MapLocation(5, -5),
        new MapLocation(10, 0),
        new MapLocation(-10, 0),
    };
    public static MapLocation flagOffset = new MapLocation(-100, -100);
    
    public static void run() throws GameActionException {
        FlagInfo[] flags = rc.senseNearbyFlags(-1, rc.getTeam());
        FlagInfo closestFlag = Motion.getClosestFlag(flags, false);
        if (closestFlag != null && rc.canPickupFlag(closestFlag.getLocation())) {
            if (!GlobalArray.hasLocation(rc.readSharedArray(0)) || !GlobalArray.hasLocation(rc.readSharedArray(1)) || !GlobalArray.hasLocation(rc.readSharedArray(2))) {
                // rc.pickupFlag(closestFlag.getLocation());
            }
        }
        if (rc.hasFlag()) {
            if (flagIndex == -1) {
                int flagId = rc.senseNearbyFlags(0, rc.getTeam())[0].getID();
                for (int i = 0; i <= 2; i++) {
                    if (rc.readSharedArray(i) == 0) {
                        rc.writeSharedArray(i, flagId);
                        flagIndex = i;
                        break;
                    }
                    else if (rc.readSharedArray(i) == flagId) {
                        flagIndex = i;
                        break;
                    }
                }
            }
            if (!GlobalArray.hasLocation(rc.readSharedArray(15))) {
                //set flag target
                MapLocation[] spawns = rc.getAllySpawnLocations();
                rc.writeSharedArray(15, GlobalArray.intifyLocation(Motion.getFarthest(spawns, new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2))));
            }
            MapLocation flagTarget = GlobalArray.parseLocation(rc.readSharedArray(15));
            MapLocation toPlace = new MapLocation(flagTarget.x+flagOffset.x, flagTarget.y+flagOffset.y);
            if (flagOffset.x == -100) {
                switch (flagIndex) {
                    case 0:
                        flagOffset = new MapLocation(0, 0);
                        break;
                    case 1:
                        for (MapLocation loc : placementLocationsOne) {
                            flagOffset = loc;
                            toPlace = new MapLocation(flagTarget.x+flagOffset.x, flagTarget.y+flagOffset.y);
                            if (toPlace.x>= 0 && toPlace.x <= rc.getMapWidth() && toPlace.y >= 0 && toPlace.y <= rc.getMapHeight()) {
                                break;
                            }
                        }
                        break;
                    case 2:
                        for (MapLocation loc : placementLocationsTwo) {
                            flagOffset = loc;
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
                if (!rc.senseLegalStartingFlagPlacement(toPlace)) {
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
            if (rc.canDropFlag(toPlace)) {
                rc.dropFlag(toPlace);
                rc.writeSharedArray(flagIndex + 3, GlobalArray.intifyLocation(toPlace));
                rc.writeSharedArray(flagIndex + 6, GlobalArray.intifyLocation(toPlace));
                flagIndex = -1;
            }
            else {
                rc.setIndicatorLine(me, toPlace, 255, 255, 255);
                indicatorString.append("FLAG"+flagIndex+"->("+(flagTarget.x+flagOffset.x)+","+(flagTarget.y+flagOffset.y)+");");
                rc.writeSharedArray(flagIndex + 3, (1 << 13) | GlobalArray.intifyLocation(rc.getLocation()));
            }
        }
        else {
            MapInfo[] info = rc.senseNearbyMapInfos();
            Boolean action = false;
            for (MapInfo i : info) {
                if (i.getCrumbs() > 0) {
                    Motion.bugnavTowards(i.getMapLocation(), 500);
                    indicatorString.append("CRUMB("+i.getMapLocation().x+","+i.getMapLocation().y+");");
                    action = true;
                    break;
                }
            }
            if (!action) {
                int meetingPt = rc.readSharedArray(16);
                if (GlobalArray.hasLocation(meetingPt)) {
                    Motion.bugnavTowards(GlobalArray.parseLocation(meetingPt), 500);
                    indicatorString.append("MEET("+GlobalArray.parseLocation(meetingPt).x+","+GlobalArray.parseLocation(meetingPt).y+");");
                } else {
                    for (MapInfo i : info) {
                        if (!i.isPassable() && !i.isWall() && !i.isWater()) {
                            rc.writeSharedArray(16, GlobalArray.intifyLocation(rc.getLocation()));
                            action = true;
                        }
                    }
                }
                if (!action) {
                    Motion.moveRandomly();
                }
            }
        }
    }
    public static void jailed() throws GameActionException {
    }
}
