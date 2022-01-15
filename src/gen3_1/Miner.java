package gen3_1;

import battlecode.common.*;
import gen3_1.helpers.GoldMiningHelper;
import gen3_1.helpers.LeadMiningHelper;
import gen3_1.common.MovementHelper;
import gen3_1.util.Functions;
import gen3_1.util.Logger;

import java.util.Random;

import static gen3_1.RobotPlayer.*;
import static gen3_1.util.Functions.getBits;
import static gen3_1.util.Functions.sigmoid;

public strictfp class Miner {
	private static final double GOLD_MINER_RATIO = 0.25;
	private static double getExplorerRatio() {
		return 0.65 * sigmoid((300-rc.getRoundNum())/200.0);
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

		LeadMiningHelper.updateLeadAmountInGridCell();

		logger.flush();
	}

	private static boolean move() throws GameActionException {
		Direction goldDirection = GoldMiningHelper.spotGold();
		if (goldDirection != null) {
			return MovementHelper.tryMove(goldDirection, false);
		}

		Direction runAway = getAntiSoldierDirection();
		if (runAway != null) {
			return MovementHelper.tryMove(runAway, false);
		}

		if (!isExplorer && isGoldMiner) {
			goldDirection = GoldMiningHelper.spotGoldOnGrid();
			if (goldDirection != null) {
				return MovementHelper.tryMove(goldDirection, false);
			}
		}
		Direction leadDirection = LeadMiningHelper.spotLead();
		if (leadDirection != null) {
			return MovementHelper.moveAndAvoid(leadDirection, myArchonLocation, 2);
		}
		if (!isExplorer && (leadDirection = LeadMiningHelper.spotLeadOnGrid()) != null) {
			return MovementHelper.moveAndAvoid(leadDirection, myArchonLocation, 2);
		}

		Direction antiCorner = LeadMiningHelper.getAntiEdgeDirection();
		if (antiCorner != null) {
			myDirection = antiCorner;
		}
		return MovementHelper.moveAndAvoid(myDirection, myArchonLocation, 2);
	}

	public static void init() throws GameActionException {
		isGoldMiner = random.nextDouble() < GOLD_MINER_RATIO;
		isExplorer = random.nextDouble() < getExplorerRatio();
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


	private static Direction getAntiSoldierDirection() {
		int dx = 0, dy = 0, count = 0;
		for (RobotInfo ri: rc.senseNearbyRobots(myType.visionRadiusSquared, enemyTeam)) {
			if (ri.type == RobotType.SOLDIER) {
				Direction d = ri.location.directionTo(rc.getLocation());
				dx += d.dx;
				dy += d.dy;
				count++;
			}
		}
		if (count < 2) return null;
		if (dx == 0 && dy == 0) return null;
		return (new MapLocation(0, 0).directionTo(new MapLocation(dx, dy)));
	}
}
