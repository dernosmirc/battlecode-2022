package gen6.builder;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import gen5.common.CommsHelper;

import static gen5.RobotPlayer.rc;

public class FarmingHelper {

    public static MapLocation getFarmCenter() throws GameActionException {
        int minDistance = Integer.MAX_VALUE, w = rc.getMapWidth()-1, h = rc.getMapHeight()-1;
        MapLocation[] myArchons = CommsHelper.getFriendlyArchonLocations();
        MapLocation[] possible = {
                new MapLocation(0, h/2),
                new MapLocation(w/2, 0),
                new MapLocation(w-1, h/2),
                new MapLocation(w/2, h-1),
        };
        MapLocation optimal = null;
        for (int i = possible.length; --i >= 0;) {
            MapLocation loc = possible[i];
            int total = 0;
            for (int j = myArchons.length; --j >= 0; ) {
                if (myArchons[j] != null) {
                    total -= myArchons[j].distanceSquaredTo(loc);
                }
            }
            if (total < minDistance) {
                minDistance = total;
                optimal = loc;
            }
        }
        return optimal;
    }
}
