package gen4.helpers;

import battlecode.common.RobotInfo;
import battlecode.common.GameActionException;

import static gen4.RobotPlayer.*;

public strictfp class AttackHelper {
	// ARCHON
	// LABORATORY
	// WATCHTOWER
	// MINER
	// BUILDER
	// SOLDIER
	// SAGE
	private static final int[] priority = {4, 0, 5, 2, 1, 6, 3};

	public static void attack() throws GameActionException {
		if (!rc.isActionReady()) {
			return;
		}

		int minHp = 2000;
		int maxPriority = -1;
		RobotInfo robotToAttack = null;
		RobotInfo[] enemyRobots = rc.senseNearbyRobots(myType.actionRadiusSquared, enemyTeam);
		for (int i = enemyRobots.length; --i >= 0; ) {
			RobotInfo robot = enemyRobots[i];
			int typeIndex = robot.type.ordinal();
			int p = priority[typeIndex];
			if (p > maxPriority) {
				maxPriority = p;
				minHp = robot.health;
				robotToAttack = robot;
			} else if (p == maxPriority && robot.health < minHp) {
				minHp = robot.health;
				robotToAttack = robot;
			}
		}

		if (robotToAttack != null && rc.canAttack(robotToAttack.location)) {
			rc.attack(robotToAttack.location);
		}
	}
}
