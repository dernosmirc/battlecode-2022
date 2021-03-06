package gen6.archon;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

import gen6.Archon;
import gen6.RobotPlayer;
import gen6.common.*;
import gen6.common.bellmanford.HeuristicsProvider;
import gen6.common.bellmanford.heuristics.Heuristics20;
import gen6.common.util.Vector;
import gen6.soldier.SoldierDensity;

import java.util.Comparator;

import static gen6.RobotPlayer.*;

public class ArchonMover {

    public static final int RUBBLE_THRESHOLD = 16;
    public static final int RUBBLE_THRESHOLD_STOP = 30;
    public static final int MIN_DISTANCE_BETWEEN_ARCHONS = 13;
    public static final int TOO_CLOSE_RANGE = 13;

    public static RubbleGrid rubbleGrid = new RubbleGrid();

    public static void updateRubble() throws GameActionException {
        int rubble = rc.senseRubble(rc.getLocation());
        rc.writeSharedArray(21 + Archon.myIndex, rubble);
    }
    private static int getRubble(int index) throws GameActionException {
        return rc.readSharedArray(21 + index);
    }

    private static int archonOnMostRubble() throws GameActionException {
        boolean[] dead = CommsHelper.getDeadArchons();
        int most = 0, archon = Archon.myIndex;
        for (int i = maxArchonCount; --i >= 0; ) {
            if (!dead[i]) {
                int r = getRubble(i);
                if (most < r) {
                    most = r;
                    archon = i;
                }
            }
        }
        return archon;
    }

    private static double getWeightedAverageRubble(MapLocation center, RubbleGrid grid) {
        int centerRubble = grid.get(center);
        int totalRuble = centerRubble, totalLocations = 4;
        Direction[] dirs = RobotPlayer.directions;
        for (int i = dirs.length; --i >= 0; ) {
            int rubble = grid.get(center.add(dirs[i]));
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
        if (relocate == null) return false;
        if (CommsHelper.anyArchonMoving()) return false;
        if (CommsHelper.getFarthestArchon() != Archon.myIndex) return false;
        if (CommsHelper.anyArchonMoving()) return false;
        if (rc.getLocation().isWithinDistanceSquared(relocate, TOO_CLOSE_RANGE)) return false;
        if (rc.senseNearbyRobots(myType.visionRadiusSquared).length > 15) return false;
        return !isEnemyAround();
    }

    public static boolean shouldRelocateNearby(MapLocation betterSpot) throws GameActionException {
        if (!rc.canTransform()) return false;
        if (rc.getRoundNum() < 50) return false;
        if (maxArchonCount == 1) return false;
        if (betterSpot == null) return false;
        if (rc.senseRubble(rc.getLocation()) == 0) return false;
        if (archonOnMostRubble() != Archon.myIndex) return false;
        if (CommsHelper.anyArchonMoving()) return false;
        if (rc.senseNearbyRobots(myType.visionRadiusSquared).length > 30) return false;
        return !isEnemyAround();
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
        }

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

    private static final HeuristicsProvider heuristicsProvider = new Heuristics20();
    private static final int r = (int) Math.sqrt(20) + 1;
    public static MapLocation getSpotToSettle(Direction direction) throws GameActionException {
        RubbleGrid grid = rubbleGrid;
        grid.populate();
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
            if (!tooClose) {
                int rubble = grid.get(ml);
                if (rubble < RUBBLE_THRESHOLD && distanceFromEdge(ml) > 5) {
                    spots.add(new GridInfo(rubble, ml));
                }
            }
        }
        spots.sort(Comparator.comparingInt(GridInfo::getCount));
        double bestAvg = Double.MAX_VALUE;
        MapLocation theSpot = rn;
        for (int i = spots.length; --i >= 0; ) {
            MapLocation ml = spots.get(i).location;
            double avg = getWeightedAverageRubble(ml, grid);
            if (bestAvg > avg) {
                bestAvg = avg;
                theSpot = ml;
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
        MapLocation[] locs = rc.getAllLocationsWithinRadiusSquared(rn, 20);
        Vector<GridInfo> spots = new Vector<>(locs.length);
        RubbleGrid grid = rubbleGrid;
        for (int i = locs.length; --i >= 0; ) {
            MapLocation ml = locs[i];
            if (!rn.isWithinDistanceSquared(ml, 5)) {
                int r = grid.get(ml);
                if (r < RUBBLE_THRESHOLD && distanceFromEdge(ml) > 5) {
                    spots.add(new GridInfo(r, ml));
                }
            }
        }
        double bestAvg = getWeightedAverageRubble(rn, grid);
        MapLocation theSpot = null;
        for (int i = Math.min(25, spots.length); --i >= 0; ) {
            MapLocation ml = spots.get(i).location;
            double avg = getWeightedAverageRubble(ml, grid);
            if (bestAvg > avg) {
                bestAvg = avg;
                theSpot = ml;
            }
        }
        return theSpot;
    }
}
