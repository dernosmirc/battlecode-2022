package gen3.soldier;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import gen3.common.CommsHelper;
import gen3.common.MovementHelper;
import gen3.util.SymmetryType;

import static gen3.RobotPlayer.rc;
import static gen3.Soldier.*;
import static gen3.util.Functions.getBits;

public class BellmanFordMovement {

    public static void move() throws GameActionException {
        MapLocation enemyArchonLocation = CommsHelper.getEnemyArchonLocation();
        if (enemyArchonLocation != null) {
            Direction dir = rc.getLocation().directionTo(enemyArchonLocation);
            MovementHelper.greedyTryMove(dir);
            return;
        }

        if (guessedEnemyArchonLocation == null) {
            // TODO: Go to nearest alive archon instead
            Direction dir = rc.getLocation().directionTo(myArchonLocation);
            MovementHelper.greedyTryMove(dir);
            return;
        }

        Direction dir = rc.getLocation().directionTo(guessedEnemyArchonLocation);
        MovementHelper.greedyTryMove(dir);
    }
}
