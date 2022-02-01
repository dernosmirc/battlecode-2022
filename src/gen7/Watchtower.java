package gen7;

import battlecode.common.*;
import gen7.soldier.AttackHelper;

import static gen7.RobotPlayer.*;

public strictfp class Watchtower {
	public static void run() throws GameActionException {
		if (rc.isActionReady()) {
			AttackHelper.attack();
		}
	}
}
