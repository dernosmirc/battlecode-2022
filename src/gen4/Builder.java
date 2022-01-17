package gen4;

import battlecode.common.*;
import gen4.builder.BuildingHelper;
import gen4.common.CommsHelper;
import gen4.common.MovementHelper;
import gen4.builder.MutationHelper;
import gen4.builder.BuilderType;
import gen4.common.Functions;
import gen4.common.util.Logger;
import gen4.common.util.Pair;

import static gen4.RobotPlayer.*;
import static gen4.common.Functions.getBits;

public strictfp class Builder {

	public static class ConstructionInfo {
		public MapLocation location;
		public RobotType type;
		public ConstructionInfo(RobotType t, MapLocation loc) {
			type = t;
			location = loc;
		}
	}

	public static MapLocation myArchonLocation;
	public static Direction myDirection;
	public static int myArchonIndex;
	private static ConstructionInfo nextBuilding;
	public static BuilderType myBuilderType;

	private static void act() throws GameActionException {
		Pair<MapLocation, Boolean> mutate = MutationHelper.getLocationToMutate();
		if (mutate != null) {
			if (mutate.value) {
				if (rc.canMutate(mutate.key)) {
					rc.mutate(mutate.key);
					return;
				}
			} else {
				MovementHelper.tryMove(rc.getLocation().directionTo(mutate.key), false);
			}
		}

		MapLocation repair = BuildingHelper.getRepairLocation();
		if (repair != null && rc.canRepair(repair)) {
			rc.repair(repair);
			return;
		}

		if (nextBuilding != null) {
			Direction buildDirection = rc.getLocation().directionTo(nextBuilding.location);
			if (
					rc.getLocation().isWithinDistanceSquared(nextBuilding.location, 2) &&
							rc.canBuildRobot(nextBuilding.type, buildDirection)
			) {
				rc.buildRobot(nextBuilding.type, buildDirection);
				switch (nextBuilding.type) {
					case WATCHTOWER:
						CommsHelper.incrementWatchtowersBuilt(myArchonIndex);
					case LABORATORY:
						CommsHelper.updateLabBuilt(myArchonIndex);
				}
				nextBuilding = null;
			} else {
				MovementHelper.tryMove(buildDirection,
						rc.getLocation().isWithinDistanceSquared(nextBuilding.location, 5));
			}
		}
	}

	private static void move() throws GameActionException {
		Direction direction = BuildingHelper.getAntiArchonDirection(myArchonLocation);
		if (direction != null) {
			MovementHelper.tryMove(direction, false);
			return;
		}
		direction = BuildingHelper.getRepairDirection();
		if (direction != null) {
			MovementHelper.tryMove(direction, false);
			return;
		}
		direction = Functions.getPerpendicular(myArchonLocation.directionTo(rc.getLocation()));
		if (direction != null) {
			// TODO Improve
			MovementHelper.tryMove(direction, true);
		}
	}

	public static void run() throws GameActionException {
		Logger logger = new Logger("Builder", true);
		if (rc.isActionReady()) {
			act();
		}

		MapLocation construction = null;
		if (nextBuilding != null && rc.getTeamLeadAmount(myTeam) > nextBuilding.type.buildCostLead) {
			construction = nextBuilding.location;
		}
		if (rc.isMovementReady() && BuildingHelper.shouldMove(myArchonLocation, construction)) {
			move();
		}
		logger.flush();
	}

	public static void init() throws GameActionException {
		maxArchonCount = 0;
		for (int i = 32; i < 36; ++i) {
			int value = rc.readSharedArray(i);
			if (getBits(value, 15, 15) == 1) {
				++maxArchonCount;
				MapLocation archonLocation = new MapLocation(
						getBits(value, 6, 11), getBits(value, 0, 5)
				);
				if (rc.getLocation().distanceSquaredTo(archonLocation) <= 2) {
					myArchonLocation = new MapLocation(archonLocation.x, archonLocation.y);
					myArchonIndex = i - 32;
					nextBuilding = BuildingHelper.getNextConstruction();
				}
			} else {
				break;
			}
		}
	}
}
