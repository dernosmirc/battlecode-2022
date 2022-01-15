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

//			Direction dir = rc.getLocation().directionTo(enemyArchonLocation);
//			if (rc.canMove(dir)) {
//				rc.move(dir);
//				updateMovement(dir);
//			} else if (rc.canMove(dir.rotateLeft())) {
//				rc.move(dir.rotateLeft());
//				updateMovement(dir);
//			} else if (rc.canMove(dir.rotateRight())) {
//				rc.move(dir.rotateRight());
//				updateMovement(dir);
//			}

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
            BugPathingHelper.moveTowards(myArchonLocation);

//			Direction dir = rc.getLocation().directionTo(myArchonLocation);
//			if (rc.canMove(dir)) {
//				rc.move(dir);
//				updateMovement(dir);
//			} else if (rc.canMove(dir.rotateLeft())) {
//				rc.move(dir.rotateLeft());
//				updateMovement(dir);
//			} else if(rc.canMove(dir.rotateRight())) {
//				rc.move(dir.rotateRight());
//				updateMovement(dir);
//			}
            return;
        }

        BugPathingHelper.moveTowards(guessedEnemyArchonLocation);
//		Direction dir = rc.getLocation().directionTo(guessedEnemyArchonLocation);
//		if (rc.canMove(dir)) {
//			rc.move(dir);
//			updateMovement(dir);
//		} else if (rc.canMove(dir.rotateLeft())) {
//			rc.move(dir.rotateLeft());
//			updateMovement(dir);
//		} else if(rc.canMove(dir.rotateRight())) {
//			rc.move(dir.rotateRight());
//			updateMovement(dir);
//		}
    }
}
