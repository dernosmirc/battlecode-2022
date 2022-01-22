package gen6.sage;

import battlecode.common.*;
import gen6.builder.BuildingHelper;
import gen6.common.CommsHelper;
import gen6.common.MovementHelper;
import gen6.common.util.Vector;
import gen6.soldier.AttackHelper;
import gen6.soldier.DefenseHelper;
import gen6.soldier.SoldierMovementHelper;
import gen6.soldier.TailHelper;

import static gen6.RobotPlayer.*;
import static gen6.common.Functions.getDistance;
import static gen6.soldier.SoldierMovementHelper.tryConcave;

public class SageMovementHelper {

    public static final int HP_THRESHOLD = 40;
    private static final int FULL_HEAL_THRESHOLD = 80;
    private static final int TURNS_THRESHOLD = 10;

    private static final int INNER_DEFENSE_RADIUS = 5;
    private static final int OUTER_DEFENSE_RADIUS = 20;

    public static void defenseRevolution(MapLocation defenseLocation) throws GameActionException {
        SoldierMovementHelper.circleAround(defenseLocation);
    }

    public static void move() throws GameActionException {
        TailHelper.updateTarget();

        MapLocation antiCharge = getAntiChargeLocation();
        if (antiCharge != null) {
            MovementHelper.moveBellmanFord(antiCharge);
            return;
        }

        if (rc.getActionCooldownTurns()/10 >= TURNS_THRESHOLD) {
            moveTowards(BuildingHelper.getOptimalLabLocation());
            return;
        }

        MapLocation defenseLocation = DefenseHelper.getDefenseLocation();
        if (defenseLocation != null) {
            if (rc.getLocation().isWithinDistanceSquared(defenseLocation, RobotType.SOLDIER.visionRadiusSquared)) {
                if (tryConcave()) {
                    return;
                }
            }

            SoldierMovementHelper.circleAround(defenseLocation);
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
                System.out.println("No friendly archons");
                return;
            }

            if (rc.getLocation().isWithinDistanceSquared(archonLocation, RobotType.SOLDIER.visionRadiusSquared)) {
                if (tryConcave()) {
                    return;
                }
            }

            SoldierMovementHelper.circleAround(archonLocation);
            return;
        }

        Direction dir = AttackHelper.shouldMoveBack();
        if (dir != null) {
            MovementHelper.greedyTryMove(dir);
            return;
        }

        // if (SoldierMovementHelper.tryConcave()) {
        //     return;
        // }

        RobotInfo[] robots = rc.senseNearbyRobots(myType.visionRadiusSquared, myTeam); // try vision radius?
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
            MapLocation target = TailHelper.getTargetLocation();
            moveTowards(target);
            return;
        }

        MapLocation enemyArchonLocation = CommsHelper.getEnemyArchonLocation();
        if (enemyArchonLocation != null) {
            moveTowards(enemyArchonLocation);
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
            System.out.println("No friendly archons");
            return;
        }

        SoldierMovementHelper.circleAround(archonLocation);
        return;
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

    private static MapLocation getAntiChargeLocation() throws GameActionException {
        while (!chargeRounds.isEmpty()) {
            int dif = chargeRounds.last() - rc.getRoundNum();
            if (dif <= 0) {
                chargeRounds.popLast();
            } else if (dif < 75) {
                return BuildingHelper.getOptimalLabLocation();
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
