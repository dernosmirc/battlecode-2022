package gen6.archon;

import battlecode.common.*;

import gen6.Archon;
import gen6.common.CommsHelper;
import gen6.builder.BuilderType;
import gen6.common.Functions;

import java.util.Random;

import static gen6.RobotPlayer.*;
import static gen6.common.Functions.getBits;
import static gen6.common.Functions.sigmoid;

public strictfp class SpawnHelper {
	private static final int ARCHON_MUTATE_WINDOW = 50;
	private static final int LAB_WINDOW = 75;
	private static final int SOLDIER_SAGE_RATIO = 3;

	private static final Random random = new Random(rc.getID());

	private static double getSoldierMinerRatio() {
		return 2 + sigmoid((rc.getRoundNum() - 1000.0)/200);
	}

	private static boolean shouldBuildMiner() throws GameActionException {
		int attackWeight = CommsHelper.getAliveSoldierCount() + SOLDIER_SAGE_RATIO * CommsHelper.getAliveSageCount();
		return attackWeight > getSoldierMinerRatio()*CommsHelper.getAliveMinerCount();
	}

	private static double getSoldierWeight() throws GameActionException {
		if (!shouldBuildMiner()) {
			return 1;
		}
		return 0;
	}

	private static double getMinerWeight() throws GameActionException {
		if (shouldBuildMiner()) {
			return 1;
		}
		return 0;
	}

	private static double getBuilderWeight() {
		if (rc.getRoundNum() < 1000 && farmSeedsBuilt > 15) return 0.00;
		if (rc.getRoundNum() < 250) return 0.00;
		if (rc.getRoundNum() < 500) return 0.5;
		if (rc.getRoundNum() < 1000) return 0.25;
		if (labBuildersBuilt < 2 && rc.getRoundNum() > 1000) return 0.100;
		return 0.005;
	}

	private static BuilderType geNextBuilderType() {
		if (rc.getRoundNum() < 1000) {
			farmSeedsBuilt++;
			return BuilderType.FarmSeed;
		} else {
			labBuildersBuilt++;
			return BuilderType.LabBuilder;
		}
	}

	public static double getLeadThreshold() throws GameActionException {
		if (175 <= rc.getRoundNum() && rc.getRoundNum() < 175 + LAB_WINDOW &&
				CommsHelper.getNumberOfLabs() < 1
		) return 260;
		if (1250 <= rc.getRoundNum() && rc.getRoundNum() < 1250 + ARCHON_MUTATE_WINDOW &&
				!CommsHelper.allLArchonsMutated(2)
		) return 375;
		if (1000 <= rc.getRoundNum() && rc.getRoundNum() < 1000 + LAB_WINDOW &&
				!CommsHelper.allLabsBuilt()
		) return 260;
		return 75;
	}

	private static double getSageGoldThreshold() throws GameActionException {
		if (CommsHelper.getCentralArchon() != Archon.myIndex) return 100;
		return 20;
	}

	private static int droidsBuilt = 0;
	private static int minersBuilt = 0;
	private static int soldiersBuilt = 0;
	private static int sagesBuilt = 0;
	private static int buildersBuilt = 0;
	private static int labBuildersBuilt = 0;
	private static int repairersBuilt = 0;
	private static int farmSeedsBuilt = 0;

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
			case SAGE:
				sagesBuilt++;
				break;
		}
		droidsBuilt++;
		int val = Functions.setBits(rc.readSharedArray(10 + Archon.myIndex), 0, 11, droidsBuilt);
		rc.writeSharedArray(10 + Archon.myIndex, val);
	}

	public static void levelDroidsBuilt() throws GameActionException {
		updateDeadArchons();
		for (int i = 0; i < maxArchonCount; ++i) {
			if (Archon.myIndex != i && !archonDead[i] && !CommsHelper.isArchonPortable(i)) {
				droidsBuilt = getBits(rc.readSharedArray(10 + i), 0, 11);
			}
		}
	}

	private static boolean[] archonDead = new boolean[4];

	private static void updateDeadArchons() throws GameActionException {
		archonDead = CommsHelper.getDeadArchons();
	}

	private static int getArchonDroidPriority() throws GameActionException {
		updateDeadArchons();
		int p = 1;
		for (int i = 0; i < maxArchonCount; ++i) {
			if (Archon.myIndex != i && !archonDead[i] && !CommsHelper.isArchonPortable(i) &&
					getBits(rc.readSharedArray(10 + i), 0, 11) < droidsBuilt
			) {
				p++;
			}
		}
		return p;
	}

	private static boolean isBuilderAround() {
		RobotInfo[] ris = rc.senseNearbyRobots(myType.visionRadiusSquared, myTeam);
		for (int i = ris.length; --i >= 0;) {
			if (ris[i].type == RobotType.BUILDER) {
				return true;
			}
		}
		return false;
	}

	private static boolean isEnemyAround() {
		RobotInfo[] ris = rc.senseNearbyRobots(myType.visionRadiusSquared, enemyTeam);
		for (int i = ris.length; --i >= 0;) {
			if (ris[i].type.canAttack()) {
				return true;
			}
		}
		return false;
	}

	private static final boolean[] builderSpawned = new boolean[8];
	private static Direction getOptimalBuilderSpawnDirection() throws GameActionException {
		MapLocation center = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
		int ideal = rc.getLocation().directionTo(center).ordinal();
		for (int i = 0; i < 5; i++) {
			int l = (ideal + i) % 8, r = (ideal - i + 8) % 8;
			MapLocation ml = rc.getLocation().add(directions[l]);
			if (!builderSpawned[l] && rc.onTheMap(ml) && !rc.isLocationOccupied(ml)) {
				builderSpawned[l] = true;
				return directions[l];
			}
			ml = rc.getLocation().add(directions[r]);
			if (!builderSpawned[r] && rc.onTheMap(ml) && !rc.isLocationOccupied(ml)) {
				builderSpawned[r] = true;
				return directions[r];
			}
		}
		return getOptimalSoldierSpawnDirection();
	}

	private static Direction getOptimalSoldierSpawnDirection() throws GameActionException{
		int minRubble = 1000;
		Direction d = null;
		Direction cur = Direction.NORTH;
		MapLocation curLoc = rc.getLocation();
		for (int i = 8; --i >= 0;) {
			MapLocation loc = curLoc.add(cur);
			if (rc.canSenseLocation(loc) && !rc.isLocationOccupied(loc) && rc.senseRubble(loc) < minRubble){
				minRubble = rc.senseRubble(loc);
				d = cur;
			}
			cur = cur.rotateRight();
		}
		if (d == null) return Direction.NORTH;
		else return d;
	}

	private static Direction getOptimalMinerSpawnDirection() throws GameActionException {
		if (rc.getRoundNum() > 100) {
			return getOptimalSoldierSpawnDirection();
		}
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

	private static boolean isVeryCloseToEnemy() {
		RobotInfo[] ris = rc.senseNearbyRobots(myType.visionRadiusSquared, enemyTeam);
		for (int i = ris.length; --i >= 0;) {
			if (ris[i].type == RobotType.ARCHON) {
				return true;
			}
		}
		MapLocation center = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
		return rc.getLocation().isWithinDistanceSquared(center, 34);
	}

	public static int mapSizeType = 0;

	public static RobotType getNextDroid() throws GameActionException {
		if (getSageGoldThreshold() <= rc.getTeamGoldAmount(myTeam)) {
			return RobotType.SAGE;
		}

		if (CommsHelper.getFarthestArchon() == Archon.myIndex &&
				labBuildersBuilt < 1 && 100 <= rc.getRoundNum()
		) {
			CommsHelper.setBuilderType(BuilderType.LabBuilder, Archon.myIndex);
			labBuildersBuilt++;
			return RobotType.BUILDER;
		}

		double threshold = getLeadThreshold();
		if (rc.getTeamLeadAmount(myTeam) < threshold * getArchonDroidPriority()) {
			return null;
		}

		if (isVeryCloseToEnemy()) {
			int centerFactor = maxArchonCount;
			if (minersBuilt < 3/centerFactor) return RobotType.MINER;
			if (soldiersBuilt < 6*centerFactor) return RobotType.SOLDIER;
		} else {
			if (minersBuilt < 4) return RobotType.MINER;
			if (soldiersBuilt < 6) return RobotType.SOLDIER;
			if (minersBuilt < 6) return RobotType.MINER;
			if (soldiersBuilt < 9) return RobotType.SOLDIER;
		}

		if (!isBuilderAround() && !isEnemyAround() && repairersBuilt < 3) {
			CommsHelper.setBuilderType(BuilderType.Repairer, Archon.myIndex);
			repairersBuilt++;
			return RobotType.BUILDER;
		}

		if (
				labBuildersBuilt < 2 &&
						1000 <= rc.getRoundNum() && rc.getRoundNum() < 1000 + LAB_WINDOW
		) {
			CommsHelper.setBuilderType(BuilderType.LabBuilder, Archon.myIndex);
			labBuildersBuilt++;
			return RobotType.BUILDER;
		}

		double sol = getSoldierWeight(),
				min = getMinerWeight(),
				bui = getBuilderWeight(),
				total = sol + min + bui,
				rand = random.nextDouble();

		if (total == 0) {
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
			BuilderType type = geNextBuilderType();
			if (type != null) {
				CommsHelper.setBuilderType(type, Archon.myIndex);
				return RobotType.BUILDER;
			}
		}
		return null;
	}
}
