package gen5.common.generated;
	
	
public class Heuristics13 implements HeuristicProvider {

	private final int[][] locationDumpX = {
		{ 1, 5, 1, 5, 2, 4, 3, 2, 4, 3, 2, 4, 3, },
		{ 5, 6, 3, 6, 4, 6, 5, 3, 5, 4, 5, 4, 3, 4, },
		{ 6, 6, 5, 5, 6, 6, 6, 5, 5, 5, 4, 4, 4, },
		{ 5, 6, 3, 6, 4, 6, 5, 3, 5, 4, 5, 4, 3, 4, },
		{ 1, 5, 1, 5, 2, 4, 3, 2, 4, 3, 2, 4, 3, },
		{ 0, 1, 0, 3, 0, 2, 1, 1, 3, 1, 2, 2, 2, 3, },
		{ 0, 0, 1, 1, 0, 0, 0, 1, 1, 1, 2, 2, 2, },
		{ 0, 1, 0, 3, 0, 2, 1, 1, 3, 1, 2, 2, 2, 3, },
	};

	private final int[][] locationDumpY = {
		{ 6, 6, 5, 5, 6, 6, 6, 5, 5, 5, 4, 4, 4, },
		{ 6, 5, 6, 3, 6, 4, 5, 5, 3, 5, 4, 4, 4, 3, },
		{ 1, 5, 1, 5, 2, 4, 3, 2, 4, 3, 2, 4, 3, },
		{ 0, 1, 0, 3, 0, 2, 1, 1, 3, 1, 2, 2, 2, 3, },
		{ 0, 0, 1, 1, 0, 0, 0, 1, 1, 1, 2, 2, 2, },
		{ 1, 0, 3, 0, 2, 0, 1, 3, 1, 2, 1, 2, 3, 2, },
		{ 1, 5, 1, 5, 2, 4, 3, 2, 4, 3, 2, 4, 3, },
		{ 5, 6, 3, 6, 4, 6, 5, 3, 5, 4, 5, 4, 3, 4, },
	};

	private final int[][] directionDumpX = {
		{ 1, -1, 0, },
		{ 0, -1, -1, },
		{ -1, -1, -1, },
		{ 0, -1, -1, },
		{ 1, -1, 0, },
		{ 0, 1, 1, },
		{ 1, 1, 1, },
		{ 1, 0, 1, },
	};

	private final int[][] directionDumpY = {
		{ -1, -1, -1, },
		{ -1, 0, -1, },
		{ -1, 1, 0, },
		{ 1, 0, 1, },
		{ 1, 1, 1, },
		{ 1, 0, 1, },
		{ 1, -1, 0, },
		{ 0, -1, -1, },
	};

	private final int[][] destinationDumpX = {
		{ 1, 5, 2, 4, 3, },
		{ 4, 6, 5, 5, 6, },
		{ 6, 6, 6, 6, 6, },
		{ 4, 6, 5, 5, 6, },
		{ 1, 5, 2, 4, 3, },
		{ 0, 2, 1, 0, 1, },
		{ 0, 0, 0, 0, 0, },
		{ 0, 2, 1, 0, 1, },
	};

	private final int[][] destinationDumpY = {
		{ 6, 6, 6, 6, 6, },
		{ 6, 4, 5, 6, 5, },
		{ 1, 5, 2, 4, 3, },
		{ 0, 2, 1, 0, 1, },
		{ 0, 0, 0, 0, 0, },
		{ 2, 0, 1, 1, 0, },
		{ 1, 5, 2, 4, 3, },
		{ 4, 6, 5, 5, 6, },
	};

	@Override
	public int[][] getDestinationsX() {
		return destinationDumpX;
	}

	@Override
	public int[][] getDestinationsY() {
		return destinationDumpY;
	}

	@Override
	public int[][] getLocationsX() {
		return locationDumpX;
	}

	@Override
	public int[][] getLocationsY() {
		return locationDumpY;
	}

	@Override
	public int[][] getDirectionsX() {
		return directionDumpX;
	}

	@Override
	public int[][] getDirectionsY() {
		return directionDumpY;
	}
}
