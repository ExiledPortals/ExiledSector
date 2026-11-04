package exiledsector.charts.anomaly.forge;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import exiledsector.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.ForgeChoice;
import exiledsector.items.ForgeOperation;
import exiledsector.items.Item;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.RolledLine;
import exiledsector.items.RolledModifier;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartForgeTest {

    private static final String RUN_ID = "run1";
    private static final String PLATE = "forge_test_plate";

    private MockSector mockSector;
    private final CargoAPI cargo = mock(CargoAPI.class);
    private final Map<String, Integer> storedMaterials = new HashMap<>();
    private final CraftingMaterials materials = CraftingMaterials.storedForTests(storedMaterials);
    private ForgePrograms.Program excise;

    @BeforeEach
    void setUp() throws Exception {
        mockSector = new MockSector();
        ItemDefinitions.register(new JSONArray().put(new JSONObject().put("id", PLATE).put("name", "Forge Plate")
                .put("icon", "graphics/icons/cargo/chip1.png").put("rarity", "20").put("prefixes", "FLUX_CAPACITY_MULT:4:6; HULL_MULT:4:6")
                .put("suffixes", "ARMOR_PERCENT:6:9").put("section", "armor_plating")));
        ForgePrograms.register(new JSONArray()
                .put(new JSONObject().put("id", "excise").put("weight", "1").put("min_tier", "1").put("item_types", "hull_item")
                        .put("metals", "40").put("rare_metals", "10").put("feedstock", "20"))
                .put(new JSONObject().put("id", "retemper").put("weight", "1").put("min_tier", "1").put("item_types", "subroutine")
                        .put("metals", "40").put("rare_metals", "10").put("feedstock", "20")));
        excise = ForgePrograms.byId("excise");
    }

    @AfterEach
    void tearDown() {
        ForgePrograms.resetForTests();
        ItemDefinitions.clear();
        mockSector.close();
    }

    private static ChartRun liveForgeRun(List<ForgePrograms.Program> programs, int runs) {
        ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(1, "pirates", "boss", 3, 2, List.of(DerelictForge.ANOMALY_ID)));
        run.recordArrival();
        ForgeStates.record(ForgeStates.of(run, ForgeSite.DERELICT_FORGE), programs, runs);
        ChartRuns.store(run);
        return run;
    }

    private void owns(float metals, float rareMetals, int feedstock) {
        storedMaterials.put(ChartForge.FEEDSTOCK_ITEM_ID, feedstock);
        when(cargo.getCommodityQuantity(Commodities.METALS)).thenReturn(metals);
        when(cargo.getCommodityQuantity(Commodities.RARE_METALS)).thenReturn(rareMetals);
    }

    private static Item plate() {
        return Item.withEffects("plate", PLATE, 1L, List.of(new RolledLine("FLUX_CAPACITY_MULT", 5f), new RolledLine("ARMOR_PERCENT", 7f)));
    }

    @Test
    void runningAProgramChangesTheItemTakesTheMetalsAndSpendsARun() {
        ChartRun run = liveForgeRun(List.of(excise), 1);
        owns(100f, 100f, 50);
        Item item = plate();
        ForgeRules.Cost cost = ChartForge.costFor(run, ForgeSite.DERELICT_FORGE, excise);

        assertTrue(ChartForge.verdict(run, ForgeSite.DERELICT_FORGE, excise, item, cargo, materials).allowed());
        assertTrue(ChartForge.run(run, ForgeSite.DERELICT_FORGE, excise, item, ForgeChoice.modifier(item.explicits().get(1)), cargo, materials, new Random(1L)));

        assertEquals(1, item.explicits().size());
        verify(cargo).removeCommodity(Commodities.METALS, cost.metals());
        verify(cargo).removeCommodity(Commodities.RARE_METALS, cost.rareMetals());
        assertEquals(50 - cost.feedstock(), materials.count(ChartForge.FEEDSTOCK_ITEM_ID));
        assertEquals(0, ForgeStates.runsLeft(run, ForgeSite.DERELICT_FORGE));
        assertSame(ChartForge.DARK, ChartForge.verdict(run, ForgeSite.DERELICT_FORGE, excise, item, cargo, materials));
    }

    @Test
    void aProgramThatCannotRunCostsNothing() {
        ChartRun run = liveForgeRun(List.of(excise), 1);
        owns(100f, 100f, 50);
        Item item = plate();
        RolledModifier notOnTheItem = plate().explicits().get(0).asImmutable();

        assertFalse(ChartForge.run(run, ForgeSite.DERELICT_FORGE, excise, item, ForgeChoice.modifier(notOnTheItem), cargo, materials, new Random(1L)));

        verify(cargo, never()).removeCommodity(anyString(), anyFloat());
        assertEquals(1, ForgeStates.runsLeft(run, ForgeSite.DERELICT_FORGE));
        assertEquals(2, item.explicits().size());
        assertEquals(50, materials.count(ChartForge.FEEDSTOCK_ITEM_ID));
    }

    @Test
    void theForgeRefusesWhatItCannotPayForOrDoesNotOffer() {
        ForgePrograms.Program retemper = ForgePrograms.byId("retemper");
        ChartRun run = liveForgeRun(List.of(excise), 1);
        owns(100f, 100f, 5);

        assertSame(ChartForge.COST, ChartForge.verdict(run, ForgeSite.DERELICT_FORGE, excise, plate(), cargo, materials));
        assertSame(ChartForge.DARK, ChartForge.verdict(run, ForgeSite.DERELICT_FORGE, retemper, plate(), cargo, materials));
        assertEquals("ui.forge.refused.loadFirst", ChartForge.verdict(run, ForgeSite.DERELICT_FORGE, excise, null, cargo, materials).refusalKey());
        assertSame(ChartForge.DARK, ChartForge.verdict(null, ForgeSite.DERELICT_FORGE, excise, plate(), cargo, materials));
        ChartRun bothPrograms = liveForgeRun(List.of(excise, retemper), 1);
        assertSame(ChartForge.ITEM_TYPE, ChartForge.verdict(bothPrograms, ForgeSite.DERELICT_FORGE, retemper, plate(), cargo, materials));
    }

    @Test
    void theCostFollowsTheRunTier() {
        ChartRun run = liveForgeRun(List.of(excise), 1);

        assertEquals(ForgeRules.costFor(excise, 1f), ChartForge.costFor(run, ForgeSite.DERELICT_FORGE, excise));
        assertNull(ChartForge.liveRun("another", ForgeSite.DERELICT_FORGE));
        assertEquals(ForgeOperation.EXCISE, excise.operation());
    }
}
