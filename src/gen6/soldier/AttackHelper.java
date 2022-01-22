package gen6.soldier;

import battlecode.common.RobotInfo;
import battlecode.common.GameActionException;
import battlecode.common.Direction;

import static gen6.RobotPlayer.*;
import static gen6.Soldier.allRobots;
import static gen6.soldier.SoldierMovementHelper.HEAL_THRESHOLD;
import static gen6.sage.SageMovementHelper.HP_THRESHOLD;

public strictfp class AttackHelper {
	// ARCHON
	// LABORATORY
	// WATCHTOWER
	// MINER
	// BUILDER
	// SOLDIER
	// SAGE
	public static final int[] priority = {3, 0, 4, 2, 1, 5, 6};

	public static void attack() throws GameActionException {
		if (!rc.isActionReady()) {
			return;
		}

		int minHp = 2000;
		int maxPriority = -1;
		RobotInfo robotToAttack = null;
		// RobotInfo[] enemyRobots = rc.senseNearbyRobots(myType.actionRadiusSquared, enemyTeam);
		RobotInfo[] enemyRobots = allRobots;
		for (int i = enemyRobots.length; --i >= 0; ) {
			RobotInfo robot = enemyRobots[i];
			if (robot.team != enemyTeam || !rc.getLocation().isWithinDistanceSquared(
				robot.location, myType.actionRadiusSquared)) {
				continue;
			}

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
		int enemyRobots = 0;
		int ourRobots = 0;
		switch (myType) {
			case SOLDIER:
				ourRobots = 1;
				break;

			case SAGE:
				ourRobots = 4;
				break;
		}

		int[] robotsInDirection = new int[8];
		for (int i = robots.length; --i >= 0; ) {
			RobotInfo robot = robots[i];
			if (robot.team == enemyTeam) {
				int ordinal = rc.getLocation().directionTo(robot.location).ordinal();
				switch (robot.type) {
					case SOLDIER:
						++enemyRobots;
						++robotsInDirection[ordinal];
						break;

					case WATCHTOWER:
						switch (robot.mode) {
							case PROTOTYPE:
								++enemyRobots;
								++robotsInDirection[ordinal];
								break;

							case PORTABLE:
								enemyRobots += robot.level;
								robotsInDirection[ordinal] += robot.level;
								break;

							case TURRET:
								enemyRobots += 2 * robot.level;
								robotsInDirection[ordinal] += 2 * robot.level;
								break;
						}
						break;

					case SAGE:
						enemyRobots += 4;
						robotsInDirection[ordinal] += 4;
						break;
				}
			} else if (rc.getLocation().distanceSquaredTo(robot.location) <= myType.actionRadiusSquared) {
				switch (robot.type) {
					case SOLDIER:
						if (robot.health > HEAL_THRESHOLD) {
							++ourRobots;
						}
						break;

					case WATCHTOWER:
						switch (robot.mode) {
							case PROTOTYPE:
								++ourRobots;
								break;

							case PORTABLE:
								ourRobots += robot.level;
								break;

							case TURRET:
								ourRobots += 2 * robot.level;
								break;
						}
						break;

					case SAGE:
						if (robot.health >= HP_THRESHOLD) {
							ourRobots += 4;
						}
						break;
				}
			}
		}

		if (ourRobots >= enemyRobots + 1) {
			return null;
		}

		int maxRobots = 0;
		Direction dir = Direction.NORTH;
		for (int i = 8; --i >= 0; ) {
			int robotsCount = robotsInDirection[i];
			if (robotsCount > maxRobots) {
				maxRobots = robotsCount;
				dir = directions[i];
			}
		}

		// rc.setIndicatorString("" + dir.opposite().ordinal());
		return dir.opposite();
	}
}
