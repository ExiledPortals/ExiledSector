package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.ModSettings;
import exiledsector.ModSettingsListener;

import java.util.concurrent.atomic.AtomicReference;

public final class NpcBonusScale {

    public static final String LOW_TREE_POWER_FIELD_ID = "exiledSector_npcBonusScaleLowTreePower";
    public static final String HIGH_TREE_POWER_FIELD_ID = "exiledSector_npcBonusScaleHighTreePower";
    public static final String MAX_MULTIPLIER_FIELD_ID = "exiledSector_npcBonusScaleMaxMultiplier";

    public static final int DEFAULT_LOW_TREE_POWER = 15;
    public static final int DEFAULT_HIGH_TREE_POWER = 60;
    public static final float DEFAULT_MAX_MULTIPLIER = 3f;

    private static final float MULTIPLIER_STEPS_PER_UNIT = 100f;
    private static final float REFRESH_DAYS = 1f;
    private static final AtomicReference<Snapshot> SNAPSHOT = new AtomicReference<>();

    private record Snapshot(float multiplier, long timestamp) {
    }

    record Config(int lowTreePower, int highTreePower, float maxMultiplier) {

        static Config current() {
            return new Config(ModSettings.intOr(LOW_TREE_POWER_FIELD_ID, DEFAULT_LOW_TREE_POWER),
                    ModSettings.intOr(HIGH_TREE_POWER_FIELD_ID, DEFAULT_HIGH_TREE_POWER),
                    ModSettings.floatOr(MAX_MULTIPLIER_FIELD_ID, DEFAULT_MAX_MULTIPLIER));
        }
    }

    public static final class SettingsListener extends ModSettingsListener {

        @Override
        protected void reload() {
            invalidate();
            PlayerTreePower.logNpcScaling();
        }
    }

    private NpcBonusScale() {
    }

    public static float current() {
        SectorAPI sector = Global.getSector();
        if (sector == null || sector.getClock() == null) {
            return 1f;
        }
        Snapshot snapshot = SNAPSHOT.get();
        if (snapshot != null && sector.getClock().getElapsedDaysSince(snapshot.timestamp()) < REFRESH_DAYS) {
            return snapshot.multiplier();
        }
        float multiplier = rounded(multiplier(PlayerTreePower.average(PlayerTreePower.current()), Config.current()));
        SNAPSHOT.set(new Snapshot(multiplier, sector.getClock().getTimestamp()));
        return multiplier;
    }

    public static void invalidate() {
        SNAPSHOT.set(null);
        PlayerTreePower.invalidate();
    }

    static float multiplier(float averageTreePower, Config config) {
        if (config.maxMultiplier() <= 1f) {
            return 1f;
        }
        int powerSpan = config.highTreePower() - config.lowTreePower();
        float progress = powerSpan <= 0
                ? (averageTreePower >= config.highTreePower() ? 1f : 0f)
                : (averageTreePower - config.lowTreePower()) / powerSpan;
        float clampedProgress = Math.max(0f, Math.min(1f, progress));
        return 1f + clampedProgress * (config.maxMultiplier() - 1f);
    }

    private static float rounded(float multiplier) {
        return Math.round(multiplier * MULTIPLIER_STEPS_PER_UNIT) / MULTIPLIER_STEPS_PER_UNIT;
    }
}
