package gen4.common;


import battlecode.common.*;

import static gen4.RobotPlayer.*;

public class RubbleGrid {
    
    public static final int INFINITY = 101;

    public final int radiusSquared, radius, diameter;
    public final MapLocation center;

    private int centerX, centerY, centerX_r, centerY_r;

    public RubbleGrid (MapLocation loc, int radiusSquared) {
        this.radiusSquared = radiusSquared;
        center = loc;
        radius = (int) Math.sqrt(radiusSquared);
        diameter = radius*2 + 1;
        centerX = center.x;
        centerY = center.y;
        centerX_r = centerX-radius;
        centerY_r = centerY-radius;
    }

    public int get (int x, int y) throws GameActionException {
        return get(new MapLocation(x, y));
    }

    public int getRelative (int x, int y) throws GameActionException {
        return get(new MapLocation(x+centerX, y+centerY));
    }

    public int getIndexed (int x, int y) throws GameActionException {
        return get(new MapLocation(x+centerX_r, y+centerY_r));
    }

    public boolean isBlockedOrOutsideRelative (int x, int y) throws GameActionException {
        return isBlockedOrOutside(new MapLocation(x+centerX, y+centerY));
    }

    public boolean isBlockedOrOutside (MapLocation ml) throws GameActionException {
        if (center.distanceSquaredTo(ml) > myType.visionRadiusSquared) {
            return true;
        }
        return !rc.onTheMap(ml);
    }

    public int get (MapLocation ml) throws GameActionException {
        if (!rc.onTheMap(ml)) {
            return INFINITY;
        }
        return rc.senseRubble(ml);
    }
}