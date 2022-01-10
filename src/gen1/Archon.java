package gen1;

import battlecode.common.*;
import java.util.Random;

import static gen1.RobotPlayer.*;
import static gen1.util.Functions.getBits;
import static gen1.util.Functions.setBits;

public strictfp class Archon {
	private static final Random rng = new Random(rc.getID());

	private static final int BUILD_THRESHOLD = 80; // make dynamic?

	private static int buildDirectionIndex = 0;
	// private static MapLocation previousLeadLoc = new MapLocation(-1, -1);
	// private static int previousLeadRound = -100;
	private static int myIndex;

	private static void updateLeadInArray() throws GameActionException {
		int[] minersInDirection = new int[8];
		for (RobotInfo robot : rc.senseNearbyRobots(myType.visionRadiusSquared, myTeam)) {
			if (robot.type == RobotType.MINER) {
				++minersInDirection[rc.getLocation().directionTo(robot.location).ordinal()];
			}
		}

		boolean[] leadInDirection = new boolean[8];
		int minMinersInDirection = 100;
		for (MapLocation loc : rc.senseNearbyLocationsWithLead(myType.visionRadiusSquared, 2)) {
			Direction toLead = rc.getLocation().directionTo(loc);
			if (toLead != Direction.CENTER) {
				leadInDirection[toLead.ordinal()] = true;
				minMinersInDirection = Math.min(minMinersInDirection, minersInDirection[toLead.ordinal()]);
			}
		}

		int value = 0;
		for (Direction dir : directions) {
			if (leadInDirection[dir.ordinal()]) {
				value |= (1 << dir.ordinal());
				if (minersInDirection[dir.ordinal()] == minMinersInDirection) {
					value |= (1 << (dir.ordinal() + 8));
				}
			}
		}

		rc.writeSharedArray(32 + archonCount + myIndex, value);

		// MapLocation leadLoc = new MapLocation(-1, -1);
		// int maxLead = -1;
		// for (MapLocation loc : rc.senseNearbyLocationsWithLead(myType.visionRadiusSquared)) {
		// 	int lead = rc.senseLead(loc);
		// 	if (lead > maxLead) {D
		// 		if (!loc.equals(previousLeadLoc)
		// 			|| rc.getRoundNum() - previousLeadRound >= GameConstants.ADD_LEAD_EVERY_ROUNDS) {
		// 			maxLead = lead;
		// 			leadLoc = new MapLocation(loc.x, loc.y);
		// 		}
		// 	}
		// }

		// if (maxLead <= 0)
		// 	return;
		// // for storing in shared array
		// maxLead = (maxLead + 4) / 5; // ceil(a / b) = floor((a + b - 1) / b)

		// int minLead = Integer.MAX_VALUE;
		// int updateIndex = -1;
		// int freeIndex = -1;
		// for (int i = 0; i < 10; ++i) {
		// 	int value = rc.readSharedArray(i);
		// 	MapLocation loc = new MapLocation((value >> 6) & 0b111111, value & 0b111111);
		// 	// 1 means > 0 lead, 2 means > 5 lead, and so on
		// 	int lead = (((value >> 12) & 0b1111) - 1) * 5;

		// 	if (lead == 0) {
		// 		freeIndex = i;
		// 	} else if (loc.equals(leadLoc)) {
		// 		updateLead(i, leadLoc, maxLead);
		// 		return;
		// 	} else if (lead < minLead) {
		// 		minLead = lead;
		// 		updateIndex = i;
		// 	}
		// }

		// if (freeIndex != -1) {
		// 	updateLead(freeIndex, leadLoc, maxLead);
		// } else if (updateIndex != -1) { // can also include minLead < maxLead
		// 	updateLead(updateIndex, leadLoc, maxLead);
		// }
	}

	// private static void updateLead(int index, MapLocation loc, int lead) throws GameActionException {
	// 	lead = Math.min(lead, 15);
	// 	int value = (lead << 6) | loc.x;
	// 	value = (value << 6) | loc.y;
	// 	rc.writeSharedArray(index, value);

	// 	previousLeadLoc = new MapLocation(loc.x, loc.y);
	// 	previousLeadRound = rc.getRoundNum();
	// }

	public static void run() throws GameActionException {
		int lead = rc.getTeamLeadAmount(myTeam);
		if (rc.getRoundNum() == 1) {
			if (rc.canBuildRobot(RobotType.MINER, Direction.NORTHWEST)) {
				rc.buildRobot(RobotType.MINER, Direction.NORTHWEST);
			}
		} else if (rc.isActionReady() && lead >= BUILD_THRESHOLD) {
			RobotType spawnType = RobotType.BUILDER;
			switch (rng.nextInt(3)) {
				case 0:
					spawnType = RobotType.BUILDER;
					break;
				case 1:
					spawnType = RobotType.MINER;
					break;
				case 2:
					spawnType = RobotType.SOLDIER;
					break;
			}

			for (int i = 0; i < directions.length; ++i) {
				if (rc.canBuildRobot(spawnType, directions[buildDirectionIndex])) {
					rc.buildRobot(spawnType, directions[buildDirectionIndex]);
					break;
				}

				++buildDirectionIndex;
				if (buildDirectionIndex == directions.length)
					buildDirectionIndex = 0;
			}
		}

		updateLeadInArray();
	}

	public static void init() throws GameActionException {
		for (int i = 32; i < 32 + archonCount; ++i) {
			int value = rc.readSharedArray(i);
			if (getBits(value, 15, 15) == 0) {
				value = setBits(0, 15, 15, 1);
				value = setBits(value, 6, 11, rc.getLocation().x);
				value = setBits(value, 0, 5, rc.getLocation().y);
				myIndex = i - 32;
				rc.writeSharedArray(i, value);
				break;
			}
		}
	}
}
