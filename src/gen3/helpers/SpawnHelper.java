package gen3.helpers;

import battlecode.common.*;
import gen3.Archon;

import static gen3.RobotPlayer.*;
import static gen3.common.MovementHelper.directionVector;
import static gen3.util.Functions.getBits;

public strictfp class SpawnHelper {

	private static double getSoldierWeight() {
		if (rc.getRoundNum() < 250) return 0.65;
		if (rc.getRoundNum() < 500) return 0.65;
		if (rc.getRoundNum() < 750) return 0.65;
		if (rc.getRoundNum() < 1000) return 0.65;
		return 0.80;
	}

	private static double getMinerWeight() {
		if (rc.getRoundNum() < 250) return 0.30;
		if (rc.getRoundNum() < 500) return 0.30;
		if (rc.getRoundNum() < 750) return 0.30;
		if (rc.getRoundNum() < 1000) return 0.30;
		return 0.30;
	}

	private static double getBuilderWeight() {
		if (rc.getRoundNum() < 250) return 0.00;
		if (rc.getRoundNum() < 500) return 0.05;
		if (rc.getRoundNum() < 750) return 0.05;
		if (rc.getRoundNum() < 1000) return 0.05;
		return 0.10;
	}

	private static double getSkipWeight() {
		if (rc.getRoundNum() < 1000) return 0.00;
		if (rc.getRoundNum() < 1500) return 0.50;
		return 1;
	}

	private static double getLeadThreshold() {
		return 75;
	}

	private static double getLeadIncomeThreshold() {
		if (rc.getRoundNum() < 150) return 2;
		if (rc.getRoundNum() < 500) return 2;
		if (rc.getRoundNum() < 1000) return 2;
		if (rc.getRoundNum() < 1500) return 2;
		return 2;
	}

	private static int droidsBuilt = 0;
	private static int minersBuilt = 0;
	private static int soldiersBuilt = 0;
	private static int buildersBuilt = 0;

	public static void incrementDroidsBuilt(RobotType droid) throws GameActionException {
		switch (droid) {
			case MINER:
				minersBuilt++;
				break;
			case SOLDIER:
				Archon.broadcastSymmetry();
				soldiersBuilt++;
				break;
			case BUILDER:
				buildersBuilt++;
				break;
		}
		droidsBuilt++;
		rc.writeSharedArray(10 + Archon.myIndex, droidsBuilt);
	}

	private static int getArchonPriority() throws GameActionException {
		int p = 1;
		for (int i = 0; i < archonCount; ++i) {
			if (Archon.myIndex != i &&
					getBits(rc.readSharedArray(10 + i), 0, 15) < droidsBuilt
			) {
				p++;
			}
		}
		return p;
	}

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

		return optimalDirection;
	}

	public static Direction getOptimalDirection (Direction to, RobotType type) throws GameActionException {
		if (type == RobotType.MINER) {
			to = getOptimalMinerSpawnDirection();
		}
		MapLocation current = rc.getLocation();
		if (to == null) {
			to = Direction.SOUTHEAST;
		}
		int dirInt = directionVector.indexOf(to);
		// if blocked by another robot, find the next best direction
		for (int i = 0; i < 5; i++) {
			Direction got = directions[Math.floorMod(dirInt + i, 8)];
			MapLocation ml = current.add(got);
			if (rc.onTheMap(ml) && !rc.isLocationOccupied(ml)) {
				return got;
			}
			got = directions[Math.floorMod(dirInt - i, 8)];
			ml = current.add(got);
			if (rc.onTheMap(ml) && !rc.isLocationOccupied(ml)) {
				return got;
			}
		}
		return null;
	}


	public static RobotType getNextDroid() throws GameActionException {
		double threshold = getLeadThreshold();
		if (rc.getTeamLeadAmount(myTeam) < threshold * getArchonPriority()) {
			return null;
		}
		if (droidsBuilt < 2) {
			return RobotType.MINER;
		}
		if (soldiersBuilt < 3) {
			return RobotType.SOLDIER;
		}

		double sol = getSoldierWeight(),
				min = getMinerWeight(),
				bui = getBuilderWeight(),
				ski = getSkipWeight(),
				total = sol + min + bui + ski,
				rand = Math.random();

		if (total - ski == 0) {
			return null;
		}

		sol /= total;
		min /= total;
		bui /= total;
		if (rand < sol) {
			return RobotType.SOLDIER;
		}
		if (rand < sol + min) {
			return RobotType.MINER;
		}
		if (rand < sol + min + bui) {
			return RobotType.BUILDER;
		}
		return null;
	}
}
