package exiledsector.items;

import exiledlib.ProjectFiles;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.npc.RealSkillData;
import exiledsector.ui.cargo.CacheIconFx;
import exiledsector.ui.cargo.CacheLook;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.cargo.CargoIconLooks;
import exiledsector.ui.cargo.DoctrineLook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheIconLookTest {

    private static final String CACHE = "cache_still_test";
    private static final String PLAIN_CACHE = "cache_plain_icon_test";

    @BeforeEach
    void setUp() throws Exception {
        SkillTree.clearTypes();
        SkillTree.clearNodes();
        for (String nodeId : List.of("notable_a", "notable_b", "notable_c")) {
            SkillType type = new SkillType.Builder(nodeId + "_type", nodeId, "graphics/icons/" + nodeId + ".png", SkillTier.NOTABLE)
                    .effects(List.of()).build();
            SkillTree.register(new SkillNode(nodeId, type, List.of(), 0f, 0f, SkillNodeDecoration.NONE, List.of("core")));
        }
        JSONArray rows = new JSONArray()
                .put(cacheRow(CACHE).put("icon", CacheIconFx.STILL_PATH))
                .put(cacheRow(PLAIN_CACHE));
        ItemDefinitions.register(rows);
        ModspecCaches.register(rows);
    }

    @AfterEach
    void tearDown() {
        ModspecCaches.clear();
        ItemDefinitions.clear();
        SkillTree.clearTypes();
        SkillTree.clearNodes();
        RealSkillData.clear();
    }

    private static JSONObject cacheRow(String id) throws Exception {
        return ItemFixtures.subroutineRow(id, SubroutineKind.MODSPEC_CACHE).put(ModspecCaches.NODE_COUNT_CHANCES_COLUMN, "0;0;1");
    }

    @Test
    void aCacheWithItsStillProjectsEachRolledNotableInTheCorruptionColour() {
        Subroutine cache = assertInstanceOf(Subroutine.class, new ItemStore().add(ItemDefinitions.get(CACHE), 11L));
        CargoIconLook look = CargoIconLooks.forItem(cache);

        List<String> expected = new ArrayList<>();
        for (String nodeId : cache.currentNodeIds()) {
            expected.add("graphics/icons/" + nodeId + ".png");
        }
        assertEquals(3, expected.size());
        assertEquals(expected, ((CacheLook) look.art()).chipIconPaths());
        assertEquals(cache.frameColor(), look.color());
        assertEquals(CraftingCurrency.CORRUPTION.color(), look.color());
        assertTrue(look.framed());
        assertTrue(look.ownArt());
        assertFalse(look.art() instanceof DoctrineLook);
        assertEquals(look.art(), look.unframed().art());
    }

    @Test
    void aCacheWithAnotherIconKeepsTheCorruptedRarityLook() {
        CargoIconLook look = CargoIconLooks.forItem(new ItemStore().add(ItemDefinitions.get(PLAIN_CACHE), 11L));

        assertFalse(look.art() instanceof CacheLook);
        assertFalse(look.ownArt());
    }

    @Test
    void theShippedCacheUsesTheBakedStill() throws Exception {
        JSONArray subroutines = ItemFixtures.shippedRows(SubroutineDefinitions.dataPath());
        int caches = 0;
        for (int row = 0; row < subroutines.length(); row++) {
            JSONObject subroutine = subroutines.getJSONObject(row);
            if (SubroutineKind.MODSPEC_CACHE.id().equals(subroutine.optString(SubroutineDefinitions.SUBROUTINE_KIND_COLUMN))) {
                assertEquals(CacheIconFx.STILL_PATH, subroutine.getString("icon"), subroutine.getString("id"));
                caches++;
            }
        }
        assertEquals(1, caches);
        assertTrue(Files.isRegularFile(ProjectFiles.projectRoot().resolve(CacheIconFx.STILL_PATH)));
    }
}
