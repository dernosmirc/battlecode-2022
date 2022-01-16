package gen3.helpers;

import battlecode.common.MapLocation;
import battlecode.common.GameActionException;
import gen3.common.CommsHelper;

import static gen3.RobotPlayer.*;
import static gen3.util.Functions.getBits;
import static gen3.util.Functions.getDistance;

public strictfp class AttackHelper {

	public static MapLocation getDefenseLocation() throws GameActionException {
		boolean[] archonDead = getDeadArchons();
		MapLocation nearestLocation = null;
		int minDistance = rc.getMapWidth() * rc.getMapHeight();
		for (int i = 32; i < 32 + maxArchonCount; ++i) {
			int value = rc.readSharedArray(i);
			if (getBits(value, 14, 14) == 1 && !archonDead[i - 32]) {
				MapLocation location = CommsHelper.getLocationFrom12Bits(value);
				int distance = getDistance(rc.getLocation(), location);
				if (distance < minDistance) {
					minDistance = distance;
					nearestLocation = new MapLocation(location.x, location.y);
				}
			}
		}

		return nearestLocation;
	}

	private static boolean[] getDeadArchons() throws GameActionException {
		boolean[] isDead = new boolean[maxArchonCount];
		int latestCount = rc.getArchonCount();
		if (latestCount == maxArchonCount) {
			return isDead;
		}

		int min0 = 2000, min1 = 2000, min2 = 2000;
		int i0 = -1, i1 = -1, i2 = -1;
		for (int i = 0; i < maxArchonCount; ++i) {
			int hp = rc.readSharedArray(14 + i);
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
}
