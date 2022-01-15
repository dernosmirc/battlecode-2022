package gen3;

import battlecode.common.*;
import battlecode.common.MapLocation;
import gen3.common.CommsHelper;
import gen3.helpers.SpawnHelper;


import static gen3.RobotPlayer.*;
import static gen3.util.Functions.getBits;
import static gen3.util.Functions.setBits;

public strictfp class Archon {
	public static int myIndex;
	private static int buildDirectionIndex = 0;

	private static boolean[] isPossibleEnemyArchonSymmetry;
	private static int symmetryIndex = 0;

	private static final RobotType[] priority = {
			RobotType.SAGE,
			RobotType.SOLDIER,
			RobotType.MINER,
			RobotType.BUILDER,
	};

	private static MapLocation getHealLocation() {
		RobotInfo[] infos = rc.senseNearbyRobots(myType.actionRadiusSquared, myTeam);
		for (RobotType type: priority) {
			for (RobotInfo ri : infos) {
				if (ri.type == type) {
					return ri.location;
				}
			}
		}
		return null;
	}

	public static void run() throws GameActionException {
		// DON'T SPAWN SOLDIER ON FIRST ROUND
		if (rc.getRoundNum() == 2) {
			setCentralArchon();
		}

		if (rc.isActionReady()) {
			RobotType toSpawn = SpawnHelper.getNextDroid();
			if (toSpawn != null) {
				Direction direction = SpawnHelper.getOptimalDirection(directions[buildDirectionIndex], toSpawn);
				if (direction != null && rc.canBuildRobot(toSpawn, direction)) {
					buildDirectionIndex = direction.ordinal() + 1;
					SpawnHelper.incrementDroidsBuilt(toSpawn);
					rc.buildRobot(toSpawn, direction);
				}
			}
			if (rc.isActionReady()) {
				MapLocation toHeal = getHealLocation();
				if (toHeal != null && rc.canRepair(toHeal)) {
					rc.repair(toHeal);
				}
			}
		}

		if (buildDirectionIndex == 8) {
			buildDirectionIndex = 0;
		}
	}

	// Bits		Meaning
	// 0-7		Symmetries for next soldier, per archon
	// 8-10		Bad symmetries
	// 11-12	Archon index
	// 15		Indicator
	public static void setCentralArchon() throws GameActionException {
		if (getBits(rc.readSharedArray(4), 15, 15) == 1) {
			return;
		}

		MapLocation centre = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
		int minDistance = rc.getMapWidth() * rc.getMapHeight();
		int archonIndex = 0;
		for (int i = 32; i < 32 + archonCount; ++i) {
			MapLocation archonLocation = CommsHelper.getLocationFrom12Bits(rc.readSharedArray(i));
			int distance = Math.max(Math.abs(archonLocation.x - centre.x), Math.abs(archonLocation.y - centre.y));
			if (distance < minDistance) {
				minDistance = distance;
				archonIndex = i - 32;
			}
		}

		int value = setBits(0, 15, 15, 1);
		value = setBits(value, 11, 12, archonIndex);
		rc.writeSharedArray(4, value);
	}

	public static void broadcastSymmetry() throws GameActionException {
		if (CommsHelper.foundEnemyArchon()) {
			return;
		}

		int value = getBits(rc.readSharedArray(4), 8, 10);
		if (getBits(value, 8, 8) == 1) {
			isPossibleEnemyArchonSymmetry[0] = false;
		}
		if (getBits(value, 9, 9) == 1) {
			isPossibleEnemyArchonSymmetry[1] = false;
		}
		if (getBits(value, 10, 10) == 1) {
			isPossibleEnemyArchonSymmetry[2] = false;
		}

		for (int i = 0; i < 3; ++i) {
			if (symmetryIndex == 3) {
				symmetryIndex = 0;
			}
			if (isPossibleEnemyArchonSymmetry[symmetryIndex]) {
				CommsHelper.updateSymmetry(myIndex, symmetryIndex);
				++symmetryIndex;
				return;
			}

			++symmetryIndex;
		}

		CommsHelper.updateSymmetry(myIndex, 3);
	}

	public static void init() throws GameActionException {
		archonCount = rc.getArchonCount();
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

		isPossibleEnemyArchonSymmetry = new boolean[3];
		isPossibleEnemyArchonSymmetry[0] = isPossibleEnemyArchonSymmetry[1] = isPossibleEnemyArchonSymmetry[2] = true;
	}
}
