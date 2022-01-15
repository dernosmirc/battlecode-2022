package gen3.helpers;

import battlecode.common.MapLocation;
import battlecode.common.GameActionException;
import gen3.common.CommsHelper;

import static gen3.RobotPlayer.*;
import static gen3.util.Functions.getBits;
import static gen3.util.Functions.getDistance;

public strictfp class AttackHelper {

	public static MapLocation getDefenseLocation() throws GameActionException {
		MapLocation nearestLocation = null;
		int minDistance = rc.getMapWidth() * rc.getMapHeight();
		for (int i = 32; i < 32 + maxArchonCount; ++i) {
			int value = rc.readSharedArray(i);
			if (getBits(value, 14, 14) == 1) {
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
}
