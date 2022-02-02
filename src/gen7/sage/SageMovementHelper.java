package gen7.sage;

import battlecode.common.*;
import gen7.Sage;
import gen7.common.CommsHelper;
import gen7.common.Functions;
import gen7.common.MovementHelper;
import gen7.common.util.Pair;
import gen7.common.util.Vector;
import gen7.soldier.AttackHelper;
import gen7.soldier.DefenseHelper;
import gen7.soldier.SoldierMovementHelper;
import gen7.soldier.TailHelper;

import java.util.Map;

import static gen7.RobotPlayer.*;
import static gen7.common.Functions.getDistance;

public class SageMovementHelper {

    public static final int HP_THRESHOLD = 46;
    private static final int FULL_HEAL_THRESHOLD = 80;
    private static final int TURNS_THRESHOLD = 10;

    private static final int INNER_DEFENSE_RADIUS = 5;
    private static final int OUTER_DEFENSE_RADIUS = 20;

    private static final double TURNS_PER_MOVE = 3;

    public static void defenseRevolution(MapLocation defenseLocation) throws GameActionException {
        SoldierMovementHelper.circleAround(defenseLocation);
    }

    private static void moveOrWait(MapLocation target) throws GameActionException {
        int actionCooldown = rc.getActionCooldownTurns() / 10;
        int distance = getDistance(rc.getLocation(), target);
        rc.setIndicatorString("" + actionCooldown + " " + distance + " " + target);
        if (actionCooldown <= (distance - 4) * TURNS_PER_MOVE || actionCooldown <= 5 || distance > 10) {
            moveTowards(target);
        }
    }

    private static int distanceFromEdge(MapLocation ml) {
        int w = rc.getMapWidth(), h = rc.getMapHeight();
        return Math.min(
                Math.min(w-ml.x, ml.x + 1),
                Math.min(h-ml.y, ml.y + 1)
        );
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

    public static void moveToHuntLabs() throws GameActionException {
        MapLocation rn = rc.getLocation();
        Direction alongEdge = Functions.getDirectionAlongEdge(Sage.isClockWise, 6);
        if (alongEdge != null) {
            MovementHelper.tryMove(alongEdge, false);
        } else {
            MovementHelper.moveBellmanFord(getClosestEdge(rn));
        }
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

            return;
        }

        Direction antiCharge = getAntiChargeLocation();
        if (antiCharge != null) {
            MovementHelper.tryMove(antiCharge, false);
            return;
        }

        if (moveBack != null) {
            MovementHelper.greedyTryMove(moveBack);
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
}
