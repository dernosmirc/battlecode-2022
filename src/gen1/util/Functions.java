package gen1.util;

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
}
