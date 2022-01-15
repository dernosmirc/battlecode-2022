package gen3;

import battlecode.common.*;

import static gen3.RobotPlayer.*;

public strictfp class Watchtower {

	private static final RobotType[] priority = {
			RobotType.SAGE,
			RobotType.SOLDIER,
			RobotType.MINER,
	};

	private static MapLocation getAttackLocation() {
		RobotInfo[] infos = rc.senseNearbyRobots(myType.actionRadiusSquared, enemyTeam);
		for (RobotType type: priority) {
			for (RobotInfo ri : infos) {
				if (ri.type == type) {
					return ri.location;
				}
			}
		}
		for (RobotInfo ri : infos) {
			return ri.location;
		}
		return null;
	}

	public static void run() throws GameActionException {
		if (rc.isActionReady()) {
			MapLocation attack = getAttackLocation();
			if (attack != null && rc.canAttack(attack)) {
				rc.attack(attack);
			}
		}
	}
}
