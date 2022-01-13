package gen2.util;

import battlecode.common.MapLocation;

public class MetalInfo {
    public int amount;
    public MapLocation location;

    public MetalInfo(int amount, MapLocation loc) {
        this.amount = amount;
        location = loc;
    }

    @Override
    public String toString() {
        return "{" +
                "a=" + amount +
                ",l=" + location +
                '}';
    }
}
