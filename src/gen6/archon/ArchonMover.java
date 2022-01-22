package gen6.archon;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import gen5.common.bellmanford.HeuristicsProvider;
import gen5.common.bellmanford.heuristics.Heuristics34;
import gen6.common.Functions;
import gen6.common.util.Vector;
import gen6.Archon;
import gen6.RobotPlayer;
import gen6.common.CommsHelper;
import gen6.common.GridInfo;
import gen6.soldier.SoldierDensity;

import java.util.Comparator;

import static gen6.RobotPlayer.rc;

public class ArchonMover {

    public static final int RUBBLE_THRESHOLD = 20;

    private static double getWeightedAverageRubble(MapLocation center) throws GameActionException {
        int centerRubble = rc.senseRubble(center);
        if (centerRubble > RUBBLE_THRESHOLD) {
            return Double.MAX_VALUE;
        }
        int totalRuble = centerRubble, totalLocations = 4;
        Direction[] dirs = RobotPlayer.directions;
        for (int i = dirs.length; --i >= 0; ) {
            MapLocation ml = center.add(dirs[i]);
            int rubble;
            if (rc.canSenseLocation(ml)) {
                rubble = rc.senseRubble(ml);
            } else {
                rubble = 500;
            }

            if (rubble < centerRubble) {
                return Double.MAX_VALUE;
            }
            totalRuble += rubble;
            totalLocations++;
        }
        return totalRuble / (double) totalLocations;
    }

    public static boolean shouldRelocate(MapLocation relocate) throws GameActionException {
        if (!rc.canTransform()) return false;
        if (relocate == null || rc.getLocation().isWithinDistanceSquared(relocate, 54)) return false;
        return CommsHelper.getFarthestArchon() == Archon.myIndex && !CommsHelper.anyArchonMoving();
    }

    public static MapLocation getRelocateLocation() throws GameActionException {
        GridInfo[] infos = SoldierDensity.getTop();
        MapLocation[] mls = CommsHelper.getFriendlyArchonLocations();
        int maxDistance = 0;
        MapLocation best = null;
        for (int i = infos.length; --i >= 0;) {
            if (infos[i] != null) {
                MapLocation ml = infos[i].location;
                int dist = Math.min(ml.x*ml.x, ml.y*ml.y);
                for (int j = mls.length; --j >= 0; ) {
                    if (j != Archon.myIndex && mls[j] != null) {
                        dist += ml.distanceSquaredTo(mls[j]);
                    }
                }
                if (maxDistance < dist) {
                    maxDistance = dist;
                    best = ml;
                }
            }
        }
        return best;
    }

    private static final HeuristicsProvider heuristicsProvider = new Heuristics34();
    private static final int r = (int) Math.sqrt(34) + 1;
    public static MapLocation getSpotToSettle(Direction direction) throws GameActionException {
        MapLocation rn = rc.getLocation();
        int rnX = rn.x, rnY = rn.y,
                rnX_r = rnX - r, rnY_r = rnY - r;
        if (direction == Direction.CENTER) {
            direction = Functions.getRandomDirection();
        }
        int[] locsX = heuristicsProvider.getLocationsX(direction.ordinal());
        int[] locsY = heuristicsProvider.getLocationsY(direction.ordinal());
        Vector<GridInfo> spots = new Vector<>(locsX.length);
        for (int i = locsX.length; --i >= 0; ) {
            MapLocation ml = new MapLocation(locsX[i] + rnX_r, locsY[i] + rnY_r);
            if (rc.canSenseLocation(ml)) {
                spots.add(new GridInfo(rc.senseRubble(ml), ml));
            }
        }
        spots.sort(Comparator.comparingInt(GridInfo::getCount));
        double bestAvg = Double.MAX_VALUE;
        MapLocation theSpot = null;
        for (int i = 10; --i >= 0; ) {
            MapLocation ml = spots.get(i).location;
            double avg = getWeightedAverageRubble(ml);
            if (bestAvg > avg) {
                bestAvg = avg;
                theSpot = ml;
            }
        }
        return theSpot;
    }
}
