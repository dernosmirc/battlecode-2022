package gen2;

import java.util.Random;
import battlecode.common.*;
import gen2.helpers.SpawnHelper;
import gen2.util.Logger;

import static gen2.RobotPlayer.*;
import static gen2.util.Functions.getBits;
import static gen2.util.Functions.setBits;

public strictfp class Archon {
	private static final Random rng = new Random(rc.getID());

	private static final int BUILD_THRESHOLD = 80; // make dynamic?

	private static double minersRatio = 0.3;
	private static double soldiersRatio = 0.5;
	private static double buildersRatio = 0.2;

	private static int buildDirectionIndex = 0;
	private static int myIndex;

	private static int droidsBuilt = 0;
	private static int minersBuilt = 0;
	private static int soldiersBuilt = 0;
	private static int buildersBuilt = 0;

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
			Logger logger = new Logger("", false);
			logger.log(dir.name() + " " + priority);

			if (priority > maxPriority) {
				maxPriority = priority;
				optimalDirection = dir;
			}
		}

		return SpawnHelper.getForceSpawnDirection(optimalDirection);
	}

	private static int getBuildArchonIndex() throws GameActionException {
		int minDroids = 100000;
		int index = 0;
		for (int i = 10; i < 10 + archonCount; ++i) {
			int droids = getBits(rc.readSharedArray(i), 0, 15);
			if (droids < minDroids) {
				minDroids = droids;
				index = i - 10;
			}
		}

		return index;
	}

	private static void updateDroidsBuilt() throws GameActionException {
		rc.writeSharedArray(10 + myIndex, droidsBuilt);
	}

	public static void run() throws GameActionException {
		int lead = rc.getTeamLeadAmount(myTeam);
		int archonIndex = getBuildArchonIndex();
		if (archonIndex != myIndex) {
			if (lead >= 2 * BUILD_THRESHOLD) {}
			else return;
		}

		if (rc.isActionReady() && lead >= BUILD_THRESHOLD) {
			RobotType spawnType = RobotType.BUILDER;
			if (droidsBuilt < 2) {
				spawnType = RobotType.MINER;
			} else if (soldiersBuilt < 3) {
				spawnType = RobotType.SOLDIER;
			} else {
				double randomNumber = rng.nextDouble();
				if (randomNumber < soldiersRatio) {
					spawnType = RobotType.SOLDIER;
				} else if (randomNumber < soldiersRatio + minersRatio) {
					spawnType = RobotType.MINER;
				} else {
					spawnType = RobotType.BUILDER;
				}
			}

			if (spawnType == RobotType.MINER) {
				Direction spawnDirection = getOptimalMinerSpawnDirection();
				if (spawnDirection != Direction.CENTER && rc.canBuildRobot(RobotType.MINER, spawnDirection)) {
					rc.buildRobot(RobotType.MINER, spawnDirection);
					++droidsBuilt;
					++minersBuilt;
					updateDroidsBuilt();
				}
			} else {
				for (int i = 0; i < directions.length; ++i) {
					if (rc.canBuildRobot(spawnType, directions[buildDirectionIndex])) {
						rc.buildRobot(spawnType, directions[buildDirectionIndex]);
						++droidsBuilt;
						if (spawnType == RobotType.SOLDIER) {
							++soldiersBuilt;
						} else if (spawnType == RobotType.BUILDER) {
							++buildersBuilt;
						}
						updateDroidsBuilt();
						break;
					}

					++buildDirectionIndex;
					if (buildDirectionIndex == directions.length)
						buildDirectionIndex = 0;
				}
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
