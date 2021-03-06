package gen5.common;

import battlecode.common.MapLocation;
import battlecode.common.GameActionException;
import gen5.builder.BuilderType;
import gen5.builder.BuildingHelper;

import static gen5.RobotPlayer.*;
import static gen5.common.Functions.getBits;
import static gen5.common.Functions.setBits;
import static gen5.common.Functions.getDistance;

public strictfp class CommsHelper {

	// TODO improve hyperparameter
	private static final double ENEMY_ZONE_FACTOR = 8;

	public static MapLocation getLocationFrom12Bits(int bits) {
		return new MapLocation(getBits(bits, 6, 11), getBits(bits, 0, 5));
	}

	public static int getBitsFromLocation(MapLocation loc) {
		int val = setBits(0, 6, 11, loc.x);
		return setBits(val, 0, 5, loc.y);
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
				int value = rc.readSharedArray(32 + i);
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
		if ((value & 0b1) == 0) {
			return SymmetryType.values()[0];
		} else if ((value & 0b10) == 0) {
			return SymmetryType.values()[1];
		} else if ((value & 0b100) == 0) {
			return SymmetryType.values()[2];
		} else {
			return SymmetryType.NONE;
		}
	}

	public static boolean[] getDeadArchons() throws GameActionException {
		boolean[] isDead = new boolean[maxArchonCount];
		int latestCount = rc.getArchonCount();
		if (latestCount == maxArchonCount) {
			return isDead;
		}

		int min0 = 2000, min1 = 2000, min2 = 2000;
		int i0 = -1, i1 = -1, i2 = -1;
		for (int i = 0; i < maxArchonCount; ++i) {
			int hp = Functions.getBits(rc.readSharedArray(14 + i), 0, 10);
			if (hp < min2) {
				if (hp < min1) {
					i2 = i1;
					min2 = min1;
					if (hp < min0) {
						i1 = i0;
						min1 = min0;
						i0 = i;
						min0 = hp;
					} else {
						i1 = i;
						min1 = hp;
					}
				} else {
					i2 = i;
					min2 = hp;
				}
			}
		}

		switch (maxArchonCount - latestCount) {
			case 3:
				isDead[i2] = true;
			case 2:
				isDead[i1] = true;
			case 1:
				isDead[i0] = true;
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
		int val = Functions.setBits(rc.readSharedArray(14 + archonIndex), 12, 13, type.ordinal());
		rc.writeSharedArray(14 + archonIndex, val);
	}

	public static BuilderType getBuilderType(int archonIndex) throws GameActionException {
		return BuilderType.values()[
				Functions.getBits(rc.readSharedArray(14 + archonIndex), 12, 13)
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
			if (archons[i] != null) {
				int distance = getDistance(archons[i], centre);
				if (distance < minDistance) {
					minDistance = distance;
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
}
