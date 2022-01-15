package gen3_2.helpers;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

import static gen3_2.RobotPlayer.*;

public class BuildingHelper {

    private static final int WATCHTOWER_DISTANCE = 2;

    private static final int MIN_DISTANCE_FROM_ARCHON = 4;
    private static final int MAX_DISTANCE_FROM_ARCHON = 25;

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

    public static Direction getPerpendicular(MapLocation archon) {
        if (archon == null) {
            return null;
        }
        switch (archon.directionTo(rc.getLocation())) {
            case NORTH:
                return Direction.EAST;
            case NORTHEAST:
                return Direction.SOUTHEAST;
            case EAST:
                return Direction.SOUTH;
            case SOUTHEAST:
                return Direction.SOUTHWEST;
            case SOUTH:
                return Direction.WEST;
            case SOUTHWEST:
                return Direction.NORTHWEST;
            case WEST:
                return Direction.NORTH;
            case NORTHWEST:
                return Direction.NORTHEAST;
        }
        return null;
    }
}
