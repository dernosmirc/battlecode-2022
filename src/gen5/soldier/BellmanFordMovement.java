package gen5.soldier;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import battlecode.common.RobotInfo;
import gen5.common.CommsHelper;
import gen5.common.MovementHelper;

import static gen5.RobotPlayer.*;
import static gen5.Soldier.*;
import static gen5.common.Functions.getDistance;

public class BellmanFordMovement {
    private static int INNER_DEFENSE_RADIUS = 4;
    private static int OUTER_DEFENSE_RADIUS = 8;
    private static int INNER_ATTACK_RADIUS = 8;
    private static int OUTER_ATTACK_RADIUS = 13;

    private static int HEAL_THRESHOLD = 20;

    public static void move() throws GameActionException {
        MapLocation defenseLocation = DefenseHelper.getDefenseLocation();

        RobotInfo[] robots = rc.senseNearbyRobots(myType.actionRadiusSquared, myTeam);
        RobotInfo archon = null;
        for (int i = robots.length; --i >= 0; ) {
            RobotInfo robot = robots[i];
            if (robot.type == RobotType.ARCHON) {
                archon = robot;
                break;
            }
        }

        if (defenseLocation == null && archon != null && rc.getHealth() < 45) {
            defenseLocation = archon.location;
        }

        if (defenseLocation == null && rc.getHealth() <= HEAL_THRESHOLD) {
            MapLocation[] archons = CommsHelper.getFriendlyArchonLocations();
            int minDistance = rc.getMapWidth() * rc.getMapHeight();
            int archonIndex = 0;
            for (int i = maxArchonCount; --i >= 0; ) {
                if (archons[i] != null) {
                    int distance = getDistance(archons[i], rc.getLocation());
                    if (distance < minDistance) {
                        minDistance = distance;
                        archonIndex = i;
                    }
                }
            }

            defenseLocation = new MapLocation(archons[archonIndex].x, archons[archonIndex].y);
        }

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