package gen3_2;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotType;
import gen3_2.common.CommsHelper;
import gen3_2.helpers.SpawnHelper;


import static gen3_2.RobotPlayer.*;
import static gen3_2.util.Functions.getBits;
import static gen3_2.util.Functions.setBits;

public strictfp class Archon {
	public static int myIndex;
	private static int buildDirectionIndex = 0;

	private static boolean[] isPossibleEnemyArchonSymmetry;
	private static int symmetryIndex = 0;

	public static void run() throws GameActionException {
		if (rc.isActionReady()) {
			RobotType toSpawn = SpawnHelper.getNextDroid();
			if (toSpawn != null) {
				Direction direction = SpawnHelper.getOptimalDirection(directions[buildDirectionIndex], toSpawn);
				if (direction != null && rc.canBuildRobot(toSpawn, direction)) {
					buildDirectionIndex = direction.ordinal() + 1;
					SpawnHelper.incrementDroidsBuilt(toSpawn);
					rc.buildRobot(toSpawn, direction);
				}
			}
		}

		if (buildDirectionIndex == 8) {
			buildDirectionIndex = 0;
		}
	}

	public static void broadcastSymmetry() throws GameActionException {
		if (CommsHelper.foundEnemyArchon()) {
			return;
		}

		int value = getBits(rc.readSharedArray(5), 3 * myIndex, 3 * myIndex + 2);
		if ((value & 0b1) != 0) {
			isPossibleEnemyArchonSymmetry[0] = false;
		}
		if ((value & 0b10) != 0) {
			isPossibleEnemyArchonSymmetry[1] = false;
		}
		if ((value & 0b100) != 0) {
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
		archonCount = rc.getArchonCount();
		for (int i = 32; i < 32 + archonCount; ++i) {
			int value = rc.readSharedArray(i);
			if (getBits(value, 15, 15) == 0) {
				value = setBits(0, 15, 15, 1);
				value = setBits(value, 6, 11, rc.getLocation().x);
				value = setBits(value, 0, 5, rc.getLocation().y);
				myIndex = i - 32;
				rc.writeSharedArray(i, value);
				break;
			}
		}

		isPossibleEnemyArchonSymmetry = new boolean[3];
		isPossibleEnemyArchonSymmetry[0] = isPossibleEnemyArchonSymmetry[1] = isPossibleEnemyArchonSymmetry[2] = true;
	}
}
