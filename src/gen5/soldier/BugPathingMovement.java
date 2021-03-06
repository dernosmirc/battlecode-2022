package gen5.soldier;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import battlecode.common.RobotInfo;
import gen5.common.BugPathingHelper;
import gen5.common.CommsHelper;
import gen5.common.MovementHelper;

import static gen5.RobotPlayer.*;
import static gen5.Soldier.*;
import static gen5.common.Functions.getDistance;

public class BugPathingMovement {
    private static int INNER_DEFENSE_RADIUS = 4;
    private static int OUTER_DEFENSE_RADIUS = 8;
    private static int INNER_ATTACK_RADIUS = 8;
    private static int OUTER_ATTACK_RADIUS = 13;

    private static final int HEAL_THRESHOLD = 20; // 21
    private static final int FULL_HEAL_THRESHOLD = 45;

    public static void move() throws GameActionException {
        MapLocation defenseLocation = DefenseHelper.getDefenseLocation();
        if (defenseLocation != null) {
            circleAround(defenseLocation);
            return;
        }

        RobotInfo[] robots = rc.senseNearbyRobots(myType.actionRadiusSquared, myTeam); // try vision radius?
        RobotInfo archon = null;
        for (int i = robots.length; --i >= 0; ) {
            RobotInfo robot = robots[i];
            if (robot.type == RobotType.ARCHON) {
                archon = robot;
                break;
            }
        }

        if (archon != null && rc.getHealth() < FULL_HEAL_THRESHOLD) {
            circleAround(archon.location);
            return;
        }

        if (rc.getHealth() <= HEAL_THRESHOLD) {
            MapLocation[] archons = CommsHelper.getFriendlyArchonLocations();
            int minDistance = rc.getMapWidth() * rc.getMapHeight();
            MapLocation archonLocation = null;
            for (int i = maxArchonCount; --i >= 0; ) {
                if (archons[i] != null) {
                    // if (i == myArchonIndex) {
                    //     archonLocation = archons[i];
                    //     break;
                    // }

                    int distance = getDistance(archons[i], rc.getLocation());
                    if (distance < minDistance) {
                        minDistance = distance;
                        archonLocation = archons[i];
                    }
                }
            }

            circleAround(archonLocation);
            return;
        }

        Direction dir = AttackHelper.shouldMoveBack();
        MapLocation enemyArchonLocation = CommsHelper.getEnemyArchonLocation();
        if (dir != null) {
            if (enemyArchonLocation == null && guessedEnemyArchonLocation == null) {
                TailHelper.updateTarget();
            }
            
            BugPathingHelper.setDefault();
            MovementHelper.greedyTryMove(dir);
            return;
        }

        if (enemyArchonLocation != null) {
            if (sensedEnemyAttackRobot) {
                int distance = rc.getLocation().distanceSquaredTo(enemyArchonLocation);
                dir = rc.getLocation().directionTo(enemyArchonLocation);
                // if (distance < INNER_ATTACK_RADIUS) keep spawn blocking?
                if (distance <= OUTER_ATTACK_RADIUS) {
                    // stay here
                } else {
                    BugPathingHelper.moveTowards(enemyArchonLocation);
                }
            } else {
                BugPathingHelper.moveTowards(enemyArchonLocation);
            }

            return;
        }

        if (guessedEnemyArchonLocation == null) {
            TailHelper.updateTarget();
            if (TailHelper.foundTarget()) {
                MapLocation target = TailHelper.getTargetLocation();
                BugPathingHelper.moveTowards(target);
            } else {
                dir = directions[rng.nextInt(directions.length)];
                MovementHelper.greedyTryMove(dir);
            }

            return;
        }

        BugPathingHelper.moveTowards(guessedEnemyArchonLocation);
    }

    private static void circleAround(MapLocation location) throws GameActionException {
        int distance = rc.getLocation().distanceSquaredTo(location);
        if (distance < INNER_DEFENSE_RADIUS) {
            BugPathingHelper.setDefault();
            Direction dir = rc.getLocation().directionTo(location).opposite();
            MovementHelper.greedyTryMove(dir);
        } else if (INNER_DEFENSE_RADIUS <= distance && distance <= OUTER_DEFENSE_RADIUS) {
            BugPathingHelper.setDefault();
            DefenseHelper.tryMoveRight(location);
        } else if (distance <= RobotType.ARCHON.visionRadiusSquared) {
            BugPathingHelper.setDefault();
            Direction dir = rc.getLocation().directionTo(location);
            if (MovementHelper.greedyTryMove(dir)) {
                return;
            } else {
                DefenseHelper.tryMoveRightAndBack(dir);
            }
        } else {
            BugPathingHelper.moveTowards(location);
        }
    }
}
