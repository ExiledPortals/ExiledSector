package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.charts.content.ChartContentPlanner;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;

import java.util.Random;

public final class ExcavationRules {

    public static final int MAX_SHIP_SIZE = 2;
    public static final int FRIGATES_ONLY_SHIP_SIZE = 1;
    public static final int WIDENED_SHIP_SIZE = 3;
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
    static final float CORRUPTED_SIZE_MULT = 1.5f;
    static final int CORRUPTED_STRENGTH_BONUS_PERCENT = 20;
    static final int CORRUPTION_COST = 1;

    private ExcavationRules() {
    }

    public static int siteCount(Random random, int extraSites) {
        return MIN_SITES + random.nextInt(MAX_SITES - MIN_SITES + 1) + Math.max(0, extraSites);
    }

    public static ExcavationSite.Access access(Random random, ExcavationTuning tuning) {
        ExcavationSite.Access rolled = random.nextFloat() < OPEN_SHAFT_CHANCE ? ExcavationSite.Access.OPEN : ExcavationSite.Access.COLLAPSED;
        if (tuning.twinShafts()) {
            return ExcavationSite.Access.BOTH;
        }
        if (tuning.openShafts() != tuning.collapsedShafts()) {
            return tuning.openShafts() ? ExcavationSite.Access.OPEN : ExcavationSite.Access.COLLAPSED;
        }
        return rolled;
    }

    public static int maxShipSize(ExcavationTuning tuning) {
        if (tuning.frigatesOnly()) {
            return FRIGATES_ONLY_SHIP_SIZE;
        }
        return tuning.cruisersAllowed() ? WIDENED_SHIP_SIZE : MAX_SHIP_SIZE;
    }

    static float fleetSize(ChartTier adjustedTier) {
        return Math.max(adjustedTier.bossSize(), adjustedTier.fleetSize()) * BOSS_SIZE_SHARE;
    }

    static float guardianFleetSize(ChartTier adjustedTier, ExcavationGuardians guardians, ExcavationTuning tuning) {
        float size = fleetSize(adjustedTier);
        if (guardians.secondCavern()) {
            size *= multiplier(tuning.secondCavernFleetSizePercent());
        }
        return guardians.corrupted() ? size * CORRUPTED_SIZE_MULT : size;
    }

    static int strengthBonus(ChartTier adjustedTier) {
        return strengthBonus(fleetSize(adjustedTier), false);
    }

    static int strengthBonus(float fleetSize, boolean corrupted) {
        return ChartContentPlanner.overflowStrength(fleetSize, ChartContentPlanner.BOSS_SIZE_CAP) + (corrupted ? CORRUPTED_STRENGTH_BONUS_PERCENT : 0);
    }

    static float guardianInhibitors(ExcavationGuardians guardians, ExcavationTuning tuning) {
        return INHIBITORS_PER_GUARDIAN_FLEET + tuning.guardianInhibitors() + (guardians.secondCavern() ? tuning.secondCavernInhibitors() : 0)
                + (guardians.corrupted() ? tuning.corruptedGuardianInhibitors() : 0);
    }

    static float raidInhibitors(ExcavationTuning tuning) {
        return INHIBITORS_PER_RAID + tuning.raidInhibitors();
    }

    static float inhibitorQuantityPercent(ExcavationTuning tuning, boolean openShaft, int dustStacks) {
        return (openShaft ? tuning.openShaftInhibitorPercent() : 0f) + Math.max(0, dustStacks) * tuning.inhibitorPercentPerDust();
    }

    static float multiplier(float percent) {
        return Math.max(0f, 1f + percent / ChartGenContext.PERCENT);
    }

    public static float raidShare(float raidEffectiveness) {
        float share = raidEffectiveness / FULL_LOOT_EFFECTIVENESS;
        return Math.max(MIN_RAID_SHARE, Math.min(1f, share));
    }

    public static int shipSize(HullSize size) {
        if (size == null) {
            return 0;
        }
        return switch (size) {
            case FRIGATE -> 1;
            case DESTROYER -> 2;
            case CRUISER -> 3;
            case CAPITAL_SHIP -> 4;
            default -> 0;
        };
    }

    public static boolean fitsExcavation(HullSize size, int maxShipSize) {
        int shipSize = shipSize(size);
        return shipSize > 0 && shipSize <= maxShipSize;
    }

    public static boolean holdsAbove(HullSize size, int maxShipSize) {
        return shipSize(size) > maxShipSize;
    }
}
