package gen1;

import battlecode.common.*;
import gen1.helpers.MiningHelper;
import gen1.helpers.MovementHelper;

import static gen1.RobotPlayer.*;
import static gen1.util.Functions.getBits;

public strictfp class Miner {
	private static MapLocation myArchonLocation;
	private static Direction myDirection;
	private static int myArchonIndex;
	private static int spawnMiningCooldown = 5;

	public static void run() throws GameActionException {

		if (spawnMiningCooldown > 0) {
			MovementHelper.tryMove(myDirection, false);
			spawnMiningCooldown--;
		}


		MapLocation goldLocation = MiningHelper.spotGold();
		if (goldLocation != null) {
			MovementHelper.tryMove(rc.getLocation().directionTo(goldLocation), false);
		}

		if (MiningHelper.canMineLead()) {
			MiningHelper.mineLead();
		} else {
			Direction leadDirection = MiningHelper.spotLead(myArchonLocation);
			if (leadDirection != null) {
				MovementHelper.tryMove(leadDirection, false);
			} else {
				MovementHelper.tryMove(myDirection, false);
			}
		}
	}

	public static void init() throws GameActionException {
		for (int i = 32; i < 32 + archonCount; ++i) {
			int value = rc.readSharedArray(i);
			myArchonLocation = new MapLocation(getBits(value, 6, 11), getBits(value, 0, 5));
			myDirection = myArchonLocation.directionTo(rc.getLocation());
			if (rc.getLocation().distanceSquaredTo(myArchonLocation) <= 2) {
				myArchonIndex = i - 32;
				break;
			}
		}
	}
}
