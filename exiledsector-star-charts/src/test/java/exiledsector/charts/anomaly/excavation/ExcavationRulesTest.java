package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.loot.DropType;
import exiledsector.charts.anomaly.RaidDangerLevels;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcavationRulesTest {

    @BeforeAll
    static void loadDangerLevels() {
        RaidDangerLevels.load();
    }

    @Test
    void onlyFrigatesAndDestroyersFitTheShaft() {
        assertTrue(ExcavationRules.fitsShaft(HullSize.FRIGATE));
        assertTrue(ExcavationRules.fitsShaft(HullSize.DESTROYER));
        assertFalse(ExcavationRules.fitsShaft(HullSize.CRUISER));
        assertFalse(ExcavationRules.fitsShaft(HullSize.CAPITAL_SHIP));
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
    void theSiteBudgetGrowsWithTierAndTheQuantityLine() {
        assertEquals(ExcavationRules.LOOT_STEPS, ExcavationRules.lootSteps(1, 1f), 0.0001f);
        assertTrue(ExcavationRules.lootSteps(10, 1f) > ExcavationRules.lootSteps(1, 1f));
        assertEquals(ExcavationRules.lootSteps(5, 1f) * 1.5f, ExcavationRules.lootSteps(5, 1.5f), 0.0001f);
        assertEquals(ExcavationRules.ITEM_MULT * 1.5f, ExcavationRules.itemMult(1.5f), 0.0001f);
        assertEquals(0f, ExcavationRules.itemMult(-1f), 0.0001f);
    }

    @Test
    void theTuningLinesAddSitesAndRaiseTheSiteQuantity() {
        ChartGenContext context = ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_EXTRA_EXCAVATION_SITES, 2f),
                new ChartGenContext.AppliedEffect(ChartEffect.CHART_EXTRA_EXCAVATION_SITES, 1f),
                new ChartGenContext.AppliedEffect(ChartEffect.CHART_EXCAVATION_LOOT_QUANTITY_PERCENT, 50f)));

        assertEquals(3, Math.round(context.anomalyPercent(ExcavationSites.EXTRA_KEY)));
        assertEquals(1.5f, ExcavationLoot.quantityMult(context), 0.0001f);
        assertEquals(1f, ExcavationLoot.quantityMult(ChartGenContext.none()), 0.0001f);
    }

    @Test
    void aSiteLeansTowardItsDropType() {
        ChartGenContext context = ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_QUANTITY_PERCENT, 10f)));

        ChartGenContext leaned = ExcavationLoot.contextFor(context, DropType.WEAPON);

        float expected = 1f + (10f + ExcavationRules.LEAN_PERCENT) / ChartGenContext.PERCENT;
        assertEquals(expected, leaned.dropMult(DropType.WEAPON), 0.0001f);
        assertEquals(context.dropMult(DropType.AI_CORE), leaned.dropMult(DropType.AI_CORE), 0.0001f);
        assertSame(ChartEffect.CHART_WEAPON_PERCENT, ExcavationLoot.effectFor(DropType.WEAPON));
        assertSame(context, ExcavationLoot.contextFor(context, null));
        ExcavationRules.LEANS.forEach(lean -> assertNotNull(ExcavationLoot.effectFor(lean), lean.name()));
    }
}
