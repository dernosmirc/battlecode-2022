package gen5.common;

import battlecode.common.*;

import gen5.RobotPlayer;
import gen5.common.generated.*;
import gen5.common.util.Logger;
import gen5.common.util.Vector;

import java.util.Arrays;

import static gen5.RobotPlayer.*;

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
            fillArrays();
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

    /*
    * approx bellmanFord for normal directions
    *
    *
    * */

    private static HeuristicsProvider heuristicsProvider;
    private static ArrayFiller arrayFiller;
    private static int[][] distancesDump, parentDump;
    private static boolean[][] occupiedDump;
    private static final int INFINITY = 100000;
    private static int radius, radiusSquared;
    private static int diameter;
    private static boolean arraysFilled = false;
    private static boolean usingBellmanFord = false;
    public static void prepareBellmanFord(int rSq) {
        usingBellmanFord = true;
        int r, d;
        radiusSquared = rSq;

        radius = r = (int) Math.sqrt(rSq) + 1;
        diameter = d = r * 2 + 1;

        distancesDump = new int[d][d];
        parentDump = new int[d][d];
        occupiedDump = new boolean[d][d];

        for (int i = d; --i >= 0;) {
            for (int j = d; --j >= 0;) {
                distancesDump[i][j] = INFINITY;
                occupiedDump[i][j] = true;
            }
        }
        arraysFilled = true;

        switch (rSq) {
            case 20:
                heuristicsProvider = new Heuristics20();
                arrayFiller = new ArrayFiller20();
                break;
            case 34:
                heuristicsProvider = new Heuristics34();
                arrayFiller = new ArrayFiller34();
                break;
            default:
                heuristicsProvider = new Heuristics13();
                arrayFiller = new ArrayFiller13();
        }
    }

    private static void fillArrays() {
        if (arraysFilled || !usingBellmanFord) {
            return;
        }
        arrayFiller.fillDistance(distancesDump);
        arrayFiller.fillOccupied(occupiedDump);
        arraysFilled = true;
    }

    private static Vector<Direction> path = null;
    private static Direction pathDirection = null;
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

        if (path == null) {
            pathDirection = null;
            return false;
        }
        Direction d = path.last();
        if (rc.canMove(d)) {
            rc.move(d);
            path.popLast();
            updateMovement(d, !usingBellman);
            lastLocation = rc.getLocation();
            if (path.isEmpty()) {
                pathDirection = null;
            }
            return true;
        }

        return false;
    }

    private static Vector<Direction> getBellmanFordPath(Direction dir) throws GameActionException {
        Logger log = new Logger("Bellman Ford", false);
        RobotController rc = RobotPlayer.rc;
        MapLocation rn = rc.getLocation();
        int ordinal = dir.ordinal();
        HeuristicsProvider heuristics = heuristicsProvider;
        int[] locationX = heuristics.getLocationsX(ordinal),
                locationY = heuristics.getLocationsY(ordinal),
                destinationX = heuristics.getDestinationsX(ordinal),
                destinationY = heuristics.getDestinationsY(ordinal),
                dirX = heuristics.getDirectionsX(ordinal),
                dirY = heuristics.getDirectionsY(ordinal);
        int[][] dist = distancesDump,
                parent = parentDump;
        boolean[][] notOccupied = occupiedDump;

        int r = radius, d = diameter, rnInd = r * d + r,
                rnX = rn.x, rnY = rn.y,
                rnX_r = rnX - r, rnY_r = rnY - r;

        log.log("init variables");

        fillArrays();

        log.log("filled arrays");

        RobotInfo[] ris = rc.senseNearbyRobots(radiusSquared);
        for (int i = ris.length; --i >= 0;) {
            RobotInfo ri = ris[i];
            if (ri != null && (ri.mode == RobotMode.PROTOTYPE || ri.mode == RobotMode.TURRET)) {
                int x = ri.location.x - rnX_r, y = ri.location.y - rnY_r;
                notOccupied[x][y] = false;
            }
        }

        log.log("detected nearby fixed bots");

        dist[r][r] = 0;
        arraysFilled = false;
        int vx, vy, ux, uy, w;
        int dirX0 = dirX[0], dirY0 = dirY[0];
        int dirX1 = dirX[1], dirY1 = dirY[1];
        int dirX2 = dirX[2], dirY2 = dirY[2];
        int dirX3 = dirX[3], dirY3 = dirY[3];
        int dirX4 = dirX[4], dirY4 = dirY[4];

        // 1st iteration
        for (int li = locationX.length; --li >= 0;) {
            vx = locationX[li];
            vy = locationY[li];
            MapLocation location = new MapLocation(rnX_r + vx, rnY_r + vy);
            if (notOccupied[vx][vy] && rc.canSenseLocation(location)) {
                w = rc.senseRubble(location);
                // unrolled loop, ugly but faster
                ux = vx + dirX4; uy = vy + dirY4;
                if (dist[vx][vy] > dist[ux][uy] + w) {
                    dist[vx][vy] = dist[ux][uy] + w;
                    parent[vx][vy] = ux * d + uy;
                }
                // another direction
                ux = vx + dirX3; uy = vy + dirY3;
                if (dist[vx][vy] > dist[ux][uy] + w) {
                    dist[vx][vy] = dist[ux][uy] + w;
                    parent[vx][vy] = ux * d + uy;
                }
                // another direction
                ux = vx + dirX2; uy = vy + dirY2;
                if (dist[vx][vy] > dist[ux][uy] + w) {
                    dist[vx][vy] = dist[ux][uy] + w;
                    parent[vx][vy] = ux * d + uy;
                }
                // another direction
                ux = vx + dirX1; uy = vy + dirY1;
                if (dist[vx][vy] > dist[ux][uy] + w) {
                    dist[vx][vy] = dist[ux][uy] + w;
                    parent[vx][vy] = ux * d + uy;
                }
                // another direction
                ux = vx + dirX0; uy = vy + dirY0;
                if (dist[vx][vy] > dist[ux][uy] + w) {
                    dist[vx][vy] = dist[ux][uy] + w;
                    parent[vx][vy] = ux * d + uy;
                }
            }
        }

        log.log("first iteration");

        // find best destination
        int minDistance = INFINITY, minInd = -1;
        for (int li = destinationX.length; --li >= 0;) {
            vx = destinationX[li];
            vy = destinationY[li];
            if (minDistance > dist[vx][vy]) {
                minDistance = dist[vx][vy];
                minInd = vx * d + vy;
            }
        }

        log.log("best destination");

        if (minInd == -1) {
            return null;
        }

        // construct path
        Vector<Direction> ret = new Vector<>(15);
        MapLocation lastLocation = new MapLocation(minInd / d, minInd % d);
        while (minInd != rnInd) {
            minInd = parent[minInd/d][minInd % d];
            MapLocation n = new MapLocation(minInd / d, minInd % d);
            ret.add(n.directionTo(lastLocation));
            lastLocation = n;
        }
        log.log("constructed path");
        log.flush();
        return ret;
    }
}
