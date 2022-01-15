package gen3;

import battlecode.common.*;
import gen3.helpers.BuildingHelper;
import gen3.common.MovementHelper;
import gen3.util.Functions;

import static gen3.RobotPlayer.*;
import static gen3.util.Functions.getBits;

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
	private static int myArchonIndex;
	private static ConstructionInfo nextBuilding;

	public static void run() throws GameActionException {
		if (rc.isActionReady()) {
			MapLocation repair = BuildingHelper.getRepairLocation();
			if (repair != null && rc.canRepair(repair)) {
				rc.repair(repair);
			} else {
				if (nextBuilding != null) {
					Direction buildDirection = rc.getLocation().directionTo(nextBuilding.location);
					if (
							rc.getLocation().isWithinDistanceSquared(nextBuilding.location, 2) &&
									rc.canBuildRobot(nextBuilding.type, buildDirection)
					) {
						rc.buildRobot(nextBuilding.type, buildDirection);
						nextBuilding = null;
					}
				}
			}
		}

		if (rc.isMovementReady()) {
			Direction direction = BuildingHelper.getAntiArchonDirection(myArchonLocation);
			if (direction != null) {
				MovementHelper.tryMove(direction, false);
			} else {
				direction = BuildingHelper.getRepairDirection();
				if (direction != null) {
					MovementHelper.tryMove(direction, true);
				} else {
					direction = BuildingHelper.getPerpendicular(myArchonLocation);
					if (direction != null) {
						MovementHelper.tryMove(direction, false);
					}
				}
			}
		}
	}

	public static void init() throws GameActionException {
		archonCount = 0;
		for (int i = 32; i < 36; ++i) {
			int value = rc.readSharedArray(i);
			if (getBits(value, 15, 15) == 1) {
				++archonCount;
				MapLocation archonLocation = new MapLocation(getBits(value, 6, 11), getBits(value, 0, 5));
				if (rc.getLocation().distanceSquaredTo(archonLocation) <= 2) {
					myArchonLocation = new MapLocation(archonLocation.x, archonLocation.y);
					myArchonIndex = i - 32;
					myDirection = myArchonLocation.directionTo(rc.getLocation());
					nextBuilding = new ConstructionInfo(
							RobotType.WATCHTOWER,
							Functions.translate(myArchonLocation, myDirection, 2)
					);
				}
			} else {
				break;
			}
		}
	}
}
