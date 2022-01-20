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
import static gen5.common.Functions.*;

public strictfp class Miner {
	private static final double GOLD_MINER_RATIO = 0.5;
	private static double getExplorerRatio() {
		return 0.65 * sigmoid((300-rc.getRoundNum())/200.0);
	}

	private static MapLocation myArchonLocation;
	public static Direction myDirection;
	private static int myArchonIndex;
	private static boolean isGoldMiner = false;
	private static boolean isExplorer = false;
	private static final Random random = new Random(rc.getID());

	private static int stillCount = 0;
	public static void run() throws GameActionException {
		Logger logger = new Logger("Miner", true);
		int round = rc.getRoundNum();

		GoldMiningHelper.mineGold();
		GoldMiningHelper.updateGoldAmountInGridCell();
		LeadMiningHelper.mineLead();

		if (rc.isMovementReady()) {
			logger.log("moving");
			if (!move()) {
				stillCount++;
			}
			if (stillCount > 3) {
				if (MovementHelper.tryMove(getRandomDirection(), false)) {
					stillCount = 0;
				}
			}
			logger.log("moved");
		}

		if (Clock.getBytecodesLeft() >= 3500 && round == rc.getRoundNum()) {
			logger.log("Updating Lead");
			LeadMiningHelper.updateLeadAmountInGridCell();
			logger.log("Updated Lead");
		}
		//logger.flush();
	}

	private static boolean clockwise = false;
	private static boolean move() throws GameActionException {
		Direction antiSoldier = getAntiSoldierDirection();
		if (antiSoldier != null) {
			return MovementHelper.moveBellmanFord(antiSoldier);
		}

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

		boolean gotFromAntiCorner = false;
		Direction antiCorner = LeadMiningHelper.getAntiEdgeDirection(clockwise);
		if (antiCorner != null) {
			myDirection = antiCorner;
			gotFromAntiCorner = true;
		}
		if (
				!CommsHelper.isLocationInEnemyZone(rc.getLocation()) &&
						CommsHelper.isLocationInEnemyZone(Functions.translate(rc.getLocation(), myDirection, 3))
		) {
			if (gotFromAntiCorner) {
				clockwise = !clockwise;
			}
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
					if (rc.getRoundNum() > 100) {
						myDirection = getRandomDirection();
					} else {
						myDirection = myArchonLocation.directionTo(rc.getLocation());
					}
					myArchonIndex = i - 32;
				}
			} else {
				break;
			}
		}
	}

	private static final int ANTI_SOLDIER_MOMENTUM = 5;
	private static Direction antiSoldier = null;
	private static int momentum = 0;
	private static Direction getAntiSoldierDirection() throws GameActionException {
		int dx = 0, dy = 0, count = 0;
		for (RobotInfo ri: rc.senseNearbyRobots(myType.visionRadiusSquared, enemyTeam)) {
			if (ri.type.canAttack() || ri.type == RobotType.ARCHON) {
				Direction d = ri.location.directionTo(rc.getLocation());
				dx += d.dx;
				dy += d.dy;
				count++;
			}
		}
		if (count < 1) {
			if (momentum > 0) {
				if (!rc.onTheMap(translate(rc.getLocation(), antiSoldier, 3))) {
					momentum = 0;
					return null;
				}
				momentum--;
				return antiSoldier;
			}
			return null;
		}

		if (dx == 0 && dy == 0) {
			return null;
		}

		myDirection = antiSoldier = directionTo(dx, dy);
		momentum = count * ANTI_SOLDIER_MOMENTUM;
		return antiSoldier;
	}
}
