package gen4;

import battlecode.common.*;
import gen4.soldier.AttackHelper;

import static gen4.RobotPlayer.*;

public strictfp class Watchtower {
	public static void run() throws GameActionException {
		if (rc.isActionReady()) {
			AttackHelper.attack();
		}
	}
}
