package gen2.helpers;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import gen2.util.Functions;
import gen2.util.MetalInfo;

import static gen2.RobotPlayer.myType;
import static gen2.RobotPlayer.rc;

public class GoldMiningHelper {

    private static final int GRID_DIM = 4;
    private static final int GOLD_SCALE = 16;

    private static final int SA_START = 44;
    private static final int SA_COUNT = 2;

    private static MetalInfo[] getGoldOnGrid() throws GameActionException {
        MetalInfo[] infos = new MetalInfo[SA_COUNT];
        for (int i = SA_START; i < SA_START + SA_COUNT; i++) {
            int val = rc.readSharedArray(i);
            int amount = Functions.getBits(val, 8, 15) * GOLD_SCALE;
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
            int scaled = Math.min(mInfo.amount / GOLD_SCALE, 255);
            int val = Functions.setBits(0, 8, 15, scaled);
            val = Functions.setBits(val, 0, 3, mInfo.location.x / GRID_DIM);
            val = Functions.setBits(val, 4, 7, mInfo.location.y / GRID_DIM);
            rc.writeSharedArray(index + SA_START, val);
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

    public static void clearGoldAmountInGrid() throws GameActionException {
        for (int i = SA_START; i < SA_START + SA_COUNT; i++) {
            rc.writeSharedArray(i, 0);
        }
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
