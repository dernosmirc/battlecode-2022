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

    private static final int THRESHOLD_WATCHTOWER_LEAD = 300;
    private static final int THRESHOLD_ARCHON_LEAD = 300;

    private static final int THRESHOLD_WATCHTOWER_GOLD = 80;
    private static final int THRESHOLD_ARCHON_GOLD = 80;

    public static Pair<MapLocation, Boolean> getLocationToMutate() throws GameActionException {
        if (CommsHelper.getArchonHpPriority(Builder.myArchonIndex) > 1) {
            return null;
        }

        RobotInfo[] infos = rc.senseNearbyRobots(myType.visionRadiusSquared, myTeam);
        for (RobotInfo ri: infos) {
            if (
                    ri.type == RobotType.ARCHON && (
                            ri.level == 1 && rc.getTeamLeadAmount(myTeam) >= THRESHOLD_ARCHON_LEAD ||
                            ri.level == 2 && rc.getTeamGoldAmount(myTeam) >= THRESHOLD_ARCHON_GOLD
                    )
            ) {
                return new Pair<>(
                        ri.location,
                        rc.getLocation().isWithinDistanceSquared(ri.location, myType.actionRadiusSquared)
                );
            }
        }
        for (RobotInfo ri: infos) {
            if (
                    ri.type == RobotType.ARCHON && (
                            ri.level == 1 && rc.getTeamLeadAmount(myTeam) >= THRESHOLD_WATCHTOWER_LEAD ||
                                    ri.level == 2 && rc.getTeamGoldAmount(myTeam) >= THRESHOLD_WATCHTOWER_GOLD
                    )
            ) {
                return new Pair<>(
                        ri.location,
                        rc.getLocation().isWithinDistanceSquared(ri.location, myType.actionRadiusSquared)
                );
            }
        }
        return null;
    }
}
