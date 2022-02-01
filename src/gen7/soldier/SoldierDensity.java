package gen7.soldier;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import gen7.common.Functions;
import gen7.common.GridInfo;

import static gen7.RobotPlayer.*;

public class SoldierDensity {

    private static final int SA_START = 18;
    private static final int SA_COUNT = 3;

    private static final int GRID_DIM = 3;
    private static final int CLUSTER_RADIUS = 25;

    private static final int SOLDIER_CLUSTER_THRESHOLD = 35;

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

    private static GridInfo getInfoFromInt16(int bits) {
        return new GridInfo(
                Functions.getBits(bits, 9, 15)*4,
                getLocationFrom9Bits(Functions.getBits(bits, 0, 8))
        );
    }

    private static int getInt16FromInfo(GridInfo info) {
        int v = Functions.setBits(0, 0, 8, get9BitsFromLocation(info.location));
        v = Functions.setBits(v, 9, 15, info.count/4);
        return v;
    }

    private static int getNearbySoldierCount() {
        int count = myType == RobotType.SAGE ? 2 : 1;
        RobotInfo[] ris = rc.senseNearbyRobots(myType.visionRadiusSquared, myTeam);
        for (int i = ris.length; --i >= 0;) {
            switch (ris[i].type) {
                case SAGE:
                    count++;
                case SOLDIER:
                    count++;
            }
        }
        return count;
    }

    public static GridInfo[] getTop() throws GameActionException {
        GridInfo[] infos = new GridInfo[SA_COUNT];
        for (int i = SA_COUNT; --i >= 0;) {
            GridInfo info = getInfoFromInt16(rc.readSharedArray(SA_START + i));
            if (info.count != 0) {
                infos[i] = info;
            }
        }
        return infos;
    }

    public static void reset() throws GameActionException {
        for (int i = SA_COUNT; --i >= 0;) {
            rc.writeSharedArray(SA_START + i, 0);
        }
    }

    public static void update() throws GameActionException {
        int count = getNearbySoldierCount();
        if (count < SOLDIER_CLUSTER_THRESHOLD) return;
        GridInfo[] infos = getTop();
        GridInfo my = new GridInfo(count, rc.getLocation());
        int ind = -1, minCount = 10000;
        for (int i = SA_COUNT; --i >= 0; ) {
            GridInfo gi = infos[i];
            if (gi != null) {
                if (gi.location.isWithinDistanceSquared(my.location, CLUSTER_RADIUS)) {
                    ind = i;
                    minCount = gi.count;
                    break;
                }
                if (gi.count < minCount) {
                    minCount = gi.count;
                    ind = i;
                }
            } else {
                ind = i;
                minCount = 0;
            }
        }
        if (minCount < my.count) {
            rc.writeSharedArray(SA_START + ind, getInt16FromInfo(my));
        }
    }
}
