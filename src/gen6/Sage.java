package gen6;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import gen6.common.MovementHelper;
import gen6.sage.SageAttackHelper;
import gen6.sage.SageMovementHelper;

import static gen6.RobotPlayer.maxArchonCount;
import static gen6.RobotPlayer.rc;
import static gen6.common.Functions.getBits;

public strictfp class Sage {
	public static MapLocation myArchonLocation;
	public static int myArchonIndex;


	public static void run() throws GameActionException {
		SageMovementHelper.move();
		SageAttackHelper.attack();
	}

	public static void init() throws GameActionException {
		maxArchonCount = 0;
		for (int i = 32; i < 36; ++i) {
			int value = rc.readSharedArray(i);
			if (getBits(value, 15, 15) == 1) {
				++maxArchonCount;
				MapLocation archonLocation = new MapLocation(
						getBits(value, 6, 11), getBits(value, 0, 5)
				);
				if (rc.getLocation().distanceSquaredTo(archonLocation) <= 2) {
					myArchonLocation = new MapLocation(archonLocation.x, archonLocation.y);
					myArchonIndex = i - 32;
				}
			} else {
				break;
			}
		}
		SageMovementHelper.checkForCharge();
		MovementHelper.prepareBellmanFord(34);
		Soldier.init();
	}
}
