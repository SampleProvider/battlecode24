package SPAARKatk;

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

    protected static int getAttack(RobotInfo r) throws GameActionException {
        int baseAttack = 150;
        GlobalUpgrade[] upgrades = rc.getGlobalUpgrades(r.getTeam());
        for (GlobalUpgrade u : upgrades) {
            baseAttack += u.baseAttackChange;
        }
        switch (r.getAttackLevel()) {
            case 1:
                baseAttack = (int)((float)baseAttack * 1.05);
                break;
            case 2:
                baseAttack = (int)((float)baseAttack * 1.07);
                break;
            case 3:
                baseAttack = (int)((float)baseAttack * 1.10);
                break;
            case 4:
                baseAttack = (int)((float)baseAttack * 1.30);
                break;
            case 5:
                baseAttack = (int)((float)baseAttack * 1.35);
                break;
            case 6:
                baseAttack = (int)((float)baseAttack * 1.60);
                break;
            default:
                break;
        }
        return baseAttack;
    }

    protected static int getHeal(RobotInfo r) throws GameActionException {
        int baseAttack = 80;
        GlobalUpgrade[] upgrades = rc.getGlobalUpgrades(r.getTeam());
        for (GlobalUpgrade u : upgrades) {
            baseAttack += u.baseHealChange;
        }
        switch (r.getAttackLevel()) {
            case 1:
                baseAttack = (int)((float)baseAttack * 1.03);
                break;
            case 2:
                baseAttack = (int)((float)baseAttack * 1.05);
                break;
            case 3:
                baseAttack = (int)((float)baseAttack * 1.07);
                break;
            case 4:
                baseAttack = (int)((float)baseAttack * 1.10);
                break;
            case 5:
                baseAttack = (int)((float)baseAttack * 1.15);
                break;
            case 6:
                baseAttack = (int)((float)baseAttack * 1.25);
                break;
            default:
                break;
        }
        return baseAttack;
    }
}