package gen7;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Clock;
import gen7.common.MovementHelper;
import gen7.common.bellmanford.BellmanFord;
import gen7.miner.GoldMiningHelper;
import gen7.miner.LeadMiningHelper;
import gen7.sage.SageAttackHelper;
import gen7.sage.SageMovementHelper;
import gen7.soldier.SoldierDensity;
import gen7.soldier.TailHelper;

import java.util.Random;

import static gen7.RobotPlayer.*;
import static gen7.common.Functions.getBits;

public strictfp class Sage {

	private static double LAB_HUNTER_RATIO = 0.05;

	public static MapLocation myArchonLocation;
	public static int myArchonIndex;
	private static final Random random = new Random(rc.getID());
	public static boolean isLabHunter = false, isClockWise = false;


	public static void run() throws GameActionException {
		// Update Sage count
		if (rc.getRoundNum()%2 == 1){
			rc.writeSharedArray(9, rc.readSharedArray(9) + 1);
		}
		SoldierDensity.update();
		TailHelper.updateTarget();
		if (rc.isActionReady()) {
			SageAttackHelper.attack();
		}
		if (rc.isMovementReady()) {
			if (isLabHunter) {
				SageMovementHelper.moveToHuntLabs();
			} else {
				SageMovementHelper.move();
			}
		} else {
			BellmanFord.fillArrays();
		}
		GoldMiningHelper.updateGoldAmountInGridCell();
		if (Clock.getBytecodeNum() < 5500) {
			LeadMiningHelper.updateLeadAmountInGridCell();
		}
	}

	public static void init() throws GameActionException {
		LAB_HUNTER_RATIO = Double.parseDouble(
				System.getProperty(
						"bc.testing.team-" + myTeam.name() + ".lab-hunter-ratio",
						"0.05"
				)
		);

		maxArchonCount = 0;
		isLabHunter = random.nextDouble() < LAB_HUNTER_RATIO;
		isClockWise = random.nextDouble() < 0.5;
		for (int i = 0; i < 4; ++i) {
			int value = rc.readSharedArray(i + 32);
			if (getBits(value, 15, 15) == 1) {
				++maxArchonCount;
				value = rc.readSharedArray(i + 50);
				MapLocation archonLocation = new MapLocation(
						getBits(value, 6, 11), getBits(value, 0, 5)
				);
				if (rc.getLocation().distanceSquaredTo(archonLocation) <= 2) {
					myArchonLocation = archonLocation;
					myArchonIndex = i;
				}
			} else {
				break;
			}
		}
		SageMovementHelper.checkForCharge();
		MovementHelper.prepareBellmanFord(34);
	}
}
