package exiledsector.items;

import exiledsector.items.ItemDataIntegrity.Violation;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.npc.RealSkillData;
import exiledsector.skills.tags.SkillTags;
import exiledsector.ui.cargo.AnimatedItemIcons;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WormholeAnchorDataTest {

    private static final Map<String, String> EXPECTED_EXITS = Map.ofEntries(
            Map.entry("hegemony", "hegemony_2"),
            Map.entry("tritachyon", "tritach_2"),
            Map.entry("luddic", "luddic_2"),
            Map.entry("pirate", "pirate_wormhole_2"),
            Map.entry("persean_league", "league_wormhole_2"),
            Map.entry("sindrian_dictat", "dictat_wormhole_2"),
            Map.entry("REDACTED", "REDACTED_2"),
            Map.entry("kesteven", "kesteven_wormhole1"),
            Map.entry("enigma", "lost_sector_wormhole1"),
            Map.entry("nightcross", "nightcross_wormhole_1"),
            Map.entry("stargazer", "stargazer_wormhole_1"),
            Map.entry("abyss", "rat_abyss_wormhole_1"),
            Map.entry("threat", "threat_wormhole_1"),
            Map.entry("tania", "tania_wormhole_1"),
            Map.entry("exotech", "exoship"));

    @AfterEach
    void tearDown() {
        WormholeAnchorDefinitions.clear();
        ItemDefinitions.clear();
        RealSkillData.clear();
    }

    private static JSONArray shippedAnchors() throws Exception {
        return ItemFixtures.shippedRows(WormholeAnchorDefinitions.DATA_PATH);
    }

    @Test
    void theShippedFileHasOneAnchorForEveryVolumeLeadingToItsCurrentExit() throws Exception {
        WormholeAnchorDefinitions.register(shippedAnchors());

        Map<String, String> exitsByVolume = new LinkedHashMap<>();
        for (ItemDefinition definition : ItemDefinitions.ofType(ItemType.WORMHOLE_ANCHOR)) {
            exitsByVolume.put(WormholeAnchorDefinitions.volume(definition.id()), WormholeAnchorDefinitions.exitNodeId(definition.id()));
        }
        assertEquals(EXPECTED_EXITS, exitsByVolume);
        Set<String> volumes = SkillTags.REGION.stream().filter(region -> !SkillTags.CORE_REGION.equals(region)).collect(Collectors.toSet());
        assertEquals(volumes, exitsByVolume.keySet());
        ItemDefinition beacon = ItemDefinitions.get("wormhole_anchor_exotech");
        assertEquals("Hyper-Nav Beacon", beacon.name());
        assertEquals(AnimatedItemIcons.BEACON_PATH, beacon.icon());
        assertEquals(ItemUnlock.RAT_PLAYER_EXOSHIP.id(), beacon.unlock());
        assertTrue(ItemDefinitions.ofType(ItemType.WORMHOLE_ANCHOR).stream()
                .filter(definition -> definition != beacon).allMatch(definition -> definition.unlock().isEmpty()));
        assertTrue(ItemDefinitions.ofType(ItemType.WORMHOLE_ANCHOR).stream().noneMatch(ItemDefinition::unique));
    }

    @Test
    void everyShippedAnchorButTheBeaconHasItsOwnBakedIconAndEveryAnchorAColourAndCrest() throws Exception {
        WormholeAnchorDefinitions.register(shippedAnchors());

        Set<Color> colors = new HashSet<>();
        for (ItemDefinition definition : ItemDefinitions.ofType(ItemType.WORMHOLE_ANCHOR)) {
            String volume = WormholeAnchorDefinitions.volume(definition.id());
            WormholeAnchorDefinitions.AnchorStyle style = WormholeAnchorDefinitions.style(definition.id());
            if (!"exotech".equals(volume)) {
                assertEquals("graphics/icons/cargo/exiledSector_wormhole_anchor_" + volume.toLowerCase(Locale.ROOT) + ".png", definition.icon());
                assertTrue(new File(definition.icon()).isFile(), definition.icon() + " is missing");
            }
            assertNotEquals(WormholeAnchorDefinitions.DEFAULT_COLOR, style.color(), definition.id() + " has no colour");
            assertNotNull(style.crestFactionId(), definition.id() + " has no crest faction");
            colors.add(style.color());
        }
        assertEquals(EXPECTED_EXITS.size(), colors.size());
    }

    @Test
    void theShippedAnchorsPassTheIntegrityCheckAgainstTheShippedTree() throws Exception {
        RealSkillData.load();

        List<Violation> violations = ItemDataIntegrity.checkWormholeAnchors(shippedAnchors(), SkillTree.getDeclaredNodes(),
                otherShippedIds());

        assertEquals(List.of(), violations);
    }

    private static Set<String> otherShippedIds() throws Exception {
        Set<String> ids = new TreeSet<>();
        for (String path : List.of(ItemDefinitions.SUBROUTINES_PATH, ItemDefinitions.HULL_ITEMS_PATH, StarChartDefinitions.DATA_PATH)) {
            JSONArray rows = ItemFixtures.shippedRows(path);
            for (int index = 0; rows != null && index < rows.length(); index++) {
                ids.add(rows.getJSONObject(index).getString("id"));
            }
        }
        return ids;
    }

    @Test
    void theIntegrityCheckReportsBadExitsVolumesIdsAndUnlocks() throws Exception {
        SkillType wormhole = new SkillType.Builder("wormhole_exit_type", "Wormhole Exit", "", SkillTier.WORMHOLE_EXIT).build();
        SkillType notable = new SkillType.Builder("notable_type", "Notable", "", SkillTier.NOTABLE).build();
        Map<String, SkillNode> tree = Map.of(
                "hegemony_2", node("hegemony_2", wormhole, "hegemony"),
                "pirate_notable", node("pirate_notable", notable, "pirate"),
                "luddic_2", node("luddic_2", wormhole, "luddic"),
                "stargazer_wormhole_1", node("stargazer_wormhole_1", wormhole, "stargazer"));
        JSONArray rows = new JSONArray()
                .put(WormholeAnchorTest.anchorRow("good", "hegemony", "hegemony_2"))
                .put(WormholeAnchorTest.anchorRow("good", "kesteven", "missing_exit"))
                .put(WormholeAnchorTest.anchorRow("second_hegemony", "hegemony", "hegemony_2"))
                .put(WormholeAnchorTest.anchorRow("wrong_volume", "tritachyon", "luddic_2"))
                .put(WormholeAnchorTest.anchorRow("not_a_wormhole", "pirate", "pirate_notable"))
                .put(WormholeAnchorTest.anchorRow("core_anchor", "core", ""))
                .put(WormholeAnchorTest.anchorRow("taken", "luddic", "luddic_2"))
                .put(WormholeAnchorTest.anchorRow("odd_unlock", "abyss", "luddic_2").put("unlock", "made_up"))
                .put(WormholeAnchorTest.anchorRow("chart_unlock", "stargazer", "stargazer_wormhole_1").put("unlock", "cleared_chart_tier_3")
                        .put("color", "orange"))
                .put(new JSONObject().put("volume", "enigma"));

        List<String> messages = ItemDataIntegrity.checkWormholeAnchors(rows, tree, Set.of("taken")).stream()
                .map(violation -> "line " + violation.line() + ": " + violation.message()).toList();

        assertEquals(List.of(
                "line 3: id good is already used on line 2",
                "line 3: good exits at missing_exit, which is not in the skill tree",
                "line 4: second_hegemony leads to hegemony, which already has an anchor on line 2",
                "line 5: wrong_volume exits at luddic_2, which is in luddic rather than tritachyon",
                "line 6: not_a_wormhole exits at pirate_notable, which is a NOTABLE node rather than a wormhole exit",
                "line 7: core_anchor has volume \"core\"; expected a tree region other than core",
                "line 7: core_anchor has no exit node",
                "line 8: id taken is already used by a subroutine, hull item or star chart",
                "line 9: odd_unlock exits at luddic_2, which is in luddic rather than abyss",
                "line 9: odd_unlock has the unknown unlock \"made_up\"; use an item unlock id, cleared_chart_tier_N or leave it blank",
                "line 10: chart_unlock has the color \"orange\"; expected #RRGGBB or blank",
                "line 11: the row has no id"), messages);
    }

    @Test
    void aModifierMayNotRollOnAnchors() throws Exception {
        JSONArray modifiers = new JSONArray().put(ItemFixtures.modifierRow("HULL_MULT", "prefix", "wormhole_anchor", "HULL_MULT:1:2", "Sturdy"));

        List<String> messages = ItemDataIntegrity.check(modifiers, new JSONArray(), new JSONArray(), new JSONArray()).stream()
                .map(Violation::message).toList();

        assertTrue(messages.contains("HULL_MULT rolls on wormhole_anchor, but wormhole anchors do not roll modifiers"), messages.toString());
    }

    private static SkillNode node(String id, SkillType type, String region) {
        return new SkillNode(id, type, List.of(), 0f, 0f, SkillNodeDecoration.NONE, List.of(region));
    }
}
