package gen5.soldier;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import battlecode.common.RobotInfo;
import gen5.common.CommsHelper;
import gen5.common.MovementHelper;
import gen5.soldier.TailHelper;

import java.util.Random;

import static gen5.RobotPlayer.*;
import static gen5.Soldier.*;
import static gen5.common.Functions.getDistance;

public strictfp class BellmanFordMovement {
    private static final int INNER_DEFENSE_RADIUS = 4;
    private static final int OUTER_DEFENSE_RADIUS = 8;
    private static final int INNER_ATTACK_RADIUS = 8;
    private static final int OUTER_ATTACK_RADIUS = 13;

    public static final int HEAL_THRESHOLD = 20; // 21
    private static final int FULL_HEAL_THRESHOLD = 45;

    private static final int INNER_CONCAVE_RADIUS = 8;
    private static final int OUTER_CONCAVE_RADIUS = 13;

    private static int slideDirection = 0;

    public static void move() throws GameActionException {
        TailHelper.updateTarget();
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

            if (archonLocation == null) {
                System.out.println("No friendly archons");
                return;
            }

            circleAround(archonLocation);
            return;
        }

        Direction dir = AttackHelper.shouldMoveBack();
        MapLocation enemyArchonLocation = CommsHelper.getEnemyArchonLocation();
        if (dir != null) {
            // if (enemyArchonLocation == null && guessedEnemyArchonLocation == null) {
            //     TailHelper.updateTarget();
            // }

            MovementHelper.greedyTryMove(dir);
            return;
        }

        if (tryConcave()) {
            return;
        }

        if (TailHelper.foundTarget() && TailHelper.getTargetPriority() >= 5) {
            MapLocation target = TailHelper.getTargetLocation();
            // dir = rc.getLocation().directionTo(target);
            // MovementHelper.greedyTryMove(dir);
            // MovementHelper.moveBellmanFord(target);
            moveTowards(target);
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
                    // MovementHelper.moveBellmanFord(enemyArchonLocation);
                    moveTowards(enemyArchonLocation);
                }

                // else if (MovementHelper.greedyTryMove(dir)) {
                //     return;
                // } else {
                //     DefenseHelper.tryMoveRightAndBack(dir);
                // }
            } else {
                // dir = rc.getLocation().directionTo(enemyArchonLocation);
                // MovementHelper.greedyTryMove(dir);
                // MovementHelper.moveBellmanFord(enemyArchonLocation);
                moveTowards(enemyArchonLocation);
            }

            return;
        }

        if (guessedEnemyArchonLocation == null) {
            // TailHelper.updateTarget();
            if (TailHelper.foundTarget()) {
                MapLocation target = TailHelper.getTargetLocation();
                // dir = rc.getLocation().directionTo(target);
                // MovementHelper.greedyTryMove(dir);
                // MovementHelper.moveBellmanFord(target);
                moveTowards(target);
            } else {
                dir = directions[rng.nextInt(directions.length)];
                MovementHelper.greedyTryMove(dir);
            }

            return;
        }

        // dir = rc.getLocation().directionTo(guessedEnemyArchonLocation);
        // MovementHelper.greedyTryMove(dir);
        // MovementHelper.moveBellmanFord(guessedEnemyArchonLocation);
        moveTowards(guessedEnemyArchonLocation);
    }

    private static boolean tryConcave() throws GameActionException {
        String s = "here ";
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(myType.visionRadiusSquared, enemyTeam);
        RobotInfo nearestRobot = null;
        int minDistance = 100000;
        for (int i = enemyRobots.length; --i >= 0; ) {
            RobotInfo robot = enemyRobots[i];
            switch (robot.type) {
                case SOLDIER:
                case SAGE:
                case WATCHTOWER:
                case ARCHON:
                    int distance = rc.getLocation().distanceSquaredTo(robot.location);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestRobot = robot;
                    }
                    break;
            }
        }

        if (nearestRobot == null) {
            return false;
        }

        if (minDistance < INNER_CONCAVE_RADIUS) {
            Direction dir = MovementHelper.whereGreedyTryMove(
                                rc.getLocation().directionTo(nearestRobot.location).opposite());
            if (dir == null) {
                return true;
            } else {
                MapLocation tentativeMoveLocation = rc.getLocation().add(dir);
                int rubbleHere = rc.senseRubble(rc.getLocation());
                int rubbleThere = rc.senseRubble(tentativeMoveLocation);
                if (rubbleThere < rubbleHere) {
                    MovementHelper.tryMove(dir, true);
                }
            }

            return true;
        }

        if (minDistance <= OUTER_CONCAVE_RADIUS) {
            Direction dir = getBestLateralDirection(rc.getLocation().directionTo(nearestRobot.location), nearestRobot);
            rc.setIndicatorString(s + dir.ordinal());
            if (dir != Direction.CENTER) {
                MovementHelper.tryMove(dir, true);
            }

            return true;
        }

        if (MovementHelper.greedyTryMove(rc.getLocation().directionTo(nearestRobot.location))) {
            return true;
        }

        if (slideDirection == 0) {
            slideDirection = rng.nextBoolean() ? 1 : -1;
        }
        Direction dir = rc.getLocation().directionTo(nearestRobot.location);
        switch (slideDirection) {
            case 1:
                dir = dir.rotateRight().rotateRight();
                break;
            case -1:
                dir = dir.rotateLeft().rotateLeft();
                break;
        }

        MovementHelper.greedyTryMove(dir);
        return true;
    }

    private static Direction getBestLateralDirection(Direction dir, RobotInfo enemyRobot) throws GameActionException {
        Direction left = dir.rotateLeft().rotateLeft();
        Direction right = dir.rotateRight().rotateRight();
        MapLocation leftLocation = rc.getLocation().add(left);
        MapLocation rightLocation = rc.getLocation().add(right);

        Direction best = Direction.CENTER;
        int minRubble = rc.senseRubble(rc.getLocation());
        if (leftLocation.isWithinDistanceSquared(enemyRobot.location, myType.actionRadiusSquared) && rc.canMove(left)) {
            int rubble = rc.senseRubble(leftLocation);
            if (rubble < minRubble) {
                minRubble = rubble;
                best = left;
            }
        }
        if (rightLocation.isWithinDistanceSquared(enemyRobot.location, myType.actionRadiusSquared) && rc.canMove(right)) {
            int rubble = rc.senseRubble(rightLocation);
            if (rubble < minRubble) {
                minRubble = rubble;
                best = right;
            }
        }

        return best;
    }

    private static void circleAround(MapLocation location) throws GameActionException {
        int distance = rc.getLocation().distanceSquaredTo(location);
        if (distance < INNER_DEFENSE_RADIUS) {
            Direction dir = rc.getLocation().directionTo(location).opposite();
            MovementHelper.greedyTryMove(dir);
        } else if (INNER_DEFENSE_RADIUS <= distance && distance <= OUTER_DEFENSE_RADIUS) {
            DefenseHelper.tryMoveRight(location);
        } else if (distance <= RobotType.ARCHON.visionRadiusSquared) {
            Direction dir = rc.getLocation().directionTo(location);
            if (MovementHelper.greedyTryMove(dir)) {
                return;
            } else {
                DefenseHelper.tryMoveRightAndBack(dir);
            }
        } else {
            // Direction dir = rc.getLocation().directionTo(location);
            // MovementHelper.greedyTryMove(dir);
            // MovementHelper.moveBellmanFord(location);
            moveTowards(location);
        }
    }

    private static void moveTowards(MapLocation location) throws GameActionException {
        if (MovementHelper.moveBellmanFord(location))
            return;

        Direction dir = rc.getLocation().directionTo(location);
        MovementHelper.greedyTryMove(dir);
    }
}
