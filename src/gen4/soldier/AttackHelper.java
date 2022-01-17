package gen4.soldier;

import battlecode.common.RobotInfo;
import battlecode.common.GameActionException;
import battlecode.common.Direction;

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

	public static Direction shouldMoveBack() throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots(myType.visionRadiusSquared);
		double enemyRobots = 0;
		double ourRobots = 1;
		double[] robotsInDirection = new double[8];

		// TODO: take mutation level into consideration
		// TODO: take sage into consideration
		for (int i = robots.length; --i >= 0; ) {
			RobotInfo robot = robots[i];
			if (robot.team == enemyTeam) {
				switch (robot.type) {
					case SOLDIER:
						++enemyRobots;
						++robotsInDirection[rc.getLocation().directionTo(robot.location).ordinal()];
						break;
					case WATCHTOWER:
						enemyRobots += 1.5;
						robotsInDirection[rc.getLocation().directionTo(robot.location).ordinal()] += 1.5;
						break;
				}
			} else if (rc.getLocation().distanceSquaredTo(robot.location) <= myType.actionRadiusSquared - 3) {
				switch (robot.type) {
					case SOLDIER:
						++ourRobots;
						break;
					case WATCHTOWER:
						ourRobots += 1.5;
						break;
				}
			}
		}

		if (ourRobots >= enemyRobots + 1) {
			return null;
		}

		double maxRobots = 0;
		Direction dir = Direction.NORTH;
		for (int i = 8; --i >= 0; ) {
			double robotsCount = robotsInDirection[i];
			if (robotsCount > maxRobots) {
				maxRobots = robotsCount;
				dir = directions[i];
			}
		}

		return dir.opposite();
	}
}
