package gen6;

import battlecode.common.*;
import battlecode.common.MapLocation;
import gen6.archon.ArchonMover;
import gen6.common.CommsHelper;
import gen6.archon.SpawnHelper;
import gen6.common.Functions;
import gen6.common.MovementHelper;
import gen6.soldier.SoldierDensity;


import java.util.Map;

import static gen6.RobotPlayer.*;
import static gen6.common.Functions.getBits;
import static gen6.common.Functions.setBits;

public strictfp class Archon {
	private static final int DEFENSE_ROUNDS_THRESHOLD = 5;

	public static int myIndex;
	private static int buildDirectionIndex = 0;

	private static boolean[] isPossibleEnemyArchonSymmetry;
	private static int symmetryIndex = 0;

	private static int lastDefenseRound = -100;


	// ARCHON
	// LABORATORY
	// WATCHTOWER
	// MINER
	// BUILDER
	// SOLDIER
	// SAGE
	public static final int[] priority = {0, 0, 0, 2, 1, 3, 4};

	private static MapLocation getHealLocation() {
		if (!rc.isActionReady()) {
			return null;
		}

		int maxHpDiff = 0;
		int maxPriority = -1;
		MapLocation robotLoc = null;
		RobotInfo[] ris = rc.senseNearbyRobots(myType.actionRadiusSquared, myTeam);
		for (int i = ris.length; --i >= 0; ) {
			RobotInfo robot = ris[i];
			int typeIndex = robot.type.ordinal();
			int p = priority[typeIndex], hpDiff = robot.type.getMaxHealth(robot.level) - robot.health;
			if (p > maxPriority) {
				maxPriority = p;
				maxHpDiff = hpDiff;
				robotLoc = robot.location;
			} else if (
					p == maxPriority && maxHpDiff < hpDiff
			) {
				maxHpDiff = hpDiff;
				robotLoc = robot.location;
			}
		}
		if (maxHpDiff == 0) return null;
		return robotLoc;
	}

	private static int lastRoundHp = 0;
	private static void updateArchonHp() throws GameActionException {
		if (lastRoundHp != rc.getHealth()) {
			lastRoundHp = rc.getHealth();
			int val = Functions.setBits(rc.readSharedArray(14 + myIndex), 0, 10, lastRoundHp);
			val = Functions.setBits(val, 13, 14, rc.getLevel());
			rc.writeSharedArray(14 + myIndex, val);
		}
	}

	public static void updateSharedArray() throws GameActionException{
		// Set count 0 before each round start
		if (rc.getRoundNum()%2 == 1){
			rc.writeSharedArray(7, 0);
			rc.writeSharedArray(8, 0);
			rc.writeSharedArray(9, 0);
		}
		int v = rc.readSharedArray(32 + myIndex);
		if (getBits(v, 13, 13) == 1){
			return;
		}
		int v1 = rc.readSharedArray(6);
		int cur = (getBits(v1, 2 * myIndex + 6, 2 * myIndex + 7) + 1)%3;
		for (int i = 0; i < maxArchonCount; i++){
			if (i == myIndex)	continue;
			if ((cur - getBits(v1, 6 + 2 * i, 7 + 2 * i) + 3)%3 > 1){
				rc.writeSharedArray(32 + i, setBits(rc.readSharedArray(32 + i), 13, 13, 1));
			}
		}
		rc.writeSharedArray(6, setBits(v1, 2 * myIndex + 6, 2 * myIndex + 7, cur));
	}

//	public static void updateArchonMovement(MapLocation m) throws GameActionException{
//		for (int i = maxArchonCount; --i >= 0; ){
//			if (CommsHelper.getLocationFrom12Bits(rc.readSharedArray(50 + i)).equals(m)){
//				int value = 0;
//				value = setBits(value, 6, 11, rc.getLocation().x);
//				value = setBits(value, 0, 5, rc.getLocation().y);
//				rc.writeSharedArray(i + 50, value);
//				return;
//			}
//		}
//
//	}

	public static void run() throws GameActionException {
		if (rc.getRoundNum() % 10 == 0) SoldierDensity.reset();
		updateSharedArray();

		// DON'T SPAWN SOLDIER ON FIRST ROUND
		if (rc.getRoundNum() == 2) {
			setCentralArchon();
		}

		checkIfDefenseNeeded();
		updateArchonHp();

		switch (rc.getMode()) {
			case TURRET:
				if (ArchonMover.shouldRelocate() && rc.canTransform()) {
					relocate = ArchonMover.getRelocateLocation();
					if (relocate != null) {
						rc.transform();
						CommsHelper.setArchonPortable(myIndex, true);
						break;
					}
				}
				act();
				CommsHelper.setArchonPortable(myIndex, false);
				break;
			case PORTABLE:
				MapLocation myPrevLoc = rc.getLocation();
				move();
				if (CommsHelper.getLocationFrom12Bits(rc.readSharedArray(50 + myIndex)).equals(myPrevLoc)){
					int value = 0;
					value = setBits(value, 6, 11, rc.getLocation().x);
					value = setBits(value, 0, 5, rc.getLocation().y);
					rc.writeSharedArray(myIndex + 50, value);
				}
				else{
					System.err.println("ERROR UPDATING ARCHON LOCATION AFTER MOVEMENT");
				}
				break;
		}

	}

	private static MapLocation relocate = null;
	private static MapLocation goodSpot = null;
	private static void move() throws GameActionException {
		MapLocation rn = rc.getLocation();
		if (rn.equals(goodSpot)) {
			if (rc.isTransformReady() && rc.canTransform()) {
				rc.transform();
			}
			relocate = null;
			return;
		}
		if (goodSpot != null) {
			MovementHelper.moveBellmanFord(goodSpot);
			return;
		}
		if (relocate == null) {
			relocate = ArchonMover.getRelocateLocation();
		}
		if (rn.isWithinDistanceSquared(relocate, 25)) {
			goodSpot = ArchonMover.getSpotToSettle(rn.directionTo(relocate));
			if (goodSpot == null) {
				relocate = ArchonMover.getRelocateLocation();
			}
		}
		if (relocate != null) {
			MovementHelper.moveBellmanFord(relocate);
		}
	}

	private static void act() throws GameActionException {
		if (!rc.isActionReady()) return;
		RobotType toSpawn = SpawnHelper.getNextDroid();
		if (toSpawn != null) {
			Direction direction = SpawnHelper.getOptimalDirection(directions[buildDirectionIndex], toSpawn);
			if (direction != null && rc.canBuildRobot(toSpawn, direction)) {
				buildDirectionIndex = direction.ordinal() + 1;
				SpawnHelper.incrementDroidsBuilt(toSpawn);
				rc.buildRobot(toSpawn, direction);
			}
		}
		if (rc.isActionReady()) {
			MapLocation toHeal = getHealLocation();
			if (toHeal != null && rc.canRepair(toHeal)) {
				rc.repair(toHeal);
			}
		}
		if (buildDirectionIndex == 8) {
			buildDirectionIndex = 0;
		}
	}

	private static void checkIfDefenseNeeded() throws GameActionException {
		boolean defenseNeeded = false;
		for (RobotInfo ri : rc.senseNearbyRobots(myType.visionRadiusSquared, enemyTeam)) {
			switch (ri.type) {
				case SOLDIER:
				case WATCHTOWER:
				case SAGE:
					defenseNeeded = true;
					break;
			}
			if (defenseNeeded) {
				lastDefenseRound = rc.getRoundNum();
				break;
			}
		}

		if (!defenseNeeded && rc.getRoundNum() - lastDefenseRound < DEFENSE_ROUNDS_THRESHOLD) {
			defenseNeeded = true;
		}

		int value = rc.readSharedArray(32 + myIndex);
		int bit = getBits(value, 14, 14);
		if (defenseNeeded && bit == 0) {
			value = setBits(value, 14, 14, 1);
		} else if (!defenseNeeded && bit == 1) {
			value = setBits(value, 14, 14, 0);
		}
		if (value != rc.readSharedArray(32 + myIndex)) {
			rc.writeSharedArray(32 + myIndex, value);
		}
	}

	private static void setCentralArchon() throws GameActionException {
		if (getBits(rc.readSharedArray(4), 15, 15) == 1) {
			return;
		}

		MapLocation centre = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
		int minDistance = rc.getMapWidth() * rc.getMapHeight();
		int archonIndex = 0;
		for (int i = 32; i < 32 + maxArchonCount; ++i) {
			MapLocation archonLocation = CommsHelper.getLocationFrom12Bits(rc.readSharedArray(i));
			int distance = Math.max(Math.abs(archonLocation.x - centre.x), Math.abs(archonLocation.y - centre.y));
			if (distance < minDistance) {
				minDistance = distance;
				archonIndex = i - 32;
			}
		}

		int value = setBits(0, 15, 15, 1);
		value = setBits(value, 11, 12, archonIndex);
		value = setBits(value, 13, 14, 3);
		rc.writeSharedArray(4, value);
	}

	public static void broadcastSymmetry() throws GameActionException {
		if (CommsHelper.foundEnemyArchon()) {
			return;
		}

		int value = getBits(rc.readSharedArray(4), 8, 10);
		if (getBits(value, 8, 8) == 1) {
			isPossibleEnemyArchonSymmetry[0] = false;
		}
		if (getBits(value, 9, 9) == 1) {
			isPossibleEnemyArchonSymmetry[1] = false;
		}
		if (getBits(value, 10, 10) == 1) {
			isPossibleEnemyArchonSymmetry[2] = false;
		}

		for (int i = 0; i < 3; ++i) {
			if (symmetryIndex == 3) {
				symmetryIndex = 0;
			}
			if (isPossibleEnemyArchonSymmetry[symmetryIndex]) {
				CommsHelper.updateSymmetry(myIndex, symmetryIndex);
				++symmetryIndex;
				return;
			}

			++symmetryIndex;
		}

		CommsHelper.updateSymmetry(myIndex, 3);
	}

	public static void init() throws GameActionException {
		MovementHelper.prepareBellmanFord(34);
		maxArchonCount = rc.getArchonCount();
		for (int i = 32; i < 32 + maxArchonCount; ++i) {
			int value = rc.readSharedArray(i);
			if (getBits(value, 15, 15) == 0) {
				value = setBits(value, 6, 11, rc.getLocation().x);
				value = setBits(value, 0, 5, rc.getLocation().y);
				myIndex = i - 32;
				rc.writeSharedArray(50 + myIndex, value);
				value = setBits(0, 15, 15, 1);
				rc.writeSharedArray(i, value);
				break;
			}
		}

		isPossibleEnemyArchonSymmetry = new boolean[3];
		isPossibleEnemyArchonSymmetry[0] = isPossibleEnemyArchonSymmetry[1] = isPossibleEnemyArchonSymmetry[2] = true;

		int mapArea = rc.getMapHeight() * rc.getMapWidth();
		if (mapArea <= 33 * 33) {
			SpawnHelper.mapSizeType = 0;
		} else if (mapArea <= 47 * 47) {
			SpawnHelper.mapSizeType = 1;
		} else {
			SpawnHelper.mapSizeType = 2;
		}
	}
}
