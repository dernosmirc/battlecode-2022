package gen4;

import battlecode.common.*;

import static gen4.RobotPlayer.myTeam;
import static gen4.RobotPlayer.rc;

public strictfp class Laboratory {

	private static final int RATE_THRESHOLD = 10;

	private static double getLeadThreshold() {
		if (rc.getRoundNum() < 1000) return 125;
		if (rc.getRoundNum() < 1500) return 250;
		return 500;
	}

	public static void run() throws GameActionException {
		if (rc.isActionReady()) {
			if (
					rc.getTeamLeadAmount(myTeam) >= getLeadThreshold() &&
					rc.getTransmutationRate() <= RATE_THRESHOLD
			) {
				if (rc.canTransmute()) {
					rc.transmute();
				}
			}
		}
	}
}
