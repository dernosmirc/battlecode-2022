package gen4;

import battlecode.common.*;
import gen4.common.util.Vector;

import java.util.Comparator;

public strictfp class RobotPlayer {

	// toggle logs
	public static final boolean DEBUG = true;

	public static RobotController rc;
	public static Team myTeam, enemyTeam;
	public static RobotType myType;
	public static int maxArchonCount;

	public static double leadIncome = 0;
	public static double goldIncome = 0;

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

	private static final double LEAD_BETA = 0.1;
	private static final double GOLD_BETA = 0.1;

	private static int lastRoundLead = 0;
	private static int lastRoundGold = 0;
	private static void updateIncome() {
		leadIncome = LEAD_BETA * (rc.getTeamLeadAmount(myTeam) - lastRoundLead) + (1 - LEAD_BETA) * leadIncome;
		goldIncome = GOLD_BETA * (rc.getTeamGoldAmount(myTeam) - lastRoundGold) + (1 - GOLD_BETA) * goldIncome;
		lastRoundLead = rc.getTeamLeadAmount(myTeam);
		lastRoundGold = rc.getTeamGoldAmount(myTeam);
	}

	public static void run (RobotController robotController) throws GameActionException {
		rc = robotController;
		myTeam = rc.getTeam();
		enemyTeam = myTeam.opponent();
		myType = rc.getType();

		Integer[] arr = {5 ,6 ,2 ,4 ,9, -1, 4, 5,};
		Vector<Integer> v = new Vector<>(arr);
		v.sort(Comparator.comparingInt(o -> o));
		
		switch (myType) {
			case MINER:
				Miner.init();
				break;
			case SOLDIER:
				Soldier.init();
				break;
			case BUILDER:
				Builder.init();
				break;
			case ARCHON:
				Archon.init();
				break;
			case SAGE:
				Sage.init();
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

			updateIncome();

			Clock.yield();
		}
	}
}
