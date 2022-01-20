package gen6.builder;

import battlecode.common.*;
import gen6.Archon;
import gen6.Builder;
import gen6.common.CommsHelper;
import gen6.common.Functions;

import static gen6.RobotPlayer.*;

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
            if (ri.health < ri.type.getMaxHealth(ri.level)) {
                return ri.location;
            }
        }
        return null;
    }

    public static boolean shouldMove(MapLocation archon, MapLocation construction) {
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
        int minDistance = Integer.MAX_VALUE, w = rc.getMapWidth()-1, h = rc.getMapHeight()-1;
        MapLocation[] enemyArchons = CommsHelper.getEnemyArchonLocations();
        MapLocation[] possible = {
                new MapLocation(0, 0),
                new MapLocation(0, h),
                new MapLocation(w, 0),
                new MapLocation(w, h),
        };
        MapLocation optimal = null, rn = rc.getLocation();
        for (int i = possible.length; --i >= 0;) {
            MapLocation loc = possible[i];
            int total = rn.distanceSquaredTo(loc);
            if (enemyArchons != null) {
                for (int j = enemyArchons.length; --j >= 0; ) {
                    if (enemyArchons[j] != null) {
                        total -= enemyArchons[j].distanceSquaredTo(loc);
                    }
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
}
