package gen6.common.bellmanford;

public interface HeuristicsProvider {
    int[] getDestinationsX(int d);
    int[] getDestinationsY(int d);

    int[] getLocationsX(int d);
    int[] getLocationsY(int d);

    int[] getDirectionsX(int d);
    int[] getDirectionsY(int d);
}
