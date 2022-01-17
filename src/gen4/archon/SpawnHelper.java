package gen4.archon;

import battlecode.common.*;
import gen4.Archon;
import gen4.common.CommsHelper;
import gen4.builder.BuilderType;

import java.util.Random;

import static gen4.RobotPlayer.*;
import static gen4.common.Functions.getBits;
import static gen4.common.Functions.setBits;

public strictfp class SpawnHelper {
	private static final Random random = new Random(rc.getID());

	private static double getSoldierWeight() {
		return 0.70;
	}

	private static double getMinerWeight() {
		return 0.35;
	}

	private static double getBuilderWeight() throws GameActionException {
		if (rc.getRoundNum() < 750) return 0.00;
		if (getArchonWatchtowerPriority() > 1) return 0.00;
		if (buildersBuilt >= 4) return 0.01;
		if (buildersBuilt >= 2) return 0.050;
		return 0.100;
	}

	private static double getLabBuilderProbability() {
		if (labBuildersBuilt > 0) return 0;
		if (watchtowerBuildersBuilt >= 2) return 1;
		return 0;
	}

	private static double getSkipWeight() {
		return 0.0;
	}

	private static double getLeadThreshold() {
		if (rc.getRoundNum() < 1000) return 75;
		if (rc.getRoundNum() < 1500) return 225;
		return 450;
	}

	private static double getSageGoldThreshold() throws GameActionException {
		if (CommsHelper.getCentralArchonIndex() != Archon.myIndex) return 1000;
		if (rc.getRoundNum() < 1500) return 20;
		return 85;
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
	private static int labBuildersBuilt = 0;
	private static int watchtowerBuildersBuilt = 0;

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

	private static boolean[] archonDead = new boolean[4];

	private static int lastArchonCount = -1;
	private static void updateDeadArchons() throws GameActionException {
		if (lastArchonCount == -1) {
			lastArchonCount = maxArchonCount;
		}
		int latestCount = rc.getArchonCount();
		if (lastArchonCount == latestCount) {
			return;
		}
		lastArchonCount = latestCount;
		archonDead = CommsHelper.getDeadArchons();
	}

	private static int getArchonDroidPriority() throws GameActionException {
		updateDeadArchons();
		int p = 1;
		for (int i = 0; i < maxArchonCount; ++i) {
			if (Archon.myIndex != i && !archonDead[i] &&
					getBits(rc.readSharedArray(10 + i), 0, 11) < droidsBuilt
			) {
				p++;
			}
		}
		return p;
	}

	private static int getArchonWatchtowerPriority() throws GameActionException {
		updateDeadArchons();
		int p = 1, myHp = rc.getHealth();
		for (int i = 0; i < maxArchonCount; ++i) {
			if (Archon.myIndex != i && !archonDead[i]) {
				int theirHp = getBits(rc.readSharedArray(14 + i), 0, 10);
				int theirBuilders = getBits(rc.readSharedArray(10 + i), 12, 15);
				if (theirBuilders < buildersBuilt || theirBuilders == buildersBuilt && theirHp < myHp) {
					p++;
				}
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
		if (soldiersBuilt < 6) return RobotType.SOLDIER;
		if (minersBuilt < 5) return RobotType.MINER;
		if (soldiersBuilt < 9) return RobotType.SOLDIER;
		if (buildersBuilt < 1) {
			CommsHelper.setBuilderType(BuilderType.RepairBuilder, Archon.myIndex);
			return RobotType.BUILDER;
		}

		if (getSageGoldThreshold() <= rc.getTeamGoldAmount(myTeam)) {
			return RobotType.SAGE;
		}

		double sol = getSoldierWeight(),
				min = getMinerWeight(),
				bui = getBuilderWeight(),
				ski = getSkipWeight(),
				total = sol + min + bui + ski,
				rand = random.nextDouble();

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
			if (random.nextDouble() < getLabBuilderProbability()) {
				CommsHelper.setBuilderType(BuilderType.LabBuilder, Archon.myIndex);
				labBuildersBuilt++;
			} else {
				CommsHelper.setBuilderType(BuilderType.WatchtowerBuilder, Archon.myIndex);
				watchtowerBuildersBuilt++;
			}
			return RobotType.BUILDER;
		}
		return null;
	}
}
