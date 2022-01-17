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
            Direction d = MovementHelper.whereGreedyTryMove(dir);
            if (d != null){
                MapLocation tentativeMoveLocation = rc.getLocation().add(d);
                MapLocation[] friendlyArchon = CommsHelper.getFriendlyArchonLocations();
                for (int i = maxArchonCount; --i >= 0;){
                    if (friendlyArchon[i] == null)  continue;
                    if (tentativeMoveLocation.distanceSquaredTo(friendlyArchon[i]) <= 34){
                        return;
                    }
                }
                MovementHelper.tryMove(d, true);
            }
            return;
        }

        MapLocation enemyArchonLocation = CommsHelper.getEnemyArchonLocation();
        if (enemyArchonLocation != null) {
            dir = rc.getLocation().directionTo(enemyArchonLocation);
            MovementHelper.greedyTryMove(dir);
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
