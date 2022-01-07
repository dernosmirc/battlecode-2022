package gen0;

import battlecode.common.*;

public strictfp class RobotPlayer {
	public static void run(RobotController rc) throws GameActionException {
		if (rc.getType() == RobotType.ARCHON) {
			// Bytecodes used: 334
			// for (AnomalyScheduleEntry anomaly : rc.getAnomalySchedule()) {
			// 	System.out.println(anomaly.anomalyType.toString() + " " + anomaly.roundNumber);
			// }

			// Bytecodes used: 276
			AnomalyScheduleEntry[] anomalyScheduleEntry = rc.getAnomalySchedule();
			System.out.println(anomalyScheduleEntry[0].anomalyType.toString() + " " + anomalyScheduleEntry[0].roundNumber);
			System.out.println(anomalyScheduleEntry[1].anomalyType.toString() + " " + anomalyScheduleEntry[1].roundNumber);
			System.out.println(anomalyScheduleEntry[2].anomalyType.toString() + " " + anomalyScheduleEntry[2].roundNumber);
			System.out.println(anomalyScheduleEntry[3].anomalyType.toString() + " " + anomalyScheduleEntry[3].roundNumber);
			System.out.println(anomalyScheduleEntry[4].anomalyType.toString() + " " + anomalyScheduleEntry[4].roundNumber);
			System.out.println(anomalyScheduleEntry[5].anomalyType.toString() + " " + anomalyScheduleEntry[5].roundNumber);
			System.out.println(anomalyScheduleEntry[6].anomalyType.toString() + " " + anomalyScheduleEntry[6].roundNumber);
			System.out.println(anomalyScheduleEntry[7].anomalyType.toString() + " " + anomalyScheduleEntry[7].roundNumber);
			System.out.println(anomalyScheduleEntry[8].anomalyType.toString() + " " + anomalyScheduleEntry[8].roundNumber);
			System.out.println(anomalyScheduleEntry[9].anomalyType.toString() + " " + anomalyScheduleEntry[9].roundNumber);
		}

		while (true) {
			Clock.yield();
		}
	}
}
