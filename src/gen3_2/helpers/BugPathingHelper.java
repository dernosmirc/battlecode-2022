package gen3_2.helpers;

import battlecode.common.*;
import java.lang.Math;
import java.util.Map;
import static gen3.RobotPlayer.*;
import static gen3.common.MovementHelper.updateMovement;

public class BugPathingHelper {
    /**
     * Any location with less than or equal to this amount of rubble is not an obstacle.
     * All other squares are obstacles.
     */
    private static int ACCEPTABLE_RUBBLE = 35;

    /**
     * The direction that we are trying to use to go around the obstacle.
     * It is null if we are not trying to go around an obstacle.
     */
    private static Direction bugDirection = null;

    /**
     * True iff looping around obstacle, false elsewhere
     */
    private static boolean gen3_2 = false;
    /**
     * Distance to target from where we start looping around obstacle, > 0 if looping, -1 else
     */
    private static int curDistanceToTarget = -1;
    /**
     * Location where looping starts
     */
    private static MapLocation startLocation;
    /**
     * Default Constructor
     */
//    public BugPathingHelper(){
//        gen3_2 = false;
//        curDistanceToTarget = -1;
//        startLocation = null;
//    }
//    /**
//     *
//     * @param rubbleThreshold Value above it is set as obstacle, -1 for default value(25)
//     */
//    public BugPathingHelper(int rubbleThreshold){
//        if (rubbleThreshold == -1){
//            ACCEPTABLE_RUBBLE = 25;
//        }
//        else{
//            ACCEPTABLE_RUBBLE = rubbleThreshold;
//        }
//        gen3_2 = false;
//        curDistanceToTarget = -1;
//        startLocation = null;
//    }

    /**
     * Checks if the square we reach by moving in direction d is an obstacle.
     * @param rc Robot Controller
     * @param d Direction of movement
     * @return True if there is obstacle in direction d
     * @throws GameActionException
     */
    private static boolean isObstacle(RobotController rc, Direction d) throws GameActionException {
        MapLocation adjacentLocation = rc.getLocation().add(d);
        int rubbleOnLocation = rc.senseRubble(adjacentLocation);
        return rubbleOnLocation > ACCEPTABLE_RUBBLE;
    }

    private static double valInLine(double m, double c, int x, int y){
        return y - m * x - c;
    }

    /**
     *
     * @param start
     * @param target
     * @param cur
     * @return
     */
    private static boolean checkIfForward(MapLocation start, MapLocation target, MapLocation cur){
        int x1 = start.x, y1 = start.y, x2 = target.x, y2 = target.y, x3 = cur.x, y3 = cur.y;
        if (y1 - y2 == 0){
            return (x2 - x1) * (x3 - x1) > 0;
        }
        else{
            double m = (double) (x1 - x2) / (double) (y2 - y1);
            double c = y1 - m * x1;
            if (Math.abs(valInLine(m, c, x3, y3)) < 1e-5){
                return true;
            }
            return valInLine(m, c, x3, y3) * valInLine(m, c, x2, y2) > 0;
        }
    }

    /**
     * Moves the Robot Controller to the target using bug path finding algo.
     * @param target the final target MapLocation
     * @throws GameActionException
     *
     */
    public static void moveTowards(MapLocation target) throws GameActionException {
        if (!rc.isMovementReady()) {
            // If our cooldown is too high, then don't even bother!
            // There is nothing we can do anyway.
            return;
        }

        // indicator string log
        String indicator = "";
        indicator += gen3_2;
        if (startLocation == null){
            indicator += "00";
        }
        else{
            indicator += (startLocation.x + "," + startLocation.y);
        }
        if (bugDirection == null){
            indicator += "?";
        }
        else{
            indicator += bugDirection.name();
        }
        indicator += (target.x + "," + target.y);
        rc.setIndicatorString(indicator);
        MapLocation currentLocation = rc.getLocation();
        // if (currentLocation == target) // this is BAD! see Lecture 2 for why.
        if (currentLocation.equals(target)) {
            // We're already at our goal! Nothing to do either.
            return;
        }

        if (gen3_2){
            // Calculate new direction
            MapLocation newLocation = new MapLocation(rc.getLocation().x, rc.getLocation().y);
            Direction newDirection = newLocation.directionTo(target);
            // check if we can move towards target
            // see case if you don't return to same place accidentally.
            if (rc.canMove(newDirection) && !isObstacle(rc, newDirection) && checkIfForward(startLocation, target, newLocation)){
                System.out.println("CAN MOVE" + newLocation.toString());
                rc.setIndicatorDot(rc.getLocation(), 0, 0, 255);
                gen3_2 = false;
                startLocation = null;
                bugDirection = null;
                rc.move(newDirection);
                updateMovement(newDirection);
                return;
            }
        }

        Direction d = currentLocation.directionTo(target);
        // moveTowardsDirection(rc, d);
        if (!gen3_2 && rc.canMove(d) && !isObstacle(rc, d)) {
            // Easy case of Bug 0!
            // No obstacle in the way, so let's just go straight for it!
            rc.move(d);
            updateMovement(d);
            bugDirection = null;
            currentLocation = null;
        } else {
            gen3_2 = true;
            // Hard case of Bug 0 :<
            // There is an obstacle in the way, so we're gonna have to go around it.
            if (startLocation == null){
                startLocation = new MapLocation(currentLocation.x, currentLocation.y);
                rc.setIndicatorDot(startLocation, 255, 0, 0);
            }
            if (bugDirection == null) {
                // If we don't know what we're trying to do
                // make something up
                // And, what better than to pick as the direction we want to go in
                // the best direction towards the goal?
                bugDirection = d;
            }
            // Now, try to actually go around the obstacle
            // using bugDirection!
            // Repeat 8 times to try all 8 possible directions.
            for (int i = 0; i < 8; i++) {
                if (rc.canMove(bugDirection) && !isObstacle(rc, bugDirection)) {
                    rc.move(bugDirection);
                    updateMovement(bugDirection);
                    bugDirection = bugDirection.rotateLeft();
                    rc.setIndicatorDot(rc.getLocation(), 0, 255, 0);
                    break;
                } else {
                    bugDirection = bugDirection.rotateRight();
                }
            }
        }
    }
}
