package gen1;

import battlecode.common.*;

import static gen1.RobotPlayer.*;
import static gen1.util.Functions.getBits;

public strictfp class Soldier {
	private static MapLocation myArchonLocation;
	private static int myArchonIndex;

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
	}
}
