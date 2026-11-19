package exiledsector.charts.anomaly.magneticstorm;

import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.LiveAnomalyRuns;
import exiledsector.charts.anomaly.MagneticStorm;
import exiledsector.charts.anomaly.StellarFlares;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemKind;
import exiledsector.items.modifier.ItemRoll;
import exiledsector.items.modifier.LuckyRolls;
import exiledsector.items.modifier.PoolEntry;
import exiledsector.modifiers.ModifierSlot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeomagneticSurgeLuckTest {

    private static final String PLATE = "surge_plate";
    private static final AppliedEffect SURGE_LINE = new AppliedEffect(ChartModifier.CHART_MAGNETIC_STORM_LUCKY_MODIFIERS, 1f);

    @BeforeEach
    void setUp() {
        ItemDefinitions.register(List.of(new ItemDefinition(PLATE, ItemKind.ARMOR_PLATING, PLATE, ItemDefinition.FALLBACK_ICON, 1f, false, "",
                List.of(PoolEntry.of(ModifierSlot.PREFIX, "HULL_PERCENT", 1f, 100f, 1f)),
                List.of(PoolEntry.of(ModifierSlot.SUFFIX, "ARMOR_PERCENT", 1f, 100f, 1f)), "")));
    }

    @AfterEach
    void tearDown() {
        ItemDefinitions.clear();
        ChartAnomalies.resetForTests();
    }

    private static StarSystemAPI systemOf(List<AppliedEffect> treeLines, String... anomalyIds) {
        ChartRun run = LiveAnomalyRuns.run(LiveAnomalyRuns.SITE_TIER, anomalyIds);
        run.recordAtlasTreeLines(treeLines);
        ChartRuns.replace(run);
        StarSystemAPI system = mock(StarSystemAPI.class);
        when(system.getOptionalUniqueId()).thenReturn(ChartIds.systemId(LiveAnomalyRuns.RUN_ID));
        return system;
    }

    private static ItemData dropped(long seed) {
        return ItemData.rolled(ItemDefinitions.get(PLATE), seed, ItemRoll.withModifierCount(2));
    }

    @Test
    void theSurgeGivesFlareDropsAnotherLuckyModifierInAMagneticStorm() {
        try (MockSector sector = new MockSector()) {
            StarSystemAPI system = systemOf(List.of(SURGE_LINE), MagneticStorm.ANOMALY_ID, StellarFlares.ANOMALY_ID);

            assertEquals(1, GeomagneticSurge.extraLuckyModifiers(system));
            for (long seed = 0L; seed < 50L; seed++) {
                ItemData item = dropped(seed);
                assertEquals(LuckyRolls.withLuckyModifiers(item, 2), StellarFlares.INSTANCE.shapeDrop(sector.sector, item, system));
            }
        }
    }

    @Test
    void withoutTheLineOrTheStormFlareDropsKeepOneLuckyModifier() {
        try (MockSector sector = new MockSector()) {
            StarSystemAPI calm = systemOf(List.of(), MagneticStorm.ANOMALY_ID, StellarFlares.ANOMALY_ID);
            assertEquals(0, GeomagneticSurge.extraLuckyModifiers(calm));
            ItemData item = dropped(3L);
            assertEquals(LuckyRolls.withLuckyModifier(item), StellarFlares.INSTANCE.shapeDrop(sector.sector, item, calm));

            StarSystemAPI stormless = systemOf(List.of(SURGE_LINE), StellarFlares.ANOMALY_ID);
            assertEquals(0, GeomagneticSurge.extraLuckyModifiers(stormless));
            assertEquals(LuckyRolls.withLuckyModifier(item), StellarFlares.INSTANCE.shapeDrop(sector.sector, item, stormless));
            assertEquals(0, GeomagneticSurge.extraLuckyModifiers(mock(StarSystemAPI.class)));
        }
    }
}
