package gen5.common;

import battlecode.common.*;

import gen5.common.bellmanford.*;
import gen5.common.util.Vector;

import static gen5.RobotPlayer.*;
import static gen5.common.bellmanford.BellmanFord.getBellmanFordPath;

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

    public static boolean tryMove (Direction dir, boolean force) throws GameActionException {
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

    public static void prepareBellmanFord(int rSq) {
        BellmanFord.prepareBellmanFord(rSq);
    }

    private static Vector<Direction> path = null;
    private static Direction pathDirection = null;
    private static MapLocation destination = null;
    private static MapLocation lastLocation = null;
    public static boolean moveBellmanFord(Direction dir) throws GameActionException {
        if (dir == null || dir == Direction.CENTER) {
            pathDirection = null;
            return false;
        }

        boolean usingBellman = false;
        if (dir != pathDirection || !rc.getLocation().equals(lastLocation)) {
            path = getBellmanFordPath(dir);
            usingBellman = true;
            pathDirection = dir;
        }

        if (path == null || path.isEmpty()) {
            pathDirection = null;
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

    public static boolean moveBellmanFord(MapLocation mapLocation) throws GameActionException {
        if (mapLocation == null || mapLocation.equals(rc.getLocation())) {
            destination = null;
            return false;
        }

        if (!rc.getLocation().isWithinDistanceSquared(mapLocation, BellmanFord.radiusSquared)) {
            return moveBellmanFord(rc.getLocation().directionTo(mapLocation));
        }

        boolean usingBellman = false;
        if (destination != mapLocation || !rc.getLocation().equals(lastLocation)) {
            path = getBellmanFordPath(mapLocation, false);
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
}
