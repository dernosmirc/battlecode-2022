package gen5;

import battlecode.common.*;
import gen5.soldier.BellmanFordMovement;
import gen5.soldier.BugPathingMovement;
import gen5.miner.GoldMiningHelper;
import gen5.miner.LeadMiningHelper;
import gen5.soldier.AttackHelper;
import gen5.common.CommsHelper;
import gen5.common.MovementHelper;
import gen5.common.util.Logger;
import gen5.common.SymmetryType;

import static gen5.RobotPlayer.myTeam;
import static gen5.RobotPlayer.*;
import static gen5.common.Functions.getBits;
import static gen5.common.Functions.setBits;

import java.util.Random;

public strictfp class Soldier {
	public static final Random rng = new Random(rc.getID());

	public static MapLocation myArchonLocation;
	public static int myArchonIndex;
	public static MapLocation guessedEnemyArchonLocation;
	private static MapLocation centralArchon;
	public static boolean sensedEnemyAttackRobot;

	private static void updateEnemyArchonLocations() throws GameActionException {
		for (int i = 0; i < maxArchonCount; ++i) {
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

	public static void run() throws GameActionException {
		Logger logger = new Logger("Soldier", true);
		sensedEnemyAttackRobot = false;
		updateEnemyArchonLocations();
		AttackHelper.attack();

		for (RobotInfo robot : rc.senseNearbyRobots(myType.visionRadiusSquared, enemyTeam)) {
			if (robot.type == RobotType.ARCHON) {
				calculateEnemyArchonLocations(robot);
			}

			switch (robot.type) {
				case SAGE:
				case WATCHTOWER:
				case SOLDIER:
					sensedEnemyAttackRobot = true;
					break;
			}
		}

		updateGuessedEnemyArchonSymmetries();
		logger.log("Before movement");

		String movement = System.getProperty(
				"bc.testing.team-" + myTeam.name() + ".movement",
				"bellmanford"
		);
		switch (movement) {
			case "bugpathing":
				BugPathingMovement.move();
				break;
			case "bellmanford":
				BellmanFordMovement.move();
				break;
		}

		logger.log("After movement");

		// Update lead and gold sources nearby to help miners
		GoldMiningHelper.updateGoldAmountInGridCell();

		if (Clock.getBytecodesLeft() > 5000) {
			logger.log("Updating Lead and gold");
			LeadMiningHelper.updateLeadAmountInGridCell();
			logger.log("Updated Lead and gold");
		}

		logger.flush();
	}

	private static void updateGuessedEnemyArchonSymmetries() throws GameActionException {
		if (CommsHelper.foundEnemyArchon()) {
			return;
		}

		MapLocation[] symmetricalLocations;
		int value = rc.readSharedArray(4);
		for (int i = 32; i < 32 + maxArchonCount; ++i) {
			MapLocation archonLocation = CommsHelper.getLocationFrom12Bits(rc.readSharedArray(i));
			symmetricalLocations = SymmetryType.getSymmetricalLocations(archonLocation);
			for (MapLocation location : symmetricalLocations) {
				if (rc.getLocation().distanceSquaredTo(location) <= myType.visionRadiusSquared) {
					RobotInfo robot = rc.senseRobotAtLocation(location);
					if (robot == null || robot.type != RobotType.ARCHON || robot.team != enemyTeam) {
						int symmetryIndex = SymmetryType.getSymmetryType(archonLocation, location).ordinal();
						int bit = 8 + symmetryIndex;
						value = setBits(value, bit, bit, 1);
					}
				}
			}
		}

		if (value != rc.readSharedArray(4)) {
			rc.writeSharedArray(4, value);
		}

		updateGuessedEnemyArchonLocation();
	}

	public static void updateGuessedEnemyArchonLocation() throws GameActionException {
		if (guessedEnemyArchonLocation == null) {
			return;
		}

		SymmetryType symmetryType = SymmetryType.getSymmetryType(centralArchon, guessedEnemyArchonLocation);
		int bit = 8 + symmetryType.ordinal();
		if (getBits(rc.readSharedArray(4), bit, bit) == 0) {
			return;
		}

		symmetryType = CommsHelper.getPossibleSymmetry();
		if (symmetryType == SymmetryType.NONE) {
			guessedEnemyArchonLocation = null;
		} else {
			guessedEnemyArchonLocation = SymmetryType.getSymmetricalLocation(centralArchon, symmetryType);
		}
	}

	private static void calculateEnemyArchonLocations(RobotInfo archon) throws GameActionException {
		boolean alreadyUpdated = false;
		boolean foundThisArchon = false;
		for (int i = 0; i < maxArchonCount; ++i) {
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
				for (int i = 0; i < maxArchonCount; ++i) {
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
		for (int j = 0; j < maxArchonCount; j++) {
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
			SymmetryType.setMapSymmetry(symType);
			for (int j = 0; j < maxArchonCount; j++) {
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
		maxArchonCount = 0;
		for (int i = 32; i < 36; ++i) {
			int value = rc.readSharedArray(i);
			if (getBits(value, 15, 15) == 1) {
				++maxArchonCount;
				MapLocation archonLocation = new MapLocation(getBits(value, 6, 11), getBits(value, 0, 5));
				if (rc.getLocation().distanceSquaredTo(archonLocation) <= 2) {
					myArchonLocation = new MapLocation(archonLocation.x, archonLocation.y);
					myArchonIndex = i - 32;
				}
			} else {
				break;
			}
		}

		int centralArchonIndex = getBits(rc.readSharedArray(4), 11, 12);
		centralArchon = CommsHelper.getLocationFrom12Bits(rc.readSharedArray(32 + centralArchonIndex));

		SymmetryType symmetryType = CommsHelper.getBroadcastedSymmetry(myArchonIndex);
		if (symmetryType == SymmetryType.NONE) {
			guessedEnemyArchonLocation = null;
		} else {
			guessedEnemyArchonLocation = SymmetryType.getSymmetricalLocation(centralArchon, symmetryType);
		}

		MovementHelper.prepareBellmanFord(20);
	}
}