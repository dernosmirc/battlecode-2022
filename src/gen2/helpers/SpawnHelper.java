package gen2.helpers;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;

import static gen2.RobotPlayer.*;

public strictfp class SpawnHelper {
	
	public static Direction getForceSpawnDirection(Direction dir) throws GameActionException {
		if (canSpawn(dir)) {
			return dir;
		} else if (canSpawn(dir.rotateLeft())) {
			return dir.rotateLeft();
		} else if (canSpawn(dir.rotateRight())) {
			return dir.rotateRight();
		} else if (canSpawn(dir.rotateLeft().rotateLeft())) {
			return dir.rotateLeft().rotateLeft();
		} else if (canSpawn(dir.rotateRight().rotateRight())) {
			return dir.rotateRight().rotateRight();
		} else if (canSpawn(dir.rotateLeft().rotateLeft().rotateLeft())) {
			return dir.rotateLeft().rotateLeft().rotateLeft();
		} else if (canSpawn(dir.rotateRight().rotateRight().rotateRight())) {
			return dir.rotateRight().rotateRight().rotateRight();
		} else if (canSpawn(dir.opposite())) {
			return dir.opposite();
		}

		return Direction.CENTER;
	}

	public static boolean canSpawn(Direction dir) throws GameActionException {
		MapLocation loc = rc.getLocation().add(dir);
		return rc.onTheMap(loc) && !rc.canSenseRobotAtLocation(loc);
	}

	private static double getSoldierProbability() {
		return 0.65;
	}

	private static double getBuilderProbability() {
		if (rc.getRoundNum() < 100) return 0;
		if (rc.getRoundNum() < 200) return 0.2;
		return 0.05;
	}

	private static double getMinerProbability() {
		if (rc.getRoundNum() < 300) return 0.30;
		if (rc.getRoundNum() < 500) return 0.20;
		return 0.10;
	}

	private static double getSkipProbability() {
		if (rc.getRoundNum() < 250) return 0.00;
		if (rc.getRoundNum() < 1000) return 0.40;
		if (rc.getRoundNum() < 1500) return 1;
		return 3;
	}

	public static RobotType getNextDroid() {
		double sol = getSoldierProbability(),
				min = getMinerProbability(),
				bui = getBuilderProbability(),
				ski = getSkipProbability(),
				total = sol + min + bui + ski,
				rand = Math.random();
		if (total == 0) {
			return null;
		}
		sol /= total;
		min /= total;
		bui /= total;
		if (rand < sol) {
			return RobotType.SOLDIER;
		} else if (rand < sol + min) {
			return RobotType.MINER;
		} else if (rand < sol + min + bui) {
			return RobotType.BUILDER;
		} else {
			return null;
		}
	}
}
