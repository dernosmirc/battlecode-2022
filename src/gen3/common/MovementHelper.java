package gen3.common;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import gen3.util.Functions;
import static gen3.RobotPlayer.rc;

public class MovementHelper {

    private static final double[] DIRECTION_WEIGHTS = {0, 1, 6, 1, 0};

    private static final double DIRECTION_BETA = 0.334;
    private static double dx = 0, dy = 0;
    public static Direction getInstantaneousDirection() {
        return Functions.directionTo(dx, dy);
    }
    public static void updateMovement(Direction d) {
        dx = DIRECTION_BETA * d.dx + (1-DIRECTION_BETA) * dx;
        dy = DIRECTION_BETA * d.dy + (1-DIRECTION_BETA) * dy;
    }

    public static boolean moveAndAvoid(
            Direction direction, MapLocation location, int distanceSquared
    ) throws GameActionException {
        Direction[] dirs = {
                direction,
                direction.rotateLeft(),
                direction.rotateRight()
        };
        for (Direction dir: dirs) {
            if (!rc.getLocation().add(dir).isWithinDistanceSquared(location, distanceSquared)) {
                if (tryMove(dir, false)) {
                    return true;
                }
            }
        }
        return tryMove(direction, false);
    }

    public static boolean tryMove (Direction dir, boolean force) throws GameActionException {
        if (rc.isMovementReady()) {
            if (!force) {
                Direction[] dirs = {
                        dir.rotateRight().rotateRight(),
                        dir.rotateRight(),
                        dir,
                        dir.rotateLeft(),
                        dir.rotateLeft().rotateLeft(),
                };
                MapLocation ml = rc.getLocation();
                Direction opt = null;
                double bestFact = 0;
                for (int i = 0; i < dirs.length; i++) {
                    if (rc.canMove(dirs[i])) {
                        double fact = DIRECTION_WEIGHTS[i] / (1 + rc.senseRubble(ml.add(dirs[i])));
                        if (fact > bestFact) {
                            opt = dirs[i];
                            bestFact = fact;
                        }
                    }
                }
                if (opt != null) {
                    rc.move(opt);
                    updateMovement(opt);
                    return true;
                }
            } else {
                if (rc.canMove(dir)) {
                    rc.move(dir);
                    updateMovement(dir);
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean greedyTryMove(Direction dir) throws GameActionException {
        if (!rc.isMovementReady()) {
            return false;
        }

        // incomplete
        return false;
    }
}
