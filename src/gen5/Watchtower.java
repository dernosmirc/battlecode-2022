package gen5;

import battlecode.common.*;
import gen5.soldier.AttackHelper;

import static gen5.RobotPlayer.*;

public strictfp class Watchtower {
	public static void run() throws GameActionException {
		if (rc.isActionReady()) {
			AttackHelper.attack();
		}
	}
}
