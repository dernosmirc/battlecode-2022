package gen1;

import battlecode.common.*;

public strictfp class RobotPlayer {
	public static RobotController rc;
	public static Team myTeam, enemyTeam;

	public static final Direction[] directions = {
		Direction.NORTH,
		Direction.NORTHEAST,
		Direction.EAST,
		Direction.SOUTHEAST,
		Direction.SOUTH,
		Direction.SOUTHWEST,
		Direction.WEST,
		Direction.NORTHWEST,
	};

	public static void run(RobotController robotController) throws GameActionException {
		rc = robotController;
		myTeam = rc.getTeam();
		enemyTeam = myTeam.opponent();

		while (true) {
			switch (rc.getType()) {
				case LABORATORY:
					Laboratory.run();
					break;
				case MINER:
					Miner.run();
					break;
				case BUILDER:
					Builder.run();
					break;
				case SAGE:
					Sage.run();
					break;
				case SOLDIER:
					Soldier.run();
					break;
				case WATCHTOWER:
					Watchtower.run();
					break;
				case ARCHON:
					Archon.run();
					break;
			}

			Clock.yield();
		}
	}
}
