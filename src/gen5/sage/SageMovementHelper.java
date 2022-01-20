package gen5.sage;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import gen5.common.CommsHelper;
import gen5.common.MovementHelper;
import gen5.soldier.AttackHelper;
import gen5.soldier.DefenseHelper;

import static gen5.RobotPlayer.*;
import static gen5.Sage.*;

public class SageMovementHelper {

    private static final int HP_THRESHOLD = 40;
    private static final int TURNS_THRESHOLD = 25;

    private static final int INNER_DEFENSE_RADIUS = 4;
    private static final int OUTER_DEFENSE_RADIUS = 20;

    public static void defenseRevolution(MapLocation defenseLocation) throws GameActionException {
        int distance = rc.getLocation().distanceSquaredTo(defenseLocation);
        if (distance < INNER_DEFENSE_RADIUS) {
            Direction dir = rc.getLocation().directionTo(defenseLocation).opposite();
            MovementHelper.greedyTryMove(dir);
        } else if (distance <= OUTER_DEFENSE_RADIUS) {
            DefenseHelper.tryMoveRight(defenseLocation);
        } else if (distance <= RobotType.ARCHON.visionRadiusSquared) {
            Direction dir = rc.getLocation().directionTo(defenseLocation);
            if (!MovementHelper.greedyTryMove(dir)) {
                DefenseHelper.tryMoveRightAndBack(dir);
            }
        } else {
            MovementHelper.moveBellmanFord(defenseLocation);
        }
    }

    public static void move() throws GameActionException {
        if (rc.getHealth() < HP_THRESHOLD || rc.getActionCooldownTurns()/10 >= TURNS_THRESHOLD) {
            defenseRevolution(myArchonLocation);
            return;
        }

        MapLocation attack = SageAttackHelper.getArchonAttackLocation();
        if (attack != null) {
            MovementHelper.moveBellmanFord(attack);
            return;
        }

        MapLocation defenseLocation = DefenseHelper.getDefenseLocation();
        if (defenseLocation != null) {
            defenseRevolution(defenseLocation);
            return;
        }

        Direction dir = AttackHelper.shouldMoveBack();
        if (dir != null) {
            Direction d = MovementHelper.whereGreedyTryMove(dir);
            if (d != null) {
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
            MovementHelper.moveBellmanFord(enemyArchonLocation);
            return;
        }

        defenseRevolution(myArchonLocation);
    }
}
