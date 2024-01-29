package RANDOM;

import battlecode.common.*;

public class Atk {
    public static RobotController rc;
    public static StringBuilder indicatorString;

    protected static boolean attack() throws GameActionException {
        RobotInfo[] opponentRobots = rc.senseNearbyRobots(4, rc.getTeam().opponent());
        while (rc.isActionReady()) {
            if (opponentRobots.length > 0) {
                RobotInfo robot = getPrioritizedOpponentRobot(opponentRobots);
                if (rc.canAttack(robot.getLocation())) {
                    indicatorString.append("ATK-" + robot.getLocation().toString() + "; ");
                    while (robot != null && rc.canAttack(robot.getLocation())) {
                        rc.attack(robot.getLocation());
                    }
                    return true;
                }
                else {
                    return false;
                }
            }
            else {
                return false;
            }
        }
        return false;
    }
    protected static void heal() throws GameActionException {
        RobotInfo[] friendlyRobots = rc.senseNearbyRobots(4, rc.getTeam());
        while (rc.isActionReady()) {
            if (friendlyRobots.length > 0) {
                RobotInfo robot = getPrioritizedFriendlyRobot(friendlyRobots);
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
        RobotInfo best = null;
        for (RobotInfo robot : opponentRobots) {
            if (best == null) {
                best = robot;
            }
            else if (best.hasFlag()) {
                if (!robot.hasFlag()) {
                    best = robot;
                }
                else if (best.getHealth() > robot.getHealth()) {
                    best = robot;
                }
                else if (best.getHealth() == robot.getHealth() && best.getID() > robot.getID()) {
                    best = robot;
                }
            }
            else if (best.getHealth() > robot.getHealth()) {
                best = robot;
            }
            else if (best.getHealth() == robot.getHealth() && best.getID() > robot.getID()) {
                best = robot;
            }
        }
        return best;
    }
    protected static RobotInfo getPrioritizedFriendlyRobot(RobotInfo[] friendlyRobots) throws GameActionException {
        RobotInfo best = null;
        for (RobotInfo robot : friendlyRobots) {
            if (best == null) {
                best = robot;
            }
            else if (best.hasFlag()) {
                if (!robot.hasFlag()) {
                    best = robot;
                }
                else if (best.getHealth() > robot.getHealth()) {
                    best = robot;
                }
                else if (best.getHealth() == robot.getHealth() && best.getID() > robot.getID()) {
                    best = robot;
                }
            }
            else if (best.getHealth() > robot.getHealth()) {
                best = robot;
            }
            else if (best.getHealth() == robot.getHealth() && best.getID() > robot.getID()) {
                best = robot;
            }
        }
        return best;
    }
}