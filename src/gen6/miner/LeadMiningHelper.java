package gen6.miner;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import gen6.common.CommsHelper;
import gen6.common.Functions;
import gen6.common.SymmetryType;
import gen6.common.util.Vector;

import static gen6.RobotPlayer.*;

public class LeadMiningHelper {

    private static final int GRID_DIM = 3;

    private static final int SA_START = 36;
    private static final int SA_COUNT = 12;

    private static final double DISTANCE_FACTOR = -1.5;

    private static final int MAX_7BITS = 127;
    private static final double COMPRESSION = 0.001;

    private static final int LEAD_SYMMETRY_THRESHOLD = 75;
    private static final int ROUND_SYMMETRY_THRESHOLD = 350;

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
        for (int i = SA_COUNT; --i >= 0;) {
            MetalInfo info = getInfoFromInt16(rc.readSharedArray(SA_START + i));
            if (info.amount != 0) {
                infos[i] = info;
            }
        }
        return infos;
    }

    private static MetalInfo[] getAdjacentLeadInfos() throws GameActionException {
        MetalInfo[] infos = new MetalInfo[8];
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
                infos[i] = new MetalInfo(0, new MapLocation(3 * (i / 3) + x0_1, 3 * (i % 3) + y0_1));
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
                        infos[ind].amount += lead;
                }
            }
        }
        return infos;
    }

    public static MapLocation spotLeadOnGrid() throws GameActionException {
        MapLocation location = rc.getLocation(), leadLoc = null;
        double maxFac = 0;
        MetalInfo[] infos = getLeadOnGrid();
        for (int i = infos.length; --i >= 0;) {
            MetalInfo o = infos[i];
            if (o != null && o.amount > 0) {
                double fac = o.amount * Math.pow(o.location.distanceSquaredTo(location), DISTANCE_FACTOR);
                if (fac > maxFac) {
                    leadLoc = o.location;
                    maxFac = fac;
                }
            }
        }
        return leadLoc;
    }

    private static int getLocationIndex(MetalInfo[] infos, MapLocation location) {
        for (int i = SA_COUNT; --i >= 0;) {
            if (infos[i] != null && infos[i].location.equals(location)) {
                return i;
            }
        }
        return -1;
    }

    private static void addPositions(MetalInfo[] infos, MetalInfo info) throws GameActionException {
        int amount = info.amount;
        MapLocation loc = info.location;
        Vector<MetalInfo> arr = new Vector<>(4);
        arr.add(info);
        SymmetryType[] syms = SymmetryType.values();
        if (amount >= LEAD_SYMMETRY_THRESHOLD && rc.getRoundNum() <= ROUND_SYMMETRY_THRESHOLD) {
            for (int i = 2; --i >= 0;) {
                MapLocation l = SymmetryType.getSymmetricalLocation(loc, syms[i]);
                if (!CommsHelper.isLocationInEnemyZone(l)) {
                    arr.add(new MetalInfo(amount, l));
                }
            }
        }
        for (int j = arr.length; --j >= 0;) {
            MetalInfo itj = arr.get(j);
            int index = getLocationIndex(infos, itj.location);
            if (index == -1) {
                int minAmount = Integer.MAX_VALUE;
                for (int i = SA_COUNT; --i >= 0;) {
                    MetalInfo it = infos[i];
                    if (it == null) {
                        index = i;
                        minAmount = Integer.MAX_VALUE;
                        break;
                    } else if (it.amount < minAmount) {
                        index = i;
                        minAmount = it.amount;
                    }
                }
                if (minAmount == Integer.MAX_VALUE || minAmount < itj.amount) {
                    infos[index] = itj;
                    rc.writeSharedArray(index + SA_START, getInt16FromInfo(itj));
                }
            }
        }
    }

    private static boolean shouldUpdateGrid() {
        if (rc.senseNearbyRobots(myType.visionRadiusSquared).length >= 12) return false;
        int count = 0;
        RobotInfo[] ris = rc.senseNearbyRobots(myType.visionRadiusSquared, enemyTeam);
        for (int i = ris.length; --i >= 0;) {
            if (ris[i].type.canAttack()) {
                count++;
            }
            if (count >= 2) {
                return false;
            }
        }
        return true;
    }

    public static void updateLeadAmountInGridCell() throws GameActionException {
        if (!shouldUpdateGrid()) {
            return;
        }
        MetalInfo[] infos = getLeadOnGrid(), adj = getAdjacentLeadInfos();
        MetalInfo bestCandidate = new MetalInfo(0, rc.getLocation());
        for (int i = adj.length; --i >= 0;) {
            MetalInfo mInfo = adj[i];
            if (mInfo == null) {
                continue;
            }
            int index = getLocationIndex(infos, mInfo.location);
            if (index != -1) {
                infos[index] = mInfo;
                rc.writeSharedArray(index + SA_START, getInt16FromInfo(mInfo));
            }
            if (mInfo.amount > bestCandidate.amount) {
                bestCandidate = mInfo;
            }
        }
        if (bestCandidate.amount > 0) {
            addPositions(infos, bestCandidate);
        }
    }

    public static void mineLead() throws GameActionException {
        MapLocation[] mps = rc.senseNearbyLocationsWithLead(myType.actionRadiusSquared, 2);
        for (int i = mps.length; --i >= 0;) {
            MapLocation mp = mps[i];
            while (rc.isActionReady() && rc.senseLead(mp) > 1) {
                rc.mineLead(mp);
            }
            if (!rc.isActionReady()) {
                break;
            }
        }
    }

    public static MapLocation spotLead() throws GameActionException {
        MapLocation location = null;
        int bestFactor = 0;
        MapLocation[] mps = rc.senseNearbyLocationsWithLead(myType.visionRadiusSquared);
        for (int i = mps.length; --i >= 0;) {
            MapLocation mp = mps[i];
            int factor = rc.senseLead(mp) - 1;
            if (factor > bestFactor) {
                location = mp;
                bestFactor = factor;
            }
        }
        return location;
    }
}
