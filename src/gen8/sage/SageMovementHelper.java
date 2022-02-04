package gen8.sage;

import battlecode.common.*;
import gen8.Sage;
import gen8.common.CommsHelper;
import gen8.common.MovementHelper;
import gen8.common.util.Vector;
import gen8.soldier.AttackHelper;
import gen8.soldier.DefenseHelper;
import gen8.soldier.SoldierMovementHelper;
import gen8.soldier.TailHelper;


import static gen8.RobotPlayer.*;
import static gen8.common.Functions.*;

public class SageMovementHelper {

    public static final int HP_THRESHOLD = 46;
    private static final int FULL_HEAL_THRESHOLD = 80;

    private static final double TURNS_PER_MOVE = 3;

    public static void defenseRevolution(MapLocation defenseLocation) throws GameActionException {
        SoldierMovementHelper.circleAround(defenseLocation);
    }

    private static void moveOrWait(MapLocation target) throws GameActionException {
        int actionCooldown = rc.getActionCooldownTurns() / 10;
        int distance = getDistance(rc.getLocation(), target);
        int distanceSquared = rc.getLocation().distanceSquaredTo(target);
        if (actionCooldown > 0 && distanceSquared <= 53) {
            // wait
        } else if (actionCooldown <= (distance - 4) * TURNS_PER_MOVE || actionCooldown <= 5 || distance > 10) {
            moveTowards(target);
        }
    }

    static boolean retreat(Direction moveBack) throws GameActionException {
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
            return false;
        }

        if (Sage.isLabHunter && !getLocationIn3x3Grid(rc.getLocation()).equals(getLocationIn3x3Grid(archonLocation))) {
            return false;
        }

        int actionCooldown = rc.getActionCooldownTurns() / 10;
        if (rc.getLocation().isWithinDistanceSquared(archonLocation, RobotType.ARCHON.actionRadiusSquared)) {
            if (moveBack != null && actionCooldown <= 5) {
                moveTowards(archonLocation);
            } else if (moveBack != null) {
                MovementHelper.greedyTryMove(moveBack);
            } else {
                SoldierMovementHelper.circleAround(archonLocation);
            }
        } else {
            SoldierMovementHelper.circleAround(archonLocation);
        }

        return true;
    }


    public static void move() throws GameActionException {
        Direction moveBack = AttackHelper.shouldMoveBack();

        MapLocation defenseLocation = DefenseHelper.getDefenseLocation();
        if (defenseLocation != null) {
            int actionCooldown = rc.getActionCooldownTurns() / 10;
            if (rc.getLocation().isWithinDistanceSquared(defenseLocation, RobotType.ARCHON.actionRadiusSquared)) {
                if (actionCooldown <= 5) {
                    moveTowards(defenseLocation);
                } else if (moveBack != null) {
                    MovementHelper.greedyTryMove(moveBack);
                } else {
                    SoldierMovementHelper.circleAround(defenseLocation);
                }
            } else {
                SoldierMovementHelper.circleAround(defenseLocation);
            }

            return;
        }

        if (rc.getHealth() < HP_THRESHOLD) {
            retreat(moveBack);
            return;
        }

        if (moveBack != null) {
            MovementHelper.greedyTryMove(moveBack);
            return;
        }

        Direction antiCharge = getAntiChargeLocation();
        if (antiCharge != null) {
            MovementHelper.tryMove(antiCharge, false);
            return;
        }

        RobotInfo[] robots = rc.senseNearbyRobots(RobotType.ARCHON.actionRadiusSquared, myTeam); // try vision radius?
        RobotInfo archon = null;
        for (int i = robots.length; --i >= 0; ) {
            RobotInfo robot = robots[i];
            if (robot.type == RobotType.ARCHON && robot.mode == RobotMode.TURRET) {
                archon = robot;
                break;
            }
        }

        if (archon != null && rc.getHealth() < FULL_HEAL_THRESHOLD) {
            SoldierMovementHelper.circleAround(archon.location);
            return;
        }

        if (TailHelper.foundTarget() && TailHelper.getTargetPriority() >= 5) {
            moveOrWait(TailHelper.getTargetLocation());
            return;
        }

        MapLocation enemyArchonLocation = CommsHelper.getEnemyArchonLocation();
        if (enemyArchonLocation != null) {
            moveOrWait(enemyArchonLocation);
            return;
        }

        if (TailHelper.foundTarget()) {
            if (TailHelper.getTargetPriority() >= 3) {
                moveOrWait(TailHelper.getTargetLocation());
            } else {
                moveTowards(TailHelper.getTargetLocation());
            }
            return;
        }

        MapLocation[] archons = CommsHelper.getFriendlyArchonLocations();
        int minDistance = rc.getMapWidth() * rc.getMapHeight();
        MapLocation archonLocation = null;
        for (int i = maxArchonCount; --i >= 0; ) {
            if (archons[i] != null) {
                int distance = getDistance(archons[i], rc.getLocation());
                if (distance < minDistance) {
                    minDistance = distance;
                    archonLocation = archons[i];
                }
            }
        }

        if (archonLocation == null) {
            return;
        }

        SoldierMovementHelper.circleAround(archonLocation);
    }

    private static Vector<Integer> chargeRounds;
    public static void checkForCharge() {
        AnomalyScheduleEntry[] entries = rc.getAnomalySchedule();
        chargeRounds = new Vector<>(entries.length);
        for (int i = entries.length; --i >= 0;) {
            if (entries[i].anomalyType == AnomalyType.CHARGE) {
                chargeRounds.add(entries[i].roundNumber);
            }
        }
    }

    static Direction getAntiChargeLocation() {
        while (!chargeRounds.isEmpty()) {
            int dif = chargeRounds.last() - rc.getRoundNum();
            if (dif < 0) {
                chargeRounds.popLast();
            } else if (dif < 50) {
                return MovementHelper.getAntiCrowdingDirection();
            } else {
                break;
            }
        }

        return null;
    }

    private static void moveTowards(MapLocation location) throws GameActionException {
        if (!Sage.attackedThisRound && MovementHelper.moveBellmanFord(location))
            return;

        Direction dir = rc.getLocation().directionTo(location);
        MovementHelper.greedyTryMove(dir);
    }

    private static MapLocation getLocationIn3x3Grid(MapLocation location) {
        return new MapLocation(getCoordinateIn3Segments(location.x, rc.getMapWidth()),
                                getCoordinateIn3Segments(location.y, rc.getMapHeight()));
    }

    private static int getCoordinateIn3Segments(int x, int length) {
        ++x;
        int segment = length / 3;
        if (x <= segment) {
            return 0;
        } else if (x <= 2 * segment + (length % 3 == 2 ? 1 : 0)) {
            return 1;
        } else {
            return 2;
        }
    }
}
