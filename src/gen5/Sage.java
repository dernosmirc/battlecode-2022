package gen5;

import battlecode.common.AnomalyScheduleEntry;
import battlecode.common.AnomalyType;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import gen5.common.MovementHelper;
import gen5.common.util.Vector;
import gen5.sage.SageAttackHelper;
import gen5.sage.SageMovementHelper;

import static gen5.RobotPlayer.maxArchonCount;
import static gen5.RobotPlayer.rc;
import static gen5.common.Functions.getBits;

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
