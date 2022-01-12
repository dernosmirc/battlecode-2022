package gen2.util;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

import static gen2.RobotPlayer.directions;

public strictfp class Functions {

	public static int getBits(int num, int low, int high) {
		return (num & ((1 << (high + 1)) - 1)) >> low;
	}

	public static int setBits(int num, int low, int high, int val) {
		return ((num >> (high + 1)) << (high + 1)) | (val << low) | (num & ((1 << low) - 1));
	}

	public static double sigmoid (double x) {
		return 1 / (1 + Math.exp(-x));
	}

	public static double gaussian (double x) {
		return Math.exp(-x*x);
	}

	public static Object getRandom(Object[] col) {
		return col[(int) (Math.random() * col.length)];
	}

	public static Direction getRandomDirection() {
		return (Direction) getRandom(directions);
	}

	public static Direction vectorAddition(Direction ... dirs) {
		MapLocation yeah = new MapLocation(0,0);
		for (Direction d : dirs) {
			yeah = yeah.add(d);
		}
		return (new MapLocation(0,0)).directionTo(yeah);
	}

	public static MapLocation translate(MapLocation loc, Direction dir, int dist) {
		MapLocation yeah = loc;
		for (int i = 0; i < dist; i++) {
			yeah = yeah.add(dir);
		}
		return yeah;
	}
}
