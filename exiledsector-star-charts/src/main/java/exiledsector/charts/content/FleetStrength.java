package exiledsector.charts.content;

import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import exiledsector.charts.generation.ChartTier;

import java.util.Random;

public record FleetStrength(float combatPoints, float quality, int officerLevelBonus, int officerCountBonus, int averageSMods) {

    public static FleetStrength regular(ChartTier tier, float sizePercent, float sizeCap) {
        float qualityPercent = ChartContentPlanner.fleetQuality(tier);
        return new FleetStrength(ChartTier.fleetPoints(Math.min(sizeCap, sizePercent)), qualityPercent / ChartTier.PERCENT,
                tier.officerLevelBonus(), tier.officerCountBonus(), ChartContentPlanner.averageSMods(qualityPercent));
    }

    public static FleetStrength boss(ChartTier tier, float sizePercent) {
        float qualityPercent = ChartContentPlanner.bossQuality(ChartContentPlanner.fleetQuality(tier));
        return new FleetStrength(ChartTier.fleetPoints(Math.min(ChartContentPlanner.BOSS_SIZE_CAP, sizePercent)), qualityPercent / ChartTier.PERCENT,
                tier.officerLevelBonus() + ChartContentPlanner.BOSS_OFFICER_LEVEL_BONUS,
                tier.officerCountBonus() + ChartContentPlanner.BOSS_OFFICER_COUNT_BONUS, ChartContentPlanner.averageSMods(qualityPercent));
    }

    public FleetStrength atLeast(float minCombatPoints) {
        return combatPoints >= minCombatPoints ? this : new FleetStrength(minCombatPoints, quality, officerLevelBonus, officerCountBonus, averageSMods);
    }

    FleetParamsV3 toParams(String factionId, String fleetType, Random random) {
        FleetParamsV3 params = new FleetParamsV3(null, factionId, quality, fleetType, combatPoints, 0f, 0f, 0f, 0f, 0f, 0f);
        params.random = random;
        params.ignoreMarketFleetSizeMult = true;
        params.doNotPrune = true;
        params.officerLevelBonus = officerLevelBonus;
        params.officerNumberBonus = officerCountBonus;
        params.averageSMods = averageSMods > 0 ? averageSMods : null;
        return params;
    }
}
