package gen2;

import battlecode.common.*;
import gen2.util.Functions;

import static gen2.RobotPlayer.*;
import static gen2.util.Functions.getBits;

public strictfp class Builder {

	public static class ConstructionInfo {
		public MapLocation location;
		public RobotType type;
		public ConstructionInfo(RobotType t, MapLocation loc) {
			type = t;
			location = loc;
		}
	}

	private static MapLocation myArchonLocation;
	public static Direction myDirection;
	private static int myArchonIndex;
	private static ConstructionInfo nextBuilding;

	public static void run() throws GameActionException {

	}

	public static void init() throws GameActionException {
		for (int i = 32; i < 32 + archonCount; ++i) {
			int value = rc.readSharedArray(i);
			myArchonLocation = new MapLocation(getBits(value, 6, 11), getBits(value, 0, 5));
			if (rc.getLocation().distanceSquaredTo(myArchonLocation) <= 2) {
				myArchonIndex = i - 32;
				break;
			}
		}
		myDirection = myArchonLocation.directionTo(rc.getLocation());
		nextBuilding = new ConstructionInfo(
				RobotType.WATCHTOWER,
				Functions.translate(myArchonLocation, myDirection, 2)
		);
	}
}
