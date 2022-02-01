package gen7.common.bellmanford;


public class Heuristics34 implements HeuristicsProvider {

	private final int[][] locationDumpX = {
		{ 2, 10, 3, 9, 4, 8, 3, 9, 5, 7, 6, 3, 9, 4, 8, 5, 7, 6, 4, 8, 4, 8, 5, 7, 6, 5, 7, 6, 5, 7, 6, },
		{ 9, 11, 6, 11, 10, 8, 11, 7, 11, 9, 10, 6, 10, 8, 10, 7, 10, 9, 8, 9, 6, 9, 7, 9, 8, 6, 8, 7, 8, 7, 6, 7, },
		{ 10, 10, 11, 11, 11, 11, 10, 10, 11, 11, 11, 9, 9, 10, 10, 10, 10, 10, 9, 9, 8, 8, 9, 9, 9, 8, 8, 8, 7, 7, 7, },
		{ 9, 11, 6, 11, 10, 8, 11, 7, 11, 9, 10, 6, 10, 8, 10, 7, 10, 9, 8, 9, 6, 9, 7, 9, 8, 6, 8, 7, 8, 7, 6, 7, },
		{ 2, 10, 3, 9, 4, 8, 3, 9, 5, 7, 6, 3, 9, 4, 8, 5, 7, 6, 4, 8, 4, 8, 5, 7, 6, 5, 7, 6, 5, 7, 6, },
		{ 1, 3, 1, 6, 2, 1, 4, 1, 5, 2, 3, 2, 6, 2, 4, 2, 5, 3, 3, 4, 3, 6, 3, 5, 4, 4, 6, 4, 5, 5, 5, 6, },
		{ 2, 2, 1, 1, 1, 1, 2, 2, 1, 1, 1, 3, 3, 2, 2, 2, 2, 2, 3, 3, 4, 4, 3, 3, 3, 4, 4, 4, 5, 5, 5, },
		{ 1, 3, 1, 6, 2, 1, 4, 1, 5, 2, 3, 2, 6, 2, 4, 2, 5, 3, 3, 4, 3, 6, 3, 5, 4, 4, 6, 4, 5, 5, 5, 6, },
	};

	private final int[][] locationDumpY = {
		{ 10, 10, 11, 11, 11, 11, 10, 10, 11, 11, 11, 9, 9, 10, 10, 10, 10, 10, 9, 9, 8, 8, 9, 9, 9, 8, 8, 8, 7, 7, 7, },
		{ 11, 9, 11, 6, 10, 11, 8, 11, 7, 10, 9, 10, 6, 10, 8, 10, 7, 9, 9, 8, 9, 6, 9, 7, 8, 8, 6, 8, 7, 7, 7, 6, },
		{ 2, 10, 3, 9, 4, 8, 3, 9, 5, 7, 6, 3, 9, 4, 8, 5, 7, 6, 4, 8, 4, 8, 5, 7, 6, 5, 7, 6, 5, 7, 6, },
		{ 1, 3, 1, 6, 2, 1, 4, 1, 5, 2, 3, 2, 6, 2, 4, 2, 5, 3, 3, 4, 3, 6, 3, 5, 4, 4, 6, 4, 5, 5, 5, 6, },
		{ 2, 2, 1, 1, 1, 1, 2, 2, 1, 1, 1, 3, 3, 2, 2, 2, 2, 2, 3, 3, 4, 4, 3, 3, 3, 4, 4, 4, 5, 5, 5, },
		{ 3, 1, 6, 1, 2, 4, 1, 5, 1, 3, 2, 6, 2, 4, 2, 5, 2, 3, 4, 3, 6, 3, 5, 3, 4, 6, 4, 5, 4, 5, 6, 5, },
		{ 2, 10, 3, 9, 4, 8, 3, 9, 5, 7, 6, 3, 9, 4, 8, 5, 7, 6, 4, 8, 4, 8, 5, 7, 6, 5, 7, 6, 5, 7, 6, },
		{ 9, 11, 6, 11, 10, 8, 11, 7, 11, 9, 10, 6, 10, 8, 10, 7, 10, 9, 8, 9, 6, 9, 7, 9, 8, 6, 8, 7, 8, 7, 6, 7, },
	};

	private final int[][] directionDumpX = {
		{ 1, -1, 1, -1, 0, },
		{ 1, -1, 0, -1, -1, },
		{ 0, 0, -1, -1, -1, },
		{ 1, -1, 0, -1, -1, },
		{ 1, -1, 1, -1, 0, },
		{ 1, -1, 0, 1, 1, },
		{ 0, 0, 1, 1, 1, },
		{ 1, -1, 1, 0, 1, },
	};

	private final int[][] directionDumpY = {
		{ 0, 0, -1, -1, -1, },
		{ -1, 1, -1, 0, -1, },
		{ 1, -1, -1, 1, 0, },
		{ 1, -1, 1, 0, 1, },
		{ 0, 0, 1, 1, 1, },
		{ -1, 1, 1, 0, 1, },
		{ 1, -1, 1, -1, 0, },
		{ 1, -1, 0, -1, -1, },
	};

	private final int[][] destinationDumpX = {
		{ 4, 8, 5, 7, 6, },
		{ 9, 10, 9, 11, 10, },
		{ 11, 11, 11, 11, 11, },
		{ 9, 10, 9, 11, 10, },
		{ 4, 8, 5, 7, 6, },
		{ 2, 3, 1, 3, 2, },
		{ 1, 1, 1, 1, 1, },
		{ 2, 3, 1, 3, 2, },
	};

	private final int[][] destinationDumpY = {
		{ 11, 11, 11, 11, 11, },
		{ 10, 9, 11, 9, 10, },
		{ 4, 8, 5, 7, 6, },
		{ 2, 3, 1, 3, 2, },
		{ 1, 1, 1, 1, 1, },
		{ 3, 2, 3, 1, 2, },
		{ 4, 8, 5, 7, 6, },
		{ 9, 10, 9, 11, 10, },
	};


	@Override
	public int[] getDestinationsX(int d) {
		return destinationDumpX[d];
	}

	@Override
	public int[] getDestinationsY(int d) {
		return destinationDumpY[d];
	}

	@Override
	public int[] getLocationsX(int d) {
		return locationDumpX[d];
	}

	@Override
	public int[] getLocationsY(int d) {
		return locationDumpY[d];
	}

	@Override
	public int[] getDirectionsX(int d) {
		return directionDumpX[d];
	}

	@Override
	public int[] getDirectionsY(int d) {
		return directionDumpY[d];
	}

}
