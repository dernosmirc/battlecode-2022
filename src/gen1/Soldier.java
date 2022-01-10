package gen1;

import battlecode.common.*;
import scala.collection.Map;
import sun.tools.tree.DivideExpression;

import static gen1.RobotPlayer.*;
import static gen1.util.Functions.getBits;
import static gen1.util.Functions.setBits;

import java.util.Random;

public strictfp class Soldier {
	private static MapLocation myArchonLocation;
	private static int myArchonIndex;
	private static MapLocation enemyArchon;
	private static int mapWidth, mapHeight;
	static final Random rng = new Random(rc.getID());
	private static boolean enemyArchonFound;
	private static MapLocation calculatedEnemyAnchorLocation;
	private static MapLocation sensedEnemyAnchorLocation;
	private static Direction dir;


	public static void run() throws GameActionException {
		MapLocation curLocation = rc.getLocation();

		int arrayRead = rc.readSharedArray(0);
		if (getBits(arrayRead, 15, 15) == 1){
			enemyArchonFound = true;
			sensedEnemyAnchorLocation = new MapLocation(getBits(arrayRead, 6, 11), getBits(arrayRead, 0, 5));
		}

		RobotInfo[] enemyRobotInfo1;
		enemyRobotInfo1 = rc.senseNearbyRobots(-1, myTeam.opponent());
		int n = enemyRobotInfo1.length;
		for (int i = 0; i < n; i++){
			// TODO: Check if two enemy robots are same sense radius, only one is set in shared array
			if (enemyRobotInfo1[i].getType() == RobotType.ARCHON){
				rc.attack(enemyRobotInfo1[i].getLocation());
				return;
			}
		}
		if (n > 0){
			rc.attack(enemyRobotInfo1[0].getLocation());
		}

		if (enemyArchonFound) {
			dir = curLocation.directionTo(sensedEnemyAnchorLocation);
			if (rc.canMove(dir)){
				rc.move(dir);
			}
			else if (rc.canMove(dir.rotateLeft())){
				rc.move(dir.rotateLeft());
			}
			else if(rc.canMove(dir.rotateRight())){
				rc.move(dir.rotateRight());
			}
			return;
		}

		if (curLocation.distanceSquaredTo(calculatedEnemyAnchorLocation) <= myType.visionRadiusSquared){
			RobotInfo[] enemyRobotInfo;
			enemyRobotInfo = rc.senseNearbyRobots(myType.actionRadiusSquared, myTeam.opponent());
			n = enemyRobotInfo.length;
			for (int i = 0; i < n; i++){
				// TODO: Check if two enemy robots are same sense radius, only one is set in shared array
				if (enemyRobotInfo[i].getType() == RobotType.ARCHON){
					enemyArchonFound = true;
					sensedEnemyAnchorLocation = enemyRobotInfo[i].getLocation();
					int value = 0;
					value = setBits(0, 15, 15, 1);
					value = setBits(value, 6, 11, sensedEnemyAnchorLocation.x);
					value = setBits(value, 0, 5, sensedEnemyAnchorLocation.y);
					rc.writeSharedArray(0, value);
					break;
				}
			}
		}


		dir = curLocation.directionTo(calculatedEnemyAnchorLocation);
		if (rc.canMove(dir)){
			rc.move(dir);
		}
		else if (rc.canMove(dir.rotateLeft())){
			rc.move(dir.rotateLeft());
		}
		else if(rc.canMove(dir.rotateRight())){
			rc.move(dir.rotateRight());
		}
	}

	public static void preCalculate(){
		mapHeight = rc.getMapHeight();
		mapWidth = rc.getMapWidth();
		enemyArchonFound = false;
		int randomNumber = rng.nextInt(3);
		if (randomNumber == 0){
			calculatedEnemyAnchorLocation = new MapLocation(myArchonLocation.x, mapHeight - myArchonLocation.y - 1);
		}
		else if (randomNumber == 1){
			calculatedEnemyAnchorLocation = new MapLocation(mapWidth - myArchonLocation.x - 1, myArchonLocation.y);
		}
		else{
			calculatedEnemyAnchorLocation = new MapLocation(mapWidth - myArchonLocation.x - 1, mapHeight - myArchonLocation.y - 1);
		}
	}

	public static void init() throws GameActionException {
		for (int i = 32; i < 32 + archonCount; ++i) {
			int value = rc.readSharedArray(i);
			myArchonLocation = new MapLocation(getBits(value, 6, 11), getBits(value, 0, 5));
			if (rc.getLocation().distanceSquaredTo(myArchonLocation) <= 2) {
				myArchonIndex = i - 32;
				break;
			}
		}
		preCalculate();
	}
}
