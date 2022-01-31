package gen6;

import battlecode.common.*;
import gen6.common.CommsHelper;
import gen6.common.Functions;
import gen6.soldier.TailHelper;

import static gen6.RobotPlayer.myTeam;
import static gen6.RobotPlayer.rc;
import static gen6.RobotPlayer.maxArchonCount;

public strictfp class Laboratory {

	private static final int RATE_THRESHOLD = 10;

	private static final int mapDim = (int) Math.sqrt(rc.getMapHeight() * rc.getMapWidth());

	public static double getExpectedMiners() {
		int expectedMiners = (int) (2 + 10 * Functions.sigmoid((rc.getRoundNum()-1000)/500.0) * (mapDim/20.0));
		if (rc.getRoundNum() < 150) {
			expectedMiners = Math.min(expectedMiners, maxArchonCount * 3);
		}
		return expectedMiners;
	}

	private static double getLeadThreshold() throws GameActionException {
		if (CommsHelper.getAliveMinerCount() < getExpectedMiners()) {
			return 52;
		}
		return 1;
	}

	public static void run() throws GameActionException {
		// Update the lab count
		if (rc.getRoundNum()%2 == 1){
			rc.writeSharedArray(26, rc.readSharedArray(26) + 1);
		}
		TailHelper.updateTarget();

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

	public static void init() throws GameActionException {
		maxArchonCount = 0;
		for (int i = 0; i < 4; ++i) {
			int value = rc.readSharedArray(i + 32);
			if (Functions.getBits(value, 15, 15) == 1) {
				++maxArchonCount;
			} else {
				break;
			}
		}
	}
}
