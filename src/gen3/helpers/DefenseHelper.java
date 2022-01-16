package gen3.helpers;

import battlecode.common.MapLocation;
import battlecode.common.GameActionException;
import battlecode.common.Direction;
import gen3.common.CommsHelper;
import gen3.common.MovementHelper;

import static gen3.RobotPlayer.*;
import static gen3.util.Functions.getBits;
import static gen3.util.Functions.getDistance;

public strictfp class DefenseHelper {
	public static MapLocation getDefenseLocation() throws GameActionException {
		boolean[] archonDead = CommsHelper.getDeadArchons();
		MapLocation nearestLocation = null;
		int minDistance = rc.getMapWidth() * rc.getMapHeight();
		for (int i = 32; i < 32 + maxArchonCount; ++i) {
			int value = rc.readSharedArray(i);
			if (getBits(value, 14, 14) == 1 && !archonDead[i - 32]) {
				MapLocation location = CommsHelper.getLocationFrom12Bits(value);
				int distance = getDistance(rc.getLocation(), location);
				if (distance < minDistance) {
					minDistance = distance;
					nearestLocation = new MapLocation(location.x, location.y);
				}
			}
		}

		return nearestLocation;
	}

	public static boolean tryMoveRight(MapLocation location) throws GameActionException {
        if (!rc.isMovementReady()) {
            return false;
        }

        Direction dir = rc.getLocation().directionTo(location).rotateRight();
        if (!rc.getLocation().add(dir).isAdjacentTo(location) && MovementHelper.tryMove(dir, true)) {
            return true;
        } else if (MovementHelper.tryMove(dir.rotateRight(), true)) {
            return true;
        } else if (MovementHelper.tryMove(dir.rotateRight().rotateRight(), true)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean tryMoveRightAndBack(Direction dir) throws GameActionException {
    	if (!rc.isMovementReady()) {
    		return false;
    	}

    	dir = dir.rotateRight().rotateRight();
    	if (MovementHelper.tryMove(dir, true)) {
    		return true;
    	} else {
    		return MovementHelper.tryMove(dir.rotateRight(), true);
    	}
    }
}
