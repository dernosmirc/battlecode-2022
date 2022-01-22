package gen6;

import battlecode.common.*;
import gen6.common.CommsHelper;
import gen6.common.util.LogCondition;
import gen6.miner.GoldMiningHelper;
import gen6.miner.LeadMiningHelper;
import gen6.common.MovementHelper;
import gen6.common.Functions;
import gen6.common.util.Logger;

import java.util.Random;

import static gen6.RobotPlayer.*;
import static gen6.common.Functions.*;

public strictfp class Miner {
	private static final int ANTI_SOLDIER_MOMENTUM = 5;
	private static final double GOLD_MINER_RATIO = 0.25;

	private static double getExplorerRatio() {
		return sigmoid((250-rc.getRoundNum())/100.0);
	}

	private static MapLocation myArchonLocation, myTargetLocation;
	private static Direction myDirection;
	private static int myArchonIndex;
	private static boolean isGoldMiner = false;
	private static boolean isExplorer = false;
	private static final Random random = new Random(rc.getID());

	private static int stillCount = 0;
	public static void run() throws GameActionException {
		// update location each round
		myArchonLocation = CommsHelper.getArchonLocation(myArchonIndex);

		// Update the miner count
		if (rc.getRoundNum()%2 == 1){
			rc.writeSharedArray(8, rc.readSharedArray(8) + 1);
		}

		Logger logger = new Logger("Miner", LogCondition.Never);
		int round = rc.getRoundNum();

		if (myDirection == null) {
			myDirection = getRandomDirection();
		}
		if (isExplorer) {
			updateDirectionsExplored();
		}

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
		logger.flush();
	}

	private static boolean clockwise = random.nextDouble() > 0.5;
	private static boolean move() throws GameActionException {
		Direction antiSoldier = getAntiSoldierDirection();
		if (antiSoldier != null) {
			rc.setIndicatorString("anti soldier");
			return MovementHelper.moveBellmanFord(antiSoldier);
		}

		MapLocation gold = GoldMiningHelper.spotGold();
		if (gold != null) {
			rc.setIndicatorString("gold near");
			return MovementHelper.moveBellmanFord(gold);
		}
		if (!isExplorer && isGoldMiner) {
			gold = GoldMiningHelper.spotGoldOnGrid();
			if (gold != null) {
				rc.setIndicatorString("gold far");
				return MovementHelper.moveBellmanFord(gold);
			}
		}

		MapLocation lead = LeadMiningHelper.spotLead();
		if (lead != null) {
			rc.setIndicatorString("lead near");
			return MovementHelper.moveBellmanFord(lead);
		}
		if (isExplorer) {
			if (myTargetLocation != null) {
				return MovementHelper.moveBellmanFord(myTargetLocation);
			}
		} else {
			lead = LeadMiningHelper.spotLeadOnGrid();
			if (lead != null) {
				rc.setIndicatorString("lead far");
				return MovementHelper.moveBellmanFord(lead);
			}
		}

		rc.setIndicatorString("chilling");
		boolean gotFromAntiCorner = false;
		Direction antiCorner = Functions.getDirectionAlongEdge(clockwise, 3);
		if (antiCorner != null) {
			myDirection = antiCorner;
			gotFromAntiCorner = true;
			rc.setIndicatorString("anti corner");
		}
		if (
				!CommsHelper.isLocationInEnemyZone(rc.getLocation()) &&
						CommsHelper.isLocationInEnemyZone(Functions.translate(rc.getLocation(), myDirection, 3))
		) {
			if (gotFromAntiCorner) {
				clockwise = !clockwise;
			}
			myDirection = myDirection.opposite();
			rc.setIndicatorString("anti enemy area");
		}
		return MovementHelper.moveBellmanFord(myDirection);
	}

	private static boolean[] directionsExplored = new boolean[9];
	private static int directionsExploredCount = 0;

	private static void updateDirectionsExplored() throws GameActionException {
		if (directionsExploredCount == 9) {
			return;
		}
		if (myTargetLocation != null) {
			if (rc.getLocation().isWithinDistanceSquared(myTargetLocation, 5)) {
				myTargetLocation = null;
				setRandomLocation();
			}
		} else {
			setRandomLocation();
		}
	}

	private static void setRandomLocation() throws GameActionException {
		double yea = random.nextDouble(), cumulative = 0, step = 1.0/(9-directionsExploredCount);
		Direction direction = Direction.CENTER;
		for (int i = 9; --i >= 0; ) {
			if (!directionsExplored[i]) {
				cumulative += step;
				if (yea <= cumulative) {
					direction = Direction.values()[i];
					directionsExplored[i] = true;
					directionsExploredCount++;
					break;
				}
			}
		}
		int wb2 = rc.getMapWidth()/2, hb2 = rc.getMapHeight()/2;
		myTargetLocation = new MapLocation(wb2 + direction.dx*(wb2-2), hb2 + direction.dy*(hb2-2));
		if (CommsHelper.isLocationInEnemyZone(myTargetLocation)) {
			myTargetLocation = null;
			myDirection = getRandomDirection();
		} else {
			myDirection = rc.getLocation().directionTo(myTargetLocation);
		}
	}

	public static void init() throws GameActionException {
		isGoldMiner = random.nextDouble() < GOLD_MINER_RATIO;
		isExplorer = random.nextDouble() < getExplorerRatio();
		maxArchonCount = 0;
		MovementHelper.prepareBellmanFord(20);
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
	}

	private static Direction antiSoldier = null;
	private static int momentum = 0;
	private static Direction getAntiSoldierDirection() {
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
				antiSoldier = Functions.vectorAddition(antiSoldier, getAntiEdgeDirection(rc.getLocation(), 3));
				if (antiSoldier == Direction.CENTER) {
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
