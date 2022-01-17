package gen4;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import gen4.common.MovementHelper;
import gen4.sage.SageAttackHelper;
import gen4.sage.SageMovementHelper;

import static gen4.RobotPlayer.maxArchonCount;
import static gen4.RobotPlayer.rc;
import static gen4.common.Functions.getBits;

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
		Soldier.init();
	}


}
