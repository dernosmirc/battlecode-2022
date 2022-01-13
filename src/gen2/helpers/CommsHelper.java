package gen2.helpers;

import battlecode.common.MapLocation;
import battlecode.common.GameActionException;

import static gen2.RobotPlayer.*;
import static gen2.util.Functions.getBits;

public strictfp class CommsHelper {

	public static MapLocation getLocationFrom12Bits(int bits) {
		return new MapLocation(getBits(bits, 6, 11), getBits(bits, 0, 5));
	}

	public static MapLocation getEnemyArchonLocation() throws GameActionException {
		for (int i = 0; i < archonCount; ++i) {
			int value = rc.readSharedArray(i);
			if (getBits(value, 15, 15) == 1) {
				return getLocationFrom12Bits(value);
			}
		}

		return null;
	}
}
