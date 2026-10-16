package exiledsector.skills.npc;

import exiledsector.ModSettings;

import java.util.List;
import java.util.Random;

public final class NpcTreeSize {

    public static final String MIN_PERCENT_FIELD_ID = "exiledSector_npcTreeSizeMinPercent";
    public static final String MAX_PERCENT_FIELD_ID = "exiledSector_npcTreeSizeMaxPercent";
    public static final int DEFAULT_MIN_PERCENT = 80;
    public static final int DEFAULT_MAX_PERCENT = 120;
    public static final int LOWEST_PERCENT = 0;
    public static final int HIGHEST_PERCENT = 300;

    public record PercentRange(int min, int max) {
    }

    private NpcTreeSize() {
    }

    public static PercentRange percentRange() {
        int configuredMin = clamp(ModSettings.intOr(MIN_PERCENT_FIELD_ID, DEFAULT_MIN_PERCENT));
        int configuredMax = clamp(ModSettings.intOr(MAX_PERCENT_FIELD_ID, DEFAULT_MAX_PERCENT));
        return new PercentRange(Math.min(configuredMin, configuredMax), Math.max(configuredMin, configuredMax));
    }

    public static int roll(List<Integer> playerShipPowers, Random random) {
        return roll(playerShipPowers, percentRange(), random);
    }

    static int roll(List<Integer> playerShipPowers, PercentRange percentRange, Random random) {
        if (playerShipPowers.isEmpty()) {
            return 0;
        }
        int mirroredPower = playerShipPowers.get(random.nextInt(playerShipPowers.size()));
        int percent = percentRange.min() + random.nextInt(percentRange.max() - percentRange.min() + 1);
        return nodesFor(mirroredPower, percent);
    }

    public static int nodesFor(int shipPower, int percent) {
        return Math.round(shipPower * percent / 100f);
    }

    private static int clamp(int percent) {
        return Math.max(LOWEST_PERCENT, Math.min(HIGHEST_PERCENT, percent));
    }
}
