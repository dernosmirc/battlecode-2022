package gen4;

import battlecode.common.*;

import static gen4.RobotPlayer.myTeam;
import static gen4.RobotPlayer.rc;

public strictfp class Laboratory {

	private static final int RATE_THRESHOLD = 7;
	private static final int LEAD_THRESHOLD = 110;

	public static void run() throws GameActionException {
		if (rc.isActionReady()) {
			if (
					rc.getTeamLeadAmount(myTeam) >= LEAD_THRESHOLD &&
					rc.getTransmutationRate() <= RATE_THRESHOLD
			) {
				if (rc.canTransmute()) {
					rc.transmute();
				}
			}
		}
	}
}
