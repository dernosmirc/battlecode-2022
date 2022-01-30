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

	private static final int LAB_RUBBLE_THRESHOLD = 20;

	public static MapLocation myArchonLocation;
	public static Direction myDirection;
	public static int myArchonIndex;
	private static ConstructionInfo nextBuilding;
	private static ConstructionInfo constructedBuilding = null;
	public static BuilderType myBuilderType;
	private static MapLocation farmCenter = null;

	private static boolean amEarlyBuilder = false;
	private static MapLocation labLocation = null;

	private static void act() throws GameActionException {
		MapLocation rn = rc.getLocation();
		if (!amEarlyBuilder && myBuilderType == BuilderType.FarmSeed
			&& FarmingHelper.isLocationInFarm(rn) && rc.senseLead(rn) == 0) {
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

		if (amEarlyBuilder) {
			if (labLocation == null) {
				if (rc.getTeamLeadAmount(myTeam) < RobotType.LABORATORY.buildCostLead - 80) {
					if (rc.isMovementReady()) {
						MovementHelper.moveBellmanFord(BuildingHelper.getNearestCorner(myArchonIndex));
					}
					return;
				}
				labLocation = BuildingHelper.getOptimalEarlyLabLocation();
			}

			if (rc.getLocation().isAdjacentTo(labLocation)) {
				RobotInfo lab = rc.senseRobotAtLocation(labLocation);
				if (lab != null && lab.type == RobotType.LABORATORY && lab.health == lab.type.getMaxHealth(lab.level)) {
					amEarlyBuilder = false;
					myBuilderType = BuilderType.LabBuilder;
					nextBuilding = new ConstructionInfo(RobotType.LABORATORY, BuildingHelper.getOptimalLabLocation());
				}
				if (lab != null && lab.type == RobotType.LABORATORY) {
					return;
				}
			}

			// Lab has not been built yet
			if (rc.getTeamLeadAmount(myTeam) < RobotType.LABORATORY.buildCostLead - 80) {
				labLocation = null;
				if (rc.isMovementReady()) {
					MovementHelper.moveBellmanFord(BuildingHelper.getNearestCorner(myArchonIndex));
				}
				return;
			}

			if (rc.getTeamLeadAmount(myTeam) >= RobotType.LABORATORY.buildCostLead) {
				int minRubble = 1000;
				Direction optimalDirection = null;
				int minEdgeDistance1 = rc.getMapWidth() * rc.getMapHeight();
				int minEdgeDistance2 = minEdgeDistance1;
				for (int i = directions.length; --i >= 0; ) {
					Direction dir = directions[i];
					if (rc.canBuildRobot(RobotType.LABORATORY, dir)) {
						MapLocation location = rc.getLocation().add(dir);
						int rubble = rc.senseRubble(location);
						if (rubble < minRubble) {
							minRubble = rubble;
							optimalDirection = dir;
							minEdgeDistance1 = BuildingHelper.getDistanceFromEdge(location);
							minEdgeDistance2 = BuildingHelper.getLargerDistanceFromEdge(location);
						} else if (rubble == minRubble) {
							int edgeDistance1 = BuildingHelper.getDistanceFromEdge(location);
							int edgeDistance2 = BuildingHelper.getLargerDistanceFromEdge(location);
							if (edgeDistance1 < minEdgeDistance1) {
								minRubble = rubble;
								optimalDirection = dir;
								minEdgeDistance1 = edgeDistance1;
								minEdgeDistance2 = edgeDistance2;
							} else if (edgeDistance1 == minEdgeDistance1 && edgeDistance2 < minEdgeDistance2) {
		                        minRubble = rubble;
		                        optimalDirection = dir;
		                        minEdgeDistance1 = edgeDistance1;
		                        minEdgeDistance2 = edgeDistance2;
		                    }
						}
					}
				}

				if (optimalDirection != null && rc.canBuildRobot(RobotType.LABORATORY, optimalDirection)) {
					rc.buildRobot(RobotType.LABORATORY, optimalDirection);
					labLocation = rc.getLocation().add(optimalDirection);
					CommsHelper.updateLabBuilt(myArchonIndex);
				}
			} else if (rc.getLocation().equals(labLocation)) {
				int minRubble = 1000;
				Direction optimalDirection = null;
				for (int i = directions.length; --i >= 0; ) {
					Direction dir = directions[i];
					MapLocation location = rc.getLocation().add(dir);
					if (rc.canMove(dir)) {
						int rubble = rc.senseRubble(location);
						if (rubble < minRubble) {
							minRubble = rubble;
							optimalDirection = dir;
						}
					}
				}

				if (optimalDirection != null && rc.canMove(optimalDirection)) {
					MovementHelper.tryMove(optimalDirection, true);
				}
			} else if (!rc.getLocation().isAdjacentTo(labLocation)) {
				MovementHelper.moveBellmanFord(labLocation);
			}

			return;
		}

		if (nextBuilding != null) {
			Direction buildDirection = rc.getLocation().directionTo(nextBuilding.location);
			if (
					rc.getLocation().isWithinDistanceSquared(nextBuilding.location, 2)
			) {
				boolean highRubble = rc.senseRubble(nextBuilding.location) > LAB_RUBBLE_THRESHOLD;
				if (rc.canBuildRobot(nextBuilding.type, buildDirection) && !highRubble) {
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
					if (
							(lab != null && lab.mode == RobotMode.TURRET || highRubble)
									&& antiRight != null && antiLeft != null
					) {
						MapLocation left = req.add(antiLeft), right = req.add(antiRight);
						for (int d = 1; d < 4; d++) {
							if (rc.canSenseLocation(left)) {
								lab = rc.senseRobotAtLocation(left);
								if ((lab == null || lab.mode != RobotMode.TURRET) && rc.senseRubble(left) <= LAB_RUBBLE_THRESHOLD) {
									nextBuilding = new ConstructionInfo(
											RobotType.LABORATORY, left
									);
									break;
								}
							}
							if (rc.canSenseLocation(right)) {
								lab = rc.senseRobotAtLocation(right);
								if ((lab == null || lab.mode != RobotMode.TURRET) && rc.senseRubble(right) <= LAB_RUBBLE_THRESHOLD) {
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
			return false;
		}

		if (myBuilderType == BuilderType.FarmSeed) {
			MapLocation ml = FarmingHelper.getBaldSpot();
			if (ml != null) {
				return MovementHelper.tryMove(ml, false);
			}
		} else {
			MapLocation repair = BuildingHelper.getRepairLocation();
			if (repair != null) {
				return MovementHelper.tryMove(repair, false);
			}
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
		// Update the builder count
		if (rc.getRoundNum()%2 == 1){
			rc.writeSharedArray(25, rc.readSharedArray(25) + 1);
		}
		// update location each round
		myArchonLocation = CommsHelper.getArchonLocation(myArchonIndex);

		if (rc.getRoundNum() > 1150 && rc.getRoundNum() < 1425 && myBuilderType != BuilderType.FarmSeed) {
			mutateLab();
		}
		if (rc.isActionReady()) {
			act();
		}

		if (amEarlyBuilder) {
			return;
		}

		MapLocation construction = null;
		if (nextBuilding != null && rc.getTeamLeadAmount(myTeam) >= nextBuilding.type.buildCostLead) {
			construction = nextBuilding.location;
		}
		if (rc.isMovementReady() && BuildingHelper.shouldMove(myArchonLocation, construction)) {
			move();
		}
	}

	public static void init() throws GameActionException {
		maxArchonCount = 0;
		amEarlyBuilder = CommsHelper.isEarlyBuilder();
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
