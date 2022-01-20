package gen5.sage;

import battlecode.common.*;

import static gen5.RobotPlayer.*;
import static gen5.RobotPlayer.rc;

public class SageAttackHelper {

    // ARCHON
    // LABORATORY
    // WATCHTOWER
    // MINER
    // BUILDER
    // SOLDIER
    // SAGE
    private static final int[] priority = {6, -1, 5, -1, -1, 3, 4};

    public static void attack() throws GameActionException {
        if (!rc.isActionReady()) {
            return;
        }

        int maxHp = 0;
        int maxPriority = -1;
        int furyDamage = 0, chargeDamage = 0;
        RobotInfo robotToAttack = null;
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(myType.actionRadiusSquared, enemyTeam);
        for (int i = enemyRobots.length; --i >= 0; ) {
            RobotInfo robot = enemyRobots[i];
            if (robot.mode == RobotMode.TURRET) {
                furyDamage += Math.min(robot.health, robot.type.getMaxHealth(robot.level)/10);
            } else if (robot.mode == RobotMode.DROID) {
                chargeDamage += Math.min(robot.health, (robot.type.getMaxHealth(robot.level) * 22)/100);
            }
            int typeIndex = robot.type.ordinal();
            int p = priority[typeIndex];
            if (p > maxPriority) {
                maxPriority = p;
                maxHp = robot.health;
                robotToAttack = robot;
            } else if (p == maxPriority && robot.health > maxHp) {
                maxHp = robot.health;
                robotToAttack = robot;
            }
        }

        if (furyDamage >= maxHp && furyDamage >= chargeDamage) {
            if (rc.canEnvision(AnomalyType.FURY)) {
                rc.envision(AnomalyType.FURY);
                return;
            }
        }

        if (chargeDamage >= maxHp && chargeDamage >= furyDamage) {
            if (rc.canEnvision(AnomalyType.CHARGE)) {
                rc.envision(AnomalyType.CHARGE);
                return;
            }
        }

        if (maxPriority == -1 || maxHp < 35) {
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
