package gen3.helpers;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import gen3.util.Pair;

import static gen3.RobotPlayer.*;

public class MutationHelper {

    private static final int THRESHOLD_WATCHTOWER_LEAD = 200;
    private static final int THRESHOLD_ARCHON_LEAD = 400;

    public static Pair<MapLocation, Boolean> getLocationToMutate() {
        if (rc.getTeamLeadAmount(myTeam) >= THRESHOLD_WATCHTOWER_LEAD) {
            RobotInfo[] infos = rc.senseNearbyRobots(myType.visionRadiusSquared, myTeam);
            for (RobotInfo ri: infos) {
                if (
                        ri.type == RobotType.ARCHON && ri.level == 1 &&
                                rc.getTeamLeadAmount(myTeam) >= THRESHOLD_ARCHON_LEAD
                ) {
                    return new Pair<>(
                            ri.location,
                            rc.getLocation().isWithinDistanceSquared(ri.location, myType.actionRadiusSquared)
                    );
                }
            }
            for (RobotInfo ri: infos) {
                if (ri.type == RobotType.WATCHTOWER && ri.level == 1) {
                    return new Pair<>(
                            ri.location,
                            rc.getLocation().isWithinDistanceSquared(ri.location, myType.actionRadiusSquared)
                    );
                }
            }
        }
        return null;
    }
}
