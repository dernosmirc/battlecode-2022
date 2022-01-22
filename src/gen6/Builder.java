package gen6;

import battlecode.common.*;
import gen6.builder.BuildingHelper;
import gen6.builder.FarmingHelper;
import gen6.common.CommsHelper;
import gen6.common.MovementHelper;
import gen6.builder.MutationHelper;
import gen6.builder.BuilderType;
import gen6.common.util.LogCondition;
import gen6.common.util.Logger;
import gen6.common.util.Pair;
import gen6.sage.SageMovementHelper;

import static gen6.RobotPlayer.*;
import static gen6.common.Functions.*;

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
	private static MapLocation farmCenter = null;

	private static void act() throws GameActionException {
		MapLocation rn = rc.getLocation();
		if (myBuilderType == BuilderType.FarmSeed && FarmingHelper.isLocationInFarm(rn) && rc.senseLead(rn) == 0) {
			rc.disintegrate();
			return;
		}

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
					Direction antiRight = getDirectionAlongEdge(true, 5),
							antiLeft = getDirectionAlongEdge(false, 5);
					if (lab != null && lab.mode == RobotMode.TURRET) {
						MapLocation left = req.add(antiLeft), right = req.add(antiRight);
						for (int d = 1; d < 4; d++) {
							if (rc.canSenseLocation(left)) {
								lab = rc.senseRobotAtLocation(left);
								if (lab == null || lab.mode != RobotMode.TURRET) {
									nextBuilding = new ConstructionInfo(
											RobotType.LABORATORY, left
									);
									break;
								}
							}
							if (rc.canSenseLocation(right)) {
								lab = rc.senseRobotAtLocation(right);
								if (lab == null || lab.mode != RobotMode.TURRET) {
									nextBuilding = new ConstructionInfo(
											RobotType.LABORATORY, right
									);
									break;
								}
							}
							left = left.add(antiLeft);
							right = right.add(antiRight);
						}
					}
				}
			} else {
				MovementHelper.tryMove(buildDirection,
						rc.getLocation().isWithinDistanceSquared(nextBuilding.location, 5));
			}
		}
	}

	private static boolean move() throws GameActionException {
		if (!rc.isMovementReady()) {
			rc.setIndicatorString("cant move");
			return false;
		}

		if (myBuilderType == BuilderType.FarmSeed) {
			MapLocation ml = FarmingHelper.getBaldSpot();
			if (ml != null) {
				rc.setIndicatorString("farm near");
				return MovementHelper.tryMove(ml, false);
			}
		} else {
			Direction direction = BuildingHelper.getAntiArchonDirection(myArchonLocation);
			if (direction != null) {
				rc.setIndicatorString("anti archon");
				return MovementHelper.tryMove(direction, false);
			}
			MapLocation repair = BuildingHelper.getRepairLocation();
			if (repair != null) {
				rc.setIndicatorString("repair");
				return MovementHelper.tryMove(repair, false);
			}
		}

		rc.setIndicatorString("defense");
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
				rc.setIndicatorString("construction");
				MovementHelper.tryMove(my, false);
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
		// update location each round
		myArchonLocation = CommsHelper.getArchonLocation(myArchonIndex);

		rc.setIndicatorString(myBuilderType.name());
		Logger logger = new Logger("Builder", LogCondition.Never);
		if (rc.getRoundNum() > 1150 && rc.getRoundNum() < 1425 && myBuilderType != BuilderType.FarmSeed) {
			mutateLab();
			logger.log("mutated");
		}
		if (rc.isActionReady()) {
			act();
			logger.log("acted");
		}
		MapLocation construction = null;
		if (nextBuilding != null && rc.getTeamLeadAmount(myTeam) > nextBuilding.type.buildCostLead) {
			construction = nextBuilding.location;
		}
		if (rc.isMovementReady() && BuildingHelper.shouldMove(myArchonLocation, construction)) {
			move();
			logger.log("moved");
		}

		logger.flush();
	}

	public static void init() throws GameActionException {
		maxArchonCount = 0;
		MovementHelper.prepareBellmanFord(20);
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
					nextBuilding = BuildingHelper.getNextConstruction();
					farmCenter = FarmingHelper.getFarmCenter();
				}
			} else {
				break;
			}
		}
	}
}
