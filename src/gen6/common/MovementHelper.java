package gen6.common;

import battlecode.common.*;

import gen6.common.bellmanford.*;
import gen6.common.util.LogCondition;
import gen6.common.util.Logger;
import gen6.common.util.Vector;

import static gen6.RobotPlayer.*;
import static gen6.common.bellmanford.BellmanFord.getBellmanFordPath;

public class MovementHelper {

    private static final double[] DIRECTION_WEIGHTS = {1, 3, 1};

    private static final double DIRECTION_BETA = 0.334;
    private static double dx = 0, dy = 0;
    public static Direction getInstantaneousDirection() {
        return Functions.directionTo(dx, dy);
    }
    public static void updateMovement(Direction d, boolean fillArrays) {
        dx = DIRECTION_BETA * d.dx + (1-DIRECTION_BETA) * dx;
        dy = DIRECTION_BETA * d.dy + (1-DIRECTION_BETA) * dy;
        if (fillArrays) {
            BellmanFord.fillArrays();
        }
    }

    public static boolean moveAndAvoid(
            Direction direction, MapLocation location, int distanceSquared
    ) throws GameActionException {
        Direction[] dirs = {
                direction,
                direction.rotateLeft(),
                direction.rotateRight()
        };
        for (Direction dir: dirs) {
            if (!rc.getLocation().add(dir).isWithinDistanceSquared(location, distanceSquared)) {
                if (tryMove(dir, false)) {
                    return true;
                }
            }
        }
        return tryMove(direction, false);
    }

    public static boolean tryMove (MapLocation loc, boolean force) throws GameActionException {
        return tryMove(rc.getLocation().directionTo(loc), force);
    }

    public static boolean tryMove (Direction dir, boolean force) throws GameActionException {
        if (dir == null || dir == Direction.CENTER) {
            dir = Functions.getRandomDirection();
        }
        if (rc.isMovementReady()) {
            if (!force) {
                Direction[] dirs = {
                        dir.rotateRight(),
                        dir,
                        dir.rotateLeft(),
                };
                MapLocation ml = rc.getLocation();
                Direction opt = null;
                double bestFact = 0;
                for (int i = 0; i < dirs.length; i++) {
                    if (rc.canMove(dirs[i])) {
                        double fact = DIRECTION_WEIGHTS[i] / (1 + rc.senseRubble(ml.add(dirs[i])));
                        if (fact > bestFact) {
                            opt = dirs[i];
                            bestFact = fact;
                        }
                    }
                }
                if (opt != null && rc.canMove(opt)) {
                    rc.move(opt);
                    updateMovement(opt, true);
                    return true;
                }
            } else {
                if (rc.canMove(dir)) {
                    rc.move(dir);
                    updateMovement(dir, true);
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean lazyMove (MapLocation dir) throws GameActionException {
        return lazyMove(rc.getLocation().directionTo(dir));
    }

    public static boolean lazyMove (Direction dir) throws GameActionException {
        if (dir == null || dir == Direction.CENTER) {
            dir = Functions.getRandomDirection();
        }
        if (rc.isMovementReady()) {
            Direction[] dirs = {
                    dir.rotateRight(),
                    dir,
                    dir.rotateLeft(),
            };
            MapLocation ml = rc.getLocation();
            Direction opt = null;
            int leastRubble = rc.senseRubble(ml);
            for (int i = 0; i < dirs.length; i++) {
                if (rc.canMove(dirs[i])) {
                    int rubble = rc.senseRubble(ml.add(dirs[i]));
                    if (leastRubble > rubble) {
                        opt = dirs[i];
                        leastRubble = rubble;
                    }
                }
            }
            if (opt != null && rc.canMove(opt)) {
                rc.move(opt);
                updateMovement(opt, true);
                return true;
            }
        }
        return false;
    }

    public static Direction whereGreedyTryMove(Direction dir) throws GameActionException{
        Direction optimalDirection = null;
        int minRubble = 100000;
        if (rc.canMove(dir)) {
            int rubble = rc.senseRubble(rc.getLocation().add(dir));
            if (rubble < minRubble) {
                minRubble = rubble;
                optimalDirection = dir;
            }
        }
        if (rc.canMove(dir.rotateRight())) {
            int rubble = rc.senseRubble(rc.getLocation().add(dir.rotateRight()));
            if (rubble < minRubble) {
                minRubble = rubble;
                optimalDirection = dir.rotateRight();
            }
        }
        if (rc.canMove(dir.rotateLeft())) {
            int rubble = rc.senseRubble(rc.getLocation().add(dir.rotateLeft()));
            if (rubble < minRubble) {
                minRubble = rubble;
                optimalDirection = dir.rotateLeft();
            }
        }
        return optimalDirection;
    }

    public static boolean greedyTryMove(Direction dir) throws GameActionException {
        if (!rc.isMovementReady() || dir == null) {
            return false;
        }
        Direction optimalDirection = whereGreedyTryMove(dir);

        return optimalDirection != null && tryMove(optimalDirection, true);
    }

    /*
    * @param
    * accepted values for rSq = {13, 20, 34}
    *
    *
    * */

    public static void prepareBellmanFord(int rSq) {
        BellmanFord.prepareBellmanFord(rSq);
        // check for vortex
        AnomalyScheduleEntry[] anomalies = rc.getAnomalySchedule();
        vortexRounds = new Vector<>(anomalies.length);
        for (int i = anomalies.length; --i >= 0;) {
            AnomalyScheduleEntry anomaly = anomalies[i];
            if (anomaly.anomalyType == AnomalyType.VORTEX) {
                vortexRounds.add(anomaly.roundNumber);
            }
        }
    }

    private static Vector<Integer> vortexRounds;
    private static Vector<Direction> path = null;
    private static Direction pathDirection = null;
    private static MapLocation destination = null;
    private static MapLocation lastLocation = null;
    public static boolean moveBellmanFord(Direction dir) throws GameActionException {
        if (dir == null || dir == Direction.CENTER) {
            pathDirection = null;
            return false;
        }

        if (!vortexRounds.isEmpty() && vortexRounds.last() == rc.getRoundNum()) {
            pathDirection = null;
            vortexRounds.popLast();
        }

        boolean usingBellman = false;
        if (dir != pathDirection || !rc.getLocation().equals(lastLocation)) {
            path = getBellmanFordPath(dir);
            usingBellman = true;
            pathDirection = dir;
        }

        if (path == null || path.isEmpty()) {
            pathDirection = null;
            return tryMove(dir, false);
        }
        Direction d = path.last();
        if (rc.canMove(d)) {
            rc.move(d);
            updateMovement(path.popLast(), !usingBellman);
            lastLocation = rc.getLocation();
            return true;
        }

        return false;
    }

    public static boolean moveBellmanFord(MapLocation mapLocation) throws GameActionException {
        if (mapLocation == null || mapLocation.equals(rc.getLocation())) {
            destination = null;
            return false;
        }

        if (rc.getLocation().isWithinDistanceSquared(mapLocation, 2)) {
            return tryMove(rc.getLocation().directionTo(mapLocation), false);
        }

        if (!vortexRounds.isEmpty() && vortexRounds.last() == rc.getRoundNum()) {
            pathDirection = null;
            vortexRounds.popLast();
        }

        boolean usingBellman = false;
        if (destination != mapLocation || !rc.getLocation().equals(lastLocation)) {
            if (!rc.getLocation().isWithinDistanceSquared(mapLocation, BellmanFord.radiusSquared)) {
                path = getBellmanFordPath(rc.getLocation().directionTo(mapLocation));
            } else {
                path = getBellmanFordPath(mapLocation, false);
            }
            usingBellman = true;
            destination = mapLocation;
        }

        if (path == null || path.isEmpty()) {
            destination = null;
            return false;
        }

        Direction d = path.last();
        if (rc.canMove(d)) {
            rc.move(d);
            updateMovement(path.popLast(), !usingBellman);
            lastLocation = rc.getLocation();
            return true;
        }

        return false;
    }

    public static Direction getAntiCrowdingDirection() {
        double[] ratios = new double[8], filter = {.25, .50, .25};
        MapLocation current = rc.getLocation();
        RobotInfo[] mls = rc.senseNearbyRobots(myType.visionRadiusSquared, myTeam);
        double total = mls.length;
        for (int i = mls.length; --i >= 0; ) {
            ratios[current.directionTo(mls[i].location).ordinal()]++;
        }

        for (int i = 0; i < 8; i++) {
            ratios[i] /= total;
        }

        ratios = Functions.convolveCircularly(ratios, filter);

        int maxInd = -1;
        double maxRatio = 0;
        for (int i = 0; i < 8; i++) {
            if (ratios[i] > maxRatio) {
                maxRatio = ratios[i];
                maxInd = i;
            }
        }
        return directions[(maxInd+4)%8];
    }

}
