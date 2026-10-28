package exiledsector.charts.anomaly.forge;

import exiledlib.ProjectFiles;
import exiledsector.items.ForgeOperation;
import exiledsector.items.ItemType;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private static JSONObject row(String id, String itemTypes, int minTier) throws Exception {
        return new JSONObject().put("id", id).put("weight", "1").put("min_tier", Integer.toString(minTier)).put("item_types", itemTypes)
                .put("metals", "10").put("rare_metals", "5").put("feedstock", "20");
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
        List<String> problems = ForgePrograms.register(new JSONArray().put(row("melt", "hull_item", 1)).put(row("graft", "wormhole_anchor", 1)));

        assertEquals(3, problems.size(), "the unknown program, the unforgeable item type and the row it empties");
        assertNull(ForgePrograms.byId("melt"));
        assertNull(ForgePrograms.byId("graft"));
    }

    @Test
    void aRollPicksDistinctProgramsTheTierAllows() throws Exception {
        ForgePrograms.register(new JSONArray().put(row("graft", "hull_item", 1)).put(row("excise", "hull_item", 1)).put(row("perfect", "hull_item", 8)));

        for (long seed = 0; seed < 20; seed++) {
            List<ForgePrograms.Program> rolled = ForgePrograms.roll(3, 3, new Random(seed));
            assertEquals(2, rolled.size());
            assertEquals(2, new HashSet<>(rolled).size());
            assertTrue(rolled.stream().noneMatch(program -> program.id().equals("perfect")));
        }
        assertEquals(3, ForgePrograms.roll(8, 3, new Random(1L)).size());
    }

    @Test
    void costsGrowWithTierAndFollowTheCostMultiplier() throws Exception {
        ForgePrograms.register(new JSONArray().put(row("graft", "hull_item", 1)));
        ForgePrograms.Program graft = ForgePrograms.byId("graft");

        assertEquals(new ForgeRules.Cost(10, 5, 20), ForgeRules.costFor(graft, 1, 1f));
        assertTrue(ForgeRules.costFor(graft, 10, 1f).metals() > 10);
        assertEquals(new ForgeRules.Cost(5, 3, 10), ForgeRules.costFor(graft, 1, 0.5f));
    }
}
