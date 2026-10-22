package exiledsector.skills.npc;

import exiledsector.loot.FleetLevelRange;

import java.util.Random;

public record NpcNodeRange(int min, int max) {

    public NpcNodeRange {
        min = Math.max(0, min);
        max = Math.max(min, max);
    }

    public static NpcNodeRange of(FleetLevelRange fleetRange, int maxLevel, int nodeCap) {
        if (fleetRange == null) {
            return null;
        }
        int lowest = Math.max(0, Math.min(maxLevel, fleetRange.lowest()));
        int highest = Math.max(0, Math.min(maxLevel, fleetRange.highest()));
        return new NpcNodeRange(Math.min(nodeCap, lowest + fleetRange.extraLevels()), Math.min(nodeCap, highest + fleetRange.extraLevels()));
    }

    public int roll(Random random) {
        return min + random.nextInt(max - min + 1);
    }
}
