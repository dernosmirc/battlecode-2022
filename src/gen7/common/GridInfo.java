package gen7.common;

import battlecode.common.MapLocation;

public class GridInfo {
    public int count;
    public MapLocation location;

    public GridInfo(int amount, MapLocation loc) {
        this.count = amount;
        this.location = loc;
    }

    public GridInfo(MapLocation loc, int amount) {
        this.count = amount;
        this.location = loc;
    }

    public int getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "{" +
                "a=" + count +
                ",l=" + location +
                '}';
    }
}
