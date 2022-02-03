package gen8.common.bellmanford.heuristics;


import gen8.common.bellmanford.HeuristicsProvider;

public class Heuristics13 implements HeuristicsProvider {

	private final int[][] locationDumpX = {
		{ 2, 6, 2, 6, 3, 5, 4, 3, 5, 4, 3, 5, 4, },
		{ 6, 7, 4, 7, 5, 7, 6, 4, 6, 5, 6, 5, 4, 5, },
		{ 7, 7, 6, 6, 7, 7, 7, 6, 6, 6, 5, 5, 5, },
		{ 6, 7, 4, 7, 5, 7, 6, 4, 6, 5, 6, 5, 4, 5, },
		{ 2, 6, 2, 6, 3, 5, 4, 3, 5, 4, 3, 5, 4, },
		{ 1, 2, 1, 4, 1, 3, 2, 2, 4, 2, 3, 3, 3, 4, },
		{ 1, 1, 2, 2, 1, 1, 1, 2, 2, 2, 3, 3, 3, },
		{ 1, 2, 1, 4, 1, 3, 2, 2, 4, 2, 3, 3, 3, 4, },
	};

	private final int[][] locationDumpY = {
		{ 7, 7, 6, 6, 7, 7, 7, 6, 6, 6, 5, 5, 5, },
		{ 7, 6, 7, 4, 7, 5, 6, 6, 4, 6, 5, 5, 5, 4, },
		{ 2, 6, 2, 6, 3, 5, 4, 3, 5, 4, 3, 5, 4, },
		{ 1, 2, 1, 4, 1, 3, 2, 2, 4, 2, 3, 3, 3, 4, },
		{ 1, 1, 2, 2, 1, 1, 1, 2, 2, 2, 3, 3, 3, },
		{ 2, 1, 4, 1, 3, 1, 2, 4, 2, 3, 2, 3, 4, 3, },
		{ 2, 6, 2, 6, 3, 5, 4, 3, 5, 4, 3, 5, 4, },
		{ 6, 7, 4, 7, 5, 7, 6, 4, 6, 5, 6, 5, 4, 5, },
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
		{ 2, 6, 3, 5, 4, },
		{ 5, 7, 6, 6, 7, },
		{ 7, 7, 7, 7, 7, },
		{ 5, 7, 6, 6, 7, },
		{ 2, 6, 3, 5, 4, },
		{ 1, 3, 2, 1, 2, },
		{ 1, 1, 1, 1, 1, },
		{ 1, 3, 2, 1, 2, },
	};

	private final int[][] destinationDumpY = {
		{ 7, 7, 7, 7, 7, },
		{ 7, 5, 6, 7, 6, },
		{ 2, 6, 3, 5, 4, },
		{ 1, 3, 2, 1, 2, },
		{ 1, 1, 1, 1, 1, },
		{ 3, 1, 2, 2, 1, },
		{ 2, 6, 3, 5, 4, },
		{ 5, 7, 6, 6, 7, },
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
