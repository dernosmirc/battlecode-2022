package gen6.builder;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotMode;
import gen6.common.CommsHelper;

import java.awt.*;

import static gen6.RobotPlayer.myType;
import static gen6.RobotPlayer.rc;

public class FarmingHelper {

    private static final int FARM_RADIUS_SQUARED = 25;

    public static MapLocation getFarmCenter() throws GameActionException {
        int minDistance = Integer.MAX_VALUE, w = rc.getMapWidth()-1, h = rc.getMapHeight()-1;
        MapLocation[] myArchons = CommsHelper.getFriendlyArchonLocations();
        MapLocation[] theirArchons = CommsHelper.getEnemyArchonLocations();
        MapLocation[] possible = {
                new MapLocation(0, h/2),
                new MapLocation(w/2, 0),
                new MapLocation(w, h/2),
                new MapLocation(w/2, h),
        };
        MapLocation optimal = null;
        for (int i = possible.length; --i >= 0;) {
            MapLocation loc = possible[i];
            int total = 0;
            for (int j = myArchons.length; --j >= 0; ) {
                if (myArchons[j] != null) {
                    total += myArchons[j].distanceSquaredTo(loc);
                }
            }
            if (theirArchons != null) {
                for (int j = theirArchons.length; --j >= 0; ) {
                    if (theirArchons[j] != null) {
                        total -= theirArchons[j].distanceSquaredTo(loc);
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

    public static boolean isLocationInFarm(MapLocation loc) throws GameActionException {
        return loc.isWithinDistanceSquared(getFarmCenter(), FARM_RADIUS_SQUARED);
    }

    public static MapLocation getBaldSpot() throws GameActionException {
        MapLocation[] mls = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), myType.visionRadiusSquared);
        for (int i = mls.length; --i >= 0; ) {
            MapLocation ml = mls[i];
            if (isLocationInFarm(ml) && rc.senseLead(ml) == 0) {
                RobotInfo ri = rc.senseRobotAtLocation(ml);
                if (ri != null && (ri.mode == RobotMode.DROID || ri.mode == RobotMode.PORTABLE)) {
                    return ml;
                }
            }
        }
        return null;
    }
}
