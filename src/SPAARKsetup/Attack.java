package SPAARKsetup;

import battlecode.common.*;

public class Attack {
    public static RobotController rc;
    public static StringBuilder indicatorString;

    protected static void attack() throws GameActionException {
        RobotInfo[] opponentRobots = rc.senseNearbyRobots(4, rc.getTeam().opponent());
        while (rc.isActionReady()) {
            if (opponentRobots.length > 0) {
                RobotInfo robot = getPrioritizedOpponentRobot(opponentRobots);
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
        RobotInfo[] friendlyRobots = rc.senseNearbyRobots(4, rc.getTeam());
        while (rc.isActionReady()) {
            if (friendlyRobots.length > 0) {
                RobotInfo robot = getPrioritizedFriendlyRobot(friendlyRobots);
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

    protected static RobotInfo getPrioritizedOpponentRobot(RobotInfo[] opponentRobots) throws GameActionException {
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
    protected static RobotInfo getPrioritizedFriendlyRobot(RobotInfo[] friendlyRobots) throws GameActionException {
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