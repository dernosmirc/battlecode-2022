package gen2;

import battlecode.common.*;
import gen2.helpers.GoldMiningHelper;
import gen2.helpers.LeadMiningHelper;
import gen2.helpers.CommsHelper;
import gen2.util.SymmetryType;
import gen2.util.Logger;

import static gen2.RobotPlayer.*;
import static gen2.util.Functions.getBits;
import static gen2.util.Functions.setBits;

import java.util.Random;

public strictfp class Soldier {
	private static final Random rng = new Random(rc.getID());

	private static MapLocation myArchonLocation;
	private static int myArchonIndex;
	private static MapLocation guessedEnemyArchonLocation;

	private static void updateEnemyArchonLocations() throws GameActionException {
		for (int i = 0; i < archonCount; ++i) {
			int value = rc.readSharedArray(i);
			if (getBits(value, 15, 15) == 1) {
				MapLocation archonLocation = CommsHelper.getLocationFrom12Bits(value);
				if (rc.getLocation().distanceSquaredTo(archonLocation) <= myType.visionRadiusSquared) {
					RobotInfo robot = rc.senseRobotAtLocation(archonLocation);
					if (robot == null || robot.type != RobotType.ARCHON || robot.team != enemyTeam) {
						// Enemy archon is dead or has run away
						rc.writeSharedArray(i, 0);
					}
				}
			}
		}
	}

	private static void attack() throws GameActionException {
		if (!rc.isActionReady()) {
			return;
		}

		RobotInfo[] enemyRobots = rc.senseNearbyRobots(myType.actionRadiusSquared, enemyTeam);
		for (RobotInfo robot : enemyRobots) {
			if (robot.type == RobotType.ARCHON) {
				if (rc.canAttack(robot.location)) {
					rc.attack(robot.location);
					break;
				}
			}
		}

		if (enemyRobots.length > 0 && rc.canAttack(enemyRobots[0].location)) {
			rc.attack(enemyRobots[0].location);
		}
	}

	private static void move() throws GameActionException {
		MapLocation enemyArchonLocation = CommsHelper.getEnemyArchonLocation();

		if (enemyArchonLocation != null) {
			Direction dir = rc.getLocation().directionTo(enemyArchonLocation);
			if (rc.canMove(dir)){
				rc.move(dir);
			}
			else if (rc.canMove(dir.rotateLeft())){
				rc.move(dir.rotateLeft());
			}
			else if(rc.canMove(dir.rotateRight())){
				rc.move(dir.rotateRight());
			}
			return;
		}

		Direction dir = rc.getLocation().directionTo(guessedEnemyArchonLocation);
		if (rc.canMove(dir)){
			rc.move(dir);
		}
		else if (rc.canMove(dir.rotateLeft())){
			rc.move(dir.rotateLeft());
		}
		else if(rc.canMove(dir.rotateRight())){
			rc.move(dir.rotateRight());
		}
	}

	public static void run() throws GameActionException {
		updateEnemyArchonLocations();
		attack();

		for (RobotInfo robot : rc.senseNearbyRobots(myType.visionRadiusSquared, enemyTeam)) {
			if (robot.type == RobotType.ARCHON) {
				calculateEnemyArchonLocations(robot);
			}
		}

		// Update lead and gold sources nearby to help miners
		LeadMiningHelper.updateLeadAmountInGridCell();
		GoldMiningHelper.updateGoldAmountInGridCell();

		move();
	}

	private static void calculateEnemyArchonLocations(RobotInfo archon) throws GameActionException {
		boolean alreadyUpdated = false;
		boolean foundThisArchon = false;
		for (int i = 0; i < archonCount; ++i) {
			int value = rc.readSharedArray(i);
			if (getBits(value, 15, 15) == 1) {
				alreadyUpdated = true;
				if (CommsHelper.getLocationFrom12Bits(value).equals(archon.location)) {
					foundThisArchon = true;
				}
			}
		}

		if (alreadyUpdated) {
			if (!foundThisArchon) {
				for (int i = 0; i < archonCount; ++i) {
					if (getBits(rc.readSharedArray(i), 15, 15) == 0) {
						int value = setBits(0, 15, 15, 1);
						value = setBits(value, 6, 11, archon.location.x);
						value = setBits(value, 0, 5, archon.location.y);
						rc.writeSharedArray(i, value);
						return;
					}
				}
			}

			if (!foundThisArchon) {
				int value = setBits(0, 15, 15, 1);
				value = setBits(value, 6, 11, archon.location.x);
				value = setBits(value, 0, 5, archon.location.y);
				rc.writeSharedArray(0, value);
			}

			return;
		}

		SymmetryType symType = SymmetryType.NONE;
		int index = 32;
		for (int j = 0; j < archonCount; j++) {
			int value = rc.readSharedArray(index + j);
			MapLocation archonLocation = new MapLocation(getBits(value, 6, 11), getBits(value, 0, 5));
			symType = SymmetryType.getSymmetryType(archonLocation, archon.location);
			if (symType != SymmetryType.NONE) {
				break;
			}
		}

		if (symType == SymmetryType.NONE) {
			int value = setBits(0, 15, 15, 1);
			value = setBits(value, 6, 11, archon.location.x);
			value = setBits(value, 0, 5, archon.location.y);
			rc.writeSharedArray(0, value);
		} else {
			for (int j = 0; j < archonCount; j++) {
				int value = rc.readSharedArray(index + j);
				MapLocation archonLocation = new MapLocation(getBits(value, 6, 11), getBits(value, 0, 5));
				MapLocation enemyArchonLocation = SymmetryType.getSymmetricalLocation(archonLocation, symType);
				int setValue = 0;
				setValue = setBits(0, 15, 15, 1);
				setValue = setBits(setValue, 6, 11, enemyArchonLocation.x);
				setValue = setBits(setValue, 0, 5, enemyArchonLocation.y);
				rc.writeSharedArray(j, setValue);
			}
		}
	}

	public static void init() throws GameActionException {
		archonCount = 0;
		for (int i = 32; i < 36; ++i) {
			int value = rc.readSharedArray(i);
			if (getBits(value, 15, 15) == 1) {
				++archonCount;
				MapLocation archonLocation = new MapLocation(getBits(value, 6, 11), getBits(value, 0, 5));
				if (rc.getLocation().distanceSquaredTo(archonLocation) <= 2) {
					myArchonLocation = new MapLocation(archonLocation.x, archonLocation.y);
					myArchonIndex = i - 32;
				}
			} else {
				break;
			}
		}

		MapLocation[] possibleEnemyArchonLocations = SymmetryType.getSymmetricalLocations(myArchonLocation);
		int randomNumber = rng.nextInt(3);
		guessedEnemyArchonLocation = new MapLocation(possibleEnemyArchonLocations[randomNumber].x,
														possibleEnemyArchonLocations[randomNumber].y);
	}
}
