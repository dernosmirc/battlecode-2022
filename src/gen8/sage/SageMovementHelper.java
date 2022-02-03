package gen8.sage;

import battlecode.common.*;
import gen8.Sage;
import gen8.common.CommsHelper;
import gen8.common.Functions;
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

    private static MapLocation getClosestEdge(MapLocation rn) {
        int w = rc.getMapWidth() - 1, h = rc.getMapHeight() - 1;
        int x = rn.x, y = rn.y, mx = x, my = 0, d = y;

        if (x < d) {
            mx = 0;
            my = y;
            d = x;
        }
        if (w - x < d) {
            mx = w;
            my = y;
            d = w - x;
        }
        if (h - y < d) {
            mx = x;
            my = h;
        }
        return new MapLocation(mx, my);
    }

    private static MapLocation spotEnemyLab() {
        RobotInfo[] ris = rc.senseNearbyRobots(myType.visionRadiusSquared, enemyTeam);
        for (int i = ris.length; --i >= 0; ) {
            if (ris[i].type == RobotType.LABORATORY) {
                return ris[i].location;
            }
        }
        return null;
    }

    private static boolean retreat(Direction moveBack) throws GameActionException {
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

    private static int distanceFromEdge() {
        MapLocation ml = rc.getLocation();
        int w = rc.getMapWidth(), h = rc.getMapHeight();
        return Math.min(
                Math.min(w-ml.x, ml.x + 1),
                Math.min(h-ml.y, ml.y + 1)
        );
    }

    private static int getExitDistance() {
        int width = rc.getMapWidth();
        int height = rc.getMapHeight();
        return (int) Math.ceil(Math.sqrt(width * height) / 20) + 3;
    }


    private static Direction getSoldierDirection() {
        MapLocation rn = rc.getLocation();
        int dx = 0, dy = 0;
        for (RobotInfo ri: rc.senseNearbyRobots(myType.visionRadiusSquared, enemyTeam)) {
            if (ri.type.canAttack() || ri.type == RobotType.ARCHON) {
                Direction d = rn.directionTo(ri.location);
                dx += d.dx;
                dy += d.dy;
            }
        }

        if (dx == 0 && dy == 0) {
            return null;
        }
        return directionTo(dx, dy);
    }

    private static Direction lastAlongEdge = null;
    private static int clockWiseCoolDown = 0;
    public static void moveToHuntLabs() throws GameActionException {
        if (clockWiseCoolDown > 0) {
            clockWiseCoolDown--;
        }
        if (rc.getHealth() < HP_THRESHOLD) {
            if (retreat(AttackHelper.shouldMoveBack())) {
                return;
            }
        }

        Direction antiCharge = getAntiChargeLocation();
        if (antiCharge != null) {
            MovementHelper.tryMove(antiCharge, false);
            return;
        }

        Direction alongEdge = Functions.getDirectionAlongEdge(Sage.isClockWise, getExitDistance(), true);
        Direction soldier = getSoldierDirection();

        if (Functions.areAdjacent(alongEdge, soldier) && clockWiseCoolDown == 0) {
            Sage.isClockWise = !Sage.isClockWise;
            clockWiseCoolDown = getExitDistance()*3;
        }

        MapLocation lab = spotEnemyLab();
        if (lab != null) {
            MovementHelper.moveBellmanFord(lab);
            return;
        }

        alongEdge = Functions.getDirectionAlongEdge(Sage.isClockWise, 2, true);
        if (alongEdge != null) {
            MovementHelper.tryMove(alongEdge, false);
            lastAlongEdge = alongEdge;
            return;
        }

        if (distanceFromEdge() < getExitDistance() && lastAlongEdge != null) {
            MovementHelper.tryMove(lastAlongEdge, false);
            return;
        }

        lastAlongEdge = null;
        MovementHelper.moveBellmanFord(getClosestEdge(rc.getLocation()));
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

    private static Direction getAntiChargeLocation() {
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
        if (MovementHelper.moveBellmanFord(location))
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
