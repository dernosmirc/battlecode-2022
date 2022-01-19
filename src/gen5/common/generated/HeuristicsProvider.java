package gen5.common.generated;

public interface HeuristicsProvider {
    int[] getDestinationsX(int d);
    int[] getDestinationsY(int d);

    int[] getLocationsX(int d);
    int[] getLocationsY(int d);

    int[] getDirectionsX(int d);
    int[] getDirectionsY(int d);
}
