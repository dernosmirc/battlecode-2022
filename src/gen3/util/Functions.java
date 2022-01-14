package gen3.util;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

import java.util.Random;

import static gen3.RobotPlayer.directions;
import static gen3.RobotPlayer.rc;

public strictfp class Functions {
	private static final Random random = new Random(rc.getID());

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
		return col[(int) (random.nextDouble() * col.length)];
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

	public static double[] convolveCircularly (double[] array, double[] filter) {
		int sz = array.length, fsz = filter.length;
		double[] out = new double[sz];
		for (int i = 0; i < sz; i++) {
			for (int j = 0; j < fsz; j++) {
				out[i] += filter[j] * array[Math.floorMod(j-fsz/2+i, sz)];
			}
		}
		return out;
	}

	public static Direction directionTo(double dx, double dy) {
		if (dx == 0) {
			if (dy == 0) return Direction.CENTER;
			else if (dy > 0) return Direction.NORTH;
			else return Direction.SOUTH;
		}
		double deg = Math.toDegrees(Math.atan(dy/dx));
		if (dx >= 0) {
			if (-90 + 22.5 >= deg) {
				return Direction.SOUTH;
			} else if (-45 + 22.5 >= deg) {
				return Direction.SOUTHEAST;
		    } else if (0 + 22.5 >= deg) {
				return Direction.EAST;
			} else if (45 + 22.5 >= deg) {
				return Direction.NORTHEAST;
			} else {
				return Direction.NORTH;
			}
		} else {
			if (-90 + 22.5 >= deg) {
				return Direction.NORTH;
			} else if (-45 + 22.5 >= deg) {
				return Direction.NORTHWEST;
			} else if (0 + 22.5 >= deg) {
				return Direction.WEST;
			} else if (45 + 22.5 >= deg) {
				return Direction.SOUTHWEST;
			} else {
				return Direction.SOUTH;
			}
		}
	}
}
