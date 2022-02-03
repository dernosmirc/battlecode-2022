package gen8.common;


import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotMode;

import static gen8.RobotPlayer.myType;
import static gen8.RobotPlayer.rc;

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

        for (int i = diameter; --i >= 0;) {
            for (int j = diameter; --j >= 0;) {
                dump[i][j] = 500;
            }
        }

        MapLocation[] mls = rc.getAllLocationsWithinRadiusSquared(center, radiusSquared);
        for (int i = mls.length; --i >= 0;) {
            MapLocation ml = mls[i];
            dump[ml.x-centerX_r][ml.y-centerY_r] = rc.senseRubble(ml);
        }
        RobotInfo[] ris = rc.senseNearbyRobots(radiusSquared);
        for (int i = ris.length; --i >= 0;) {
            if (ris[i].mode != RobotMode.DROID) {
                dump[ris[i].location.x-centerX_r][ris[i].location.y-centerY_r] = 500;
            }
        }
        dump[center.x-centerX_r][center.y-centerY_r] = rc.senseRubble(center);
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