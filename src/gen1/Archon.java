package gen1;

import battlecode.common.*;
import java.util.Random;

import static gen1.RobotPlayer.*;

public strictfp class Archon {
	private static final Random rng = new Random(21021);

	private static final int BUILD_THRESHOLD = 80; // make dynamic?

	private static int buildDirectionIndex = 0;

	public static void run() throws GameActionException {
		int lead = rc.getTeamLeadAmount(myTeam);
		if (rc.getRoundNum() == 1) {
			if (rc.canBuildRobot(RobotType.MINER, Direction.NORTHWEST)) {
				rc.buildRobot(RobotType.MINER, Direction.NORTHWEST);
			}
		} else if (rc.isActionReady() && lead >= BUILD_THRESHOLD) {
			RobotType spawnType = RobotType.BUILDER;
			switch (rng.nextInt(3)) {
				case 0:
					spawnType = RobotType.BUILDER;
					break;
				case 1:
					spawnType = RobotType.MINER;
					break;
				case 2:
					spawnType = RobotType.SOLDIER;
					break;
			}

			for (int i = 0; i < directions.length; ++i) {
				if (rc.canBuildRobot(spawnType, directions[buildDirectionIndex])) {
					rc.buildRobot(spawnType, directions[buildDirectionIndex]);
					break;
				}

				++buildDirectionIndex;
				if (buildDirectionIndex == directions.length)
					buildDirectionIndex = 0;
			}
		}
	}
}
