package gen6.common;

import battlecode.common.MapLocation;
import battlecode.common.GameActionException;
import gen6.builder.BuilderType;
import gen6.builder.BuildingHelper;

import static gen6.RobotPlayer.*;
import static gen6.common.Functions.getBits;
import static gen6.common.Functions.setBits;
import static gen6.common.Functions.getDistance;

public strictfp class CommsHelper {

	// TODO improve hyperparameter
	private static final double ENEMY_ZONE_FACTOR = 8;
	private static int SOLDIER_COUNT = 0;
	private static int MINER_COUNT = 0;
	private static int SAGE_COUNT = 0;
	public static MapLocation getLocationFrom12Bits(int bits) {
		return new MapLocation(getBits(bits, 6, 11), getBits(bits, 0, 5));
	}

	public static int getBitsFromLocation(MapLocation loc) {
		int val = setBits(0, 6, 11, loc.x);
		return setBits(val, 0, 5, loc.y);
	}

	public static MapLocation getArchonLocation(int archonIndex) throws GameActionException {
		if (getDeadArchons()[archonIndex]) {
			return null;
		}
		return getLocationFrom12Bits(rc.readSharedArray(50 + archonIndex));
	}

	public static MapLocation getEnemyArchonLocation() throws GameActionException {
		MapLocation archonLocation = null;
		MapLocation centre = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
		int minDistance = rc.getMapWidth() * rc.getMapHeight();
		for (int i = 0; i < maxArchonCount; ++i) {
			int value = rc.readSharedArray(i);
			if (getBits(value, 15, 15) == 1) {
				MapLocation location = getLocationFrom12Bits(value);
				int distance = getDistance(centre, location);
				if (distance <  minDistance) {
					minDistance = distance;
					archonLocation = new MapLocation(location.x, location.y);
				}
			}
		}

		return archonLocation;
	}

	public static MapLocation[] getEnemyArchonLocations() throws GameActionException {
		MapLocation[] archons = new MapLocation[maxArchonCount];
		boolean found = false;
		for (int i = 0; i < maxArchonCount; ++i) {
			int value = rc.readSharedArray(i);
			if (getBits(value, 15, 15) == 1) {
				found = true;
				archons[i] = getLocationFrom12Bits(value);
			}
		}

		return found ? archons : null;
	}

	public static int getEnemyArchonCount() throws GameActionException {
		int count = 0;
		for (int i = 0; i < maxArchonCount; ++i) {
			if (getBits(rc.readSharedArray(i), 15, 15) == 1) {
				count++;
			}
		}
		return count;
	}

	public static MapLocation[] getFriendlyArchonLocations() throws GameActionException {
		boolean[] isDead = getDeadArchons();
		MapLocation[] archons = new MapLocation[maxArchonCount];
		for (int i = 0; i < maxArchonCount; ++i) {
			if (!isDead[i]) {
				int value = rc.readSharedArray(50 + i);
				archons[i] = getLocationFrom12Bits(value);
			}
		}

		return archons;
	}

	public static boolean foundEnemyArchon() throws GameActionException {
		for (int i = 0; i < maxArchonCount; ++i) {
			if (getBits(rc.readSharedArray(i), 15, 15) == 1) {
				return true;
			}
		}
		return false;
	}

	public static void updateSymmetry(int archonIndex, int value) throws GameActionException {
		value = setBits(rc.readSharedArray(4), 2 * archonIndex, 2 * archonIndex + 1, value);
		rc.writeSharedArray(4, value);
	}

	public static SymmetryType getBroadcastedSymmetry(int archonIndex) throws GameActionException {
		int value = rc.readSharedArray(4);
		return SymmetryType.values()[getBits(value, 2 * archonIndex, 2 * archonIndex + 1)];
	}

	public static SymmetryType getPossibleSymmetry() throws GameActionException {
		int value = getBits(rc.readSharedArray(4), 8, 10);
		// TODO: store values array inside SymmetryType
		if ((value & 0b100) == 0) {
			return SymmetryType.values()[2];
		} else if (rc.getMapWidth() > rc.getMapHeight()) {
			if ((value & 0b10) == 0) {
				return SymmetryType.values()[1];
			} else if ((value & 0b1) == 0) {
				return SymmetryType.values()[0];
			} else {
				return SymmetryType.NONE;
			}
		} else {
			if ((value & 0b1) == 0) {
				return SymmetryType.values()[0];
			} else if ((value & 0b10) == 0) {
				return SymmetryType.values()[1];
			} else {
				return SymmetryType.NONE;
			}
		}
	}

	public static int getAliveArchonCount() throws GameActionException {
		int count = 0;
		boolean[] dead = getDeadArchons();
		for (int i = dead.length; --i >= 0;) {
			if (!dead[i]) {
				count++;
			}
		}
		return count;
	}

	public static boolean[] getDeadArchons() throws GameActionException {
		boolean[] isDead = new boolean[maxArchonCount];
		for (int i = maxArchonCount; --i >= 0; ){
			isDead[i] = (getBits(rc.readSharedArray(32 + i), 13, 13) == 1);
		}
		return isDead;
	}

	public static boolean isLocationInEnemyZone(MapLocation l) throws GameActionException {
		MapLocation[] enemyArchons = getEnemyArchonLocations();
		if (enemyArchons == null) {
			return false;
		}
		int zoneRadius = (int) (Math.sqrt(Math.max(rc.getMapWidth(), rc.getMapHeight())) * ENEMY_ZONE_FACTOR);
		for (int i = enemyArchons.length; --i >= 0;) {
			if (enemyArchons[i] != null && l.isWithinDistanceSquared(enemyArchons[i], zoneRadius)) {
				return true;
			}
		}
		return false;
	}

	public static void setBuilderType(BuilderType type, int archonIndex) throws GameActionException {
		int val = Functions.setBits(rc.readSharedArray(14 + archonIndex), 11, 12, type.ordinal());
		rc.writeSharedArray(14 + archonIndex, val);
	}

	public static BuilderType getBuilderType(int archonIndex) throws GameActionException {
		return BuilderType.values()[
				Functions.getBits(rc.readSharedArray(14 + archonIndex), 11, 12)
				];
	}

	public static int getArchonHpPriority(int archonIndex) throws GameActionException {
		int p = 1, myHp = getBits(rc.readSharedArray(14 + archonIndex), 0, 10);
		boolean[] dead = getDeadArchons();
		for (int i = 0; i < maxArchonCount; ++i) {
			if (archonIndex != i && !dead[i]) {
				if (getBits(rc.readSharedArray(14 + i), 0, 10) < myHp) {
					p++;
				}
			}
		}
		return p;
	}

	public static int getCentralArchon() throws GameActionException {
		MapLocation[] archons = getFriendlyArchonLocations();
		MapLocation centre = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
		int minDistance = rc.getMapWidth() * rc.getMapHeight();
		int archonIndex = 0;
		for (int i = maxArchonCount; --i >= 0; ) {
			if (archons[i] != null && !isArchonPortable(i)) {
				int distance = getDistance(archons[i], centre);
				if (distance < minDistance) {
					minDistance = distance;
					archonIndex = i;
				}
			}
		}

		return archonIndex;
	}

	public static int getFarthestArchon() throws GameActionException {
		MapLocation[] archons = getFriendlyArchonLocations();
		MapLocation centre = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
		int maxDistance = 0;
		int archonIndex = 0;
		for (int i = maxArchonCount; --i >= 0; ) {
			if (archons[i] != null && !isArchonPortable(i)) {
				int distance = getDistance(archons[i], centre);
				if (maxDistance < distance) {
					maxDistance = distance;
					archonIndex = i;
				}
			}
		}

		return archonIndex;
	}

	public static void updateLabBuilt(int archonIndex) throws GameActionException {
		rc.writeSharedArray(
				14 + archonIndex,
				Functions.setBits(rc.readSharedArray(14+archonIndex), 15, 15, 1)
		);
	}

	public static boolean isLabBuilt(int archonIndex) throws GameActionException {
		return Functions.getBits(rc.readSharedArray(14+archonIndex), 15, 15) == 1;
	}

	public static void incrementWatchtowersBuilt(int archonIndex) throws GameActionException {
		int val = rc.readSharedArray(10 + archonIndex);
		int count = Functions.getBits(val, 12, 15) + 1;
		val = Functions.setBits(val, 12, 15, count);
		rc.writeSharedArray(10 + archonIndex, val);
	}

	public static int getWatchtowerCount(int archonIndex) throws GameActionException {
		return Functions.getBits(rc.readSharedArray(10+archonIndex), 12, 15);
	}

	public static boolean minWatchtowersBuilt(int count) throws GameActionException {
		boolean[] dead = getDeadArchons();
		for (int i = maxArchonCount; --i >= 0;) {
			if (!dead[i]) {
				if (getWatchtowerCount(i) < count) {
					return false;
				}
			}
		}
		return true;
	}

	public static boolean allLabsBuilt() throws GameActionException {
		MapLocation[] archons = getFriendlyArchonLocations();
		for (int i = archons.length; --i >= 0;) {
			MapLocation ml = archons[i];
			if (ml != null) {
				if (BuildingHelper.isCornerMine(ml) != isLabBuilt(i)) {
					return false;
				}
			}
		}
		return true;
	}

	public static int getNumberOfLabs() throws GameActionException {
		int count = 0;
		for (int i = maxArchonCount; --i >= 0;) {
			if (isLabBuilt(i)) {
				count++;
			}
		}
		return count;
	}

	public static boolean allLArchonsMutated(int level) throws GameActionException {
		boolean[] dead = getDeadArchons();
		for (int i = maxArchonCount; --i >= 0;) {
			if (!dead[i]) {
				if (Functions.getBits(rc.readSharedArray(14+i), 13, 14) < level) {
					return false;
				}
			}
		}
		return true;
	}

	public static int getAliveSoldierCount() throws GameActionException{
		int roundNumber = rc.getRoundNum();
		if (roundNumber%2 == 1){
			return SOLDIER_COUNT;
		}
		else{
			SOLDIER_COUNT = rc.readSharedArray(7);
			return SOLDIER_COUNT;
		}
	}

	public static int getAliveMinerCount() throws GameActionException{
		int roundNumber = rc.getRoundNum();
		if (roundNumber%2 == 1){
			return MINER_COUNT;
		}
		else{
			MINER_COUNT = rc.readSharedArray(8);
			return MINER_COUNT;
		}
	}

	public static int getAliveSageCount() throws GameActionException{
		int roundNumber = rc.getRoundNum();
		if (roundNumber%2 == 1){
			return SAGE_COUNT;
		}
		else{
			SAGE_COUNT = rc.readSharedArray(9);
			return SAGE_COUNT;
		}
	}

    public static void setArchonPortable(int archonIndex, boolean portable) throws GameActionException {
        rc.writeSharedArray(
                archonIndex + 32,
                Functions.setBits(rc.readSharedArray(archonIndex+32), 12, 12, portable ? 1 : 0)
        );
    }

	public static boolean isArchonPortable(int archonIndex) throws GameActionException {
        return Functions.getBits(rc.readSharedArray(archonIndex+32), 12, 12) == 1;
    }

	public static boolean anyArchonMoving() throws GameActionException {
		boolean[] dead = getDeadArchons();
		for (int i = maxArchonCount; --i >= 0; ) {
			if (!dead[i] && isArchonPortable(i)) {
				return true;
			}
		}
		return false;
	}
}
