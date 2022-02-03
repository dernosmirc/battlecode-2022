package gen8.miner;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import gen8.common.CommsHelper;
import gen8.common.Functions;
import gen8.common.GridInfo;
import gen8.common.SymmetryType;
import gen8.common.util.Vector;

import static gen8.RobotPlayer.*;

public class LeadMiningHelper {

    private static final int GRID_DIM = 3;

    private static final int SA_START = 36;
    private static final int SA_COUNT = 12;

    private static final double DISTANCE_FACTOR = -0.5;

    private static final int MAX_7BITS = 127;
    private static final double COMPRESSION = 0.005;

    private static final int LEAD_SYMMETRY_THRESHOLD = 25;
    private static final int ROUND_SYMMETRY_THRESHOLD = 500;

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

    private static int getInt16FromInfo(GridInfo info) {
        int v = Functions.setBits(0, 0, 8, get9BitsFromLocation(info.location));
        v = Functions.setBits(v, 9, 15, scaleLeadTo7Bits(info.count));
        return v;
    }

    private static GridInfo getInfoFromInt16(int bits) {
        return new GridInfo(
                scale7BitsToLead(Functions.getBits(bits, 9, 15)),
                getLocationFrom9Bits(Functions.getBits(bits, 0, 8))
        );
    }

    private static GridInfo[] getLeadOnGrid() throws GameActionException {
        GridInfo[] infos = new GridInfo[SA_COUNT];
        for (int i = SA_COUNT; --i >= 0;) {
            GridInfo info = getInfoFromInt16(rc.readSharedArray(SA_START + i));
            if (info.count != 0) {
                infos[i] = info;
            }
        }
        return infos;
    }

    private static GridInfo[] getAdjacentLeadInfos() throws GameActionException {
        GridInfo[] infos = new GridInfo[8];
        MapLocation[] locs = rc.senseNearbyLocationsWithLead(myType.visionRadiusSquared, 2);
        MapLocation rn = rc.getLocation();
        int x0 = 3*(rn.x/3) - 3, y0 = 3*(rn.y/3) - 3;
        int x0_1 = x0 + 1, y0_1 = y0 + 1;
        for (int i = 8; --i >= 0;) {
            switch (i) {
                case 1:
                case 3:
                case 4:
                case 5:
                case 7:
                infos[i] = new GridInfo(0, new MapLocation(3 * (i / 3) + x0_1, 3 * (i % 3) + y0_1));
            }
        }
        for (int i = locs.length; --i >= 0;) {
            MapLocation mp = locs[i];
            int lead = rc.senseLead(mp) - 1;
            int mpx = mp.x, mpy = mp.y, xa = (mpx-x0)/3, ya = (mpy-y0)/3;
            if (xa >= 0 && xa <= 2 && ya >= 0 && ya <= 2) {
                int ind = 3*xa + ya;
                switch (ind) {
                    case 1:
                    case 3:
                    case 4:
                    case 5:
                    case 7:
                        infos[ind].count += lead;
                }
            }
        }
        return infos;
    }

    public static GridInfo spotLeadOnGrid() throws GameActionException {
        MapLocation location = rc.getLocation();
        GridInfo lead = null;
        double maxFac = 0;
        GridInfo[] infos = getLeadOnGrid();
        for (int i = infos.length; --i >= 0;) {
            GridInfo o = infos[i];
            if (o != null && o.count > 0) {
                double fac = o.count * Math.pow(o.location.distanceSquaredTo(location), DISTANCE_FACTOR);
                if (fac > maxFac) {
                    lead = new GridInfo((int) fac, o.location);
                    maxFac = fac;
                }
            }
        }
        return lead;
    }

    private static int getLocationIndex(GridInfo[] infos, MapLocation location) {
        for (int i = SA_COUNT; --i >= 0;) {
            if (infos[i] != null && infos[i].location.equals(location)) {
                return i;
            }
        }
        return -1;
    }

    private static void addPositions(GridInfo[] infos, GridInfo info) throws GameActionException {
        int amount = info.count;
        MapLocation loc = info.location;
        Vector<GridInfo> arr = new Vector<>(4);
        arr.add(info);
        SymmetryType sym = SymmetryType.getMapSymmetry();
        if (sym != SymmetryType.NONE &&
                LEAD_SYMMETRY_THRESHOLD <= info.count
                && rc.getRoundNum() < ROUND_SYMMETRY_THRESHOLD
        ) {
            MapLocation l = SymmetryType.getSymmetricalLocation(loc, sym);
            if (!CommsHelper.isLocationInEnemyZone(l)) {
                arr.add(new GridInfo(amount, l));
            }
        }
        for (int j = arr.length; --j >= 0;) {
            GridInfo itj = arr.get(j);
            int index = getLocationIndex(infos, itj.location);
            if (index == -1) {
                int minAmount = Integer.MAX_VALUE;
                for (int i = SA_COUNT; --i >= 0;) {
                    GridInfo it = infos[i];
                    if (it == null) {
                        index = i;
                        minAmount = Integer.MAX_VALUE;
                        break;
                    } else if (it.count < minAmount) {
                        index = i;
                        minAmount = it.count;
                    }
                }
                if (minAmount == Integer.MAX_VALUE || minAmount < itj.count) {
                    infos[index] = itj;
                    rc.writeSharedArray(index + SA_START, getInt16FromInfo(itj));
                }
            }
        }
    }

    private static boolean shouldUpdateGrid() {
        int enemyCount = 0, minerCount = 0, friendCount = 0;
        RobotInfo[] ris = rc.senseNearbyRobots(myType.visionRadiusSquared);
        for (int i = ris.length; --i >= 0;) {
            RobotInfo ri = ris[i];
            if (ri.team == enemyTeam) {
                if (ri.type.canAttack()) {
                    enemyCount++;
                }
            } else {
                friendCount++;
                if (ri.type == RobotType.MINER) {
                    minerCount++;
                }
            }
            if (enemyCount >= 2 || minerCount >= 2 || friendCount >= 15) {
                return false;
            }
        }
        return true;
    }

    public static void updateLeadAmountInGridCell() throws GameActionException {
        GridInfo[] infos = getLeadOnGrid(), adj = getAdjacentLeadInfos();
        GridInfo bestCandidate = new GridInfo(0, rc.getLocation());
        for (int i = adj.length; --i >= 0;) {
            GridInfo mInfo = adj[i];
            if (mInfo == null) {
                continue;
            }
            int index = getLocationIndex(infos, mInfo.location);
            if (index != -1) {
                infos[index] = mInfo;
                rc.writeSharedArray(index + SA_START, getInt16FromInfo(mInfo));
            }
            if (mInfo.count > bestCandidate.count) {
                bestCandidate = mInfo;
            }
        }
        if (bestCandidate.count > 0 && shouldUpdateGrid()) {
            addPositions(infos, bestCandidate);
        }
    }

    private static int leaveMinLead(MapLocation loc) throws GameActionException {
        if (CommsHelper.isLocationInEnemyZone(loc)) {
            return 0;
        }
        return 1;
    }

    public static void mineLead() throws GameActionException {
        MapLocation[] mps = rc.senseNearbyLocationsWithLead(myType.actionRadiusSquared, 2);
        for (int i = mps.length; --i >= 0;) {
            MapLocation mp = mps[i];
            while (rc.isActionReady() && rc.senseLead(mp) > leaveMinLead(mp)) {
                rc.mineLead(mp);
            }
            if (!rc.isActionReady()) {
                break;
            }
        }
    }

    public static GridInfo spotLead() throws GameActionException {
        GridInfo lead = null;
        int bestFactor = 0;
        MapLocation[] mps = rc.senseNearbyLocationsWithLead(myType.visionRadiusSquared);
        for (int i = mps.length; --i >= 0;) {
            MapLocation mp = mps[i];
            int factor = rc.senseLead(mp) - 1;
            if (factor > bestFactor) {
                lead = new GridInfo(factor, mp);
                bestFactor = factor;
            }
        }
        return lead;
    }
}
