package gen8.sage;

import battlecode.common.*;
import gen8.Sage;
import gen8.common.Functions;
import gen8.common.MovementHelper;
import gen8.soldier.AttackHelper;

import static gen8.RobotPlayer.*;
import static gen8.RobotPlayer.rc;
import static gen8.common.Functions.directionTo;
import static gen8.sage.SageMovementHelper.*;

public class LabHunter {

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
        MapLocation closest = null, rn = rc.getLocation();
        for (int i = ris.length; --i >= 0; ) {
            if (ris[i].type == RobotType.LABORATORY) {
                MapLocation ml = ris[i].location;
                if (closest == null || rn.distanceSquaredTo(closest) > rn.distanceSquaredTo(ml)) {
                    closest = ml;
                }
            }
        }
        return closest;
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
        return (int) Math.ceil(Math.sqrt(width * height) / 13) + 3;
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

    private static int friendlySagesNearby() {
        RobotInfo[] ris = rc.senseNearbyRobots(myType.visionRadiusSquared, myTeam);
        int count = 0;
        for (int i = ris.length; --i >= 0; ) {
            if (ris[i].type == RobotType.SAGE) {
                count++;
            }
        }
        return count;
    }

    private static Direction lastAlongEdge = null;
    private static int clockWiseCoolDown = 0, stillCount = 0;
    private static MapLocation lastLocation = null;

    public static void move() throws GameActionException {
        if (lastLocation != null) {
            if (rc.getLocation().isWithinDistanceSquared(lastLocation, 5) && spotEnemyLab() == null) {
                stillCount++;
            } else {
                lastLocation = rc.getLocation();
                stillCount = 0;
            }
        } else {
            lastLocation = rc.getLocation();
        }


        if (clockWiseCoolDown > 0) {
            clockWiseCoolDown--;
        }
        if (rc.getHealth() < 56) {
            if (retreat(AttackHelper.shouldMoveBack())) {
                return;
            }
        }

        if (stillCount > 10 && clockWiseCoolDown == 0) {
            Sage.isClockWise = !Sage.isClockWise;
            clockWiseCoolDown = 10;
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
            if (rc.getLocation().isWithinDistanceSquared(lab, myType.actionRadiusSquared) &&
                    friendlySagesNearby() < 3 && rc.senseRubble(rc.getLocation()) <= 30
            ) {
                return;
            }
            MovementHelper.moveBellmanFord(lab);
            return;
        }

        alongEdge = Functions.getDirectionAlongEdge(Sage.isClockWise, 3, true);
        if (alongEdge != null) {
            MovementHelper.moveBellmanFord(alongEdge);
            lastAlongEdge = alongEdge;
            return;
        }

        if (distanceFromEdge() < getExitDistance() && lastAlongEdge != null) {
            MovementHelper.moveBellmanFord(lastAlongEdge);
            return;
        }

        lastAlongEdge = null;
        MovementHelper.moveBellmanFord(getClosestEdge(rc.getLocation()));
    }
}
