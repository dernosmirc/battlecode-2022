package gen6.common.bellmanford;

import battlecode.common.*;
import gen6.RobotPlayer;
import gen6.common.bellmanford.arrayfillers.ArrayFiller13;
import gen6.common.bellmanford.arrayfillers.ArrayFiller20;
import gen6.common.bellmanford.arrayfillers.ArrayFiller34;
import gen6.common.bellmanford.heuristics.Heuristics13;
import gen6.common.bellmanford.heuristics.Heuristics20;
import gen6.common.bellmanford.heuristics.Heuristics34;
import gen6.common.util.Vector;

public class BellmanFord {

    public static Vector<Direction> getBellmanFordPath(Direction dir) throws GameActionException {
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

        fillArrays();

        RobotInfo[] ris = rc.senseNearbyRobots(radiusSquared);
        for (int i = ris.length; --i >= 0;) {
            notOccupied[ris[i].location.x - rnX_r][ris[i].location.y - rnY_r] = false;
        }

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
        return ret;
    }

    public static Vector<Direction> getBellmanFordPath(MapLocation mapLocation, boolean precise) throws GameActionException {
        RobotController rc = RobotPlayer.rc;
        MapLocation rn = rc.getLocation();
        int ordinal = rn.directionTo(mapLocation).ordinal();
        HeuristicsProvider heuristics = heuristicsProvider;
        int[] locationX = heuristics.getLocationsX(ordinal),
                locationY = heuristics.getLocationsY(ordinal),
                dirX = heuristics.getDirectionsX(ordinal),
                dirY = heuristics.getDirectionsY(ordinal);
        int[][] dist = distancesDump,
                parent = parentDump;
        boolean[][] notOccupied = occupiedDump;

        int r = radius, d = diameter, rnInd = r * d + r,
                rnX = rn.x, rnY = rn.y,
                rnX_r = rnX - r, rnY_r = rnY - r;

        fillArrays();

        RobotInfo[] ris = rc.senseNearbyRobots(radiusSquared);
        for (int i = ris.length; --i >= 0;) {
            notOccupied[ris[i].location.x - rnX_r][ris[i].location.y - rnY_r] = false;
        }

        dist[r][r] = 0;
        arraysFilled = false;
        int vx, vy, ux, uy, w;
        int dirX0 = dirX[0], dirY0 = dirY[0];
        int dirX1 = dirX[1], dirY1 = dirY[1];
        int dirX2 = dirX[2], dirY2 = dirY[2];
        int dirX3 = dirX[3], dirY3 = dirY[3];
        int dirX4 = dirX[4], dirY4 = dirY[4];

        // 1st iteration
        for (int li = locationX.length; --li >= 0; ) {
            vx = locationX[li];
            vy = locationY[li];
            MapLocation location = new MapLocation(rnX_r + vx, rnY_r + vy);
            if (notOccupied[vx][vy] && rc.canSenseLocation(location)) {
                w = rc.senseRubble(location);
                // unrolled loop, ugly but faster
                ux = vx + dirX4;
                uy = vy + dirY4;
                if (dist[vx][vy] > dist[ux][uy] + w) {
                    dist[vx][vy] = dist[ux][uy] + w;
                    parent[vx][vy] = ux * d + uy;
                }
                // another direction
                ux = vx + dirX3;
                uy = vy + dirY3;
                if (dist[vx][vy] > dist[ux][uy] + w) {
                    dist[vx][vy] = dist[ux][uy] + w;
                    parent[vx][vy] = ux * d + uy;
                }
                // another direction
                ux = vx + dirX2;
                uy = vy + dirY2;
                if (dist[vx][vy] > dist[ux][uy] + w) {
                    dist[vx][vy] = dist[ux][uy] + w;
                    parent[vx][vy] = ux * d + uy;
                }
                // another direction
                ux = vx + dirX1;
                uy = vy + dirY1;
                if (dist[vx][vy] > dist[ux][uy] + w) {
                    dist[vx][vy] = dist[ux][uy] + w;
                    parent[vx][vy] = ux * d + uy;
                }
                // another direction
                ux = vx + dirX0;
                uy = vy + dirY0;
                if (dist[vx][vy] > dist[ux][uy] + w) {
                    dist[vx][vy] = dist[ux][uy] + w;
                    parent[vx][vy] = ux * d + uy;
                }
            }
        }

        // find best destination
        int minDistance = INFINITY, minInd = -1;
        if (precise) {
            vx = mapLocation.x - rnX_r;
            vy = mapLocation.y - rnY_r;
            if (dist[vx][vy] != INFINITY) {
                minInd = vx * d + vy;
            }
        } else {
            int x = mapLocation.x - rnX_r, y = mapLocation.y - rnY_r;
            for (int li = dirX.length; --li >= 0; ) {
                vx = x + dirX[li];
                vy = y + dirY[li];
                if (vx >= 0 && vy >= 0 && vx < d && vy < d && minDistance > dist[vx][vy]) {
                    minDistance = dist[vx][vy];
                    minInd = vx * d + vy;
                }
            }
        }

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
        return ret;
    }

    private static HeuristicsProvider heuristicsProvider;
    private static ArrayFiller arrayFiller;
    private static int[][] distancesDump, parentDump;
    private static boolean[][] occupiedDump;
    private static final int INFINITY = 100000;
    public static int radiusSquared;
    private static int radius, diameter;
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

    public static void fillArrays() {
        if (arraysFilled || !usingBellmanFord) {
            return;
        }
        arrayFiller.fillDistance(distancesDump);
        arrayFiller.fillOccupied(occupiedDump);
        arraysFilled = true;
    }
}
