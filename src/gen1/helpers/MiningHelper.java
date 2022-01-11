package gen1.helpers;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import static gen1.RobotPlayer.*;

public class MiningHelper {

    public static void mineGold() throws GameActionException {
        for (MapLocation mp: rc.senseNearbyLocationsWithGold(myType.actionRadiusSquared)) {
            while (rc.isActionReady() && rc.canMineGold(mp)) {
                rc.mineGold(mp);
            }
            if (!rc.isActionReady()) {
                break;
            }
        }
    }

    public static void mineLead() throws GameActionException {
        for (MapLocation mp: rc.senseNearbyLocationsWithLead(myType.actionRadiusSquared, 2)) {
            while (rc.isActionReady() && rc.senseLead(mp) > 1) {
                rc.mineLead(mp);
            }
            if (!rc.isActionReady()) {
                break;
            }
        }
    }

    public static boolean canMineLead() throws GameActionException {
        return rc.senseNearbyLocationsWithLead(myType.actionRadiusSquared, 2).length > 0;
    }

    public static MapLocation spotGold() throws GameActionException {
        MapLocation best = null;
        int bestGold = 0;
        for (MapLocation mp: rc.senseNearbyLocationsWithGold(myType.visionRadiusSquared)) {
            int gold = rc.senseGold(mp);
            if (gold > bestGold) {
                best = mp;
                bestGold = gold;
            }
        }
        return best;
    }

    public static Direction spotLead(MapLocation archonLocation) throws GameActionException {
        MapLocation location = rc.getLocation();
        Direction best = null;
        int bestLead = 0;
        for (Direction dir: Direction.allDirections()) {
            MapLocation dest = location.add(dir);
            if (!dest.isWithinDistanceSquared(archonLocation, 13)) {
                int lead = 0;
                for (MapLocation mp :
                        rc.getAllLocationsWithinRadiusSquared(dest, myType.actionRadiusSquared)
                ) {
                    lead += rc.senseLead(mp);
                }
                if (lead > bestLead) {
                    best = dir;
                    bestLead = lead;
                }
            }
        }
        return best;
    }
}
