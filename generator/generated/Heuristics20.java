package gen8.common.bellmanford;


public class Heuristics20 implements HeuristicsProvider {

	private final int[][] locationDumpX = {
		{ 2, 8, 3, 7, 4, 6, 5, 3, 7, 3, 7, 4, 6, 5, 4, 6, 5, 4, 6, 5, },
		{ 5, 9, 7, 9, 6, 9, 8, 7, 8, 5, 8, 6, 8, 7, 5, 7, 6, 7, 6, 5, 6, },
		{ 8, 8, 9, 9, 9, 9, 9, 8, 8, 7, 7, 8, 8, 8, 7, 7, 7, 6, 6, 6, },
		{ 5, 9, 7, 9, 6, 9, 8, 7, 8, 5, 8, 6, 8, 7, 5, 7, 6, 7, 6, 5, 6, },
		{ 2, 8, 3, 7, 4, 6, 5, 3, 7, 3, 7, 4, 6, 5, 4, 6, 5, 4, 6, 5, },
		{ 1, 5, 1, 3, 1, 4, 2, 2, 3, 2, 5, 2, 4, 3, 3, 5, 3, 4, 4, 4, 5, },
		{ 2, 2, 1, 1, 1, 1, 1, 2, 2, 3, 3, 2, 2, 2, 3, 3, 3, 4, 4, 4, },
		{ 1, 5, 1, 3, 1, 4, 2, 2, 3, 2, 5, 2, 4, 3, 3, 5, 3, 4, 4, 4, 5, },
	};

	private final int[][] locationDumpY = {
		{ 8, 8, 9, 9, 9, 9, 9, 8, 8, 7, 7, 8, 8, 8, 7, 7, 7, 6, 6, 6, },
		{ 9, 5, 9, 7, 9, 6, 8, 8, 7, 8, 5, 8, 6, 7, 7, 5, 7, 6, 6, 6, 5, },
		{ 2, 8, 3, 7, 4, 6, 5, 3, 7, 3, 7, 4, 6, 5, 4, 6, 5, 4, 6, 5, },
		{ 1, 5, 1, 3, 1, 4, 2, 2, 3, 2, 5, 2, 4, 3, 3, 5, 3, 4, 4, 4, 5, },
		{ 2, 2, 1, 1, 1, 1, 1, 2, 2, 3, 3, 2, 2, 2, 3, 3, 3, 4, 4, 4, },
		{ 5, 1, 3, 1, 4, 1, 2, 3, 2, 5, 2, 4, 2, 3, 5, 3, 4, 3, 4, 5, 4, },
		{ 2, 8, 3, 7, 4, 6, 5, 3, 7, 3, 7, 4, 6, 5, 4, 6, 5, 4, 6, 5, },
		{ 5, 9, 7, 9, 6, 9, 8, 7, 8, 5, 8, 6, 8, 7, 5, 7, 6, 7, 6, 5, 6, },
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
		{ 3, 7, 4, 6, 5, },
		{ 7, 8, 7, 9, 8, },
		{ 9, 9, 9, 9, 9, },
		{ 7, 8, 7, 9, 8, },
		{ 3, 7, 4, 6, 5, },
		{ 2, 3, 1, 3, 2, },
		{ 1, 1, 1, 1, 1, },
		{ 2, 3, 1, 3, 2, },
	};

	private final int[][] destinationDumpY = {
		{ 9, 9, 9, 9, 9, },
		{ 8, 7, 9, 7, 8, },
		{ 3, 7, 4, 6, 5, },
		{ 2, 3, 1, 3, 2, },
		{ 1, 1, 1, 1, 1, },
		{ 3, 2, 3, 1, 2, },
		{ 3, 7, 4, 6, 5, },
		{ 7, 8, 7, 9, 8, },
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
