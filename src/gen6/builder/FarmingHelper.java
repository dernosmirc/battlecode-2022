package gen6.builder;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotMode;

import gen6.Builder;

import static gen6.RobotPlayer.myType;
import static gen6.RobotPlayer.rc;

public class FarmingHelper {

    private static final int FARM_RADIUS_LOWER = 2;
    private static final int FARM_RADIUS_UPPER = 20;

    public static MapLocation getFarmCenter() {
        return Builder.myArchonLocation;
    }

    public static boolean isLocationInFarm(MapLocation loc) {
        if (loc.isWithinDistanceSquared(getFarmCenter(), FARM_RADIUS_LOWER)) {
            return false;
        }
        return loc.isWithinDistanceSquared(getFarmCenter(), FARM_RADIUS_UPPER);
    }

    public static MapLocation getBaldSpot() throws GameActionException {
        if (!rc.getLocation().isWithinDistanceSquared(getFarmCenter(), FARM_RADIUS_UPPER)) {
            return null;
        }
        MapLocation[] mls = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), myType.visionRadiusSquared);
        for (int i = mls.length; --i >= 0; ) {
            MapLocation ml = mls[i];
            if (rc.onTheMap(ml) && isLocationInFarm(ml) && rc.senseLead(ml) == 0) {
                RobotInfo ri = rc.senseRobotAtLocation(ml);
                if (ri != null && (ri.mode == RobotMode.DROID || ri.mode == RobotMode.PORTABLE)) {
                    return ml;
                }
            }
        }
        return null;
    }
}
