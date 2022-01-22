package gen6.common;


import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import static gen6.RobotPlayer.myType;
import static gen6.RobotPlayer.rc;

public class RubbleGrid {

    public final int radiusSquared, radius, diameter;
    public MapLocation center;

    private int centerX, centerY, centerX_r, centerY_r;

    private final int[][] dump;

    public RubbleGrid() {
        radiusSquared = myType.visionRadiusSquared;
        radius = (int) Math.sqrt(radiusSquared);
        diameter = radius*2 + 1;
        dump = new int[diameter][diameter];
    }

    public void populate() throws GameActionException {
        center = rc.getLocation();
        centerX = center.x;
        centerY = center.y;
        centerX_r = centerX-radius;
        centerY_r = centerY-radius;

        MapLocation[] mls = rc.getAllLocationsWithinRadiusSquared(center, radiusSquared);
        for (int i = mls.length; --i >= 0;) {
            MapLocation ml = mls[i];
            if (!rc.canSenseLocation(ml) || rc.isLocationOccupied(ml)) {
                dump[ml.x-centerX_r][ml.y-centerY_r] = 500;
            } else {
                dump[ml.x-centerX_r][ml.y-centerY_r] = rc.senseRubble(ml);
            }
        }
    }

    public int get (int x, int y) {
        return getIndexed(x-centerX_r, y-centerY_r);
    }

    public int getRelative (int x, int y) {
        return getIndexed(x+centerX, y+centerY);
    }

    public int getIndexed (int x, int y) {
        return dump[x][y];
    }

    public int get (MapLocation ml) {
        return dump[ml.x-centerX_r][ml.y-centerY_r];
    }
}