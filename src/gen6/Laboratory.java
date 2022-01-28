package gen6;

import battlecode.common.*;
import gen6.archon.SpawnHelper;

import static gen6.RobotPlayer.myTeam;
import static gen6.RobotPlayer.rc;

public strictfp class Laboratory {

	private static final int RATE_THRESHOLD = 10;

	private static double getLeadThreshold() throws GameActionException {
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
