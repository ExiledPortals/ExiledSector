package exiledsector.items;

import exiledsector.items.ItemDataIntegrity.Severity;
import exiledsector.items.ItemDataIntegrity.Violation;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.npc.RealSkillData;
import exiledsector.skills.tags.SkillTags;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VolumeSubroutinesTest {

    private static final String CORE_CHIP = "volume_core";
    private static final String HEGEMONY_CHIP = "volume_hegemony";

    private JSONArray rows;

    @BeforeEach
    void setUp() throws Exception {
        SkillTree.clearTypes();
        SkillTree.clearNodes();
        for (SkillNode node : List.of(node("core_notable_a", SkillTier.NOTABLE, "core", List.of()), node("core_notable_b", SkillTier.NOTABLE, "core", List.of()),
                node("core_choice", SkillTier.NOTABLE, "core", List.of("core_notable_a_type")), node("core_small", SkillTier.SMALL, "core", List.of()),
                node("hegemony_notable", SkillTier.NOTABLE, "hegemony", List.of()), node("hegemony_keystone", SkillTier.KEYSTONE, "hegemony", List.of()))) {
            SkillTree.register(node);
        }
        rows = new JSONArray()
                .put(volumeRow(CORE_CHIP, "core").put(VolumeSubroutines.EXCLUDED_NODES_COLUMN, "core_notable_b").put(VolumeSubroutines.SALVAGE_CHANCE_COLUMN, "1"))
                .put(volumeRow(HEGEMONY_CHIP, "hegemony").put(VolumeSubroutines.DROP_CHANCE_COLUMN, "0.5"))
                .put(volumeRow("volume_core_again", "core"))
                .put(volumeRow("volume_nowhere", "ocean"))
                .put(ItemFixtures.subroutineRow("doctrine_chip", SubroutineKind.DOCTRINE).put(VolumeSubroutines.VOLUME_COLUMN, "core"));
        ItemDefinitions.register(rows);
        VolumeSubroutines.register(rows);
        ItemDrops.register(new JSONArray().put(new JSONObject().put("site", "derelict").put("chances", "1").put("uniqueChance", "0")));
    }

    @AfterEach
    void tearDown() {
        VolumeSubroutines.clear();
        ItemDrops.clear();
        ItemDefinitions.clear();
        SkillTree.clearTypes();
        SkillTree.clearNodes();
        RealSkillData.clear();
    }

    private static JSONObject volumeRow(String id, String volume) throws Exception {
        return ItemFixtures.subroutineRow(id, SubroutineKind.VOLUME).put(VolumeSubroutines.VOLUME_COLUMN, volume);
    }

    private static SkillNode node(String id, SkillTier tier, String region, List<String> optionIds) {
        SkillType type = new SkillType.Builder(id + "_type", id, "a.png", tier).effects(List.of()).optionalOptionIds(optionIds).build();
        return new SkillNode(id, type, List.of(), 0f, 0f, SkillNodeDecoration.NONE, List.of(region));
    }

    @Test
    void aVolumeRollsFromItsNotablesLeavingOutExcludedAndOptionalOnes() {
        assertEquals(List.of("core_notable_a"), VolumeSubroutines.candidateNodeIds(ItemDefinitions.get(CORE_CHIP)));
        assertEquals(List.of("hegemony_notable"), VolumeSubroutines.candidateNodeIds(ItemDefinitions.get(HEGEMONY_CHIP)));

        Subroutine rolled = assertInstanceOf(Subroutine.class, new ItemStore().add(ItemDefinitions.get(CORE_CHIP), 42L));
        assertEquals("core_notable_a", rolled.rolledNodeId());
        assertEquals(CorruptionOutcome.INNATE, rolled.corruption());
    }

    @Test
    void aSecondSubroutineForAVolumeAndAnUnknownVolumeGrantNothing() {
        assertNull(VolumeSubroutines.base("volume_core_again"));
        assertNull(VolumeSubroutines.base("volume_nowhere"));
        assertNull(VolumeSubroutines.base("doctrine_chip"));
        assertEquals(List.of(), VolumeSubroutines.candidateNodeIds(ItemDefinitions.get("volume_nowhere")));
        assertEquals("hegemony", VolumeSubroutines.volume(HEGEMONY_CHIP));
    }

    @Test
    void factionVolumesDropFromFleetsAndTheRestFromSalvage() {
        FactionFleetDrops.Source hegemonySource = FactionFleetDrops.source(HEGEMONY_CHIP);
        assertNotNull(hegemonySource);
        assertEquals("hegemony", hegemonySource.volume());
        assertEquals(0.5f, hegemonySource.chancePer100DeploymentPoints(), 1e-6f);
        assertNull(FactionFleetDrops.source(CORE_CHIP));

        Set<String> salvaged = new HashSet<>();
        Random random = new Random(11L);
        for (int trial = 0; trial < 50; trial++) {
            ItemDrops.roll("derelict", random, ItemChances.of(1f), definition -> true).forEach(item -> salvaged.add(item.definition().id()));
        }
        assertEquals(Set.of(CORE_CHIP), salvaged);
    }

    @Test
    void aVolumeWhoseAreaIsOffNeverDrops() {
        SkillTree.unregister("hegemony_notable");
        SkillTree.unregister("hegemony_keystone");

        assertFalse(VolumeSubroutines.areaActive(HEGEMONY_CHIP));
        assertFalse(FactionFleetDrops.available(ItemDefinitions.get(HEGEMONY_CHIP)));
        assertTrue(FactionFleetDrops.available(ItemDefinitions.get(CORE_CHIP)));
    }

    @Test
    void theIntegrityCheckReportsBadVolumeRows() throws Exception {
        Map<String, SkillNode> treeNodes = new LinkedHashMap<>(SkillTree.getAllNodes());
        JSONArray checkedRows = new JSONArray()
                .put(volumeRow("dup_a", "core").put(VolumeSubroutines.SALVAGE_CHANCE_COLUMN, "0.1"))
                .put(volumeRow("dup_b", "core").put(VolumeSubroutines.DROP_CHANCE_COLUMN, "lots"))
                .put(volumeRow("excluding", "hegemony").put(VolumeSubroutines.EXCLUDED_NODES_COLUMN, "hegemony_notable; core_small; missing")
                        .put(VolumeSubroutines.DROP_CHANCE_COLUMN, "0.1"))
                .put(ItemFixtures.subroutineRow("doctrine_chip", SubroutineKind.DOCTRINE).put(VolumeSubroutines.SALVAGE_CHANCE_COLUMN, "0.1"));

        List<String> messages = ItemDataIntegrity.checkVolumeSubroutines(checkedRows, treeNodes).stream()
                .filter(violation -> violation.severity() == Severity.ERROR).map(Violation::message).toList();

        assertEquals(List.of(
                "dup_b grants from core, which already has a volume subroutine on line 2",
                "dup_b has the drop chance \"lots\"; expected a number of 0 or more",
                "excluding excludes core_small, which is not a notable of the hegemony volume",
                "excluding excludes missing, which is not in the skill tree",
                "excluding has no notable left to roll in the hegemony volume",
                "doctrine_chip fills salvage_chance, but only VOLUME subroutines use it"), messages);
    }

    @Test
    void everyShippedVolumeHasOneVolumeSubroutineWithNotablesToRoll() throws Exception {
        VolumeSubroutines.clear();
        ItemDefinitions.clear();
        RealSkillData.load();
        ItemFixtures.registerShippedItems();
        JSONArray shippedRows = ItemFixtures.shippedRows(ItemDefinitions.SUBROUTINES_PATH);
        VolumeSubroutines.register(shippedRows);

        assertEquals(List.of(), ItemDataIntegrity.checkVolumeSubroutines(shippedRows, SkillTree.getDeclaredNodes()));
        Set<String> volumes = new HashSet<>();
        for (VolumeSubroutines.VolumeBase base : VolumeSubroutines.bases()) {
            volumes.add(base.volume());
            assertFalse(VolumeSubroutines.candidateNodeIds(ItemDefinitions.get(base.definitionId())).isEmpty(), base.definitionId());
            boolean factionVolume = SkillTags.FACTION_VOLUMES.contains(base.volume());
            assertEquals(factionVolume, base.dropChance() > 0f, base.definitionId());
            assertEquals(!factionVolume, base.salvageChance() > 0f, base.definitionId());
        }
        assertEquals(new HashSet<>(SkillTags.REGION), volumes);
    }
}
