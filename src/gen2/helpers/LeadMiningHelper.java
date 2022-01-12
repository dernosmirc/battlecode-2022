package gen2.helpers;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import gen2.util.Functions;
import gen2.util.MetalInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

import static gen2.RobotPlayer.*;

public class LeadMiningHelper {

    private static final int GRID_DIM = 3;

    private static final int SA_START = 36;
    private static final int SA_COUNT = 8;

    private static final double DISTANCE_FACTOR = -0.5;

    private static final int MAX_7BITS = 127;
    private static final double COMPRESSION = 0.001;
    private static int scaleLeadTo7Bits(int lead) {
        return (int) Math.floor(127 * (1 - Math.exp(-COMPRESSION * lead)));
    }
    private static int scale7BitsToLead(int bits) {
        return (int) Math.floor(-Math.log(1 - bits / (double) MAX_7BITS) / COMPRESSION);
    }

    private static MapLocation getLocationFrom9Bits(int bits) {
        int grid_x = bits / 22;
        int grid_y = bits % 22;
        return new MapLocation(
                grid_x * GRID_DIM + GRID_DIM / 2,
                grid_y * GRID_DIM + GRID_DIM / 2
        );
    }
    private static int get9BitsFromLocation(MapLocation loc) {
        int grid_x = loc.x / GRID_DIM;
        int grid_y = loc.y / GRID_DIM;
        return grid_x * 22 + grid_y;
    }

    private static int getInt16FromInfo(MetalInfo info) {
        int v = Functions.setBits(0, 0, 8, get9BitsFromLocation(info.location));
        v = Functions.setBits(v, 9, 15, scaleLeadTo7Bits(info.amount));
        return v;
    }
    private static MetalInfo getInfoFromInt16(int bits) {
        return new MetalInfo(
                scale7BitsToLead(Functions.getBits(bits, 9, 15)),
                getLocationFrom9Bits(Functions.getBits(bits, 0, 8))
        );
    }

    private static MetalInfo[] getLeadOnGrid() throws GameActionException {
        MetalInfo[] infos = new MetalInfo[SA_COUNT];
        for (int i = SA_START; i < SA_START + SA_COUNT; i++) {
            MetalInfo info = getInfoFromInt16(rc.readSharedArray(i));
            if (info.amount != 0) {
                infos[i - SA_START] = info;
            }
        }
        /*if (DEBUG) {
            String out = "Lead -> ";
            for (Object i : Arrays.stream(infos)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingInt(o -> -o.amount))
                    .map(i -> i.location.toString() + ": " + i.amount + ", ")
                    .toArray()
            ) {
                out += i;
            }
            System.out.println(out);
        }*/
        return infos;
    }

    public static Direction spotLeadOnGrid() throws GameActionException {
        MapLocation location = rc.getLocation(), leadLoc = null;
        double maxFac = 0;
        MetalInfo[] infos = getLeadOnGrid();
        for (int i = 0; i < SA_COUNT; i++) {
            MetalInfo o = infos[i];
            if (o != null && o.amount > 0) {
                 double fac = o.amount * Math.pow(o.location.distanceSquaredTo(location), DISTANCE_FACTOR);
                 if (fac > maxFac) {
                     leadLoc = o.location;
                     maxFac = fac;
                 }
            }
        }
        if (leadLoc == null) return null;
        else return location.directionTo(leadLoc);
    }

    private static MetalInfo getLeadInfoCell(MapLocation location) throws GameActionException {
        MapLocation center = new MapLocation(
                location.x - (location.x % GRID_DIM) + GRID_DIM / 2,
                location.y - (location.y % GRID_DIM) + GRID_DIM / 2
        );

        if (!rc.canSenseLocation(center) || !rc.onTheMap(center)) return null;

        int count = 0;
        for (int x = -GRID_DIM/2; x < (GRID_DIM+1)/2; x++) {
            for (int y = -GRID_DIM/2; y < (GRID_DIM+1)/2; y++) {
                MapLocation loc = new MapLocation(center.x + x, center.y + y);
                if (rc.canSenseLocation(loc)) {
                    count += Math.max(rc.senseLead(loc) - 1, 0);
                }
            }
        }
        return new MetalInfo(count, center);
    }

    public static void updateLeadAmountInGridCell() throws GameActionException {
        MapLocation now = rc.getLocation();
        ArrayList<MapLocation> adj = new ArrayList<>();
        adj.add(new MapLocation(now.x, now.y));

        // TODO OPTIMISE
        /*if (myType != RobotType.MINER) {
            adj.add(new MapLocation(now.x, now.y + GRID_DIM));
            adj.add(new MapLocation(now.x, now.y - GRID_DIM));
            adj.add(new MapLocation(now.x + GRID_DIM, now.y));
            adj.add(new MapLocation(now.x - GRID_DIM, now.y));
        }*/
        for (MapLocation loc : adj) {
            MetalInfo mInfo = getLeadInfoCell(loc);
            if (mInfo != null) {
                MetalInfo[] infos = getLeadOnGrid();
                MetalInfo minInfo = new MetalInfo(0, new MapLocation(-1, -1));
                int index = 0;
                boolean foundLocation = false;
                for (int i = 0; i < SA_COUNT; i++) {
                    if (infos[i] == null) {
                        index = i;
                        break;
                    }
                    if (infos[i].amount < minInfo.amount) {
                        minInfo = infos[i];
                        index = i;
                    }
                    if (infos[i].location.equals(mInfo.location)) {
                        foundLocation = true;
                        index = i;
                        break;
                    }
                }
                if (foundLocation || minInfo.amount < mInfo.amount) {
                    rc.writeSharedArray(index + SA_START, getInt16FromInfo(mInfo));
                }
            }
        }
    }


    public static void mineLead() throws GameActionException {
        for (MapLocation mp: rc.senseNearbyLocationsWithLead(myType.actionRadiusSquared, 2)) {
            while (rc.isActionReady() && rc.senseLead(mp) > 1) {
                rc.mineLead(mp);
            }
            if (!rc.isActionReady()) {
                break;
            }
        }
    }

    public static boolean canMineLead() throws GameActionException {
        return rc.senseNearbyLocationsWithLead(myType.actionRadiusSquared, 2).length > 0;
    }

    public static Direction spotLead() throws GameActionException {
        MapLocation location = rc.getLocation();
        Direction best = null;
        double bestFactor = 0;
        for (MapLocation mp: rc.senseNearbyLocationsWithLead(8)) {
            double factor = (rc.senseLead(mp) - 1) / Math.pow (location.distanceSquaredTo(mp) + 1, 1) ;
            if (factor > bestFactor) {
                best = location.directionTo(mp);
                bestFactor = factor;
            }
        }
        return best;
    }
}
