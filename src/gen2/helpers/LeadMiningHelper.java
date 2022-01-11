package gen2.helpers;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import gen2.Miner;
import gen2.util.Functions;
import gen2.util.MetalInfo;

import java.util.ArrayList;

import static gen2.RobotPlayer.*;

public class LeadMiningHelper {

    private static final int GRID_DIM = 4;
    private static final int LEAD_SCALE = 16;

    private static final int SA_START = 36;
    private static final int SA_COUNT = 8;

    private static MetalInfo[] getLeadOnGrid() throws GameActionException {
        MetalInfo[] infos = new MetalInfo[SA_COUNT];
        for (int i = SA_START; i < SA_START + SA_COUNT; i++) {
            int val = rc.readSharedArray(i);
            int amount = Functions.getBits(val, 8, 15) * LEAD_SCALE;
            if (amount != 0) {
                int grid_x = Functions.getBits(val, 0, 3);
                int grid_y = Functions.getBits(val, 4, 7);
                infos[i - SA_START] = (
                        new MetalInfo(
                                amount,
                                new MapLocation(
                                        grid_x * GRID_DIM + GRID_DIM/2,
                                        grid_y * GRID_DIM + GRID_DIM/2
                                )
                        )
                );
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
                 double fac = o.amount / Math.pow(o.location.distanceSquaredTo(location), 0.5);
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
        for (int x = -2; x < 2; x++) {
            for (int y = -2; y < 2; y++) {
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
        if (myType != RobotType.MINER) {
                adj.add(new MapLocation(now.x, now.y + GRID_DIM));
                adj.add(new MapLocation(now.x, now.y - GRID_DIM));
                adj.add(new MapLocation(now.x + GRID_DIM, now.y));
                adj.add(new MapLocation(now.x - GRID_DIM, now.y));
        }
        for (MapLocation loc : adj) {
            MetalInfo mInfo = getLeadInfoCell(loc);
            if (mInfo != null && mInfo.amount / LEAD_SCALE > 0) {
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
                    int scaled = Math.min(mInfo.amount / LEAD_SCALE, 255);
                    int val = Functions.setBits(0, 8, 15, scaled);
                    val = Functions.setBits(val, 0, 3, mInfo.location.x / GRID_DIM);
                    val = Functions.setBits(val, 4, 7, mInfo.location.y / GRID_DIM);
                    rc.writeSharedArray(index + SA_START, val);
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

    public static Direction spotLead(MapLocation archonLocation) throws GameActionException {
        MapLocation location = rc.getLocation();
        Direction best = null;
        double bestFactor = 0;
        for (MapLocation mp: rc.senseNearbyLocationsWithLead(8)) {
            if (!archonLocation.isWithinDistanceSquared(mp, 13)) {
                double factor = (rc.senseLead(mp) - 1) / Math.pow (location.distanceSquaredTo(mp) + 1, 1) ;
                if (factor > bestFactor) {
                    best = location.directionTo(mp);
                    bestFactor = factor;
                }
            }
        }
        return best;
    }
}
