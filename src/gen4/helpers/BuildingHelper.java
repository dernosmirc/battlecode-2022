package gen4.helpers;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

import static gen4.RobotPlayer.*;

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
        MapLocation myLocation = rc.getLocation();
        if (archon.isWithinDistanceSquared(myLocation, MIN_DISTANCE_FROM_ARCHON)) {
            return true;
        }
        if (construction != null && myLocation.isWithinDistanceSquared(construction, myType.actionRadiusSquared)) {
            return false;
        }
        MapLocation repair = getRepairLocation();
        if (repair != null && myLocation.isWithinDistanceSquared(repair, myType.actionRadiusSquared)) {
            return false;
        }
        return true;
    }
}
