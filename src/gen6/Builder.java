package gen6;

import battlecode.common.*;
import gen6.builder.BuildingHelper;
import gen6.common.CommsHelper;
import gen6.common.MovementHelper;
import gen6.builder.MutationHelper;
import gen6.builder.BuilderType;
import gen6.common.util.LogCondition;
import gen6.common.util.Logger;
import gen6.common.util.Pair;
import gen6.sage.SageMovementHelper;

import static gen6.RobotPlayer.*;
import static gen6.common.Functions.getAntiEdgeDirection;
import static gen6.common.Functions.getBits;

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
	private static ConstructionInfo constructedBuilding = null;
	public static BuilderType myBuilderType;

	private static void act() throws GameActionException {
		Pair<MapLocation, Boolean> mutate = MutationHelper.getLocationToMutate();
		if (mutate != null) {
			if (mutate.b) {
				if (rc.canMutate(mutate.a)) {
					rc.mutate(mutate.a);
					return;
				}
			} else {
				MovementHelper.tryMove(rc.getLocation().directionTo(mutate.a), false);
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
					rc.getLocation().isWithinDistanceSquared(nextBuilding.location, 2)
			) {
				if (rc.canBuildRobot(nextBuilding.type, buildDirection)) {
					rc.buildRobot(nextBuilding.type, buildDirection);
					switch (nextBuilding.type) {
						case WATCHTOWER:
							CommsHelper.incrementWatchtowersBuilt(myArchonIndex);
							break;
						case LABORATORY:
							CommsHelper.updateLabBuilt(myArchonIndex);
					}
					constructedBuilding = nextBuilding;
					nextBuilding = null;
				} else if (nextBuilding.type == RobotType.LABORATORY) {
					MapLocation req = nextBuilding.location;
					RobotInfo lab = rc.senseRobotAtLocation(req);
					Direction anti = getAntiEdgeDirection();
					if (lab != null && lab.type == RobotType.LABORATORY && anti != null) {
						nextBuilding = new ConstructionInfo(
								RobotType.LABORATORY, req.add(anti)
						);
					}
				}
			} else {
				MovementHelper.tryMove(buildDirection,
						rc.getLocation().isWithinDistanceSquared(nextBuilding.location, 5));
			}
		}
	}

	private static boolean move() throws GameActionException {
		Direction direction = BuildingHelper.getAntiArchonDirection(myArchonLocation);
		if (direction != null) {
			return MovementHelper.tryMove(direction, false);
		}
		MapLocation repair = BuildingHelper.getRepairLocation();
		if (repair != null) {
			return MovementHelper.moveBellmanFord(repair);
		}
		SageMovementHelper.defenseRevolution(myArchonLocation);
		return true;
	}

	public static void mutateLab() throws GameActionException{
		if (constructedBuilding == null) {
			return;
		}
		MapLocation my = rc.getLocation();
		if (!my.isWithinDistanceSquared(constructedBuilding.location, myType.actionRadiusSquared)) {
			if (rc.isMovementReady()) {
				MovementHelper.moveBellmanFord(my);
			}
		}
		if (!my.isWithinDistanceSquared(constructedBuilding.location, myType.actionRadiusSquared)) {
			return;
		}
		RobotInfo lab = rc.senseRobotAtLocation(constructedBuilding.location);
		if (lab == null) {
			constructedBuilding = null;
			return;
		}
		if (rc.canMutate(lab.location)) {
			rc.mutate(lab.location);
			if (lab.level == 3) {
				constructedBuilding = null;
			}
		}
	}

	public static void run() throws GameActionException {
		rc.setIndicatorString(myBuilderType.name() + ", constr=" + (constructedBuilding != null) + ", next" + (nextBuilding != null));
		Logger logger = new Logger("Builder", LogCondition.ExceedsRound);
		if (rc.getRoundNum() > 1150 && rc.getRoundNum() < 1425){
			mutateLab();
		}
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
		MovementHelper.prepareBellmanFord(13);
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
