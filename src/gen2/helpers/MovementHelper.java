package gen2.helpers;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import java.util.Arrays;
import java.util.List;

import static gen2.RobotPlayer.directions;
import static gen2.RobotPlayer.rc;

public class MovementHelper {

    private static final int INFINITY = 101;

    public static final List<Direction> directionList = Arrays.asList(directions);

    public static boolean moveAndAvoid(
            Direction direction, MapLocation location, int distanceSquared
    ) throws GameActionException {
        Direction[] dirs = {
                direction,
                direction.rotateLeft(),
                direction.rotateRight(),
        };
        for (Direction dir: dirs) {
            if (!rc.getLocation().add(dir).isWithinDistanceSquared(location, distanceSquared)) {
                if (tryMove(dir, true)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean tryMove (Direction dir, boolean force) throws GameActionException {
        if (rc.getMovementCooldownTurns() < 10) {
            if (!force) {
                MapLocation ml = rc.getLocation();
                int left = rc.canMove(dir.rotateLeft()) ? rc.senseRubble(ml.add(dir.rotateLeft())) : INFINITY,
                        straight = rc.canMove(dir) ? rc.senseLead(ml.add(dir)) : INFINITY,
                        right = rc.canMove(dir.rotateRight()) ? rc.senseLead(ml.add(dir.rotateRight())) : INFINITY;

                if (straight != INFINITY && straight <= right && straight <= left) {
                    rc.move(dir);
                    return true;
                } else if (left != INFINITY && left <= right && straight >= left) {
                    rc.move(dir.rotateLeft());
                    return true;
                } else if (right != INFINITY && straight >= right && right <= left) {
                    rc.move(dir.rotateRight());
                    return true;
                }
            }

            if (rc.getMovementCooldownTurns() < 10) {
                int dirInt = directionList.indexOf(dir);
                // if blocked by another robot, find the next best direction
                for (int i = force ? 0 : 2; i < 5; i++) {
                    Direction got = directions[Math.floorMod(dirInt + i, 8)];
                    if (rc.canMove(got)) {
                        rc.move(got);
                        return true;
                    }
                    got = directions[Math.floorMod(dirInt - i, 8)];
                    if (rc.canMove(got)) {
                        rc.move(got);
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
