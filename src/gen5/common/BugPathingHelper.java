package gen5.common;

import battlecode.common.*;

import java.lang.Math;

import static gen5.RobotPlayer.*;
import static gen5.common.MovementHelper.greedyTryMove;
import static gen5.common.MovementHelper.updateMovement;

public class BugPathingHelper {
    /**
     * Any location with less than or equal to this amount of rubble is not an obstacle.
     * All other squares are obstacles.
     */
    private static int ACCEPTABLE_RUBBLE = 25;
    private static int ACCEPTABLE_WALL_MOVES = 15;
    private static int WIDTH_RUBBLE_THRESHOLD = 40;
    private static int WIDTH_CHECK_WALL_MOVES = 5;
    /**
     * The direction that we are trying to use to go around the obstacle.
     * It is null if we are not trying to go around an obstacle.
     */
    private static Direction bugDirection = null;
    /**
     * Previous target at which loop is aimed at
     */
    private static MapLocation loopTarget = null;
    /**
     * True iff looping around obstacle, false elsewhere
     */
    private static boolean gen3_2 = false;
    /**
     * Number of moves made while looping around object
     */
    private static int wallMoveCount = 0;
    /**
     * True if robot is on wall(due to anomaly or forced entry), else false
     */
    private static boolean anomaly = false;
    /**
     * Distance to target from where we start looping around obstacle, > 0 if looping, -1 else
     */
    private static int curDistanceToTarget = -1;
    /**
     * Location where looping starts
     */
    private static MapLocation startLocation;
    /**
     * The direction in which robot is moving before approaching wall
     */
    private static Direction initialDirection = null;
    /**
     *
     */
    private static int loopTowardsSide = 0;
//    /**
//     * Default Constructor
//     */
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
     * @param d Direction of movement
     * @return True if there is obstacle in direction d
     * @throws GameActionException
     */
    private static boolean isObstacle(Direction d) throws GameActionException {
        MapLocation adjacentLocation = rc.getLocation().add(d);
        int rubbleOnLocation = rc.senseRubble(adjacentLocation);
        return rubbleOnLocation > ACCEPTABLE_RUBBLE;
    }
    /**
     * Checks if the square we reach by moving in direction d is an obstacle.
     * @param location Direction of movement
     * @return True if there is obstacle in map location
     * @throws GameActionException
     */
    private static boolean isObstacle(MapLocation location) throws GameActionException {
        int rubbleOnLocation = rc.senseRubble(location);
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
     * Set values back to default
     */
    public static void setDefault(){;
        bugDirection = null;
        startLocation = null;
        gen3_2 = false;
        wallMoveCount = 0;
        initialDirection = null;
        loopTowardsSide = 0;
        loopTarget = null;
    }

    /**
     * Checks width
     * @param d
     * @param loc
     * @return true if width move can be made, else false
     * @throws GameActionException
     */
    private static boolean widthCheck(Direction d, MapLocation loc) throws GameActionException{
        int leftRubble = 0, midRubble = 0, rightRubble = 0;
        MapLocation leftLoc = loc.add(d.rotateLeft()), midLoc = loc.add(d), rightLoc = loc.add(d.rotateRight());
        int width = 1;
        while (true){
            if (!rc.canSenseLocation(leftLoc))   break;
            int temp1 = rc.senseRubble(leftLoc) + Math.min(leftRubble, midRubble);
            if (rc.senseRubble(leftLoc) < ACCEPTABLE_RUBBLE && temp1/width <= WIDTH_RUBBLE_THRESHOLD){
                return true;
            }
            if (!rc.canSenseLocation(midLoc))   break;
            int temp2 = rc.senseRubble(midLoc) + Math.min(midRubble, Math.min(leftRubble, rightRubble));
            if (rc.senseRubble(midLoc) < ACCEPTABLE_RUBBLE && temp2/width <= WIDTH_RUBBLE_THRESHOLD){
                return true;
            }
            if (!rc.canSenseLocation(rightLoc))  break;
            int temp3 = rc.senseRubble(rightLoc) + Math.min(midRubble, rightRubble);
            if (rc.senseRubble(rightLoc) < ACCEPTABLE_RUBBLE && temp3/width <= WIDTH_RUBBLE_THRESHOLD){
                return true;
            }
            leftLoc = leftLoc.add(d);
            rightLoc = rightLoc.add(d);
            midLoc = midLoc.add(d);
            leftRubble = temp1;
            midRubble = temp2;
            rightRubble = temp3;
            width += 1;
        }
        return false;
    }
    /**
     * Moves the Robot Controller to the target using bug path finding algo.
     * @param target the final target MapLocation
     * @throws GameActionException
     *
     */

    private static void debugString(MapLocation target){
        // indicator string log
        String indicator = "";
        indicator += gen3_2;
        indicator += loopTowardsSide;
        if (loopTarget == null){
            indicator += "00";
        }
        else{
            indicator += loopTarget.toString();
        }
        if (startLocation == null){
            indicator += "00";
        }
        else{
            indicator += startLocation.toString();
        }
        if (initialDirection == null){
            indicator += "?";
        }
        else{
            indicator += initialDirection.toString();
        }
        if (bugDirection == null){
            indicator += "?";
        }
        else{
            indicator += bugDirection.toString();
        }
        indicator += wallMoveCount;
        indicator += anomaly;
        indicator += (target.x + "," + target.y);
        // IS_LOOP, LOOP_TOWARDS_SIDE, LOOP_TARGET,START_LOC, INITIAL_DIR, BUG_DIR, WALL_MOVE_COUNT, ANOMALY, TARGET
        rc.setIndicatorString(indicator);
    }

    public static void moveTowards(MapLocation target) throws GameActionException {
        if (DEBUG){
            debugString(target);
        }
        if (!rc.isMovementReady()) {
            // If our cooldown is too high, then don't even bother!
            // There is nothing we can do anyway.
            return;
        }
        MapLocation currentLocation = rc.getLocation();
        // if (currentLocation == target) // this is BAD! see Lecture 2 for why.
        if (currentLocation.distanceSquaredTo(target) <= 2) {
            // We're already at our goal! Nothing to do either.
            setDefault();
            return;
        }
        if (isObstacle(currentLocation)){
            setDefault();
            anomaly = true;
        }
        else{
            anomaly = false;
        }
        if (loopTarget != null && gen3_2 && !loopTarget.equals(target)){
            setDefault();
        }
        // Calculate new direction
        MapLocation newLocation = new MapLocation(rc.getLocation().x, rc.getLocation().y);
        Direction newDirection = newLocation.directionTo(target);
        if (gen3_2){
            // check if we can move towards target
            // see case if you don't return to same place accidentally.
            if (rc.canMove(newDirection) && !isObstacle(newDirection) && checkIfForward(startLocation, target, newLocation)){
                if (DEBUG) {
                    System.out.println("CAN MOVE" + newLocation.toString());
                    rc.setIndicatorDot(rc.getLocation(), 0, 0, 255);
                }
                setDefault();
                rc.move(newDirection);
                initialDirection = newDirection;
                updateMovement(newDirection, true);
                return;
            }
        }
        if (anomaly) {
            greedyTryMove(newDirection);
            return;
        }
        Direction d = currentLocation.directionTo(target);
        // moveTowardsDirection(rc, d);
        if (!gen3_2 && rc.canMove(d) && !isObstacle(d)) {
            // Easy case of Bug 0!
            // No obstacle in the way, so let's just go straight for it!
            rc.move(d);
            updateMovement(d, true);
            setDefault();
            initialDirection = d;
        } else {
            gen3_2 = true;
            loopTarget = target;
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
            // Specifies loop rotate direction
            // DEFAULT: 0
            // LEFT: -1
            // RIGHT: 1
            if (loopTowardsSide == 0){
                if (initialDirection == null){
                    loopTowardsSide = 1;
                }
                else if (Functions.vectorAddition(d, initialDirection.opposite()) == Direction.WEST){
                    loopTowardsSide = -1;
                }
                else if (Functions.vectorAddition(d, initialDirection.opposite()) == Direction.EAST){
                    loopTowardsSide = 1;
                }
                else{
                    loopTowardsSide = 1;
                }
            }

            if (wallMoveCount > WIDTH_CHECK_WALL_MOVES && widthCheck(d, currentLocation)){
//                System.out.println("" + currentLocation.toString() + " " + d.toString());
                if (greedyTryMove(d)){
                    setDefault();
                    return;
                }
            }

            if (wallMoveCount > ACCEPTABLE_WALL_MOVES){
                if (greedyTryMove(d)){
                    setDefault();
                }
                return;
            }

            // Now, try to actually go around the obstacle
            // using bugDirection!
            // Repeat 8 times to try all 8 possible directions.
            for (int i = 0; i < 8; i++) {
                if (rc.canMove(bugDirection) && !isObstacle(bugDirection)) {
                    rc.move(bugDirection);
                    updateMovement(bugDirection, true);
                    wallMoveCount += 1;
                    if (loopTowardsSide == 1){
                        bugDirection = bugDirection.rotateRight();
                    }
                    else{
                        bugDirection = bugDirection.rotateLeft();
                    }
                    if (DEBUG){
                        rc.setIndicatorDot(rc.getLocation(), 0, 255, 0);
                    }
                    break;
                } else {
                    if (loopTowardsSide == 1){
                        bugDirection = bugDirection.rotateLeft();
                    }
                    else{
                        bugDirection = bugDirection.rotateRight();
                    }
                }
            }
        }
    }
}
