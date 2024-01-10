package bad_bot;

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
    
    public static void run() throws GameActionException {
        MapLocation[] crumbs = rc.senseNearbyCrumbs(-1);
        if (crumbs.length > 0) {
            Motion.bug2(crumbs[0]);
        }
        else { 
            Motion.moveRandomly();
        }
    }
}
