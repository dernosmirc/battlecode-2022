package gen3.helpers;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import gen3.common.MovementHelper;
import gen3.util.Functions;
import gen3.util.MetalInfo;
import gen3.util.SymmetryType;
import gen3.util.Vector;

import static gen3.RobotPlayer.*;

public class LeadMiningHelper {

    private static final int GRID_DIM = 3;

    private static final int SA_START = 36;
    private static final int SA_COUNT = 8;

    private static final double DISTANCE_FACTOR = -0.25;

    private static final int MAX_7BITS = 127;
    private static final double COMPRESSION = 0.001;
    
    private static final int LEAD_SYMMETRY_THRESHOLD = 75;

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
        for (int i = SA_COUNT-1; --i >= 0;) {
            MetalInfo info = getInfoFromInt16(rc.readSharedArray(SA_START + i));
            if (info.amount != 0) {
                infos[i] = info;
            }
        }
        return infos;
    }

    private static MetalInfo getLeadInfoCell(MapLocation center) throws GameActionException {
        if (!rc.canSenseLocation(center) || !rc.onTheMap(center)) {
            return null;
        }
        int count = 0, dim = GRID_DIM, dim1 = dim - 1,
                centerX = center.x - dim/2, centerY = center.y - dim/2;
        for (int x = dim1; --x >= 0;) {
            for (int y = dim1; --y >= 0;) {
                MapLocation loc = new MapLocation(centerX + x, centerY + y);
                if (rc.canSenseLocation(loc)) {
                    count += Math.max(rc.senseLead(loc) - 1, 0);
                }
            }
        }
        return new MetalInfo(count, center);
    }

    private static Vector<MapLocation> getAdjacentCells() {
        MapLocation now = rc.getLocation();
        int centerX = now.x - (now.x % GRID_DIM) + GRID_DIM / 2,
                centerY = now.y - (now.y % GRID_DIM) + GRID_DIM / 2;
        Vector<MapLocation> adj = new Vector<>(5);
        adj.add(new MapLocation(centerX, centerY));
        Direction direction = MovementHelper.getInstantaneousDirection();
        if (direction == Direction.EAST || direction == Direction.WEST) {
            adj.add(new MapLocation(centerX, centerY + GRID_DIM));
            adj.add(new MapLocation(centerX, centerY - GRID_DIM));
        } else if (direction == Direction.NORTH || direction == Direction.SOUTH) {
            adj.add(new MapLocation(centerX + GRID_DIM, centerY));
            adj.add(new MapLocation(centerX - GRID_DIM, centerY));
        } else if (direction == Direction.NORTHEAST || direction == Direction.SOUTHWEST) {
            adj.add(new MapLocation(centerX + GRID_DIM, centerY - GRID_DIM));
            adj.add(new MapLocation(centerX - GRID_DIM, centerY + GRID_DIM));
        } else if (direction == Direction.NORTHWEST || direction == Direction.SOUTHEAST) {
            adj.add(new MapLocation(centerX + GRID_DIM, centerY + GRID_DIM));
            adj.add(new MapLocation(centerX - GRID_DIM, centerY - GRID_DIM));
        } else {
            adj.add(new MapLocation(centerX, centerY + GRID_DIM));
            adj.add(new MapLocation(centerX, centerY - GRID_DIM));
            adj.add(new MapLocation(centerX + GRID_DIM, centerY));
            adj.add(new MapLocation(centerX - GRID_DIM, centerY));
        }
        return adj;
    }

    public static Direction spotLeadOnGrid() throws GameActionException {
        MapLocation location = rc.getLocation(), leadLoc = null;
        double maxFac = 0;
        MetalInfo[] infos = getLeadOnGrid();
        for (int i = infos.length-1; --i >= 0;) {
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

    private static int getLocationIndex(MetalInfo[] infos, MapLocation location) {
        for (int i = SA_COUNT-1; --i >= 0;) {
            if (infos[i] != null && infos[i].location.equals(location)) {
                return i;
            }
        }
        return -1;
    }

    private static void addSymmetricPositions(MetalInfo[] infos, MetalInfo info) throws GameActionException {
        int amount = info.amount;
        MapLocation loc = info.location;
        MetalInfo[] arr = {
                new MetalInfo(
                        amount,
                        SymmetryType.getSymmetricalLocation(loc, SymmetryType.HORIZONTAL)
                ),
                new MetalInfo(
                        amount,
                        SymmetryType.getSymmetricalLocation(loc, SymmetryType.VERTICAL)
                ),
                new MetalInfo(
                        amount,
                        SymmetryType.getSymmetricalLocation(loc, SymmetryType.ROTATIONAL)
                )
        };
        for (int j = 2; --j >= 0;) {
            MetalInfo itj = arr[j];
            int index = getLocationIndex(infos, itj.location);
            if (index == -1) {
                int minAmount = Integer.MAX_VALUE;
                for (int i = SA_COUNT - 1; --i >= 0;) {
                    MetalInfo it = infos[i];
                    if (it == null) {
                        index = i;
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

    public static void updateLeadAmountInGridCell() throws GameActionException {
        Vector<MapLocation> adj = getAdjacentCells();
        MetalInfo[] infos = getLeadOnGrid();
        MetalInfo bestCandidate = new MetalInfo(0, rc.getLocation());
        for (int i = adj.length-1; --i >= 0;) {
            MetalInfo mInfo = getLeadInfoCell(adj.get(i));
            if (mInfo != null) {
                int index = getLocationIndex(infos, mInfo.location);
                if (index != -1) {
                    infos[index] = mInfo;
                    rc.writeSharedArray(index + SA_START, getInt16FromInfo(mInfo));
                }
                if (mInfo.amount > bestCandidate.amount) {
                    bestCandidate = mInfo;
                }
            }
        }
        if (bestCandidate.amount > 0) {
            int minAmount = Integer.MAX_VALUE, index = 0;
            for (int i = SA_COUNT - 1; --i >= 0;) {
                MetalInfo it = infos[i];
                if (it == null) {
                    index = i;
                    break;
                } else if (it.amount < minAmount) {
                    index = i;
                    minAmount = it.amount;
                }
            }
            if (infos[index] == null || minAmount < bestCandidate.amount) {
                infos[index] = bestCandidate;
                rc.writeSharedArray(index + SA_START, getInt16FromInfo(bestCandidate));
            }
            if (bestCandidate.amount > LEAD_SYMMETRY_THRESHOLD) {
                addSymmetricPositions(infos, bestCandidate);
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
        int bestFactor = 0;
        for (MapLocation mp: rc.senseNearbyLocationsWithLead(myType.visionRadiusSquared)) {
            int factor = rc.senseLead(mp) - 1;
            if (factor > bestFactor) {
                best = location.directionTo(mp);
                bestFactor = factor;
            }
        }
        return best;
    }

    public static Direction getAntiEdgeDirection() {
        if (
                rc.getLocation().x < 2 &&
                        rc.getLocation().y <= rc.getMapHeight() - 3
        ) {
            return Direction.NORTH;
        } else if (
                rc.getLocation().y < 2 &&
                        rc.getLocation().x >= 2
        ) {
            return Direction.WEST;
        } else if (
                rc.getLocation().y > rc.getMapHeight() - 3 &&
                        rc.getLocation().x <= rc.getMapWidth() - 3
        ) {
            return Direction.EAST;
        } else if (
                rc.getLocation().x > rc.getMapWidth() - 3 &&
                        rc.getLocation().y >= 2
        ) {
            return Direction.SOUTH;
        } else {
            return null;
        }
    }
}
