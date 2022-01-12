package gen2.helpers;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import gen2.util.Functions;
import gen2.util.MetalInfo;

import static gen2.RobotPlayer.myType;
import static gen2.RobotPlayer.rc;

public class GoldMiningHelper {

    private static final int GRID_DIM = 3;

    private static final int SA_START = 44;
    private static final int SA_COUNT = 2;


    private static final int MAX_7BITS = 127;
    private static final double COMPRESSION = 0.01;
    private static int scaleGoldTo7Bits(int gold) {
        return (int) Math.floor(127 * (1 - Math.exp(-COMPRESSION * gold)));
    }
    private static int scale7BitsToGold(int bits) {
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
        for (int i = SA_START; i < SA_START + SA_COUNT; i++) {
            MetalInfo info = getInfoFromInt16(rc.readSharedArray(i));
            if (info.amount != 0) {
                infos[i - SA_START] = info;
            }
        }
        return infos;
    }

    public static void updateGoldAmountInGridCell() throws GameActionException {
        MapLocation goldLocation = getGoldLocation(), mloc = rc.getLocation();
        MetalInfo mInfo;
        if (goldLocation == null) {
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
        for (MapLocation mp: rc.senseNearbyLocationsWithGold(myType.visionRadiusSquared)) {
            int gold = rc.senseGold(mp);
            if (gold > bestGold) {
                best = mp;
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
