package SPAARKJAN11;

import battlecode.common.*;

public class Attack {
    public static RobotController rc;
    public static StringBuilder indicatorString;

    public static RobotInfo[] friendlyRobots;
    public static RobotInfo[] opponentRobots;

    protected static void attack() throws GameActionException {
        opponentRobots = rc.senseNearbyRobots(4, rc.getTeam().opponent());
        while (rc.isActionReady()) {
            if (opponentRobots.length > 0) {
                RobotInfo robot = getPrioritizedOpponentRobot();
                rc.setIndicatorLine(rc.getLocation(), robot.getLocation(), 255, 0, 0);
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
        friendlyRobots = rc.senseNearbyRobots(4, rc.getTeam());
        while (rc.isActionReady()) {
            if (friendlyRobots.length > 0) {
                RobotInfo robot = getPrioritizedFriendlyRobot();
                rc.setIndicatorLine(rc.getLocation(), robot.getLocation(), 255, 255, 0);
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
            else if (robot.hasFlag()) {
                if (!r.hasFlag()) {
                    robot = r;
                }
                else if (robot.getHealth() > r.getHealth()) {
                    robot = r;
                }
                else if (robot.getHealth() == r.getHealth() && robot.getID() > r.getID()) {
                    robot = r;
                }
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
            else if (robot.hasFlag()) {
                if (!r.hasFlag()) {
                    robot = r;
                }
                else if (robot.getHealth() > r.getHealth()) {
                    robot = r;
                }
                else if (robot.getHealth() == r.getHealth() && robot.getID() > r.getID()) {
                    robot = r;
                }
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