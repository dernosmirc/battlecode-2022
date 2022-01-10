package gen1;

import java.util.Random;
import battlecode.common.*;
import gen1.helpers.SpawnHelper;

import static gen1.RobotPlayer.*;
import static gen1.util.Functions.getBits;
import static gen1.util.Functions.setBits;

public strictfp class Archon {
	private static final Random rng = new Random(rc.getID());

	private static final int BUILD_THRESHOLD = 80; // make dynamic?

	private static int buildDirectionIndex = 0;
	// private static MapLocation previousLeadLoc = new MapLocation(-1, -1);
	// private static int previousLeadRound = -100;
	private static int myIndex;

	private static Direction getOptimalMinerSpawnDirection() throws GameActionException {
		int[] minersInDirection = new int[8];
		for (RobotInfo robot : rc.senseNearbyRobots(myType.visionRadiusSquared, myTeam)) {
			if (robot.type == RobotType.MINER) {
				++minersInDirection[rc.getLocation().directionTo(robot.location).ordinal()];
			}
		}

		int[] leadInDirection = new int[8];
		for (MapLocation loc : rc.senseNearbyLocationsWithLead(myType.visionRadiusSquared, 2)) {
			Direction toLead = rc.getLocation().directionTo(loc);
			if (toLead != Direction.CENTER) {
				leadInDirection[toLead.ordinal()] += rc.senseLead(loc);
			}
		}

		// Priority of a direction = theta1 * lead - theta2 * miners
		double theta1 = 0.9;
		double theta2 = 20;
		double maxPriority = -theta2 * 1000;
		Direction optimalDirection = Direction.NORTHEAST;
		for (Direction dir : directions) {
			double priority = theta1 * leadInDirection[dir.ordinal()] - theta2 * minersInDirection[dir.ordinal()];
			if (priority > maxPriority) {
				maxPriority = priority;
				optimalDirection = dir;
			}
		}

		return SpawnHelper.getForceSpawnDirection(optimalDirection);
	}

	public static void run() throws GameActionException {
		int lead = rc.getTeamLeadAmount(myTeam);
		if (rc.getRoundNum() == 1) {
			if (rc.canBuildRobot(RobotType.MINER, Direction.NORTHWEST)) {
				rc.buildRobot(RobotType.MINER, Direction.NORTHWEST);
			}
		} else if (rc.isActionReady() && lead >= BUILD_THRESHOLD) {
			RobotType spawnType = RobotType.BUILDER;
			switch (rng.nextInt(3)) {
				case 0:
					spawnType = RobotType.BUILDER;
					break;
				case 1:
					spawnType = RobotType.MINER;
					break;
				case 2:
					spawnType = RobotType.SOLDIER;
					break;
			}

			for (int i = 0; i < directions.length; ++i) {
				if (rc.canBuildRobot(spawnType, directions[buildDirectionIndex])) {
					rc.buildRobot(spawnType, directions[buildDirectionIndex]);
					break;
				}

				++buildDirectionIndex;
				if (buildDirectionIndex == directions.length)
					buildDirectionIndex = 0;
			}
		}
	}

	public static void init() throws GameActionException {
		for (int i = 32; i < 32 + archonCount; ++i) {
			int value = rc.readSharedArray(i);
			if (getBits(value, 15, 15) == 0) {
				value = setBits(0, 15, 15, 1);
				value = setBits(value, 6, 11, rc.getLocation().x);
				value = setBits(value, 0, 5, rc.getLocation().y);
				myIndex = i - 32;
				rc.writeSharedArray(i, value);
				break;
			}
		}
	}
}
