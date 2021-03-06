package gen8.builder;

import battlecode.common.*;
import gen8.Archon;
import gen8.Builder;
import gen8.common.CommsHelper;
import gen8.common.Functions;

import static gen8.RobotPlayer.*;

public class BuildingHelper {

    public static final int WATCHTOWER_DISTANCE = 2;

    private static final int MIN_DISTANCE_FROM_ARCHON = 2;
    private static final int MAX_DISTANCE_FROM_ARCHON = 20;

    public static Direction getAntiArchonDirection(MapLocation archon) {
        if (archon == null) {
            return null;
        } else if (
                archon.isWithinDistanceSquared(rc.getLocation(), MAX_DISTANCE_FROM_ARCHON) &&
                !archon.isWithinDistanceSquared(rc.getLocation(), MIN_DISTANCE_FROM_ARCHON)
        ) {
            return null;
        } else if (
                archon.isWithinDistanceSquared(rc.getLocation(), MAX_DISTANCE_FROM_ARCHON)
        ) {
            return archon.directionTo(rc.getLocation());
        } else {
            return rc.getLocation().directionTo(archon);
        }
    }

    public static Direction getRepairDirection() {
        RobotInfo[] infos = rc.senseNearbyRobots(myType.visionRadiusSquared, myTeam);
        for (RobotInfo ri: infos) {
            if (ri.type == RobotType.ARCHON && ri.health < ri.type.getMaxHealth(ri.level)) {
                return rc.getLocation().directionTo(ri.location);
            }
        }
        for (RobotInfo ri: infos) {
            if (ri.health < ri.type.getMaxHealth(ri.level)) {
                return rc.getLocation().directionTo(ri.location);
            }
        }
        return null;
    }

    public static MapLocation getRepairLocation() {
        RobotInfo[] infos = rc.senseNearbyRobots(myType.actionRadiusSquared, myTeam);
        for (RobotInfo ri: infos) {
            if (ri.type == RobotType.ARCHON && ri.health < ri.type.getMaxHealth(ri.level)) {
                return ri.location;
            }
        }
        for (RobotInfo ri: infos) {
            if (ri.mode != RobotMode.DROID && ri.health < ri.type.getMaxHealth(ri.level)) {
                return ri.location;
            }
        }
        return null;
    }

    public static boolean shouldMove(MapLocation archon, MapLocation construction) throws GameActionException {
        if (archon == null) {
            Builder.myArchonIndex = CommsHelper.getCentralArchon();
            archon = Builder.myArchonLocation = CommsHelper.getArchonLocation(Builder.myArchonIndex);
        }
        if (Builder.myBuilderType == BuilderType.FarmSeed) return true;
        MapLocation myLocation = rc.getLocation();
        if (archon.isWithinDistanceSquared(myLocation, MIN_DISTANCE_FROM_ARCHON)) {
            return true;
        }
        if (construction != null && myLocation.isWithinDistanceSquared(construction, myType.actionRadiusSquared)) {
            return false;
        }
        MapLocation repair = getRepairLocation();
        return repair == null || !myLocation.isWithinDistanceSquared(repair, myType.actionRadiusSquared);
    }

    public static MapLocation getOptimalLabLocation() throws GameActionException {
        int minDistance = Integer.MAX_VALUE, w = rc.getMapWidth()-1, h = rc.getMapHeight()-1, factor = 1;
        MapLocation[] enemyArchons = CommsHelper.getEnemyArchonLocations();
        MapLocation[] friendlyArchons = CommsHelper.getFriendlyArchonLocations();
        if (enemyArchons == null) {
            factor = -1;
        }
        MapLocation[] possible = {
                new MapLocation(0, 0),
                new MapLocation(0, h),
                new MapLocation(w, 0),
                new MapLocation(w, h),
                new MapLocation(0, h/2),
                new MapLocation(w/2, 0),
                new MapLocation(w/2, h),
                new MapLocation(w, h/2),
        };
        MapLocation optimal = null, rn = rc.getLocation();
        for (int i = possible.length; --i >= 0;) {
            MapLocation loc = possible[i];
            int total = rn.distanceSquaredTo(loc);
            if (enemyArchons != null) {
                for (int j = enemyArchons.length; --j >= 0; ) {
                    if (enemyArchons[j] != null) {
                        total -= 2*enemyArchons[j].distanceSquaredTo(loc);
                    }
                }
            }
            for (int j = friendlyArchons.length; --j >= 0; ) {
                if (friendlyArchons[j] != null) {
                    total -= factor*friendlyArchons[j].distanceSquaredTo(loc);
                }
            }

            if (total < minDistance) {
                minDistance = total;
                optimal = loc;
            }
        }
        return optimal;
    }

    public static boolean isCornerMine(MapLocation my) throws GameActionException {
        MapLocation corner = getOptimalLabLocation(), ideal = my;
        MapLocation[] archons = CommsHelper.getFriendlyArchonLocations();
        int myDistance = corner.distanceSquaredTo(my), least = myDistance;
        for (int i = archons.length; --i >= 0;) {
            MapLocation ml = archons[i];
            if (ml != null) {
                int total = ml.distanceSquaredTo(corner);
                if (total < least || (total == least && i < Archon.myIndex)) {
                    least = total;
                    ideal = ml;
                }
            }
        }
        return ideal.equals(my);
    }

    public static Builder.ConstructionInfo getNextConstruction() throws GameActionException {
        Builder.myDirection = Builder.myArchonLocation.directionTo(rc.getLocation());
        Builder.myBuilderType = CommsHelper.getBuilderType(Builder.myArchonIndex);
        switch (Builder.myBuilderType) {
            case WatchtowerBuilder:
                return new Builder.ConstructionInfo(
                        RobotType.WATCHTOWER,
                        Functions.translate(Builder.myArchonLocation, Builder.myDirection, WATCHTOWER_DISTANCE)
                );
            case LabBuilder:
                return new Builder.ConstructionInfo(
                        RobotType.LABORATORY,
                        getOptimalLabLocation()
                );
        }
        return null;
    }

    public static MapLocation getNearestCorner(int archonIndex) throws GameActionException {
        MapLocation archonLocation = CommsHelper.getLocationFrom12Bits(rc.readSharedArray(32 + archonIndex));
        int w = rc.getMapWidth() - 1;
        int h = rc.getMapHeight() - 1;
        MapLocation[] possible = {
            new MapLocation(0, 0),
            new MapLocation(0, h),
            new MapLocation(w, 0),
            new MapLocation(w, h),
        };

        int minDistance = (w + 1) * (h + 1);
        MapLocation best = possible[0];
        for (int i = possible.length; --i >= 0; ) {
            int distance = Functions.getDistance(archonLocation, possible[i]);
            if (distance < minDistance) {
                minDistance = distance;
                best = possible[i];
            }
        }

        return best;
    }

    public static MapLocation getOptimalEarlyLabLocation() throws GameActionException {
        MapLocation[] locations = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), myType.visionRadiusSquared);
        int minRubble = rc.senseRubble(rc.getLocation());
        MapLocation optimalLocation = rc.getLocation();
        int minEdgeDistance1 = getDistanceFromEdge(rc.getLocation());
        int minEdgeDistance2 = getLargerDistanceFromEdge(rc.getLocation());
        for (int i = locations.length; --i >= 0; ) {
            MapLocation location = locations[i];
            int rubble = rc.senseRubble(location);
            if (rubble <= minRubble) {
                if (rc.canSenseRobotAtLocation(location)) {
                    RobotInfo robot = rc.senseRobotAtLocation(location);
                    if (robot.mode == RobotMode.TURRET || robot.mode == RobotMode.PROTOTYPE) {
                        continue;
                    }
                }

                if (rubble < minRubble) {
                    minRubble = rubble;
                    optimalLocation = location;
                    minEdgeDistance1 = getDistanceFromEdge(location);
                    minEdgeDistance2 = getLargerDistanceFromEdge(location);
                } else if (rubble == minRubble) {
                    int edgeDistance1 = getDistanceFromEdge(location);
                    int edgeDistance2 = getLargerDistanceFromEdge(location);
                    if (edgeDistance1 < minEdgeDistance1) {
                        minRubble = rubble;
                        optimalLocation = location;
                        minEdgeDistance1 = edgeDistance1;
                        minEdgeDistance2 = edgeDistance2;
                    } else if (edgeDistance1 == minEdgeDistance1 && edgeDistance2 < minEdgeDistance2) {
                        minRubble = rubble;
                        optimalLocation = location;
                        minEdgeDistance1 = edgeDistance1;
                        minEdgeDistance2 = edgeDistance2;
                    }
                }
            }
        }

        return optimalLocation;
    }

    public static int getDistanceFromEdge(MapLocation location) {
        int width = rc.getMapWidth();
        int height = rc.getMapHeight();
        int distance = Math.min(location.x, width - 1 - location.x);
        distance = Math.min(distance, location.y);
        distance = Math.min(distance, height - 1 - location.y);
        return distance;
    }

    public static int getLargerDistanceFromEdge(MapLocation location) {
        int width = rc.getMapWidth();
        int height = rc.getMapHeight();
        int distanceX = Math.min(location.x, width - 1 - location.x);
        int distanceY = Math.min(location.y, height - 1 - location.y);
        if (distanceX > distanceY) {
            return distanceX;
        } else {
            return distanceY;
        }
    }
}
