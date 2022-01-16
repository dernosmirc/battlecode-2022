package gen4;

import battlecode.common.*;
import gen4.common.CommsHelper;
import gen4.helpers.BuildingHelper;
import gen4.common.MovementHelper;
import gen4.helpers.MutationHelper;
import gen4.types.BuilderType;
import gen4.util.Functions;
import gen4.util.Logger;
import gen4.util.Pair;

import static gen4.RobotPlayer.*;
import static gen4.util.Functions.getBits;

public strictfp class Builder {

	public static class ConstructionInfo {
		public MapLocation location;
		public RobotType type;
		public ConstructionInfo(RobotType t, MapLocation loc) {
			type = t;
			location = loc;
		}
	}

	private static MapLocation myArchonLocation;
	public static Direction myDirection;
	public static int myArchonIndex;
	private static ConstructionInfo nextBuilding;
	private static BuilderType myBuilderType;

	private static void act() throws GameActionException {
		MapLocation repair = BuildingHelper.getRepairLocation();
		if (repair != null && rc.canRepair(repair)) {
			rc.repair(repair);
		} else if (nextBuilding != null) {
			Direction buildDirection = rc.getLocation().directionTo(nextBuilding.location);
			if (
					rc.getLocation().isWithinDistanceSquared(nextBuilding.location, 2) &&
							rc.canBuildRobot(nextBuilding.type, buildDirection)
			) {
				rc.buildRobot(nextBuilding.type, buildDirection);
				nextBuilding = null;
			}
		}
		if (rc.isActionReady()) {
			Pair<MapLocation, Boolean> mutate = MutationHelper.getLocationToMutate();
			if (mutate != null) {
				if (mutate.value) {
					if (rc.canMutate(mutate.key)) {
						rc.mutate(mutate.key);
					}
				} else {
					MovementHelper.tryMove(rc.getLocation().directionTo(mutate.key), false);
				}
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
			MovementHelper.tryMove(direction, false);
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
					myDirection = myArchonLocation.directionTo(rc.getLocation());
					myBuilderType = CommsHelper.getBuilderType(myArchonIndex);
					switch (myBuilderType) {
						case WatchtowerBuilder:
							nextBuilding = new ConstructionInfo(
									RobotType.WATCHTOWER,
									Functions.translate(myArchonLocation, myDirection, BuildingHelper.WATCHTOWER_DISTANCE)
							);
							break;
					}
				}
			} else {
				break;
			}
		}
	}
}
