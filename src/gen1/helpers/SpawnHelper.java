package gen1.helpers;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import static gen1.RobotPlayer.*;

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
}
