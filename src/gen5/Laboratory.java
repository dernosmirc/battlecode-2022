package gen5;

import battlecode.common.*;

import static gen5.RobotPlayer.myTeam;
import static gen5.RobotPlayer.rc;

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
