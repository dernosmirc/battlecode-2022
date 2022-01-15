package gen3.soldier;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import gen3.common.BugPathingHelper;
import gen3.common.CommsHelper;
import gen3.util.SymmetryType;

import static gen3.RobotPlayer.rc;
import static gen3.Soldier.*;
import static gen3.util.Functions.getBits;

public class BugPathingMovement {

    public static void move() throws GameActionException {
        MapLocation enemyArchonLocation = CommsHelper.getEnemyArchonLocation();
        if (enemyArchonLocation != null) {
            BugPathingHelper.moveTowards(enemyArchonLocation);
            return;
        }

        if (guessedEnemyArchonLocation == null) {
            // TODO: Go to nearest alive archon instead
            BugPathingHelper.moveTowards(myArchonLocation);
            return;
        }

        BugPathingHelper.moveTowards(guessedEnemyArchonLocation);
    }
}
