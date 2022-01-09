package gen1.util;

public strictfp class Functions {

	public static int getBits(int num, int low, int high) {
		return (num & ((1 << (high + 1)) - 1)) >> low;
	}

	public static int setBits(int num, int low, int high, int val) {
		return ((num >> (high + 1)) << (high + 1)) | (val << low) | (num & ((1 << low) - 1));
	}
}
