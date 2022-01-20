package gen6.common;

import battlecode.common.MapLocation;
import battlecode.common.GameActionException;

import static gen6.RobotPlayer.*;
import static gen6.common.Functions.getBits;
import static gen6.common.Functions.setBits;

public enum SymmetryType {
	HORIZONTAL,
	VERTICAL,
	ROTATIONAL,
	NONE;

	public static SymmetryType getSymmetryType(MapLocation location1, MapLocation location2) throws GameActionException {
		boolean isHorizontal = location1.y + location2.y + 1 == rc.getMapHeight();
		boolean isVertical = location1.x + location2.x + 1 == rc.getMapWidth();
		if (isHorizontal && isVertical) {
			return SymmetryType.ROTATIONAL;
		} else if (isHorizontal && location1.x == location2.x) {
			return SymmetryType.HORIZONTAL;
		} else if (isVertical && location1.y == location2.y) {
			return SymmetryType.VERTICAL;
		} else {
			return SymmetryType.NONE;
		}
	}

	public static MapLocation[] getSymmetricalLocations(MapLocation location) throws GameActionException {
		MapLocation[] locations = new MapLocation[3];
		locations[0] = new MapLocation(location.x, rc.getMapHeight() - location.y - 1);
		locations[1] = new MapLocation(rc.getMapWidth() - location.x - 1, location.y);
		locations[2] = new MapLocation(rc.getMapWidth() - location.x - 1, rc.getMapHeight() - location.y - 1);
		return locations;
	}

	public static MapLocation getSymmetricalLocation(MapLocation location, SymmetryType symmetryType) throws GameActionException {
		MapLocation symmetricalLocation = new MapLocation(-1, -1);
		switch (symmetryType) {
			case HORIZONTAL:
				symmetricalLocation = new MapLocation(location.x, rc.getMapHeight() - location.y - 1);
				break;
			case VERTICAL:
				symmetricalLocation = new MapLocation(rc.getMapWidth() - location.x - 1, location.y);
				break;
			case ROTATIONAL:
				symmetricalLocation = new MapLocation(rc.getMapWidth() - location.x - 1, rc.getMapHeight() - location.y - 1);
				break;
			case NONE:
				symmetricalLocation = new MapLocation(location.x, location.y);
				break;
		}

		return symmetricalLocation;
	}

	public static SymmetryType getMapSymmetry() throws GameActionException {
		return SymmetryType.values()[getBits(rc.readSharedArray(4), 13, 14)];
	}

	public static void setMapSymmetry(SymmetryType symmetryType) throws GameActionException {
		int value = rc.readSharedArray(4);
		value = setBits(value, 13, 14, symmetryType.ordinal());
		rc.writeSharedArray(4, value);
	}
}
