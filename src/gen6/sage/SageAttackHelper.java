package gen6.sage;

import battlecode.common.*;

import static gen6.RobotPlayer.*;
import static gen6.RobotPlayer.rc;

public class SageAttackHelper {

    // ARCHON
    // LABORATORY
    // WATCHTOWER
    // MINER
    // BUILDER
    // SOLDIER
    // SAGE
    private static final int[] priority = {6, 0, 5, 2, 1, 3, 4};

    private static final int SAGE_ATTACK_THRESHOLD = 1;

    public static void attack() throws GameActionException {
        if (!rc.isActionReady()) {
            return;
        }

        int maxPriority = -1;
        int furyDamage = 0, chargeDamage = 0, attackDamage = 0;
        RobotInfo robotToAttack = null;
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(myType.actionRadiusSquared);
        for (int i = enemyRobots.length; --i >= 0; ) {
            RobotInfo robot = enemyRobots[i];
            if (robot.team == myTeam) {
                if (robot.mode == RobotMode.TURRET) {
                    furyDamage = -1000000000;
                }
                continue;
            }

            if (robot.mode == RobotMode.TURRET) {
                furyDamage += Math.min(robot.health, robot.type.getMaxHealth(robot.level)/10);
            } else if (robot.mode == RobotMode.DROID && robot.type.canAttack()) {
                chargeDamage += Math.min(robot.health, (robot.type.getMaxHealth(robot.level) * 22)/100);
            }
            int typeIndex = robot.type.ordinal();
            int p = priority[typeIndex], damage = Math.min(robot.health, RobotType.SAGE.damage);
            if (p > maxPriority) {
                maxPriority = p;
                attackDamage = damage;
                robotToAttack = robot;
            } else if (p == maxPriority && damage > attackDamage) {
                attackDamage = damage;
                robotToAttack = robot;
            }
        }

        if (furyDamage >= attackDamage && furyDamage >= chargeDamage && furyDamage > 0) {
            if (rc.canEnvision(AnomalyType.FURY)) {
                rc.envision(AnomalyType.FURY);
                return;
            }
        }

        if (chargeDamage >= attackDamage && chargeDamage >= furyDamage && chargeDamage >= SAGE_ATTACK_THRESHOLD) {
            if (rc.canEnvision(AnomalyType.CHARGE)) {
                rc.envision(AnomalyType.CHARGE);
                return;
            }
        }

        if (maxPriority == -1 || attackDamage < SAGE_ATTACK_THRESHOLD) {
            return;
        }

        if (rc.canAttack(robotToAttack.location)) {
            rc.attack(robotToAttack.location);
        }
    }

    public static MapLocation getArchonAttackLocation() {
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(myType.visionRadiusSquared, enemyTeam);
        for (int i = enemyRobots.length; --i >= 0; ) {

            if (
                    enemyRobots[i].type == RobotType.ARCHON &&
                    !enemyRobots[i].location.isWithinDistanceSquared(rc.getLocation(), myType.actionRadiusSquared))
            {
                return enemyRobots[i].location;
            }
        }
        return null;
    }
}
