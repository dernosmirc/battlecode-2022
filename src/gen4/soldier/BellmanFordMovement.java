package gen4.soldier;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import gen4.common.CommsHelper;
import gen4.common.MovementHelper;

import java.util.Map;

import static gen4.RobotPlayer.*;
import static gen4.Soldier.*;

public class BellmanFordMovement {
    private static int INNER_DEFENSE_RADIUS = 4;
    private static int OUTER_DEFENSE_RADIUS = 8;
    private static int INNER_ATTACK_RADIUS = 8;
    private static int OUTER_ATTACK_RADIUS = 13;

    public static void move() throws GameActionException {
        MapLocation defenseLocation = DefenseHelper.getDefenseLocation();
        if (defenseLocation != null) {
            int distance = rc.getLocation().distanceSquaredTo(defenseLocation);
            if (distance < INNER_DEFENSE_RADIUS) {
                Direction dir = rc.getLocation().directionTo(defenseLocation).opposite();
                MovementHelper.greedyTryMove(dir);
            } else if (INNER_DEFENSE_RADIUS <= distance && distance <= OUTER_DEFENSE_RADIUS) {
                DefenseHelper.tryMoveRight(defenseLocation);
            } else if (distance <= RobotType.ARCHON.visionRadiusSquared) {
                Direction dir = rc.getLocation().directionTo(defenseLocation);
                if (MovementHelper.greedyTryMove(dir)) {
                    return;
                } else {
                    DefenseHelper.tryMoveRightAndBack(dir);
                }
            } else {
                Direction dir = rc.getLocation().directionTo(defenseLocation);
                MovementHelper.greedyTryMove(dir);
            }

            return;
        }

        Direction dir = AttackHelper.shouldMoveBack();
        if (dir != null) {
            MovementHelper.greedyTryMove(dir);
            return;
        }

        MapLocation enemyArchonLocation = CommsHelper.getEnemyArchonLocation();
        if (enemyArchonLocation != null) {
            if (sensedEnemyAttackRobot) {
                int distance = rc.getLocation().distanceSquaredTo(enemyArchonLocation);
                dir = rc.getLocation().directionTo(enemyArchonLocation);
                // if (distance < INNER_ATTACK_RADIUS) keep spawn blocking?
                if (distance <= OUTER_ATTACK_RADIUS) {
                    // stay here
                } else if (MovementHelper.greedyTryMove(dir)) {
                    return;
                } else {
                    DefenseHelper.tryMoveRightAndBack(dir);
                }
            } else {
                dir = rc.getLocation().directionTo(enemyArchonLocation);
                MovementHelper.greedyTryMove(dir);
            }

            return;
        }

        if (guessedEnemyArchonLocation == null) {
            // TODO: Go to nearest alive archon instead
            dir = rc.getLocation().directionTo(myArchonLocation);
            MovementHelper.greedyTryMove(dir);
            return;
        }

        dir = rc.getLocation().directionTo(guessedEnemyArchonLocation);
        MovementHelper.greedyTryMove(dir);
    }
}
