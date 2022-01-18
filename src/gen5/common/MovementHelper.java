package gen5.common;

import battlecode.common.*;

import gen5.RobotPlayer;
import gen5.common.generated.Heuristics20;
import gen5.common.util.Logger;
import gen5.common.util.Vector;

import static gen5.RobotPlayer.*;

public class MovementHelper {

    private static final double[] DIRECTION_WEIGHTS = {1, 3, 1};

    private static final double DIRECTION_BETA = 0.334;
    private static double dx = 0, dy = 0;
    public static Direction getInstantaneousDirection() {
        return Functions.directionTo(dx, dy);
    }
    public static void updateMovement(Direction d) {
        dx = DIRECTION_BETA * d.dx + (1-DIRECTION_BETA) * dx;
        dy = DIRECTION_BETA * d.dy + (1-DIRECTION_BETA) * dy;
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
                if (opt != null) {
                    rc.move(opt);
                    updateMovement(opt);
                    return true;
                }
            } else {
                if (rc.canMove(dir)) {
                    rc.move(dir);
                    updateMovement(dir);
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

    private static int[][] distancesDump, parentDump;
    private static boolean[][] occupiedDump;
    private static final int INFINITY = 100000;
    private static int radius, radiusSquared;
    private static int diameter;
    private static boolean arraysFilled = false;
    public static void prepareBellmanFord() {
        int rSq = 20, r, d, size;
        radiusSquared = rSq;

        radius = r = (int) Math.sqrt(rSq);
        diameter = d = r * 2 + 1;
        size = d+1;

        distancesDump = new int[size][size];
        parentDump = new int[size][size];
        occupiedDump = new boolean[size][size];

        for (int i = size; --i >= 0;) {
            for (int j = size; --j >= 0;) {
                distancesDump[i][j] = INFINITY;
                occupiedDump[i][j] = true;
            }
        }
        arraysFilled = true;
    }

    private static void fillArrays() {
        if (arraysFilled) {
            return;
        }
        int d = diameter;
        int[][] dist = distancesDump;
        boolean[][] notOccupied = occupiedDump;
        for (int i = d; --i >= 0;) {
            for (int j = d; --j >= 0;) {
                dist[i][j] = INFINITY;
                notOccupied[i][j] = true;
            }
        }
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

        if (dir != pathDirection || !rc.getLocation().equals(lastLocation)) {
            path = getBellmanFordPath(dir);
            if (path == null) {
                return false;
            }
            pathDirection = dir;
        } else {
            fillArrays();
        }

        if (rc.canMove(path.last())) {
            rc.move(path.popLast());
            lastLocation = rc.getLocation();
            return true;
        }

        if (path.isEmpty()) {
            pathDirection = null;
            path = null;
        }
        return false;
    }

    private static Vector<Direction> getBellmanFordPath(Direction dir) throws GameActionException {
        Logger log = new Logger("Bellman Ford", false);
        RobotController rc = RobotPlayer.rc;
        MapLocation rn = rc.getLocation();
        int ordinal = dir.ordinal();
        int[] locationX = Heuristics20.locationDumpX[ordinal],
                locationY = Heuristics20.locationDumpY[ordinal],
                destinationX = Heuristics20.destinationDumpX[ordinal],
                destinationY = Heuristics20.destinationDumpY[ordinal],
                dirX = Heuristics20.directionDumpX[ordinal],
                dirY = Heuristics20.directionDumpY[ordinal];
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

        // 1st iteration
        for (int li = locationX.length; --li >= 0;) {
            vx = locationX[li];
            vy = locationY[li];
            MapLocation location = new MapLocation(
                    rnX_r + locationX[li], rnY_r + locationY[li]
            );
            if (notOccupied[vx][vy] && rc.canSenseLocation(location)) {
                w = rc.senseRubble(location);
                // unrolled loop, ugly but faster
                ux = vx + dirX2; uy = vy + dirY2;
                if (ux >= 0 && uy >= 0) {
                    if (dist[vx][vy] > dist[ux][uy] + w) {
                        dist[vx][vy] = dist[ux][uy] + w;
                        parent[vx][vy] = ux * d + uy;
                    }
                }
                // another direction
                ux = vx + dirX1; uy = vy + dirY1;
                if (ux >= 0 && uy >= 0) {
                    if (dist[vx][vy] > dist[ux][uy] + w) {
                        dist[vx][vy] = dist[ux][uy] + w;
                        parent[vx][vy] = ux * d + uy;
                    }
                }
                // another direction
                ux = vx + dirX0; uy = vy + dirY0;
                if (ux >= 0 && uy >= 0) {
                    if (dist[vx][vy] > dist[ux][uy] + w) {
                        dist[vx][vy] = dist[ux][uy] + w;
                        parent[vx][vy] = ux * d + uy;
                    }
                }
            }
        }

        log.log("first iteration");

        // 2nd iteration
        for (int li = locationX.length; --li >= 0;) {
            vx = locationX[li];
            vy = locationY[li];
            MapLocation location = new MapLocation(
                    rnX_r + locationX[li], rnY_r + locationY[li]
            );
            if (notOccupied[vx][vy] && rc.canSenseLocation(location)) {
                w = rc.senseRubble(location);
                // unrolled loop, ugly but faster
                ux = vx + dirX2; uy = vy + dirY2;
                if (ux >= 0 && uy >= 0) {
                    if (dist[vx][vy] > dist[ux][uy] + w) {
                        dist[vx][vy] = dist[ux][uy] + w;
                        parent[vx][vy] = ux * d + uy;
                    }
                }
                // another direction
                ux = vx + dirX1; uy = vy + dirY1;
                if (ux >= 0 && uy >= 0) {
                    if (dist[vx][vy] > dist[ux][uy] + w) {
                        dist[vx][vy] = dist[ux][uy] + w;
                        parent[vx][vy] = ux * d + uy;
                    }
                }
                // another direction
                ux = vx + dirX0; uy = vy + dirY0;
                if (ux >= 0 && uy >= 0) {
                    if (dist[vx][vy] > dist[ux][uy] + w) {
                        dist[vx][vy] = dist[ux][uy] + w;
                        parent[vx][vy] = ux * d + uy;
                    }
                }
            }
        }

        log.log("second iteration");


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
        Vector<Direction> ret = new Vector<>(6);
        MapLocation lastLocation = new MapLocation(minInd / d, minInd % d);
        while (minInd != rnInd) {
            minInd = parent[minInd/d][minInd % d];
            MapLocation n = new MapLocation(minInd / d, minInd % d);
            ret.add(n.directionTo(lastLocation));
            lastLocation = n;
        }
        log.log("constructed path");
        //log.flush();
        return ret;
    }
}
