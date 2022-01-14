package gen3;

import battlecode.common.*;
import gen3.helpers.GoldMiningHelper;
import gen3.helpers.LeadMiningHelper;
import gen3.common.MovementHelper;
import gen3.util.Functions;
import gen3.util.Logger;

import static gen3.RobotPlayer.*;
import static gen3.util.Functions.getBits;
import static gen3.util.Functions.sigmoid;

public strictfp class Miner {
	private static final double GOLD_MINER_RATIO = 0.25;
	private static double getExplorerRatio() {
		return 0.65 * sigmoid((300-rc.getRoundNum())/100.0);
	}

	private static MapLocation myArchonLocation;
	public static Direction myDirection;
	private static int myArchonIndex;
	private static boolean isGoldMiner = false;
	private static boolean isExplorer = false;

	public static void run() throws GameActionException {
		Logger logger = new Logger("Miner", true);

		GoldMiningHelper.mineGold();
		GoldMiningHelper.updateGoldAmountInGridCell();
		LeadMiningHelper.updateLeadAmountInGridCell();

		if (rc.isMovementReady()) {
			Direction goldDirection = GoldMiningHelper.spotGold();
			if (goldDirection != null) {
				MovementHelper.tryMove(goldDirection, true);
			} else if (!isExplorer && isGoldMiner) {
				goldDirection = GoldMiningHelper.spotGoldOnGrid();
				if (goldDirection != null) {
					MovementHelper.tryMove(goldDirection, false);
				}
			}
			if (rc.isMovementReady()) {
				Direction leadDirection = LeadMiningHelper.spotLead();
				if (leadDirection != null) {
					MovementHelper.moveAndAvoid(leadDirection, myArchonLocation, 2);
				} else {
					if (!isExplorer && (leadDirection = LeadMiningHelper.spotLeadOnGrid()) != null) {
						MovementHelper.moveAndAvoid(leadDirection, myArchonLocation, 2);
					} else {
						Direction antiCorner = LeadMiningHelper.getAntiEdgeDirection();
						if (antiCorner != null) {
							myDirection = antiCorner;
						}
						MovementHelper.moveAndAvoid(myDirection, myArchonLocation, 2);
					}
				}
			}
		}

		if (LeadMiningHelper.canMineLead()) {
			LeadMiningHelper.mineLead();
		}

		logger.flush();
	}

	public static void init() throws GameActionException {
		isGoldMiner = Math.random() < GOLD_MINER_RATIO;
		isExplorer = Math.random() < getExplorerRatio();
		myDirection = Functions.getRandomDirection();
		archonCount = 0;
		for (int i = 32; i < 36; ++i) {
			int value = rc.readSharedArray(i);
			if (getBits(value, 15, 15) == 1) {
				++archonCount;
				MapLocation archonLocation = new MapLocation(
						getBits(value, 6, 11), getBits(value, 0, 5)
				);
				if (rc.getLocation().distanceSquaredTo(archonLocation) <= 2) {
					myArchonLocation = new MapLocation(archonLocation.x, archonLocation.y);
					myDirection = myArchonLocation.directionTo(rc.getLocation());
					myArchonIndex = i - 32;
				}
			} else {
				break;
			}
		}
	}
}
