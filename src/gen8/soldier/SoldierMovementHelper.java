package gen8.soldier;

import battlecode.common.*;
import battlecode.common.RobotMode;
import gen8.common.CommsHelper;
import gen8.common.MovementHelper;

import static gen8.RobotPlayer.*;
import static gen8.Soldier.*;
import static gen8.common.Functions.getDistance;

public strictfp class SoldierMovementHelper {
    private static final int INNER_DEFENSE_RADIUS = 4;
    private static final int OUTER_DEFENSE_RADIUS = 8;
    private static final int INNER_ATTACK_RADIUS = 8;
    private static final int OUTER_ATTACK_RADIUS = 13;

    public static final int HEAL_THRESHOLD = 15; // 21
    private static final int FULL_HEAL_THRESHOLD = 45;

    private static final int INNER_CONCAVE_RADIUS = 8;
    private static final int OUTER_CONCAVE_RADIUS = 13;

    private static int slideDirection = 0;

    public static void move() throws GameActionException {
        TailHelper.updateTarget();

        MapLocation defenseLocation = DefenseHelper.getDefenseLocation();
        if (defenseLocation != null) {
            if (rc.getLocation().isWithinDistanceSquared(defenseLocation, myType.visionRadiusSquared)) {
                if (tryConcave()) {
                    return;
                }
            }

            circleAround(defenseLocation);
            return;
        }

        if (rc.getHealth() <= HEAL_THRESHOLD) {
            MapLocation[] archons = CommsHelper.getFriendlyArchonLocations();
            int minDistance = rc.getMapWidth() * rc.getMapHeight();
            MapLocation archonLocation = null;
            boolean foundTurret = false;
            for (int i = maxArchonCount; --i >= 0; ) {
                if (archons[i] != null) {
                    int distance = getDistance(archons[i], rc.getLocation());
                    if (!CommsHelper.isArchonPortable(i)) {
                        if (!foundTurret) {
                            foundTurret = true;
                            minDistance = distance;
                            archonLocation = archons[i];
                        } else if (distance < minDistance) {
                            minDistance = distance;
                            archonLocation = archons[i];
                        }
                    } else if (!foundTurret && distance < minDistance) {
                        minDistance = distance;
                        archonLocation = archons[i];
                    }
                }
            }

            if (archonLocation == null) {
                return;
            }

            if (rc.getLocation().isWithinDistanceSquared(archonLocation, myType.visionRadiusSquared)) {
                if (tryConcave()) {
                    return;
                }
            }

            circleAround(archonLocation);
            return;
        }

        Direction dir = AttackHelper.shouldMoveBack();
        if (dir != null) {
            MovementHelper.greedyTryMove(dir);
            return;
        }

        if (tryConcave()) {
            return;
        }

        // RobotInfo[] robots = rc.senseNearbyRobots(myType.actionRadiusSquared, myTeam); // try vision radius?
        RobotInfo[] robots = allRobots;
        RobotInfo archon = null;
        for (int i = robots.length; --i >= 0; ) {
            RobotInfo robot = robots[i];
            if (robot.type == RobotType.ARCHON && robot.team == myTeam && robot.mode == RobotMode.TURRET
                && rc.getLocation().isWithinDistanceSquared(robot.location, myType.actionRadiusSquared)) {
                archon = robot;
                break;
            }
        }

        if (archon != null && rc.getHealth() < FULL_HEAL_THRESHOLD) {
            circleAround(archon.location);
            return;
        }

        // TODO: try 0 priority
        if (TailHelper.foundTarget() && TailHelper.getTargetPriority() >= 5) {
            MapLocation target = TailHelper.getTargetLocation();
            moveTowards(target);
            return;
        }

        MapLocation enemyArchonLocation = CommsHelper.getEnemyArchonLocation();
        if (enemyArchonLocation != null) {
            moveTowards(enemyArchonLocation);
            return;
        }

        if (guessedEnemyArchonLocation == null) {
            if (TailHelper.foundTarget()) {
                MapLocation target = TailHelper.getTargetLocation();
                moveTowards(target);
            } else {
                dir = directions[rng.nextInt(directions.length)];
                MovementHelper.greedyTryMove(dir);
            }

            return;
        }

        moveTowards(guessedEnemyArchonLocation);
    }

    public static boolean tryConcave() throws GameActionException {
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
                if (rubbleThere <= rubbleHere) {
                    MovementHelper.tryMove(dir, true);
                }
            }

            return true;
        }

        if (minDistance == INNER_CONCAVE_RADIUS) {
            Direction dir = getBestLateralDirection(rc.getLocation().directionTo(nearestRobot.location), nearestRobot);
            if (dir != Direction.CENTER) {
                MovementHelper.tryMove(dir, true);
            } else {
                dir = rc.getLocation().directionTo(nearestRobot.location);
                Direction leftBack = dir.rotateLeft().rotateLeft().rotateLeft();
                Direction rightBack = dir.rotateRight().rotateRight().rotateRight();
                dir = getBestDirection(leftBack, rightBack);
                if (dir != null && rc.senseRubble(rc.getLocation().add(dir)) < rc.senseRubble(rc.getLocation())) {
                    MovementHelper.tryMove(dir, true);
                }
            }

            return true;
        }

        if (minDistance <= OUTER_CONCAVE_RADIUS) {
            Direction dir = getBestLateralDirection(rc.getLocation().directionTo(nearestRobot.location), nearestRobot);
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

    private static Direction getBestDirection(Direction dir1, Direction dir2) throws GameActionException {
        MapLocation location1 = rc.getLocation().add(dir1);
        MapLocation location2 = rc.getLocation().add(dir2);
        Direction best = null;
        int minRubble = 1000;
        if (rc.canMove(dir1)) {
            int rubble = rc.senseRubble(location1);
            if (rubble < minRubble) {
                minRubble = rubble;
                best = dir1;
            }
        }
        if (rc.canMove(dir2)) {
            int rubble = rc.senseRubble(location2);
            if (rubble < minRubble) {
                minRubble = rubble;
                best = dir2;
            }
        }

        return best;
    }

    public static void circleAround(MapLocation location) throws GameActionException {
        int distance = rc.getLocation().distanceSquaredTo(location);
        if (distance < INNER_DEFENSE_RADIUS) {
            Direction dir = rc.getLocation().directionTo(location).opposite();
            if (!MovementHelper.greedyTryMove(dir)) {
                dir = dir.opposite();
                if (!MovementHelper.tryMove(dir.rotateRight(), true)) {
                    MovementHelper.tryMove(dir.rotateRight().rotateRight(), true);
                }
            }
        } else if (INNER_DEFENSE_RADIUS <= distance && distance <= OUTER_DEFENSE_RADIUS) {
            if (!DefenseHelper.tryMoveRight(location)) {
                Direction dir = rc.getLocation().directionTo(location).rotateRight();
                if (!MovementHelper.tryMove(dir, true)) {
                    MovementHelper.tryMove(dir.rotateLeft(), true);
                }
            }
        } else if (distance <= RobotType.ARCHON.visionRadiusSquared) {
            Direction dir = rc.getLocation().directionTo(location);
            if (MovementHelper.greedyTryMove(dir)) {
                return;
            } else {
                DefenseHelper.tryMoveRightAndBack(dir);
            }
        } else {
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
