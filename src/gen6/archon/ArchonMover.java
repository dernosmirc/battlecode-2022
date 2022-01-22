package gen6.archon;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import gen6.common.bellmanford.HeuristicsProvider;
import gen6.common.bellmanford.heuristics.Heuristics34;
import gen6.Archon;
import gen6.RobotPlayer;
import gen6.common.CommsHelper;
import gen6.common.Functions;
import gen6.common.GridInfo;
import gen6.common.MovementHelper;
import gen6.common.util.Vector;
import gen6.soldier.SoldierDensity;

import java.util.Comparator;

import static gen6.RobotPlayer.*;

public class ArchonMover {

    public static final int RUBBLE_THRESHOLD = 20;
    public static final int RUBBLE_THRESHOLD_STOP = 30;
    public static final int MIN_DISTANCE_BETWEEN_ARCHONS = 13;
    public static final int TOO_CLOSE_RANGE = 13;

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

    public static boolean isEnemyAround() {
        RobotInfo[] infos = rc.senseNearbyRobots(myType.visionRadiusSquared, enemyTeam);
        for (int i = infos.length; --i >= 0; ) {
            if (infos[i].type.canAttack()) {
                return true;
            }
        }
        return false;
    }

    private static Direction getAntiEdge(MapLocation ml) {
        return Functions.getAntiEdgeDirection(ml, 5);
    }

    public static boolean shouldRelocate(MapLocation relocate) throws GameActionException {
        if (!rc.canTransform()) return false;
        if (rc.getRoundNum() < 25) return false;
        if (relocate == null) return false;
        if (rc.senseNearbyRobots(myType.visionRadiusSquared).length > 15) return false;
        if (isEnemyAround()) return false;
        MapLocation rn = rc.getLocation();
        if (rn.isWithinDistanceSquared(relocate, TOO_CLOSE_RANGE)) return false;
        return CommsHelper.getFarthestArchon() == Archon.myIndex && !CommsHelper.anyArchonMoving();
    }

    public static boolean shouldRelocateNearby(MapLocation betterSpot) throws GameActionException {
        if (!rc.canTransform()) return false;
        if (rc.getRoundNum() < 25) return false;
        if (betterSpot == null) return false;
        if (rc.senseNearbyRobots(myType.visionRadiusSquared).length > 30) return false;
        if (isEnemyAround()) return false;
        return !CommsHelper.anyArchonMoving();
    }

    public static MapLocation getRelocateLocation() throws GameActionException {
        GridInfo[] infos = SoldierDensity.getTop();
        MapLocation[] mls = CommsHelper.getFriendlyArchonLocations();
        int maxDistance = 0;
        MapLocation best = null;
        for (int i = infos.length; --i >= 0;) {
            if (infos[i] != null) {
                MapLocation ml = infos[i].location;
                int dist = 0;
                for (int j = mls.length; --j >= 0; ) {
                    if (mls[j] != null) {
                        dist += ml.distanceSquaredTo(mls[j]);
                    }
                }
                if (maxDistance < dist) {
                    maxDistance = dist;
                    best = ml;
                }
            }
        }/*
        if (best == null) {
            MapLocation rn = rc.getLocation();
            Direction anti = getAntiEdge(rn);
            if (anti == null) {
                return null;
            }
            best = rn;
        }*/
        if (best == null) {
            return null;
        }
        Direction anti = getAntiEdge(best);
        if (anti != null) {
            best = Functions.translate(best, anti, 8-distanceFromEdge(best));
        }
        return best;
    }

    private static int distanceFromEdge(MapLocation ml) {
        int w = rc.getMapWidth(), h = rc.getMapHeight();
        return Math.min(
                Math.min(w-ml.x, ml.x + 1),
                Math.min(h-ml.y, ml.y + 1)
        );
    }

    private static final HeuristicsProvider heuristicsProvider = new Heuristics34();
    private static final int r = (int) Math.sqrt(34) + 1;
    public static MapLocation getSpotToSettle(Direction direction) throws GameActionException {
        MapLocation rn = rc.getLocation();
        int rnX = rn.x, rnY = rn.y,
                rnX_r = rnX - r, rnY_r = rnY - r;
        if (direction == Direction.CENTER) {
            direction = MovementHelper.getInstantaneousDirection();
        }
        if (direction == Direction.CENTER) {
            direction = Functions.getRandomDirection();
        }
        int[] locsX = heuristicsProvider.getLocationsX(direction.ordinal());
        int[] locsY = heuristicsProvider.getLocationsY(direction.ordinal());
        Vector<GridInfo> spots = new Vector<>(locsX.length);
        MapLocation[] mls = CommsHelper.getFriendlyArchonLocations();
        for (int i = locsX.length; --i >= 0; ) {
            MapLocation ml = new MapLocation(locsX[i] + rnX_r, locsY[i] + rnY_r);
            boolean tooClose = false;
            for (int j = mls.length; --j >= 0; ) {
                if (mls[j] != null && ml.isWithinDistanceSquared(mls[j], MIN_DISTANCE_BETWEEN_ARCHONS)) {
                    tooClose = true;
                    break;
                }
            }
            if (!tooClose && rc.canSenseLocation(ml)) {
                spots.add(new GridInfo(rc.senseRubble(ml), ml));
            }
        }
        spots.sort(Comparator.comparingInt(GridInfo::getCount));
        double bestAvg = Double.MAX_VALUE;
        MapLocation theSpot = rn;
        for (int i = spots.length; --i >= 0; ) {
            MapLocation ml = spots.get(i).location;
            if (distanceFromEdge(ml) > 5) {
                double avg = getWeightedAverageRubble(ml);
                if (bestAvg > avg) {
                    bestAvg = avg;
                    theSpot = ml;
                }
            }
        }
        return theSpot;
    }

    public static boolean shouldStopMoving() throws GameActionException {
        return isEnemyAround() && rc.senseRubble(rc.getLocation()) <= RUBBLE_THRESHOLD_STOP;
    }

    public static MapLocation getEmergencyStop() throws GameActionException {
        MapLocation rn = rc.getLocation(), stop = null;
        int leastRubble = 101;
        for (int i = 9; --i >= 0;) {
            MapLocation ne = rn.add(Direction.values()[i]);
            if (rc.canSenseLocation(ne) && !rc.isLocationOccupied(ne)) {
                int rubble = rc.senseRubble(ne);
                if (leastRubble > rubble) {
                    leastRubble = rubble;
                    stop = ne;
                }
            }
        }
        return stop;
    }

    public static MapLocation getBetterSpotToSettle() throws GameActionException {
        MapLocation rn = rc.getLocation();
        MapLocation[] locs = rc.getAllLocationsWithinRadiusSquared(rn, 13);
        Vector<GridInfo> spots = new Vector<>(locs.length);
        MapLocation[] mls = CommsHelper.getFriendlyArchonLocations();
        Vector<MapLocation> friends = new Vector<>(maxArchonCount);
        for (int j = mls.length; --j >= 0; ) {
            if (mls[j] != null && Archon.myIndex != j) {
                friends.add(mls[j]);
            }
        }
        for (int i = locs.length; --i >= 0; ) {
            MapLocation ml = locs[i];
            if (rn.isWithinDistanceSquared(ml, 2)) continue;
            boolean tooClose = false;
            for (int j = friends.length; --j >= 0; ) {
                if (ml.isWithinDistanceSquared(friends.get(j), MIN_DISTANCE_BETWEEN_ARCHONS)) {
                    tooClose = true;
                    break;
                }
            }
            if (!tooClose && rc.canSenseLocation(ml)) {
                spots.add(new GridInfo(rc.senseRubble(ml), ml));
            }
        }
        spots.sort(Comparator.comparingInt(GridInfo::getCount));
        double bestAvg = getWeightedAverageRubble(rn);
        MapLocation theSpot = null;
        for (int i = Math.min(8, spots.length); --i >= 0; ) {
            MapLocation ml = spots.get(i).location;
            if (distanceFromEdge(ml) > 5) {
                double avg = getWeightedAverageRubble(ml);
                if (bestAvg > avg) {
                    bestAvg = avg;
                    theSpot = ml;
                }
            }
        }
        return theSpot;
    }
}
