package gen6.builder;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import gen6.Builder;

import static gen6.RobotPlayer.myType;
import static gen6.RobotPlayer.rc;

public class FarmingHelper {

    private static final int FARM_RADIUS_LOWER = 2;
    private static final int FARM_RADIUS_UPPER = 34;

    public static MapLocation getFarmCenter() {
        return Builder.myArchonLocation;
    }

    public static boolean isLocationInFarm(MapLocation loc) {
        if (getFarmCenter() == null)  {
            return true;
        }
        if (loc.isWithinDistanceSquared(getFarmCenter(), FARM_RADIUS_LOWER)) {
            return false;
        }
        return loc.isWithinDistanceSquared(getFarmCenter(), FARM_RADIUS_UPPER);
    }

    public static MapLocation getBaldSpot() throws GameActionException {
        MapLocation my = rc.getLocation();
        if (!my.isWithinDistanceSquared(getFarmCenter(), FARM_RADIUS_UPPER)) {
            return null;
        }
        MapLocation[] mls = rc.getAllLocationsWithinRadiusSquared(my, myType.visionRadiusSquared);
        MapLocation best = null;
        int minDist = 10000;
        for (int i = mls.length; --i >= 0; ) {
            MapLocation ml = mls[i];
            if (isLocationInFarm(ml) && rc.senseLead(ml) == 0 && !rc.canSenseRobotAtLocation(ml)) {
                int dist = my.distanceSquaredTo(ml);
                if (dist < minDist) {
                    best = ml;
                    minDist = dist;
                }
            }
        }
        return best;
    }
}
