package SPAARK_F1;

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
        RobotInfo robot = null;
        double score = 0;

        MapLocation me = rc.getLocation();

        int damage = rc.getAttackDamage();
        for (RobotInfo r : opponentRobots) {
            // double rScore = r.getAttackLevel() + /*r.getHealLevel()*/ - r.getHealth() / 100.0 + (r.hasFlag() ? - 999: 0) - (Math.sqrt(me.distanceSquaredTo(r.getLocation()))) * 0.5;
            // double rScore = r.getAttackLevel() + /*r.getHealLevel()*/ - r.getHealth() / 50.0 + (r.hasFlag() ? - 999: 0);

            // double rScore = /*r.getHealLevel()*/ - r.getHealth() / 200.0 + (r.hasFlag() ? - 99999: 0) - (Math.sqrt(me.distanceSquaredTo(r.getLocation())));
            // double rScore = /*r.getHealLevel()*/ - r.getHealth() / 200.0 + (r.hasFlag() ? - 99999: 0) - (Math.sqrt(me.distanceSquaredTo(r.getLocation()))) * 0.2;
            double rScore = /*r.getHealLevel()*/ - r.getHealth() / 200.0 + (r.hasFlag() ? - 99999: 0) - (Math.sqrt(me.distanceSquaredTo(r.getLocation()))) * 0.3;
            // double rScore = /*r.getHealLevel()*/ - r.getHealth() / 200.0 + (r.hasFlag() ? - 99999: 0);
            if (r.getHealth() <= damage) {
                rScore += 1000 + r.getAttackLevel() * 1000;
            }
            if (robot == null) {
                robot = r;
                score = rScore;
            }
            else if (rScore > score) {
                robot = r;
                score = rScore;
            }
        }
        return robot;
    }
    protected static RobotInfo getPrioritizedFriendlyRobot(RobotInfo[] friendlyRobots) throws GameActionException {
        RobotInfo robot = null;
        double score = 0;

        for (RobotInfo r : friendlyRobots) {
            if (r.getHealth() == 1000) {
                continue;
            }
            double rScore = r.getAttackLevel() /*r.getHealLevel()*/ - r.getHealth() / 20.0 + (r.hasFlag() ? - 99999: 0);
            // double rScore = /*r.getHealLevel()*/ - r.getHealth() / 200.0 + (r.hasFlag() ? - 99999: 0);
            if (robot == null) {
                robot = r;
                score = rScore;
            }
            else if (rScore > score) {
                robot = r;
                score = rScore;
            }
        }
        return robot;
    }

    protected static int attackXpSinceLastLevel() throws GameActionException {
        int exp = rc.getExperience(SkillType.ATTACK);
        if (exp < 20) {
            return -1;
        }
        if (exp < 40) {
            return exp - 20;
        }
        if (exp < 70) {
            return exp - 40;
        }
        if (exp < 100) {
            return exp - 70;
        }
        if (exp < 140) {
            return exp - 100;
        }
        if (exp < 180) {
            return exp - 140;
        }
        return exp - 180;
    }

    protected static int buildXpSinceLastLevel() throws GameActionException {
        int exp = rc.getExperience(SkillType.BUILD);
        if (exp < 5) {
            return -1;
        }
        if (exp < 10) {
            return exp - 5;
        }
        if (exp < 15) {
            return exp - 10;
        }
        if (exp < 20) {
            return exp - 15;
        }
        if (exp < 25) {
            return exp - 20;
        }
        if (exp < 30) {
            return exp - 25;
        }
        return exp - 30;
    }

    protected static int healXpSinceLastLevel() throws GameActionException {
        int exp = rc.getExperience(SkillType.BUILD);
        if (exp < 10) {
            return -1;
        }
        if (exp < 20) {
            return exp - 10;
        }
        if (exp < 30) {
            return exp - 20;
        }
        if (exp < 50) {
            return exp - 30;
        }
        if (exp < 75) {
            return exp - 50;
        }
        if (exp < 125) {
            return exp - 75;
        }
        return exp - 125;
    }

    protected static int deathsUntilAttackLevelDrop() throws GameActionException {
        int lvl = rc.getLevel(SkillType.ATTACK);
        switch (lvl) {
            case 0:
                return -1;
            case 1:
                return (int)Math.ceil((attackXpSinceLastLevel() - 1) / 5.0);
            case 2:
                return (int)Math.ceil((attackXpSinceLastLevel() - 1) / 5.0);
            case 3:
                return (int)Math.ceil((attackXpSinceLastLevel() - 1) / 10.0);
            case 4:
                return (int)Math.ceil((attackXpSinceLastLevel() - 1) / 10.0);
            case 5:
                return (int)Math.ceil((attackXpSinceLastLevel() - 1) / 15.0);
            case 6:
                return (int)Math.ceil((attackXpSinceLastLevel() - 1) / 15.0);
            default:
                return -1;
        }
    }

    protected static int deathsUntilBuildLevelDrop() throws GameActionException {
        int lvl = rc.getLevel(SkillType.BUILD);
        switch (lvl) {
            case 0:
                return -1;
            case 1:
                return (int)Math.ceil((buildXpSinceLastLevel() - 1) / 2.0);
            case 2:
                return (int)Math.ceil((buildXpSinceLastLevel() - 1) / 2.0);
            case 3:
                return (int)Math.ceil((buildXpSinceLastLevel() - 1) / 5.0);
            case 4:
                return (int)Math.ceil((buildXpSinceLastLevel() - 1) / 5.0);
            case 5:
                return (int)Math.ceil((buildXpSinceLastLevel() - 1) / 10.0);
            case 6:
                return (int)Math.ceil((buildXpSinceLastLevel() - 1) / 10.0);
            default:
                return -1;
        }
    }

    protected static int deathsUntilHealLevelDrop() throws GameActionException {
        int lvl = rc.getLevel(SkillType.HEAL);
        switch (lvl) {
            case 0:
                return -1;
            case 1:
                return (int)Math.ceil((healXpSinceLastLevel() - 1) / 2.0);
            case 2:
                return (int)Math.ceil((healXpSinceLastLevel() - 1) / 2.0);
            case 3:
                return (int)Math.ceil((healXpSinceLastLevel() - 1) / 5.0);
            case 4:
                return (int)Math.ceil((healXpSinceLastLevel() - 1) / 5.0);
            case 5:
                return (int)Math.ceil((healXpSinceLastLevel() - 1) / 10.0);
            case 6:
                return (int)Math.ceil((healXpSinceLastLevel() - 1) / 10.0);
            default:
                return -1;
        }
    }
}