package micro_;

import battlecode.common.*;

public class Micro {

    static final int INF = 1000000;
    static boolean shouldPlaySafe = false;
    static boolean alwaysInRange = false;
    static int myRange;
    static int myVisionRange;

    static final int[] damage = new int[]{150, 158, 161, 165, 195, 203, 240};
    static int myDamage = 0;

    static final int RANGE_EXTENDED = 10;
    static final int RANGE = 4;

    //static double myDPS;
    //static double[] DPS = new double[]{0, 0, 0, 0, 0, 0, 0};
    //static int[] rangeExtended = new int[]{0, 0, 0, 0, 0, 0, 0};

    protected static final Direction[] ALL_DIRECTIONS = {
        Direction.SOUTHWEST,
        Direction.SOUTH,
        Direction.SOUTHEAST,
        Direction.WEST,
        Direction.EAST,
        Direction.NORTHWEST,
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.CENTER,
    };

    final static int MAX_MICRO_BYTECODE_REMAINING = 2000;

    protected static RobotController rc;

    protected static int currActionRangeExtended;
    protected static double currActionRadius;
    protected static boolean canAttack;
    protected static MapLocation currLoc;
    protected static int currDamage = 0;

    protected boolean micro() throws GameActionException {
        if (!rc.isMovementReady()) {
            return false;
        }
        shouldPlaySafe = false;
        RobotInfo[] opponentRobots =  Motion.opponentRobots;
        if (opponentRobots.length == 0) {
            return false;
        }
        canAttack = rc.isActionReady();

        for (RobotInfo r : opponentRobots) {
            if (!r.hasFlag()) {
                shouldPlaySafe = true;
                break;
            }
        }

        // for (int i = 0; i < Robot.hComm.numBases; ++i){
        //     currLoc = Robot.otherComm.getLauncherLoc(i);
        //     if (currLoc != null && rc.getLocation().distanceSquaredTo(currLoc) <= 52) {
        //         shouldPlaySafe = true;
        //         break;
        //     }
        // }

        if (!shouldPlaySafe) {
            return false;
        }

        alwaysInRange = false;
        if (!canAttack) alwaysInRange = true;

        myDamage = rc.getAttackDamage();

        MicroInfo[] microInfo = new MicroInfo[9];
        for (int i = 0; i < 9; i++) {
            microInfo[i] = new MicroInfo(ALL_DIRECTIONS[i]);
        }

        for (RobotInfo r : opponentRobots) {
            if (Clock.getBytecodesLeft() < MAX_MICRO_BYTECODE_REMAINING) {
                break;
            }
            currLoc = r.getLocation();
            currDamage = damage[r.getAttackLevel()];
            currActionRangeExtended = RANGE_EXTENDED;
            currActionRadius = RANGE;
            microInfo[0].updateEnemy();
            microInfo[1].updateEnemy();
            microInfo[2].updateEnemy();
            microInfo[3].updateEnemy();
            microInfo[4].updateEnemy();
            microInfo[5].updateEnemy();
            microInfo[6].updateEnemy();
            microInfo[7].updateEnemy();
            microInfo[8].updateEnemy();
        }

        RobotInfo[] friendlyRobots = Motion.friendlyRobots;
        for (RobotInfo r : friendlyRobots) {
            if (Clock.getBytecodesLeft() < MAX_MICRO_BYTECODE_REMAINING) {
                break;
            }
            currLoc = r.getLocation();
            currDamage = damage[r.getAttackLevel()];
            currActionRangeExtended = RANGE_EXTENDED;
            currActionRadius = RANGE;
            microInfo[0].updateAlly();
            microInfo[1].updateAlly();
            microInfo[2].updateAlly();
            microInfo[3].updateAlly();
            microInfo[4].updateAlly();
            microInfo[5].updateAlly();
            microInfo[6].updateAlly();
            microInfo[7].updateAlly();
            microInfo[8].updateAlly();
        }

        MicroInfo bestMicro = microInfo[8];
        for (int i = 0; i < 8; i++) {
            if (microInfo[i].isBetter(bestMicro)) {
                bestMicro = microInfo[i];
            }
        }

        boolean b = apply(bestMicro);
        if (bestMicro != null && bestMicro.target != null) {
            while (rc.canAttack(bestMicro.target)) {
                rc.attack(bestMicro.target);
            }
        }

        return b;
    }

    static boolean apply(MicroInfo bestMicro) throws GameActionException {
        if (bestMicro.dir == Direction.CENTER) return true;

        if (rc.canMove(bestMicro.dir)) {
            Motion.move(bestMicro.dir);
            return true;
        }
        return false;
    }

    class MicroInfo {
        Direction dir;
        MapLocation location;
        int minDistanceToEnemy = INF;

        int canLandHit = 0;

        int enemyActionRange = 0;
        int enemyVisionRange = 0;

        int minDistToAlly = INF;

        int allyActionRange = 0;
        int allyVisionRange = 0;

        MapLocation target = null;

        boolean canMove = true;
        boolean canFill = true;

        public MicroInfo(Direction dir) throws GameActionException {
            this.dir = dir;
            this.location = rc.getLocation().add(dir);
            if (dir != Direction.CENTER && !rc.canMove(dir)) {
                canMove = false;
            }
            if (dir != Direction.CENTER && !rc.canFill(rc.adjacentLocation(dir))) {
                canFill = false;
            }
            if (canMove) {
            }
            else{
                minDistanceToEnemy = INF;
                //alliesTargeting += myDMG;
            }
        }

        public void updateEnemy() {
            if (!canMove) {
                return;
            }
            int dist = currLoc.distanceSquaredTo(location);
            if (dist < minDistanceToEnemy) {
                minDistanceToEnemy = dist;
            }
            if (dist <= currActionRadius) {
                enemyActionRange++;
            }
            if (dist <= 20) {
                enemyVisionRange++;
            }
            // if (dist <= RANGE_EXTENDED_LAUNCHER) {
            //     possibleEnemyLaunchers++;
            // }
            if (dist <= myRange && canAttack){
                canLandHit = 1;
                target = currLoc;
                //possibleAllies++;
            }
            //if (dist <= RANGE_EXTENDED_LAUNCHER) enemiesTargeting += currDMG;
        }

        void updateAlly() {
            if (!canMove) {
                return;
            }
            int dist = currLoc.distanceSquaredTo(location);
            if (dist < minDistToAlly) {
                minDistToAlly = dist;
            }
            if (dist <= currActionRadius) {
                allyActionRange++;
            }
            if (dist <= 20) {
                allyVisionRange++;
            }
            //if (dist <= 2) alliesTargeting += currDMG;
        }

        boolean inRange() {
            if (alwaysInRange) return true;
            return minDistanceToEnemy <= myRange;
        }

        //equal => true
        boolean isBetter(MicroInfo m){

            //if (safe() > m.safe()) return true;
            //if (safe() < m.safe()) return false;

            if (canMove && !m.canMove) return true;
            if (!canMove && m.canMove) return false;

            if (enemyActionRange - canLandHit < m.enemyActionRange - m.canLandHit) return true;
            if (enemyActionRange - canLandHit > m.enemyActionRange - m.canLandHit) return false;

            if (enemyVisionRange - canLandHit < m.enemyVisionRange - m.canLandHit) return true;
            if (enemyVisionRange - canLandHit > m.enemyVisionRange - m.canLandHit) return false;

            if (canLandHit > m.canLandHit) return true;
            if (canLandHit < m.canLandHit) return false;

            if (minDistToAlly < m.minDistToAlly) return true;
            if (minDistToAlly > m.minDistToAlly) return false;

            if (inRange()) return minDistanceToEnemy >= m.minDistanceToEnemy;
            else return minDistanceToEnemy <= m.minDistanceToEnemy;
        }
    }

}
