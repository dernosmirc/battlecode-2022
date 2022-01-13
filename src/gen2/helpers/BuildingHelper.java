package gen2.helpers;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

import static gen2.RobotPlayer.rc;

public class BuildingHelper {

    private static final int WATCHTOWER_DISTANCE = 2;

    private static final int MIN_DISTANCE_FROM_ARCHON = 9;
    private static final int MAX_DISTANCE_FROM_ARCHON = 13;

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
}
