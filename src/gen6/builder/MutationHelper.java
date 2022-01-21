package gen6.builder;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import gen6.Builder;
import gen6.common.CommsHelper;
import gen6.common.util.Pair;

import static gen6.RobotPlayer.*;

public class MutationHelper {

    private static final int THRESHOLD_LEAD = 300;

    private static final int THRESHOLD_GOLD = 80;

    // ARCHON
    // LABORATORY
    // WATCHTOWER
    // MINER
    // BUILDER
    // SOLDIER
    // SAGE
    public static final int[] priority = {3, 2, 1, 0, 0, 0, 0};

    public static Pair<MapLocation, Boolean> getLocationToMutate() throws GameActionException {
        if (CommsHelper.getArchonHpPriority(Builder.myArchonIndex) > 1) {
            return null;
        }

        RobotInfo[] infos = rc.senseNearbyRobots(myType.visionRadiusSquared, myTeam);
        MapLocation toMutate = null;
        int maxPriority = -1;
        for (int i = infos.length; --i >= 0; ) {
            RobotInfo ri = infos[i];
            if (
                    ri.level == 1 && rc.getTeamLeadAmount(myTeam) >= THRESHOLD_LEAD ||
                    ri.level == 2 && rc.getTeamGoldAmount(myTeam) >= THRESHOLD_GOLD
            ) {
                if (priority[ri.type.ordinal()] > maxPriority) {
                    maxPriority = priority[ri.type.ordinal()];
                    toMutate = ri.location;
                }
            }
        }

        if (toMutate == null) {
            return null;
        }

        return new Pair<>(
                toMutate,
                rc.getLocation().isWithinDistanceSquared(toMutate, myType.actionRadiusSquared)
        );
    }
}
