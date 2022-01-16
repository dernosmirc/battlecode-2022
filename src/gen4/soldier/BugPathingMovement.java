package gen4.soldier;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import gen4.helpers.DefenseHelper;
import gen4.helpers.AttackHelper;
import gen4.common.BugPathingHelper;
import gen4.common.CommsHelper;
import gen4.common.MovementHelper;

import static gen4.RobotPlayer.*;
import static gen4.Soldier.*;

public class BugPathingMovement {
    private static int INNER_DEFENSE_RADIUS = 4;
    private static int OUTER_DEFENSE_RADIUS = 8;

    public static void move() throws GameActionException {
        MapLocation defenseLocation = DefenseHelper.getDefenseLocation();
        if (defenseLocation != null) {
            int distance = rc.getLocation().distanceSquaredTo(defenseLocation);
            if (distance < INNER_DEFENSE_RADIUS) {
                BugPathingHelper.setDefault();
                Direction dir = rc.getLocation().directionTo(defenseLocation).opposite();
                MovementHelper.greedyTryMove(dir);
            } else if (INNER_DEFENSE_RADIUS <= distance && distance <= OUTER_DEFENSE_RADIUS) {
                BugPathingHelper.setDefault();
                DefenseHelper.tryMoveRight(defenseLocation);
            } else if (distance <= RobotType.ARCHON.visionRadiusSquared) {
                BugPathingHelper.setDefault();
                Direction dir = rc.getLocation().directionTo(defenseLocation);
                if (MovementHelper.greedyTryMove(dir)) {
                    return;
                } else {
                    DefenseHelper.tryMoveRightAndBack(dir);
                }
            } else {
                BugPathingHelper.moveTowards(defenseLocation);
            }

            return;
        }

        Direction dir = AttackHelper.shouldMoveBack();
        if (dir != null) {
            BugPathingHelper.setDefault();
            MovementHelper.greedyTryMove(dir);
            return;
        }

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
