package gen2;

import battlecode.common.*;
import gen2.helpers.GoldMiningHelper;
import gen2.helpers.LeadMiningHelper;
import gen2.helpers.MovementHelper;
import gen2.util.Logger;

import static gen2.RobotPlayer.*;
import static gen2.util.Functions.getBits;

public strictfp class Miner {
	private static final double GOLD_MINER_RATIO = 0.2;

	private static MapLocation myArchonLocation;
	public static Direction myDirection;
	private static int myArchonIndex;
	private static boolean isGoldMiner = false;

	public static void run() throws GameActionException {
		Logger logger = new Logger("Miner", true);
		GoldMiningHelper.mineGold();
		logger.log("Mined gold");
		GoldMiningHelper.updateGoldAmountInGridCell();
		logger.log("Updated Gold");
		LeadMiningHelper.updateLeadAmountInGridCell();
		logger.log("Updated lead");

		if (rc.isMovementReady()) {
			Direction goldDirection = GoldMiningHelper.spotGold();
			if (goldDirection != null) {
				MovementHelper.tryMove(goldDirection, false);
			} else if (isGoldMiner) {
				goldDirection = GoldMiningHelper.spotGoldOnGrid();
				if (goldDirection != null) {
					MovementHelper.tryMove(goldDirection, false);
				}
			}
		}
		logger.log("moved towards gold");

		if (LeadMiningHelper.canMineLead()) {
			LeadMiningHelper.mineLead();
		} else {
			logger.log("spotting lead on grid");
			Direction leadDirection = LeadMiningHelper.spotLeadOnGrid();
			logger.log("spotted lead on grid");
			if (leadDirection != null) {
				MovementHelper.moveAndAvoid(leadDirection, myArchonLocation, 2);
			}
		}
		if (rc.isMovementReady()) {
			Direction leadDirection = LeadMiningHelper.spotLead();
			logger.log("spotted lead nearby");
			if (leadDirection != null) {
				MovementHelper.moveAndAvoid(leadDirection, myArchonLocation, 2);
			} else {
				MovementHelper.moveAndAvoid(myDirection, myArchonLocation, 2);
			}
		}
		logger.flush();
	}

	public static void init() throws GameActionException {
		isGoldMiner = Math.random() < GOLD_MINER_RATIO;
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