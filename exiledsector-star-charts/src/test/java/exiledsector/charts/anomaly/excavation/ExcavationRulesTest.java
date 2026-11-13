package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.charts.content.ChartContentPlanner;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.anomaly.RaidDangerLevels;
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
        assertTrue(ExcavationRules.fitsExcavation(HullSize.FRIGATE));
        assertTrue(ExcavationRules.fitsExcavation(HullSize.DESTROYER));
        assertFalse(ExcavationRules.fitsExcavation(HullSize.CRUISER));
        assertFalse(ExcavationRules.fitsExcavation(HullSize.CAPITAL_SHIP));
        assertTrue(ExcavationRules.holdsAbove(HullSize.CRUISER));
        assertTrue(ExcavationRules.holdsAbove(HullSize.CAPITAL_SHIP));
        assertFalse(ExcavationRules.holdsAbove(HullSize.DESTROYER));
        assertFalse(ExcavationRules.holdsAbove(HullSize.FIGHTER));
        assertEquals(2, ExcavationRules.MAX_SHIP_SIZE);
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
    void shaftsDropTagInhibitorsScaledByTheirShareAndTheSiteQuantity() {
        ChartGenContext context = ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartModifier.CHART_EXCAVATION_LOOT_QUANTITY_PERCENT, 100f)));
        float perShaft = ExcavationRules.INHIBITORS_PER_RAID;
        assertEquals(2f, perShaft, 0.0001f);
        assertEquals(1f, ExcavationRules.INHIBITORS_PER_GUARDIAN_FLEET, 0.0001f);
        assertEquals(perShaft, ExcavationLoot.expectedInhibitors(ExcavationRules.INHIBITORS_PER_RAID, context, 0.5f), 0.0001f);
        assertEquals(perShaft * 0.5f, ExcavationLoot.expectedInhibitors(ExcavationRules.INHIBITORS_PER_RAID, ChartGenContext.none(), 0.5f), 0.0001f);
        assertEquals(0f, ExcavationLoot.expectedInhibitors(ExcavationRules.INHIBITORS_PER_RAID, context, -1f), 0.0001f);
        Map<String, Integer> found = ExcavationLoot.tagInhibitors(40f, new Random(1L));
        Set<String> tagInhibitorIds = CraftingInhibitor.tagInhibitors().stream().map(CraftingInhibitor::itemId).collect(Collectors.toSet());
        assertEquals(40, found.values().stream().mapToInt(Integer::intValue).sum());
        assertTrue(tagInhibitorIds.containsAll(found.keySet()) && found.size() > 1, found.toString());
        assertTrue(ExcavationLoot.tagInhibitors(0f, new Random(1L)).isEmpty());
        assertNull(ExcavationLoot.fleetItems(mock(CampaignFleetAPI.class), context, 1f, new Random(1L)));

        CampaignFleetAPI guardians = mock(CampaignFleetAPI.class);
        when(guardians.hasTag(ExcavationFleets.FLEET_TAG)).thenReturn(true);
        ItemDrops.SiteLoot loot = ExcavationLoot.fleetItems(guardians, context, 1f, new Random(1L));
        assertTrue(loot.items().isEmpty() && loot.upgrades().isEmpty());
        assertEquals(2, loot.materials().values().stream().mapToInt(Integer::intValue).sum());
        assertNull(ExcavationLoot.fleetItems(guardians, context, 0f, new Random(1L)));
    }

    @Test
    void aShaftFleetIsMostOfTheBossSizeOnEveryTier() {
        for (int tier = ChartTiers.LOWEST_TIER; tier <= ChartTiers.CORRUPTED_TIER; tier++) {
            ChartTier chartTier = ChartGenContext.none().adjust(ChartTiers.forTier(tier));
            float bossSize = Math.max(chartTier.bossSize(), chartTier.fleetSize());

            assertEquals(bossSize * ExcavationRules.BOSS_SIZE_SHARE, ExcavationRules.fleetSize(chartTier), 0.0001f, "tier " + tier);
            assertEquals(ChartContentPlanner.overflowStrength(bossSize * ExcavationRules.BOSS_SIZE_SHARE, ChartContentPlanner.BOSS_SIZE_CAP),
                    ExcavationRules.strengthBonus(chartTier), "tier " + tier);
        }
        assertEquals(0.8f, ExcavationRules.BOSS_SIZE_SHARE, 0.0001f);
    }
}
