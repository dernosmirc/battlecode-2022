package gen4.miner;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import gen4.common.util.Functions;

import static gen4.RobotPlayer.myType;
import static gen4.RobotPlayer.rc;

public class GoldMiningHelper {

    private static final int GRID_DIM = 3;

    private static final int SA_START = 48;
    private static final int SA_COUNT = 2;

    private static final int MAX_7BITS = 127;
    private static final double COMPRESSION = 0.01;
    private static int scaleGoldTo7Bits(int gold) {
        if (gold == 0) return 0;
        return (int) Math.floor(127 * (1 - Math.exp(-COMPRESSION * gold)));
    }
    private static int scale7BitsToGold(int bits) {
        if (bits == 0) return 0;
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
        v = Functions.setBits(v, 9, 15, scaleGoldTo7Bits(info.amount));
        return v;
    }
    private static MetalInfo getInfoFromInt16(int bits) {
        return new MetalInfo(
                scale7BitsToGold(Functions.getBits(bits, 9, 15)),
                getLocationFrom9Bits(Functions.getBits(bits, 0, 8))
        );
    }


    private static MetalInfo[] getGoldOnGrid() throws GameActionException {
        MetalInfo[] infos = new MetalInfo[SA_COUNT];
        for (int i = SA_COUNT - 1; --i >= 0;) {
            MetalInfo info = getInfoFromInt16(rc.readSharedArray(SA_START + i));
            if (info.amount != 0) {
                infos[i] = info;
            }
        }
        return infos;
    }

    private static int getLocationIndex(MetalInfo[] infos, MapLocation location) {
        for (int i = SA_COUNT-1; --i >= 0;) {
            MetalInfo info = infos[i];
            if (info != null && info.location.equals(location)) {
                return i;
            }
        }
        return -1;
    }

    public static void updateGoldAmountInGridCell() throws GameActionException {
        MapLocation goldLocation = getGoldLocation();
        MetalInfo mInfo;
        if (goldLocation == null) {
            MapLocation mloc = rc.getLocation();
            mInfo = new MetalInfo(0,
                    new MapLocation(
                            mloc.x - (mloc.x % GRID_DIM) + GRID_DIM / 2,
                            mloc.y - (mloc.y % GRID_DIM) + GRID_DIM / 2
                    )
            );
        } else {
            mInfo = new MetalInfo(rc.senseGold(goldLocation),
                    new MapLocation(
                            goldLocation.x - (goldLocation.x % GRID_DIM) + GRID_DIM / 2,
                            goldLocation.y - (goldLocation.y % GRID_DIM) + GRID_DIM / 2
                    )
            );
        }

        MetalInfo[] infos = getGoldOnGrid();
        int index = getLocationIndex(infos, mInfo.location);
        if (index != -1) {
            rc.writeSharedArray(index + SA_START, getInt16FromInfo(mInfo));
        } else {
            int minAmount = Integer.MAX_VALUE;
            for (int i = SA_COUNT-1; --i >= 0;) {
                MetalInfo it = infos[i];
                if (it == null) {
                    index = i;
                    minAmount = Integer.MAX_VALUE;
                    break;
                } else if (it.amount < minAmount) {
                    minAmount = it.amount;
                    index = i;
                }
            }
            if (minAmount == Integer.MAX_VALUE || minAmount < mInfo.amount) {
                rc.writeSharedArray(index + SA_START, getInt16FromInfo(mInfo));
            }
        }
    }

    public static Direction spotGold() throws GameActionException {
        MapLocation best = null;
        int bestGold = 0;
        for (MapLocation mp: rc.senseNearbyLocationsWithGold(myType.visionRadiusSquared)) {
            int gold = rc.senseGold(mp);
            if (gold > bestGold) {
                best = mp;
                bestGold = gold;
            }
        }
        if (best == null) return null;
        else return rc.getLocation().directionTo(best);
    }

    public static MapLocation getGoldLocation() throws GameActionException {
        MapLocation best = null;
        int bestGold = 0;
        MapLocation[] mps = rc.senseNearbyLocationsWithGold(myType.visionRadiusSquared);
        for (int i = mps.length; --i >= 0;) {
            int gold = rc.senseGold(mps[i]);
            if (gold > bestGold) {
                best = mps[i];
                bestGold = gold;
            }
        }
        return best;
    }

    public static Direction spotGoldOnGrid() throws GameActionException {
        MapLocation location = rc.getLocation(), goldLoc = null;
        double maxFac = 0;
        MetalInfo[] infos = getGoldOnGrid();
        for (int i = 0; i < SA_COUNT; i++) {
            MetalInfo o = infos[i];
            if (o != null && o.amount > 0) {
                double fac = o.amount / Math.pow(o.location.distanceSquaredTo(location), 0.5);
                if (fac > maxFac) {
                    goldLoc = o.location;
                    maxFac = fac;
                }
            }
        }
        if (goldLoc == null) return null;
        else return location.directionTo(goldLoc);
    }

    public static void mineGold() throws GameActionException {
        for (MapLocation mp: rc.senseNearbyLocationsWithGold(myType.actionRadiusSquared)) {
            while (rc.isActionReady() && rc.canMineGold(mp)) {
                rc.mineGold(mp);
            }
            if (!rc.isActionReady()) {
                break;
            }
        }
    }
}
