package gen8;

import battlecode.common.*;
import gen8.soldier.AttackHelper;

import static gen8.RobotPlayer.*;

public strictfp class Watchtower {
	public static void run() throws GameActionException {
		if (rc.isActionReady()) {
			AttackHelper.attack();
		}
	}
}
