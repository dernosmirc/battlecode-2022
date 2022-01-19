package gen5;

import battlecode.common.*;
import gen5.common.CommsHelper;
import gen5.miner.GoldMiningHelper;
import gen5.miner.LeadMiningHelper;
import gen5.common.MovementHelper;
import gen5.common.Functions;
import gen5.common.util.Logger;

import java.util.Random;

import static gen5.RobotPlayer.*;
import static gen5.common.Functions.getBits;
import static gen5.common.Functions.sigmoid;

public strictfp class Miner {
	private static final double GOLD_MINER_RATIO = 0.25;
	private static double getExplorerRatio() {
		return 0.5 * sigmoid((300-rc.getRoundNum())/200.0);
	}

	private static MapLocation myArchonLocation;
	public static Direction myDirection;
	private static int myArchonIndex;
	private static boolean isGoldMiner = false;
	private static boolean isExplorer = false;
	private static final Random random = new Random(rc.getID());

	public static void run() throws GameActionException {
		Logger logger = new Logger("Miner", true);
		int round = rc.getRoundNum();

		GoldMiningHelper.mineGold();
		GoldMiningHelper.updateGoldAmountInGridCell();
		LeadMiningHelper.mineLead();

		if (rc.isMovementReady()) {
			logger.log("moving");
			if (!move()) {
				MovementHelper.tryMove(myDirection, false);
			};
			logger.log("moved");
		}

		if (Clock.getBytecodesLeft() >= 3500 && round == rc.getRoundNum()) {
			logger.log("Updating Lead");
			LeadMiningHelper.updateLeadAmountInGridCell();
			logger.log("Updated Lead");
		}
		//logger.flush();
	}

	private static boolean move() throws GameActionException {
		MapLocation gold = GoldMiningHelper.spotGold();
		if (gold != null) {
			return MovementHelper.moveBellmanFord(gold);
		}
		if (!isExplorer && isGoldMiner) {
			gold = GoldMiningHelper.spotGoldOnGrid();
			if (gold != null) {
				return MovementHelper.moveBellmanFord(gold);
			}
		}

		MapLocation lead = LeadMiningHelper.spotLead();
		if (lead != null) {
			return MovementHelper.moveBellmanFord(lead);
		}
		if (!isExplorer && (lead = LeadMiningHelper.spotLeadOnGrid()) != null) {
			return MovementHelper.moveBellmanFord(lead);
		}

		Direction antiCorner = LeadMiningHelper.getAntiEdgeDirection();
		if (antiCorner != null) {
			myDirection = Functions.getPerpendicular(antiCorner);
		}
		if (
				!CommsHelper.isLocationInEnemyZone(rc.getLocation()) &&
				CommsHelper.isLocationInEnemyZone(rc.getLocation().add(myDirection))
		) {
			myDirection = myDirection.opposite();
		}
		return MovementHelper.moveBellmanFord(myDirection);
	}

	public static void init() throws GameActionException {
		isGoldMiner = random.nextDouble() < GOLD_MINER_RATIO;
		isExplorer = random.nextDouble() < getExplorerRatio();
		myDirection = Functions.getRandomDirection();
		maxArchonCount = 0;
		MovementHelper.prepareBellmanFord(20);
		for (int i = 32; i < 36; ++i) {
			int value = rc.readSharedArray(i);
			if (getBits(value, 15, 15) == 1) {
				++maxArchonCount;
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
