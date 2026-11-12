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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ThemeTagsExtension.class)
class InfusedCraftCostsTest {

    private static final String THREAT = "exiledSector_threat_feedstock";
    private static final String SHROUD = "exiledSector_shroud_feedstock";
    private static final String PHASE = "exiledSector_phase_feedstock";
    private static final Set<String> CHILD_TAGS_WITH_THEIR_OWN_CRAFTS = Set.of("ballistic", "non_beam_energy", "beam", "missile", "range",
            "rate_of_fire", "ammo");
    private static final Set<String> ROOT_TAGS_WITHOUT_CRAFTS = Set.of("weapon", "repair", "d_mods", "fleet", "conditional",
            "damage_type", "combat_readiness");
    private static final Set<String> THREAT_TAGS = Set.of("ballistic", "non_beam_energy", "beam", "missile", "strikecraft");
    private static final Set<String> SHROUD_TAGS = Set.of("defence", "range", "ammo", "logistics");

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
    void aCraftCostsItsTagsFeedstockAndUnofferedTagsCostNothing() {
        assertEquals(new InfusedCraftCosts.Cost(List.of(SHROUD), 100), InfusedCraft.REFORGE_WITH_TAG.cost("defence"));
        assertEquals(new InfusedCraftCosts.Cost(List.of(THREAT, SHROUD, PHASE), 1000), InfusedCraft.ADD_AND_REMOVE.cost("affinity"));
        assertEquals(List.of(PHASE), InfusedCraftCosts.feedstocksFor("flux"));
        assertEquals(List.of(), InfusedCraftCosts.feedstocksFor("front_shield"));
        assertNull(InfusedCraft.REFORGE_WITH_TAG.cost("front_shield"));
        assertNull(InfusedCraft.REFORGE_WITH_TAG.cost("weapon"));
        assertNull(InfusedCraft.REFORGE_WITH_TAG.cost("kinetic"));
        assertNull(InfusedCraft.REFORGE_WITH_TAG.cost(null));
    }

    @Test
    void theOfferedParentAndWeaponTagsOfferTheTaggedReforgeAndTheExchange() {
        List<String> offeredTags = ThemeTags.ids(ThemeTagScope.SHIP).stream()
                .filter(tag -> ThemeTags.parent(tag) == null ? !ROOT_TAGS_WITHOUT_CRAFTS.contains(tag) : CHILD_TAGS_WITH_THEIR_OWN_CRAFTS.contains(tag))
                .toList();
        assertEquals(offeredTags, InfusedCraft.REFORGE_WITH_TAG.tags());
        assertEquals(offeredTags, InfusedCraft.ADD_AND_REMOVE.tags());
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
            assertEquals(expectedFeedstocks(tag), InfusedCraftCosts.feedstocksFor(tag), tag);
        }
    }

    private static List<String> expectedFeedstocks(String tag) {
        if ("affinity".equals(tag)) {
            return List.of(THREAT, SHROUD, PHASE);
        }
        if (THREAT_TAGS.contains(tag)) {
            return List.of(THREAT);
        }
        return List.of(SHROUD_TAGS.contains(tag) ? SHROUD : PHASE);
    }

    @Test
    void rowsThatAreNotThemeTagsOrNameNoFeedstockAreSkipped() throws Exception {
        InfusedCraftCosts.register(new JSONArray()
                .put(new JSONObject().put("tag", "not_a_tag").put("feedstock", THREAT).put("reforge_with_tag", "10"))
                .put(new JSONObject().put("tag", "flux").put("feedstock", "").put("reforge_with_tag", "10"))
                .put(new JSONObject().put("tag", "phase").put("feedstock", PHASE).put("reforge_with_tag", "10")));

        assertEquals(List.of("phase"), InfusedCraft.REFORGE_WITH_TAG.tags());
        assertEquals(List.of(), InfusedCraft.ADD_AND_REMOVE.tags());
    }
}
