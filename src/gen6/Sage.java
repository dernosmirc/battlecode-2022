package gen6;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Clock;
import gen6.common.MovementHelper;
import gen6.miner.GoldMiningHelper;
import gen6.miner.LeadMiningHelper;
import gen6.sage.SageAttackHelper;
import gen6.sage.SageMovementHelper;
import gen6.soldier.SoldierDensity;
import gen6.soldier.TailHelper;

import static gen6.RobotPlayer.maxArchonCount;
import static gen6.RobotPlayer.rc;
import static gen6.common.Functions.getBits;

public strictfp class Sage {
	public static MapLocation myArchonLocation;
	public static int myArchonIndex;


	public static void run() throws GameActionException {
		// Update Sage count
		if (rc.getRoundNum()%2 == 1){
			rc.writeSharedArray(9, rc.readSharedArray(9) + 1);
		}
		SoldierDensity.update();
		TailHelper.updateTarget();
		if (rc.isActionReady()) {
			SageAttackHelper.attack();
		}
		if (rc.isMovementReady()) {
			SageMovementHelper.move();
		}
		GoldMiningHelper.updateGoldAmountInGridCell();
		if (Clock.getBytecodeNum() < 5500) {
			LeadMiningHelper.updateLeadAmountInGridCell();
		}
	}

	public static void init() throws GameActionException {
		maxArchonCount = 0;
		for (int i = 0; i < 4; ++i) {
			int value = rc.readSharedArray(i + 32);
			if (getBits(value, 15, 15) == 1) {
				++maxArchonCount;
				value = rc.readSharedArray(i + 50);
				MapLocation archonLocation = new MapLocation(
						getBits(value, 6, 11), getBits(value, 0, 5)
				);
				if (rc.getLocation().distanceSquaredTo(archonLocation) <= 2) {
					myArchonLocation = archonLocation;
					myArchonIndex = i;
				}
			} else {
				break;
			}
		}
		SageMovementHelper.checkForCharge();
		MovementHelper.prepareBellmanFord(34);
	}
}
