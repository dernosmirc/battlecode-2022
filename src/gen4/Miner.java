package gen4;

import battlecode.common.*;
import gen4.common.CommsHelper;
import gen4.miner.GoldMiningHelper;
import gen4.miner.LeadMiningHelper;
import gen4.common.MovementHelper;
import gen4.common.Functions;
import gen4.common.util.Logger;

import java.util.Random;

import static gen4.RobotPlayer.*;
import static gen4.common.Functions.getBits;
import static gen4.common.Functions.sigmoid;

public strictfp class Miner {
	private static final double GOLD_MINER_RATIO = 0.25;
	private static final int ANTI_SOLDIER_MOMENTUM = 0;
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

		GoldMiningHelper.mineGold();

		GoldMiningHelper.updateGoldAmountInGridCell();

		if (rc.isMovementReady()) {
			move();
		}

		if (LeadMiningHelper.canMineLead()) {
			LeadMiningHelper.mineLead();
		}

		logger.log("Updating Lead");
		LeadMiningHelper.updateLeadAmountInGridCell();
		logger.log("Updated Lead");
		logger.flush();
	}

	private static boolean move() throws GameActionException {
		Direction goldDirection = GoldMiningHelper.spotGold();
		if (goldDirection != null) {
			rc.setIndicatorString("gold near");
			return MovementHelper.tryMove(goldDirection, false);
		}

		Direction runAway = getAntiSoldierDirection();
		if (runAway != null) {
			rc.setIndicatorString("anti soldier");
			return MovementHelper.tryMove(runAway, false);
		}

		if (!isExplorer && isGoldMiner) {
			goldDirection = GoldMiningHelper.spotGoldOnGrid();
			if (goldDirection != null) {
				rc.setIndicatorString("gold afar");
				return MovementHelper.tryMove(goldDirection, false);
			}
		}
		Direction leadDirection = LeadMiningHelper.spotLead();
		if (leadDirection != null) {
			rc.setIndicatorString("lead near");
			return MovementHelper.moveAndAvoid(leadDirection, myArchonLocation, 2);
		}
		if (!isExplorer && (leadDirection = LeadMiningHelper.spotLeadOnGrid()) != null) {
			rc.setIndicatorString("lead afar");
			return MovementHelper.moveAndAvoid(leadDirection, myArchonLocation, 2);
		}

		Direction antiCorner = LeadMiningHelper.getAntiEdgeDirection();
		if (antiCorner != null) {
			rc.setIndicatorString("anti corner");
			myDirection = Functions.getPerpendicular(antiCorner);
		}
		rc.setIndicatorString("no clue");
		if (CommsHelper.isLocationInEnemyZone(rc.getLocation().add(myDirection))) {
			myDirection = myDirection.opposite();
		}
		return MovementHelper.moveAndAvoid(myDirection, myArchonLocation, 2);
	}

	public static void init() throws GameActionException {
		isGoldMiner = random.nextDouble() < GOLD_MINER_RATIO;
		isExplorer = random.nextDouble() < getExplorerRatio();
		myDirection = Functions.getRandomDirection();
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
					myDirection = myArchonLocation.directionTo(rc.getLocation());
					myArchonIndex = i - 32;
				}
			} else {
				break;
			}
		}
	}


	private static Direction antiSoldier = null;
	private static int momentum = 0;
	private static Direction getAntiSoldierDirection() {
		if (momentum > 0) {
			Direction antiCorner = LeadMiningHelper.getAntiEdgeDirection();
			if (antiCorner != null) {
				momentum = 0;
				return antiCorner;
			}
			momentum--;
			return antiSoldier;
		}
		int dx = 0, dy = 0, count = 0;
		for (RobotInfo ri: rc.senseNearbyRobots(myType.visionRadiusSquared, enemyTeam)) {
			if (ri.type == RobotType.SOLDIER) {
				Direction d = ri.location.directionTo(rc.getLocation());
				dx += d.dx;
				dy += d.dy;
				count++;
			}
		}
		if (count < 1) return null;
		if (dx == 0 && dy == 0) return null;
		myDirection = antiSoldier = new MapLocation(0, 0).directionTo(new MapLocation(dx, dy));
		momentum = count * ANTI_SOLDIER_MOMENTUM;
		return antiSoldier;
	}
}
