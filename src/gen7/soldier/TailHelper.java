package gen7.soldier;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import gen7.common.CommsHelper;

import static gen7.RobotPlayer.*;
import static gen7.soldier.AttackHelper.priority;
import static gen7.common.Functions.getBits;
import static gen7.common.Functions.setBits;

public strictfp class TailHelper {
	private static final int TARGET_UPDATE_ROUNDS_THRESHOLD = 10;

	public static MapLocation getTargetLocation() throws GameActionException {
		return CommsHelper.getLocationFrom12Bits(rc.readSharedArray(5));
	}

	public static int getTargetPriority() throws GameActionException {
		return getBits(rc.readSharedArray(5), 12, 14);
	}

	public static boolean foundTarget() throws GameActionException {
		return getBits(rc.readSharedArray(5), 15, 15) == 1;
	}

	public static int getAttackUnitsAroundTarget() throws GameActionException {
		return getBits(rc.readSharedArray(6), 0, 5);
	}

	public static void updateTarget() throws GameActionException {
		int minHp = 2000;
		int maxPriority = -1;
		RobotInfo currentTarget = null;
		int attackUnitsHere = 0;
		RobotInfo[] enemyRobots = rc.senseNearbyRobots(myType.visionRadiusSquared, enemyTeam);
		for (int i = enemyRobots.length; --i >= 0; ) {
			RobotInfo robot = enemyRobots[i];
			int typeIndex = robot.type.ordinal();
			int p = priority[typeIndex];
			if (p > maxPriority) {
				maxPriority = p;
				minHp = robot.health;
				currentTarget = robot;
			} else if (p == maxPriority && robot.health < minHp) {
				minHp = robot.health;
				currentTarget = robot;
			}

			switch (robot.type) {
				case SOLDIER:
					++attackUnitsHere;
					break;

				case WATCHTOWER:
					switch (robot.mode) {
						case PROTOTYPE:
							++attackUnitsHere;
							break;

						case PORTABLE:
							attackUnitsHere += robot.level;
							break;

						case TURRET:
							attackUnitsHere += 2 * robot.level;
							break;
					}
					break;

				case SAGE:
					attackUnitsHere += 4;
					break;
			}
		}

		if (foundTarget()) {
			MapLocation targetLocation = getTargetLocation();
			int targetPriority = getTargetPriority();
			int attackUnitsAroundTarget = getAttackUnitsAroundTarget();
			boolean targetPresent = true;
			boolean canUpdate = false;
			if (rc.getLocation().distanceSquaredTo(targetLocation) <= myType.visionRadiusSquared) {
				if (rc.canSenseRobotAtLocation(targetLocation)) {
					RobotInfo robot = rc.senseRobotAtLocation(targetLocation);
					if (priority[robot.type.ordinal()] != targetPriority || robot.team != enemyTeam) {
						targetPresent = false;
					} else if (attackUnitsHere != attackUnitsAroundTarget) {
						canUpdate = true;
					}
				} else {
					targetPresent = false;
				}
			} else {
				if (currentTarget != null && priority[currentTarget.type.ordinal()] >= targetPriority
					&& attackUnitsHere > attackUnitsAroundTarget) {
					canUpdate = true;
				}
			}

			if (targetPresent) {
				if (currentTarget != null) {
					if (priority[currentTarget.type.ordinal()] > targetPriority) {
						canUpdate = true;
					}
				}
			} else {
				if (currentTarget == null) {
					rc.writeSharedArray(5, 0);
				} else {
					canUpdate = true;
				}
			}

			if (canUpdate) {
				updateTargetInArray(currentTarget, attackUnitsHere);
			}
		} else {
			if (currentTarget != null) {
				updateTargetInArray(currentTarget, attackUnitsHere);
			}
		}
	}

	private static void updateTargetInArray(RobotInfo target, int attackUnits) throws GameActionException {
		int value = setBits(0, 15, 15, 1);
		value = setBits(value, 0, 11, CommsHelper.getBitsFromLocation(target.location));
		value = setBits(value, 12, 14, priority[target.type.ordinal()]);
		rc.writeSharedArray(5, value);

		value = rc.readSharedArray(6);
		value = setBits(value, 0, 5, Math.min(attackUnits, 63));
		rc.writeSharedArray(6, value);
	}
}
