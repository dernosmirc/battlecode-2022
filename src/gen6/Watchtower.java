package gen6;

import battlecode.common.*;
import gen6.soldier.AttackHelper;

import static gen6.RobotPlayer.*;

public strictfp class Watchtower {
	public static void run() throws GameActionException {
		if (rc.isActionReady()) {
			AttackHelper.attack();
		}
	}
}
