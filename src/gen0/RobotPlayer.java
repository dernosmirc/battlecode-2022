package gen0;

import battlecode.common.*;

public strictfp class RobotPlayer {
	public static void run(RobotController rc) throws GameActionException {
		if (rc.getType() == RobotType.ARCHON) {
			for (Direction dir : Direction.allDirections()) {
				System.out.println(dir.name() + " " + dir.ordinal());
			}
			while (true) {
				// System.out.println(rc.getActionCooldownTurns());
				if (rc.canBuildRobot(RobotType.MINER, Direction.NORTH)) {
					rc.buildRobot(RobotType.MINER, Direction.NORTH);
				}
				if (rc.canBuildRobot(RobotType.SAGE, Direction.SOUTH)) {
					rc.buildRobot(RobotType.SAGE, Direction.SOUTH);
					break;
				}

				Clock.yield();
			}

			// Bytecodes used: 334
			// for (AnomalyScheduleEntry anomaly : rc.getAnomalySchedule()) {
			// 	System.out.println(anomaly.anomalyType.toString() + " " + anomaly.roundNumber);
			// }

			// Bytecodes used: 276
			// AnomalyScheduleEntry[] anomalyScheduleEntry = rc.getAnomalySchedule();
			// System.out.println(anomalyScheduleEntry[0].anomalyType.toString() + " " + anomalyScheduleEntry[0].roundNumber);
			// System.out.println(anomalyScheduleEntry[1].anomalyType.toString() + " " + anomalyScheduleEntry[1].roundNumber);
			// System.out.println(anomalyScheduleEntry[2].anomalyType.toString() + " " + anomalyScheduleEntry[2].roundNumber);
			// System.out.println(anomalyScheduleEntry[3].anomalyType.toString() + " " + anomalyScheduleEntry[3].roundNumber);
			// System.out.println(anomalyScheduleEntry[4].anomalyType.toString() + " " + anomalyScheduleEntry[4].roundNumber);
			// System.out.println(anomalyScheduleEntry[5].anomalyType.toString() + " " + anomalyScheduleEntry[5].roundNumber);
			// System.out.println(anomalyScheduleEntry[6].anomalyType.toString() + " " + anomalyScheduleEntry[6].roundNumber);
			// System.out.println(anomalyScheduleEntry[7].anomalyType.toString() + " " + anomalyScheduleEntry[7].roundNumber);
			// System.out.println(anomalyScheduleEntry[8].anomalyType.toString() + " " + anomalyScheduleEntry[8].roundNumber);
			// System.out.println(anomalyScheduleEntry[9].anomalyType.toString() + " " + anomalyScheduleEntry[9].roundNumber);

			// if (rc.canBuildRobot(RobotType.MINER, Direction.NORTH)) {
			// 	rc.buildRobot(RobotType.MINER, Direction.NORTH);
			// }
		} else if (rc.getType() == RobotType.MINER) {
			System.out.println(rc.getActionCooldownTurns());
			while (true) {
				MapLocation me = rc.getLocation();
				for (int dx = -1; dx <= 1; ++dx) {
					for (int dy = -1; dy <= 1; ++dy) {
						MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
						while (rc.canMineLead(mineLocation)) {
							rc.mineLead(mineLocation);
						}
					}
				}

				Clock.yield();
			}
		} else if (rc.getType() == RobotType.SAGE) {
			while (true) {
				if (rc.canEnvision(AnomalyType.FURY)) {
					rc.envision(AnomalyType.FURY);
				}

				Clock.yield();
			}
		}

		while (true) {
			Clock.yield();
		}
	}
}
