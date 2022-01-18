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
        RobotInfo robotToAttack = null;
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(myType.actionRadiusSquared, enemyTeam);
        for (int i = enemyRobots.length; --i >= 0; ) {
            RobotInfo robot = enemyRobots[i];
            if (robot.type == RobotType.ARCHON) {
                if (rc.canEnvision(AnomalyType.FURY)) {
                    rc.envision(AnomalyType.FURY);
                    return;
                }
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

        if (maxPriority == -1 || maxHp < 30) {
            return;
        }

        if (robotToAttack != null && rc.canAttack(robotToAttack.location)) {
            rc.attack(robotToAttack.location);
        }
    }

    public static Direction getArchonAttackDirection() {
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(myType.visionRadiusSquared, enemyTeam);
        for (int i = enemyRobots.length; --i >= 0; ) {

            if (
                    enemyRobots[i].type == RobotType.ARCHON &&
                    !enemyRobots[i].location.isWithinDistanceSquared(rc.getLocation(), myType.actionRadiusSquared))
            {
                return rc.getLocation().directionTo(enemyRobots[i].location);
            }
        }
        return null;
    }
}
