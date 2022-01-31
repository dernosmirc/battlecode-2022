package gen6.archon;

import battlecode.common.*;

import gen6.Archon;
import gen6.common.*;
import gen6.common.bellmanford.HeuristicsProvider;
import gen6.common.bellmanford.heuristics.Heuristics20;
import gen6.common.util.Vector;
import gen6.soldier.SoldierDensity;

import static gen6.RobotPlayer.*;
import static gen6.common.Functions.directionTo;

public class ArchonMover {

    public static final int RUBBLE_THRESHOLD_STOP = 30;
    public static final int MIN_DISTANCE_BETWEEN_ARCHONS = 13;
    public static final int TOO_CLOSE_RANGE = 34;

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

    private static int getWeightedAverageRubble(MapLocation center, RubbleGrid grid) {
        return grid.get(center.add(Direction.NORTH)) +
                grid.get(center.add(Direction.NORTHEAST)) +
                grid.get(center.add(Direction.EAST)) +
                grid.get(center.add(Direction.SOUTHEAST)) +
                grid.get(center.add(Direction.SOUTH)) +
                grid.get(center.add(Direction.SOUTHWEST)) +
                grid.get(center.add(Direction.WEST)) +
                grid.get(center.add(Direction.NORTHWEST));
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

    public static int lowHpAttackerCount() {
        int count = 0;
        RobotInfo[] ris = rc.senseNearbyRobots(myType.visionRadiusSquared, myTeam);
        for (int i = ris.length; --i >= 0;) {
            RobotInfo ri = ris[i];
            if (ri.mode == RobotMode.DROID && ri.type.canAttack() && ri.health < ri.type.getMaxHealth(ri.level)) {
                count++;
            }
        }
        return count;
    }

    public static boolean shouldRelocate(MapLocation relocate) throws GameActionException {
        if (!rc.canTransform()) return false;
        if (relocate == null) return false;
        if (CommsHelper.anyArchonMoving()) return false;
        if (CommsHelper.getFarthestArchon() != Archon.myIndex) return false;
        if (CommsHelper.anyArchonMoving()) return false;
        if (rc.getLocation().isWithinDistanceSquared(relocate, TOO_CLOSE_RANGE)) return false;
        if (lowHpAttackerCount() > 5) return false;
        return !isEnemyAround();
    }

    public static boolean shouldRelocateNearby(MapLocation betterSpot) throws GameActionException {
        if (!rc.canTransform()) return false;
        if (rc.getRoundNum() < 25) return false;
        if (betterSpot == null) return false;
        if (archonOnMostRubble() != Archon.myIndex) return false;
        if (CommsHelper.anyArchonMoving()) return false;
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
        Vector<MapLocation> spots = new Vector<>(locsX.length);
        MapLocation[] mls = CommsHelper.getFriendlyArchonLocations();
        int leastRubble = 100;
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
                if (rubble <= leastRubble && distanceFromEdge(ml) > 5) {
                    spots.add(ml);
                    leastRubble = rubble;
                }
            }
        }
        int bestAvg = Integer.MAX_VALUE;
        if (grid.get(rn) == leastRubble) {
            bestAvg = getWeightedAverageRubble(rn, grid);
        }
        MapLocation theSpot = rn;
        for (int i = spots.length; --i >= 0; ) {
            MapLocation ml = spots.get(i);
            if (grid.get(ml) == leastRubble) {
                int avg = getWeightedAverageRubble(ml, grid);
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
        MapLocation[] locs = rc.getAllLocationsWithinRadiusSquared(rn, 20);
        Vector<MapLocation> spots = new Vector<>(locs.length);
        RubbleGrid grid = rubbleGrid;
        int leastRubble = 100;
        for (int i = locs.length; --i >= 0; ) {
            MapLocation ml = locs[i];
            if (!rn.isWithinDistanceSquared(ml, 2)) {
                int r = grid.get(ml);
                if (r <= leastRubble && distanceFromEdge(ml) > 5) {
                    spots.add(ml);
                    leastRubble = r;
                }
            }
        }
        int bestAvg = Integer.MAX_VALUE;
        if (grid.get(rn) == leastRubble) {
            bestAvg = getWeightedAverageRubble(rn, grid);
        }
        MapLocation theSpot = null;
        for (int i = spots.length; --i >= 0; ) {
            MapLocation ml = spots.get(i);
            if (leastRubble == grid.get(ml)) {
                int avg = getWeightedAverageRubble(ml, grid);
                if (bestAvg > avg) {
                    bestAvg = avg;
                    theSpot = ml;
                }
            }
        }
        return theSpot;
    }

    public static Direction getAntiSoldierDirection() {
        int dx = 0, dy = 0;
        for (RobotInfo ri: rc.senseNearbyRobots(myType.visionRadiusSquared, enemyTeam)) {
            if (ri.type.canAttack()) {
                Direction d = ri.location.directionTo(rc.getLocation());
                dx += d.dx;
                dy += d.dy;
            }
        }

        if (dx == 0 && dy == 0) {
            return null;
        }
        return directionTo(dx, dy);
    }
}
