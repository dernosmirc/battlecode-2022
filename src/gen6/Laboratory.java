package gen6;

import battlecode.common.*;
import gen6.common.CommsHelper;
import gen6.common.Functions;

import static gen6.RobotPlayer.myTeam;
import static gen6.RobotPlayer.rc;

public strictfp class Laboratory {

	private static final int RATE_THRESHOLD = 10;

	private static final int mapDim = Math.max(rc.getMapHeight(), rc.getMapWidth());

	private static double getExpectedMiners() {
		return 2 + 10 * Functions.sigmoid((rc.getRoundNum()-1000)/500.0) * (mapDim/20.0);
	}

	private static double getLeadThreshold() throws GameActionException {
		if (CommsHelper.getAliveMinerCount() < getExpectedMiners()) return 52;
		return 1;
	}

	public static void run() throws GameActionException {
		// Update the lab count
		if (rc.getRoundNum()%2 == 1){
			rc.writeSharedArray(26, rc.readSharedArray(26) + 1);
		}
		if (rc.isActionReady()) {
			if (
					rc.getTeamLeadAmount(myTeam) >= getLeadThreshold() &&
					rc.getTransmutationRate() <= RATE_THRESHOLD * rc.getTeamLeadAmount(myTeam) / getLeadThreshold()
			) {
				if (rc.canTransmute()) {
					rc.transmute();
				}
			}
		}
	}
}
