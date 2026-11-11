package exiledsector.items;

import exiledsector.ProjectFiles;
import exiledsector.skills.tags.ThemeTagScope;
import exiledsector.skills.tags.ThemeTags;
import exiledsector.skills.tags.ThemeTagsExtension;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ThemeTagsExtension.class)
class InfusedCraftCostsTest {

    private static final String THREAT = "exiledSector_threat_feedstock";
    private static final String SHROUD = "exiledSector_shroud_feedstock";
    private static final String PHASE = "exiledSector_phase_feedstock";
    private static final Set<String> DAMAGE_TYPES = Set.of("kinetic", "high_explosive", "fragmentation", "energy_damage", "emp");
    private static final Map<String, String> FEEDSTOCK_BY_ROOT = Map.of("weapon", THREAT, "defence", SHROUD, "repair", SHROUD,
            "combat_readiness", SHROUD, "logistics", PHASE);
    private static final Map<String, String> FEEDSTOCK_BY_TAG = Map.of("d_mods", SHROUD, "low_tech", SHROUD, "low_hull", SHROUD,
            "midline", THREAT, "on_hit", THREAT, "on_kill", THREAT);

    @BeforeEach
    void setUp() throws Exception {
        String costs = Files.readString(ProjectFiles.gameFile(InfusedCraftCosts.dataPath()), StandardCharsets.UTF_8);
        InfusedCraftCosts.register(CDL.toJSONArray(costs.replace("\r\n", "\n")));
    }

    @AfterEach
    void tearDown() {
        InfusedCraftCosts.clear();
    }

    @Test
    void aCraftCostsItsTagsFeedstockAndUnofferedCraftsCostNothing() {
        assertEquals(new InfusedCraftCosts.Cost(SHROUD, 100), InfusedCraft.REFORGE_WITH_TAG.cost("front_shield"));
        assertEquals(new InfusedCraftCosts.Cost(SHROUD, 1000), InfusedCraft.ADD_AND_REMOVE.cost("low_tech"));
        assertEquals(PHASE, InfusedCraftCosts.feedstockFor("flux"));
        assertNull(InfusedCraft.SWAP_DAMAGE_TYPE.cost("kinetic"));
        assertNull(InfusedCraft.REROLL_TAG_VALUES.cost("flux"));
        assertNull(InfusedCraft.REFORGE_WITH_TAG.cost(null));
    }

    @Test
    void everyShipTagOffersTheTaggedReforgeAndTheExchange() {
        List<String> shipTags = ThemeTags.ids(ThemeTagScope.SHIP);
        assertEquals(shipTags, InfusedCraft.REFORGE_WITH_TAG.tags());
        assertEquals(shipTags, InfusedCraft.ADD_AND_REMOVE.tags());
    }

    @Test
    void addAndRemoveIsTheDearestCraftForEveryTag() {
        for (String tag : InfusedCraft.ADD_AND_REMOVE.tags()) {
            int addAndRemove = InfusedCraft.ADD_AND_REMOVE.cost(tag).amount();
            for (InfusedCraft craft : InfusedCraft.values()) {
                InfusedCraftCosts.Cost cost = craft.cost(tag);
                assertTrue(craft == InfusedCraft.ADD_AND_REMOVE || cost == null || cost.amount() < addAndRemove, craft.id() + " " + tag);
            }
        }
    }

    @Test
    void everyTagUsesTheFeedstockOfItsFamily() {
        for (String tag : InfusedCraft.REFORGE_WITH_TAG.tags()) {
            assertTrue(ThemeTags.isThemeTag(tag), tag);
            String root = ThemeTags.withAncestors(tag).get(ThemeTags.withAncestors(tag).size() - 1);
            String expected = DAMAGE_TYPES.contains(tag) ? THREAT : FEEDSTOCK_BY_TAG.getOrDefault(tag, FEEDSTOCK_BY_ROOT.getOrDefault(root, PHASE));
            assertEquals(expected, InfusedCraftCosts.feedstockFor(tag), tag);
        }
    }

    @Test
    void rowsThatAreNotThemeTagsOrNameNoFeedstockAreSkipped() throws Exception {
        InfusedCraftCosts.register(new JSONArray()
                .put(new JSONObject().put("tag", "not_a_tag").put("feedstock", THREAT).put("reforge_with_tag", "10"))
                .put(new JSONObject().put("tag", "flux").put("feedstock", "").put("reforge_with_tag", "10"))
                .put(new JSONObject().put("tag", "phase").put("feedstock", PHASE).put("reforge_with_tag", "10")));

        assertEquals(List.of("phase"), InfusedCraft.REFORGE_WITH_TAG.tags());
        assertEquals(List.of(), InfusedCraft.REROLL_TAG_VALUES.tags());
    }
}
