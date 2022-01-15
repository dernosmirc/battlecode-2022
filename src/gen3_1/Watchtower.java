package gen3_1;

import battlecode.common.*;

import static gen3_1.RobotPlayer.*;

public strictfp class Watchtower {

	public static void run() throws GameActionException {
		if (rc.isActionReady()) {
			for (RobotInfo ri: rc.senseNearbyRobots(myType.actionRadiusSquared, enemyTeam)) {
				if (rc.canAttack(ri.location)) {
					rc.attack(ri.location);
					break;
				}
			}
		}
	}
}
