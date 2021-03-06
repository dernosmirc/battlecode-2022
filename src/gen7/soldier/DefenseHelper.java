package gen7.soldier;

import battlecode.common.MapLocation;
import battlecode.common.GameActionException;
import battlecode.common.Direction;
import gen7.common.CommsHelper;
import gen7.common.MovementHelper;

import static gen7.RobotPlayer.*;
import static gen7.common.Functions.getBits;
import static gen7.common.Functions.getDistance;

public strictfp class DefenseHelper {
	public static MapLocation getDefenseLocation() throws GameActionException {
		boolean[] archonDead = CommsHelper.getDeadArchons();
		MapLocation nearestLocation = null;
		int minDistance = rc.getMapWidth() * rc.getMapHeight();
		for (int i = 50; i < 50 + maxArchonCount; ++i) {
			int value = rc.readSharedArray(i);
			if (getBits(rc.readSharedArray(32 + (i - 50)), 14, 14) == 1 && !archonDead[i - 50]) {
				MapLocation location = CommsHelper.getLocationFrom12Bits(value);
				int distance = getDistance(rc.getLocation(), location);
				if (distance < minDistance) {
					minDistance = distance;
					nearestLocation = location;
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
