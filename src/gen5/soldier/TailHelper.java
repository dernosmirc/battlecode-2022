package gen5.soldier;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import gen5.common.CommsHelper;
import gen5.common.MovementHelper;

import static gen5.RobotPlayer.*;
import static gen5.Soldier.*;
import static gen5.soldier.AttackHelper.priority;
import static gen5.common.Functions.getBits;
import static gen5.common.Functions.setBits;

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

	public static int getTargetRoundNum() throws GameActionException {
		return getBits(rc.readSharedArray(6), 0, 10);
	}

	public static void updateTarget() throws GameActionException {
		int minHp = 2000;
		int maxPriority = -1;
		RobotInfo currentTarget = null;
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
		}

		if (foundTarget()) {
			MapLocation targetLocation = getTargetLocation();
			int targetPriority = getTargetPriority();
			int targetRoundNum = getTargetRoundNum();
			boolean targetPresent = true;
			if (rc.getLocation().distanceSquaredTo(targetLocation) <= myType.visionRadiusSquared) {
				if (rc.canSenseRobotAtLocation(targetLocation)) {
					RobotInfo robot = rc.senseRobotAtLocation(targetLocation);
					if (priority[robot.type.ordinal()] != targetPriority || robot.team != enemyTeam) {
						targetPresent = false;
					}
				} else {
					targetPresent = false;
				}
			}

			boolean canUpdate = false;
			if (targetPresent) {
				if (currentTarget != null) {
					if (priority[currentTarget.type.ordinal()] > targetPriority) {
						canUpdate = true;
					} else if (rc.getRoundNum() - targetRoundNum >= TARGET_UPDATE_ROUNDS_THRESHOLD) {
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
				updateTargetInArray(currentTarget);
			}
		} else {
			if (currentTarget != null) {
				updateTargetInArray(currentTarget);
			}
		}
	}

	private static void updateTargetInArray(RobotInfo target) throws GameActionException {
		int value = setBits(0, 15, 15, 1);
		value = setBits(value, 0, 11, CommsHelper.getBitsFromLocation(target.location));
		value = setBits(value, 12, 14, priority[target.type.ordinal()]);
		rc.writeSharedArray(5, value);

		value = setBits(0, 0, 10, rc.getRoundNum());
		rc.writeSharedArray(6, value);
	}
}
