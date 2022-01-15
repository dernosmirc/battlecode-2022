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
            MovementHelper.tryMove(dir, false);
            // if (rc.canMove(dir)) {
            // 	rc.move(dir);
            // 	updateMovement(dir);
            // } else if (rc.canMove(dir.rotateLeft())) {
            // 	rc.move(dir.rotateLeft());
            // 	updateMovement(dir);
            // } else if (rc.canMove(dir.rotateRight())) {
            // 	rc.move(dir.rotateRight());
            // 	updateMovement(dir);
            // }

            return;
        }

        if (guessedEnemyArchonLocation != null) {
            int symmetryIndex = SymmetryType.getSymmetryType(myArchonLocation, guessedEnemyArchonLocation).ordinal();
            int value = rc.readSharedArray(5);
            int bit = 3 * myArchonIndex + symmetryIndex;
            if (getBits(value, bit, bit) == 1) {
                updateGuessedEnemyArchonLocation();
            }
        }

        // TODO: Use MovementHelper's pathing
        if (guessedEnemyArchonLocation == null) {
            // TODO: Go to nearest alive archon instead
            Direction dir = rc.getLocation().directionTo(myArchonLocation);
            MovementHelper.tryMove(dir, false);
            // if (rc.canMove(dir)) {
            // 	rc.move(dir);
            // 	updateMovement(dir);
            // } else if (rc.canMove(dir.rotateLeft())) {
            // 	rc.move(dir.rotateLeft());
            // 	updateMovement(dir);
            // } else if(rc.canMove(dir.rotateRight())) {
            // 	rc.move(dir.rotateRight());
            // 	updateMovement(dir);
            // }

            return;
        }

        Direction dir = rc.getLocation().directionTo(guessedEnemyArchonLocation);
        MovementHelper.tryMove(dir, false);
        // if (rc.canMove(dir)) {
        // 	rc.move(dir);
        // 	updateMovement(dir);
        // } else if (rc.canMove(dir.rotateLeft())) {
        // 	rc.move(dir.rotateLeft());
        // 	updateMovement(dir);
        // } else if(rc.canMove(dir.rotateRight())) {
        // 	rc.move(dir.rotateRight());
        // 	updateMovement(dir);
        // }
    }
}
