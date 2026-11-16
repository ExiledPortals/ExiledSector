package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.charts.content.ChartContentPlanner;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.anomaly.RaidDangerLevels;
import exiledsector.ProjectFiles;
import exiledsector.items.CraftingDrops;
import exiledsector.items.CraftingInhibitor;
import exiledsector.items.ItemDrops;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExcavationRulesTest {

    @BeforeAll
    static void loadDangerLevels() {
        RaidDangerLevels.load();
    }

    @Test
    void onlyFrigatesAndDestroyersFitTheShaft() {
        int max = ExcavationRules.MAX_SHIP_SIZE;
        assertTrue(ExcavationRules.fitsExcavation(HullSize.FRIGATE, max));
        assertTrue(ExcavationRules.fitsExcavation(HullSize.DESTROYER, max));
        assertFalse(ExcavationRules.fitsExcavation(HullSize.CRUISER, max));
        assertFalse(ExcavationRules.fitsExcavation(HullSize.CAPITAL_SHIP, max));
        assertFalse(ExcavationRules.fitsExcavation(HullSize.FIGHTER, max));
        assertTrue(ExcavationRules.holdsAbove(HullSize.CRUISER, max));
        assertTrue(ExcavationRules.holdsAbove(HullSize.CAPITAL_SHIP, max));
        assertFalse(ExcavationRules.holdsAbove(HullSize.DESTROYER, max));
        assertFalse(ExcavationRules.holdsAbove(HullSize.FIGHTER, max));
        assertEquals(2, ExcavationRules.MAX_SHIP_SIZE);
        assertEquals(max, ExcavationRules.maxShipSize(ExcavationTuning.NONE));
    }

    @Test
    void needleBoresLetOnlyFrigatesDownAndWidenedBoresLetCruisersDown() {
        ExcavationTuning needle = tuning(line(ChartModifier.CHART_EXCAVATION_FRIGATES_ONLY, 1f));
        ExcavationTuning widened = tuning(line(ChartModifier.CHART_EXCAVATION_CRUISERS_ALLOWED, 1f));
        ExcavationTuning both = tuning(line(ChartModifier.CHART_EXCAVATION_FRIGATES_ONLY, 1f), line(ChartModifier.CHART_EXCAVATION_CRUISERS_ALLOWED, 1f),
                line(ChartModifier.CHART_EXCAVATION_GUARDIAN_INHIBITORS, 2f), line(ChartModifier.CHART_EXCAVATION_GUARDIAN_INHIBITORS, 1f));

        assertEquals(1, ExcavationRules.maxShipSize(needle));
        assertEquals(3, ExcavationRules.maxShipSize(widened));
        assertEquals(1, ExcavationRules.maxShipSize(both));
        assertTrue(ExcavationRules.fitsExcavation(HullSize.FRIGATE, 1));
        assertFalse(ExcavationRules.fitsExcavation(HullSize.DESTROYER, 1));
        assertTrue(ExcavationRules.holdsAbove(HullSize.DESTROYER, 1));
        assertTrue(ExcavationRules.fitsExcavation(HullSize.CRUISER, 3));
        assertFalse(ExcavationRules.fitsExcavation(HullSize.CAPITAL_SHIP, 3));
        assertTrue(ExcavationRules.holdsAbove(HullSize.CAPITAL_SHIP, 3));
        assertEquals(4f, ExcavationRules.guardianInhibitors(ExcavationGuardians.FIRST_CAVERN, both), 0.0001f);
    }

    @Test
    void theShaftLinesPickWhichShaftsASiteHas() {
        ExcavationTuning open = tuning(line(ChartModifier.CHART_EXCAVATION_OPEN_SHAFTS, 1f));
        ExcavationTuning collapsed = tuning(line(ChartModifier.CHART_EXCAVATION_COLLAPSED_SHAFTS, 1f));
        ExcavationTuning openAndCollapsed = tuning(line(ChartModifier.CHART_EXCAVATION_OPEN_SHAFTS, 1f),
                line(ChartModifier.CHART_EXCAVATION_COLLAPSED_SHAFTS, 1f));
        ExcavationTuning twin = tuning(line(ChartModifier.CHART_EXCAVATION_TWIN_SHAFTS, 1f));
        Random random = new Random(3L);
        int rolledOpen = 0;
        for (int i = 0; i < 200; i++) {
            assertEquals(ExcavationSite.Access.OPEN, ExcavationRules.access(random, open));
            assertEquals(ExcavationSite.Access.COLLAPSED, ExcavationRules.access(random, collapsed));
            assertEquals(ExcavationSite.Access.BOTH, ExcavationRules.access(random, twin));
            rolledOpen += ExcavationRules.access(random, openAndCollapsed) == ExcavationSite.Access.OPEN ? 1 : 0;
        }
        assertTrue(rolledOpen > 60 && rolledOpen < 140, "open " + rolledOpen);
        assertEquals(ExcavationRules.access(new Random(9L), ExcavationTuning.NONE), ExcavationRules.access(new Random(9L), openAndCollapsed));
    }

    @Test
    void theInhibitorLinesJoinTheBaseBeforeQuantity() {
        ExcavationTuning tuning = tuning(line(ChartModifier.CHART_EXCAVATION_GUARDIAN_INHIBITORS, 1f),
                line(ChartModifier.CHART_EXCAVATION_RAID_INHIBITORS, 1f), line(ChartModifier.CHART_EXCAVATION_SECOND_CAVERN_INHIBITORS, 2f),
                line(ChartModifier.CHART_EXCAVATION_CORRUPTED_GUARDIAN_INHIBITORS, 1f), line(ChartModifier.CHART_EXCAVATION_OPEN_SHAFT_INHIBITOR_PERCENT, 50f),
                line(ChartModifier.CHART_EXCAVATION_INHIBITOR_PERCENT_PER_DUST, 30f));

        assertEquals(1f, ExcavationRules.guardianInhibitors(ExcavationGuardians.FIRST_CAVERN, ExcavationTuning.NONE), 0.0001f);
        assertEquals(2f, ExcavationRules.raidInhibitors(ExcavationTuning.NONE), 0.0001f);
        assertEquals(2f, ExcavationRules.guardianInhibitors(ExcavationGuardians.FIRST_CAVERN, tuning), 0.0001f);
        assertEquals(4f, ExcavationRules.guardianInhibitors(new ExcavationGuardians(true, false), tuning), 0.0001f);
        assertEquals(5f, ExcavationRules.guardianInhibitors(new ExcavationGuardians(true, true), tuning), 0.0001f);
        assertEquals(3f, ExcavationRules.raidInhibitors(tuning), 0.0001f);
        assertEquals(50f, ExcavationRules.inhibitorQuantityPercent(tuning, true, 0), 0.0001f);
        assertEquals(90f, ExcavationRules.inhibitorQuantityPercent(tuning, false, 3), 0.0001f);
        assertEquals(140f, ExcavationRules.inhibitorQuantityPercent(tuning, true, 3), 0.0001f);
        assertEquals(0f, ExcavationRules.inhibitorQuantityPercent(ExcavationTuning.NONE, true, 5), 0.0001f);
    }

    @Test
    void secondCavernAndCorruptedFleetsAreLarger() {
        ChartTier chartTier = ChartGenContext.none().adjust(ChartTiers.forTier(ChartTiers.LOWEST_TIER));
        ExcavationTuning tuning = tuning(line(ChartModifier.CHART_EXCAVATION_SECOND_CAVERN, 1f),
                line(ChartModifier.CHART_EXCAVATION_SECOND_CAVERN_FLEET_SIZE_MULT, 50f));
        float base = ExcavationRules.fleetSize(chartTier);

        assertTrue(tuning.secondCavern());
        assertEquals(base, ExcavationRules.guardianFleetSize(chartTier, ExcavationGuardians.FIRST_CAVERN, tuning), 0.0001f);
        assertEquals(base * 1.5f, ExcavationRules.guardianFleetSize(chartTier, new ExcavationGuardians(true, false), tuning), 0.0001f);
        assertEquals(base * 1.5f, ExcavationRules.guardianFleetSize(chartTier, ExcavationGuardians.FIRST_CAVERN.asCorrupted(), tuning), 0.0001f);
        assertEquals(base * 2.25f, ExcavationRules.guardianFleetSize(chartTier, new ExcavationGuardians(true, true), tuning), 0.0001f);
        assertEquals(ExcavationRules.strengthBonus(base, false) + ExcavationRules.CORRUPTED_STRENGTH_BONUS_PERCENT,
                ExcavationRules.strengthBonus(base, true));
    }

    @Test
    void theTuningReadsEveryLine() {
        ExcavationTuning tuning = tuning(line(ChartModifier.CHART_EXCAVATION_SIGHT_RADIUS_PERCENT, -70f),
                line(ChartModifier.CHART_EXCAVATION_CORRUPTIBLE_GUARDIANS, 1f), line(ChartModifier.CHART_EXCAVATION_TWIN_SHAFTS, 1f));

        assertEquals(-70f, tuning.sightRadiusPercent(), 0.0001f);
        assertTrue(tuning.corruptibleGuardians() && tuning.twinShafts());
        assertFalse(tuning.openShafts() || tuning.collapsedShafts() || tuning.secondCavern() || tuning.frigatesOnly() || tuning.cruisersAllowed());
        assertEquals(ExcavationTuning.NONE, ExcavationTuning.of(null));
        assertEquals(ExcavationTuning.NONE, ExcavationTuning.ofRun(null));
        assertFalse(ExcavationTuning.NONE.openShafts());
        assertEquals(0f, ExcavationTuning.NONE.sightRadiusPercent(), 0.0001f);
    }

    private static ChartGenContext.AppliedEffect line(ChartModifier modifier, float magnitude) {
        return new ChartGenContext.AppliedEffect(modifier, magnitude);
    }

    private static ExcavationTuning tuning(ChartGenContext.AppliedEffect... lines) {
        return ExcavationTuning.of(ChartGenContext.of(List.of(lines)));
    }

    @Test
    void theRaidShareGrowsWithEffectiveness() {
        assertEquals(ExcavationRules.MIN_RAID_SHARE, ExcavationRules.raidShare(0f), 0.0001f);
        assertEquals(0.5f / ExcavationRules.FULL_LOOT_EFFECTIVENESS, ExcavationRules.raidShare(0.5f), 0.0001f);
        assertEquals(1f, ExcavationRules.raidShare(ExcavationRules.FULL_LOOT_EFFECTIVENESS), 0.0001f);
        assertEquals(1f, ExcavationRules.raidShare(1f), 0.0001f);
    }

    @Test
    void theTuningLinesAddSitesAndRaiseTheSiteQuantity() {
        ChartGenContext context = ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartModifier.CHART_EXTRA_EXCAVATION_SITES, 2f),
                new ChartGenContext.AppliedEffect(ChartModifier.CHART_EXTRA_EXCAVATION_SITES, 1f),
                new ChartGenContext.AppliedEffect(ChartModifier.CHART_EXCAVATION_LOOT_QUANTITY_PERCENT, 50f)));

        assertEquals(3, Math.round(context.anomalyPercent(ExcavationSites.EXTRA_KEY)));
        assertEquals(1.5f, ExcavationLoot.quantityMult(context), 0.0001f);
        assertEquals(1f, ExcavationLoot.quantityMult(ChartGenContext.none()), 0.0001f);
        assertEquals(1f, ExcavationLoot.quantityMult(null), 0.0001f);
    }

    @Test
    void shaftsDropTagInhibitorsScaledByTheirShareAndTheSiteQuantity() throws Exception {
        ChartGenContext context = ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartModifier.CHART_EXCAVATION_LOOT_QUANTITY_PERCENT, 100f)));
        float perShaft = ExcavationRules.INHIBITORS_PER_RAID;
        assertEquals(2f, perShaft, 0.0001f);
        assertEquals(1f, ExcavationRules.INHIBITORS_PER_GUARDIAN_FLEET, 0.0001f);
        assertEquals(perShaft, ExcavationLoot.expectedInhibitors(ExcavationRules.INHIBITORS_PER_RAID, context, 0.5f), 0.0001f);
        assertEquals(perShaft * 0.5f, ExcavationLoot.expectedInhibitors(ExcavationRules.INHIBITORS_PER_RAID, ChartGenContext.none(), 0.5f), 0.0001f);
        assertEquals(0f, ExcavationLoot.expectedInhibitors(ExcavationRules.INHIBITORS_PER_RAID, context, -1f), 0.0001f);
        CraftingDrops.register(ProjectFiles.csvRows(CraftingDrops.DATA_PATH));
        try {
            Map<String, Integer> found = ExcavationLoot.tagInhibitors(40f, CraftingDrops.NO_BAND_SHIFT, new Random(1L));
            Set<String> tagInhibitorIds = CraftingInhibitor.tagInhibitors().stream().map(CraftingInhibitor::itemId).collect(Collectors.toSet());
            assertEquals(40, found.values().stream().mapToInt(Integer::intValue).sum());
            assertTrue(tagInhibitorIds.containsAll(found.keySet()) && found.size() > 1, found.toString());
            assertTrue(ExcavationLoot.tagInhibitors(0f, CraftingDrops.NO_BAND_SHIFT, new Random(1L)).isEmpty());
            assertNull(ExcavationLoot.fleetItems(mock(CampaignFleetAPI.class), context, ChartTiers.LOWEST_TIER, 0, 1f, new Random(1L)));

            CampaignFleetAPI guardians = mock(CampaignFleetAPI.class);
            when(guardians.hasTag(ExcavationFleets.FLEET_TAG)).thenReturn(true);
            ItemDrops.SiteLoot loot = ExcavationLoot.fleetItems(guardians, context, ChartTiers.LOWEST_TIER, 0, 1f, new Random(1L));
            assertTrue(loot.items().isEmpty() && loot.upgrades().isEmpty());
            assertEquals(2, inhibitorCount(loot));
            assertNull(ExcavationLoot.fleetItems(guardians, context, ChartTiers.LOWEST_TIER, 0, 0f, new Random(1L)));

            ChartGenContext settled = ChartGenContext.of(List.of(line(ChartModifier.CHART_EXCAVATION_INHIBITOR_PERCENT_PER_DUST, 50f),
                    line(ChartModifier.CHART_EXCAVATION_OPEN_SHAFT_INHIBITOR_PERCENT, 100f)));
            assertEquals(3, inhibitorCount(ExcavationLoot.fleetItems(guardians, settled, ChartTiers.LOWEST_TIER, 2, 1f, new Random(1L))));
        } finally {
            CraftingDrops.clear();
        }
    }

    @Test
    void theRunsCraftingItemQuantityScalesTheSiteQuantity() {
        ChartGenContext context = ChartGenContext.of(List.of(line(ChartModifier.CHART_EXCAVATION_LOOT_QUANTITY_PERCENT, 50f),
                line(ChartModifier.CHART_CRAFTING_ITEM_PERCENT, 100f)));

        assertEquals(3f, ExcavationLoot.quantityMult(context), 0.0001f);
        assertEquals(4f, ExcavationLoot.quantityMult(context, 50f), 0.0001f);
        assertEquals(6f, ExcavationLoot.expectedInhibitors(ExcavationRules.INHIBITORS_PER_RAID, context, 1f), 0.0001f);
    }

    private static int inhibitorCount(ItemDrops.SiteLoot loot) {
        return loot.materials().values().stream().mapToInt(Integer::intValue).sum();
    }

    @Test
    void aShaftFleetIsMostOfTheBossSizeOnEveryTier() {
        for (int tier = ChartTiers.LOWEST_TIER; tier <= ChartTiers.HIGHEST_TIER; tier++) {
            ChartTier chartTier = ChartGenContext.none().adjust(ChartTiers.forTier(tier));
            float bossSize = Math.max(chartTier.bossSize(), chartTier.fleetSize());

            assertEquals(bossSize * ExcavationRules.BOSS_SIZE_SHARE, ExcavationRules.fleetSize(chartTier), 0.0001f, "tier " + tier);
            assertEquals(ChartContentPlanner.overflowStrength(bossSize * ExcavationRules.BOSS_SIZE_SHARE, ChartContentPlanner.BOSS_SIZE_CAP),
                    ExcavationRules.strengthBonus(chartTier), "tier " + tier);
        }
        assertEquals(0.8f, ExcavationRules.BOSS_SIZE_SHARE, 0.0001f);
    }
}
