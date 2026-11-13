package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.charts.content.ChartContentPlanner;
import exiledsector.charts.generation.ChartTier;

import java.util.Random;

public final class ExcavationRules {

    public static final int MAX_SHIP_SIZE = 2;
    static final int MIN_SITES = 1;
    static final int MAX_SITES = 3;
    static final int MIN_PLANETS = 3;
    static final float OPEN_SHAFT_CHANCE = 0.5f;
    static final float DERELICT_CHANCE = 0.3f;
    static final float BOSS_SIZE_SHARE = 0.8f;
    static final float FULL_LOOT_EFFECTIVENESS = 0.75f;
    static final float MIN_RAID_SHARE = 0.25f;
    static final float INHIBITORS_PER_RAID = 2f;
    static final float INHIBITORS_PER_GUARDIAN_FLEET = 1f;

    private ExcavationRules() {
    }

    public static int siteCount(Random random, int extraSites) {
        return MIN_SITES + random.nextInt(MAX_SITES - MIN_SITES + 1) + Math.max(0, extraSites);
    }

    public static ExcavationSite.Access access(Random random) {
        return random.nextFloat() < OPEN_SHAFT_CHANCE ? ExcavationSite.Access.OPEN : ExcavationSite.Access.COLLAPSED;
    }

    static float fleetSize(ChartTier adjustedTier) {
        return Math.max(adjustedTier.bossSize(), adjustedTier.fleetSize()) * BOSS_SIZE_SHARE;
    }

    static int strengthBonus(ChartTier adjustedTier) {
        return ChartContentPlanner.overflowStrength(fleetSize(adjustedTier), ChartContentPlanner.BOSS_SIZE_CAP);
    }

    public static float raidShare(float raidEffectiveness) {
        float share = raidEffectiveness / FULL_LOOT_EFFECTIVENESS;
        return Math.max(MIN_RAID_SHARE, Math.min(1f, share));
    }

    public static boolean fitsExcavation(HullSize size) {
        return size == HullSize.FRIGATE || size == HullSize.DESTROYER;
    }

    public static boolean holdsAbove(HullSize size) {
        return size == HullSize.CRUISER || size == HullSize.CAPITAL_SHIP;
    }
}
