package gen3_1.util;

import battlecode.common.MapLocation;

public class MetalInfo {
    public int amount;
    public MapLocation location;

    public MetalInfo(int amount, MapLocation loc) {
        this.amount = amount;
        this.location = loc;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "{" +
                "a=" + amount +
                ",l=" + location +
                '}';
    }
}
