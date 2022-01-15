package gen3.common;

import battlecode.common.MapLocation;
import battlecode.common.GameActionException;
import gen3.util.SymmetryType;

import static gen3.RobotPlayer.*;
import static gen3.util.Functions.getBits;
import static gen3.util.Functions.setBits;

public strictfp class CommsHelper {

	public static MapLocation getLocationFrom12Bits(int bits) {
		return new MapLocation(getBits(bits, 6, 11), getBits(bits, 0, 5));
	}

	private static int getDistance(MapLocation location1, MapLocation location2) {
		return Math.max(Math.abs(location1.x - location2.x), Math.abs(location1.y - location2.y));
	}

	public static MapLocation getEnemyArchonLocation() throws GameActionException {
		MapLocation archonLocation = null;
		MapLocation centre = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
		int minDistance = rc.getMapWidth() * rc.getMapHeight();
		for (int i = 0; i < archonCount; ++i) {
			int value = rc.readSharedArray(i);
			if (getBits(value, 15, 15) == 1) {
				MapLocation location = getLocationFrom12Bits(value);
				int distance = getDistance(centre, location);
				if (distance <  minDistance) {
					minDistance = distance;
					archonLocation = new MapLocation(location.x, location.y);
				}
			}
		}

		return archonLocation;
	}

	public static boolean foundEnemyArchon() throws GameActionException {
		for (int i = 0; i < archonCount; ++i) {
			if (getBits(rc.readSharedArray(i), 15, 15) == 1) {
				return true;
			}
		}

		return false;
	}

	public static void updateSymmetry(int archonIndex, int value) throws GameActionException {
		value = setBits(rc.readSharedArray(4), 2 * archonIndex, 2 * archonIndex + 1, value);
		rc.writeSharedArray(4, value);
	}

	public static SymmetryType getBroadcastedSymmetry(int archonIndex) throws GameActionException {
		int value = rc.readSharedArray(4);
		return SymmetryType.values()[getBits(value, 2 * archonIndex, 2 * archonIndex + 1)];
	}

	public static SymmetryType getPossibleSymmetry() throws GameActionException {
		int value = getBits(rc.readSharedArray(4), 8, 10);
		// TODO: store values array inside SymmetryType
		if ((value & 0b1) == 0) {
			return SymmetryType.values()[0];
		} else if ((value & 0b10) == 0) {
			return SymmetryType.values()[1];
		} else if ((value & 0b100) == 0) {
			return SymmetryType.values()[2];
		} else {
			return SymmetryType.NONE;
		}
	}
}
