package gen1;

import battlecode.common.*;

public strictfp class RobotPlayer {

	// toggle logs
	public static final boolean DEBUG = true;

	public static RobotController rc;
	public static Team myTeam, enemyTeam;
	public static RobotType myType;
	public static int archonCount;
	public static MapLocation[] archons;

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

	public static void run (RobotController robotController) throws GameActionException {
		rc = robotController;
		myTeam = rc.getTeam();
		enemyTeam = myTeam.opponent();
		myType = rc.getType();
		archonCount = rc.getArchonCount();
		archons = new MapLocation[archonCount];
		for (MapLocation archon : archons) {
			archon = new MapLocation(-1, -1);
		}

		switch (myType) {
			case MINER:
				Miner.init();
				break;
			case SOLDIER:
				Soldier.init();
				break;
			case ARCHON:
				Archon.init();
				break;
		}

		while (true) {
			switch (myType) {
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
