package SPAARK;

import battlecode.common.*;

public class Attack {
    public static RobotController rc;
    public static StringBuilder indicatorString;

    public static RobotInfo[] friendlyRobots;
    public static RobotInfo[] opponentRobots;
    protected static void attack() throws GameActionException {
        while (rc.isActionReady()) {
            if (opponentRobots.length > 0) {
                RobotInfo robot = getPrioritizedOpponentRobot();
                if (rc.canAttack(robot.getLocation())) {
                    indicatorString.append("ATK-" + robot.getLocation().toString() + "; ");
                    while (robot != null && rc.canAttack(robot.getLocation())) {
                        rc.attack(robot.getLocation());
                    }
                    continue;
                }
                else {
                    return;
                }
            }
            else {
                return;
            }
        }
    }
    protected static void heal() throws GameActionException {
        while (rc.isActionReady()) {
            if (friendlyRobots.length > 0) {
                RobotInfo robot = getPrioritizedFriendlyRobot();
                if (rc.canHeal(robot.getLocation())) {
                    indicatorString.append("HEAL-" + robot.getLocation().toString() + "; ");
                    while (rc.canHeal(robot.getLocation())) {
                        rc.heal(robot.getLocation());
                    }
                    continue;
                }
                else {
                    return;
                }
            }
            else {
                return;
            }
        }
    }

    protected static RobotInfo getPrioritizedOpponentRobot() throws GameActionException {
        RobotInfo robot = null;
        for (RobotInfo r : opponentRobots) {
            if (robot == null) {
                robot = r;
            }
            else if (robot.getHealth() > r.getHealth()) {
                robot = r;
            }
            else if (robot.getHealth() == r.getHealth() && robot.getID() > r.getID()) {
                robot = r;
            }
        }
        return robot;
    }
    protected static RobotInfo getPrioritizedFriendlyRobot() throws GameActionException {
        RobotInfo robot = null;
        for (RobotInfo r : friendlyRobots) {
            if (robot == null) {
                robot = r;
            }
            else if (robot.getHealth() > r.getHealth()) {
                robot = r;
            }
            else if (robot.getHealth() == r.getHealth() && robot.getID() > r.getID()) {
                robot = r;
            }
        }
        return robot;
    }
}