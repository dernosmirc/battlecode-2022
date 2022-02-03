package gen8;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Clock;
import gen8.common.MovementHelper;
import gen8.common.bellmanford.BellmanFord;
import gen8.miner.GoldMiningHelper;
import gen8.miner.LeadMiningHelper;
import gen8.sage.SageAttackHelper;
import gen8.sage.SageMovementHelper;
import gen8.soldier.SoldierDensity;
import gen8.soldier.TailHelper;

import java.util.Random;

import static gen8.RobotPlayer.*;
import static gen8.Soldier.updateEnemyArchonLocations;
import static gen8.common.Functions.getBits;

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
		updateEnemyArchonLocations();
		SoldierDensity.update();
		TailHelper.updateTarget();
		if (!rc.isMovementReady()) {
			BellmanFord.fillArrays();
		}
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
			GoldMiningHelper.updateGoldAmountInGridCell();
			if (Clock.getBytecodeNum() < 6000) {
				LeadMiningHelper.updateLeadAmountInGridCell();
			}
		}
	}

	public static void init() throws GameActionException {
		LAB_HUNTER_RATIO = Double.parseDouble(
				System.getProperty(
						"bc.testing.team-" + myTeam.name() + ".lab-hunter-ratio",
						""+LAB_HUNTER_RATIO
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
