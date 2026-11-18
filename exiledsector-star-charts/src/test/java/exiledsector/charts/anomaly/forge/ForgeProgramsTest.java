package exiledsector.charts.anomaly.forge;

import exiledsector.ProjectFiles;
import exiledsector.items.ItemType;
import exiledsector.items.crafting.ForgeOperation;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeProgramsTest {

    @AfterEach
    void tearDown() {
        ForgePrograms.resetForTests();
    }

    static void registerShipped() throws Exception {
        String csv = Files.readString(ProjectFiles.projectRoot().resolve(ForgePrograms.PATH), StandardCharsets.UTF_8).replace("\r\n", "\n");
        assertTrue(ForgePrograms.register(CDL.toJSONArray(csv)).isEmpty());
    }

    private static JSONObject row(String id, String itemTypes) throws Exception {
        return new JSONObject().put("id", id).put("item_types", itemTypes).put("feedstock", "20").put("infused_feedstock_each", "5");
    }

    @Test
    void everyShippedProgramIsAForgeOperation() throws Exception {
        registerShipped();

        Set<ForgeOperation> operations = new HashSet<>();
        ForgePrograms.all().forEach(program -> operations.add(program.operation()));
        assertEquals(Set.of(ForgeOperation.values()), operations);
        ForgePrograms.all().forEach(program -> assertEquals(Set.of(ItemType.HULL_ITEM), program.itemTypes(), program.id()));
    }

    @Test
    void rowsThatAreNotProgramsOrForgeNothingAreSkipped() throws Exception {
        List<String> problems = ForgePrograms.register(new JSONArray().put(row("melt", "hull_item")).put(row("graft", "wormhole_anchor")));

        assertEquals(3, problems.size(), "the unknown program, the unforgeable item type and the row it empties");
        assertNull(ForgePrograms.byId("melt"));
        assertNull(ForgePrograms.byId("graft"));
    }

    @Test
    void costsAreTheProgramsOwnAndTheCostMultiplierScalesBothFeedstocks() throws Exception {
        ForgePrograms.register(new JSONArray().put(row("graft", "hull_item")));
        ForgePrograms.Program graft = ForgePrograms.byId("graft");

        assertEquals(new ForgeRules.Cost(20, 5), ForgeRules.costFor(graft, 1f));
        assertEquals(new ForgeRules.Cost(10, 3), ForgeRules.costFor(graft, 0.5f));
        assertEquals(new ForgeRules.Cost(40, 10), ForgeRules.costFor(graft, 2f));
        assertEquals(new ForgeRules.Cost(0, 0), ForgeRules.costFor(graft, -1f));
    }

    @Test
    void theShippedProgramsHaveFixedFeedstockCosts() throws Exception {
        registerShipped();

        assertEquals(new ForgeRules.Cost(300, 0), ForgeRules.costFor(ForgePrograms.of(ForgeOperation.GRAFT), 1f));
        assertEquals(new ForgeRules.Cost(500, 0), ForgeRules.costFor(ForgePrograms.of(ForgeOperation.EXCISE), 1f));
        assertEquals(new ForgeRules.Cost(1000, 0), ForgeRules.costFor(ForgePrograms.of(ForgeOperation.PERFECT), 1f));
        assertEquals(new ForgeRules.Cost(0, 500), ForgeRules.costFor(ForgePrograms.of(ForgeOperation.BIOME_INFUSE), 1f));
        assertEquals(new ForgeRules.Cost(500, 0), ForgeRules.costFor(ForgePrograms.of(ForgeOperation.INFUSE), 1f));
    }

    @Test
    void theRemovedProgramsAreGone() throws Exception {
        registerShipped();

        assertNull(ForgeOperation.byIdOrNull("anchor_reroll"));
        assertNull(ForgeOperation.byIdOrNull("retemper"));
        assertNull(ForgePrograms.byId("anchor_reroll"));
        assertNull(ForgePrograms.byId("retemper"));
        assertEquals(5, ForgePrograms.all().size());
    }
}
