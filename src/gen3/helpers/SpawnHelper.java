package gen3.helpers;

import battlecode.common.*;
import gen3.Archon;

import static gen3.RobotPlayer.*;
import static gen3.util.Functions.getBits;
import static gen3.util.Functions.setBits;

public strictfp class SpawnHelper {

	private static double getSoldierWeight() {
		return 0.65;
	}

	private static double getMinerWeight() {
		return 0.35;
	}

	private static double getBuilderWeight() throws GameActionException {
		double value = 0.00;
		if (rc.getRoundNum() < 750) value = 0.00;
		else if (buildersBuilt >= 8) value = 0.00;
		else if (buildersBuilt >= 3) value = 0.025;
		else if (buildersBuilt >= 2) value = 0.05;
		else if (buildersBuilt >= 0) value = 0.10;
		return value / getArchonWatchtowerPriority();
	}

	private static double getSkipWeight() {
		return 0.0;
	}

	private static double getLeadThreshold() {
		if (rc.getRoundNum() < 750) return 75;
		if (rc.getRoundNum() < 1250) return 220;
		if (rc.getRoundNum() < 1500) return 250;
		return 400;
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
		int int16 = setBits(droidsBuilt, 12, 15, buildersBuilt);
		rc.writeSharedArray(10 + Archon.myIndex, int16);
	}

	private static int getArchonDroidPriority() throws GameActionException {
		int p = 1;
		for (int i = 0; i < archonCount; ++i) {
			if (Archon.myIndex != i &&
					getBits(rc.readSharedArray(10 + i), 0, 11) < droidsBuilt
			) {
				p++;
			}
		}
		return p;
	}

	private static int getArchonWatchtowerPriority() throws GameActionException {
		int p = 1;
		for (int i = 0; i < archonCount; ++i) {
			if (Archon.myIndex != i &&
					getBits(rc.readSharedArray(10 + i), 12, 15) < buildersBuilt
			) {
				p++;
			}
		}
		return p;
	}

	private static final boolean[] builderSpawned = new boolean[8];
	private static Direction getOptimalBuilderSpawnDirection() throws GameActionException {
		MapLocation center = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
		int ideal = rc.getLocation().directionTo(center).ordinal();
		for (int i = 0; i < 5; i++) {
			int l = (ideal + i) % 8, r = (ideal - i + 8) % 8;
			if (!builderSpawned[l] && !rc.isLocationOccupied(rc.getLocation().add(directions[l]))) {
				builderSpawned[l] = true;
				return directions[l];
			}
			if (!builderSpawned[r] && !rc.isLocationOccupied(rc.getLocation().add(directions[r]))) {
				builderSpawned[r] = true;
				return directions[r];
			}
		}
		return null;
	}

	private static Direction getOptimalSoldierSpawnDirection() {
		MapLocation center = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
		return rc.getLocation().directionTo(center);
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
		if (type == RobotType.BUILDER) {
			return getOptimalBuilderSpawnDirection();
		}
		if (type == RobotType.MINER) {
			to = getOptimalMinerSpawnDirection();
		} else if (type == RobotType.SOLDIER) {
			to = getOptimalSoldierSpawnDirection();
		}
		MapLocation current = rc.getLocation();
		if (to == null) {
			to = Direction.SOUTHEAST;
		}
		int dirInt = to.ordinal();
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
		if (rc.getTeamLeadAmount(myTeam) < threshold * getArchonDroidPriority()) {
			return null;
		}

		if (minersBuilt < 3) return RobotType.MINER;
		if (soldiersBuilt < 3) return RobotType.SOLDIER;
		if (minersBuilt < 4) return RobotType.MINER;
		if (soldiersBuilt < 6) return RobotType.SOLDIER;
		if (minersBuilt < 5) return RobotType.MINER;
		if (soldiersBuilt < 9) return RobotType.SOLDIER;

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
