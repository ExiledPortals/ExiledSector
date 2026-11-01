package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.ExiledSector;
import exiledsector.skills.ShipSkillData;
import exiledsector.ui.CampaignMessages;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class HullUpgradePoints {

    public static final int STARTING_POINTS = 1;
    public static final int MOST_POINTS = 5;
    static final List<Integer> CHART_TIER_STEPS = List.of(5, 10, 15);
    static final int NO_NEXT_CHART_TIER = 0;
    static final ItemUnlock.ChartClearListener CHART_CLEAR_LISTENER = HullUpgradePoints::chartTierRaised;
    private static final AtomicInteger REVISION = new AtomicInteger();

    private HullUpgradePoints() {
    }

    public static void install() {
        RefitSchematic.dropOnlyWhen(HullUpgradePoints::schematicsInUse);
        ItemUnlock.addChartClearListener(CHART_CLEAR_LISTENER);
    }

    public static int of(ShipSkillData shipData) {
        if (shipData == null || shipData.isNpcBuild()) {
            return 0;
        }
        return schematicsInUse() ? schematicPoints(shipData.getStoredHullUpgradePoints()) : chartPoints(Global.getSector());
    }

    public static int unspent(ShipSkillData shipData) {
        return shipData == null ? 0 : Math.max(0, of(shipData) - shipData.getUnlockedSocketTypeIds().size());
    }

    public static boolean schematicsInUse() {
        return HullUpgradesSetting.pointsMode() == HullUpgradesSetting.PointsMode.REFIT_SCHEMATICS;
    }

    public static int maxPoints() {
        return schematicsInUse() ? MOST_POINTS : STARTING_POINTS + CHART_TIER_STEPS.size();
    }

    public static int schematicPoints(int storedPoints) {
        return Math.max(STARTING_POINTS, storedPoints);
    }

    public static int chartPoints(int clearedTier) {
        int points = STARTING_POINTS;
        for (int tierStep : CHART_TIER_STEPS) {
            if (clearedTier >= tierStep) {
                points++;
            }
        }
        return points;
    }

    public static int chartPoints(SectorAPI sector) {
        return chartsEnabled() ? chartPoints(ItemUnlock.highestClearedChartTier(sector)) : STARTING_POINTS;
    }

    public static int nextChartTier(int clearedTier) {
        for (int tierStep : CHART_TIER_STEPS) {
            if (clearedTier < tierStep) {
                return tierStep;
            }
        }
        return NO_NEXT_CHART_TIER;
    }

    public static int nextChartTier(SectorAPI sector) {
        return nextChartTier(ItemUnlock.highestClearedChartTier(sector));
    }

    public static boolean chartsEnabled() {
        return ExiledSector.domainEnabledFor(ItemType.STAR_CHART);
    }

    public static int revision() {
        return REVISION.get();
    }

    static void markChanged() {
        REVISION.incrementAndGet();
    }

    static void chartTierRaised(SectorAPI sector, int previousTier, int clearedTier) {
        markChanged();
        if (schematicsInUse() || !chartsEnabled()) {
            return;
        }
        int raisedPoints = chartPoints(clearedTier);
        if (raisedPoints > chartPoints(previousTier) && CampaignMessages.available()) {
            CampaignMessages.plain(I18n.forGameText(() -> Translation.msg("points.chartRaised").arg("points", raisedPoints).text()),
                    Misc.getPositiveHighlightColor());
        }
    }
}
