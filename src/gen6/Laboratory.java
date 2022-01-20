package gen6;

import battlecode.common.*;

import static gen6.RobotPlayer.myTeam;
import static gen6.RobotPlayer.rc;

public strictfp class Laboratory {

	private static final int RATE_THRESHOLD = 7;

	private static double getLeadThreshold() {
		if (rc.getRoundNum() < 1000) return 125;
		if (rc.getRoundNum() < 1500) return 250;
		return 375;
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
