package gen2;

import battlecode.common.*;
import gen2.helpers.GoldMiningHelper;
import gen2.helpers.LeadMiningHelper;
import gen2.util.SymmetryType;
import gen2.util.Logger;

import static gen2.RobotPlayer.*;
import static gen2.util.Functions.getBits;
import static gen2.util.Functions.setBits;

import java.util.Random;

public strictfp class Soldier {
	private static MapLocation myArchonLocation;
	private static int myArchonIndex;
	private static MapLocation enemyArchon;
	private static int mapWidth, mapHeight;
	private static final Random rng = new Random(rc.getID());
	private static boolean enemyArchonFound;
	private static MapLocation calculatedEnemyArchonLocation;
	private static MapLocation sensedEnemyArchonLocation;
	private static Direction dir;
	private static Logger logger;

	public static void run() throws GameActionException {
		int arrayRead = rc.readSharedArray(0);
		if (getBits(arrayRead, 15, 15) == 1){
			enemyArchonFound = true;
			sensedEnemyArchonLocation = new MapLocation(getBits(arrayRead, 6, 11), getBits(arrayRead, 0, 5));
		}
		if (enemyArchonFound && rc.getLocation().distanceSquaredTo(sensedEnemyArchonLocation) <= myType.visionRadiusSquared){
			if (rc.senseRobotAtLocation(sensedEnemyArchonLocation) == null){
				enemyArchonFound = false;
				rc.writeSharedArray(0, 0);
			}
		}

		// Update lead and gold sources nearby to help miners
		LeadMiningHelper.updateLeadAmountInGridCell();
		GoldMiningHelper.updateGoldAmountInGridCell();

		RobotInfo[] enemyRobotInfo1;
		enemyRobotInfo1 = rc.senseNearbyRobots(myType.actionRadiusSquared, myTeam.opponent());
		int n = enemyRobotInfo1.length;
		for (int i = 0; i < n; i++){
			if (enemyRobotInfo1[i].getType() == RobotType.ARCHON){
				calculateEnemyArchonLocations(enemyRobotInfo1[i]);
				sensedEnemyArchonLocation = enemyRobotInfo1[i].location;
				enemyArchonFound = true;

				if (rc.canAttack(enemyRobotInfo1[i].getLocation())) 	rc.attack(enemyRobotInfo1[i].getLocation());
				return;
			}
		}
		if (n > 0 && rc.canAttack(enemyRobotInfo1[0].getLocation())){
			rc.attack(enemyRobotInfo1[0].getLocation());
		}

		if (enemyArchonFound) {
			dir = rc.getLocation().directionTo(sensedEnemyArchonLocation);
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

		dir = rc.getLocation().directionTo(calculatedEnemyArchonLocation);
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

	private static void calculateEnemyArchonLocations(RobotInfo archon) throws GameActionException {
		boolean alreadyUpdated = false;
		boolean foundThisArchon = false;
		for (int i = 0; i < archonCount; ++i) {
			int value = rc.readSharedArray(i);
			if (getBits(value, 15, 15) == 1) {
				alreadyUpdated = true;
				MapLocation archonLocation = new MapLocation(getBits(value, 6, 11), getBits(value, 0, 5));
				if (archonLocation.equals(archon.location)) {
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

		enemyArchonFound = false;
		MapLocation[] possibleEnemyArchonLocations = SymmetryType.getSymmetricalLocations(myArchonLocation);
		int randomNumber = rng.nextInt(3);
		calculatedEnemyArchonLocation = new MapLocation(possibleEnemyArchonLocations[randomNumber].x,
														possibleEnemyArchonLocations[randomNumber].y);
	}
}
