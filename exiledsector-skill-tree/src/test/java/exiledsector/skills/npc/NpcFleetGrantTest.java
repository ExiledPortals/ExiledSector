package exiledsector.skills.npc;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.tags.ShipProfile;
import exiledsector.skills.tags.WeaponKind;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcFleetGrantTest {

    private static final ShipProfile BALLISTIC_FRIGATE =
            new ShipProfile(HullSize.FRIGATE, ShieldType.FRONT, 0, Set.of(WeaponKind.BALLISTIC), false, 0f, false);
    private static final String ROOT = "root";

    @BeforeEach
    void setUp() {
        RealSkillData.clear();
        NpcTestAnchors.install();
    }

    @AfterEach
    void tearDown() {
        RealSkillData.clear();
        NpcTestAnchors.uninstall();
    }

    private static SkillType registerType(SkillType.Builder builder) {
        SkillType type = builder.build();
        SkillTree.registerType(type);
        return type;
    }

    private static SkillType.Builder builder(String typeId, SkillTier tier) {
        return new SkillType.Builder(typeId, typeId + " name", "a.png", tier);
    }

    private static SkillNode node(String id, SkillType type, String... connectedTo) {
        SkillNode node = new SkillNode(id, type, List.of(connectedTo), 0f, 0f, SkillNodeDecoration.NONE, List.of());
        SkillTree.register(node);
        return node;
    }

    private static void small(String id, String... connectedTo) {
        node(id, registerType(builder(id + "_type", SkillTier.SMALL)), connectedTo);
    }

    private static void root() {
        node(ROOT, registerType(builder("root_type", SkillTier.ROOT)));
    }

    private static NpcTreeBuild generate(int nodeCount, String... grantedNodeIds) {
        return NpcSkillTreeBuilder.generate(new NpcBuildRequest(BALLISTIC_FRIGATE, null, null, NpcHullMods.NONE, NpcFreedOp.NONE, nodeCount,
                null, null, null, null, List.of(grantedNodeIds)), new Random(1L));
    }

    @Test
    void aGrantedNodeIsFreeAndTheBuilderDoesNotPathOutOfIt() {
        root();
        small("a1", ROOT);
        small("a2", "a1");
        small("a3", "a2");
        node("granted", registerType(builder("granted_type", SkillTier.NOTABLE)), "beyond");
        small("beyond", "granted");

        ShipSkillData shipData = generate(2, "granted").shipData();

        assertTrue(shipData.isFleetGranted("granted"));
        assertTrue(shipData.isAllocated("a1") && shipData.isAllocated("a2"));
        assertFalse(shipData.isAllocated("beyond"));
        assertEquals(3, shipData.usedNodeSlots());
    }

    @Test
    void aGrantedNodeOnTheBuildersPathIsTakenOnceAndBridgesOnward() {
        root();
        small("a1", ROOT);
        node("granted", registerType(builder("granted_type", SkillTier.SMALL)), "a1", "a2");
        small("a2", "granted");

        ShipSkillData shipData = generate(2, "granted").shipData();

        assertEquals(List.of(ROOT, "granted", "a1", "a2"), List.copyOf(shipData.getAllocatedNodeIds()));
        assertEquals(3, shipData.usedNodeSlots());
    }

    @Test
    void theBuilderAvoidsNodesThatConflictWithAGrant() {
        root();
        node("rival", registerType(builder("rival_type", SkillTier.NOTABLE)), ROOT);
        node("granted", registerType(builder("granted_type", SkillTier.NOTABLE).exclusiveSkillTypeIds(List.of("rival_type"))));

        ShipSkillData shipData = generate(1, "granted").shipData();

        assertTrue(shipData.isFleetGranted("granted"));
        assertFalse(shipData.isAllocated("rival"));
    }

    @Test
    void aGrantTheHullCannotTakeIsSkipped() {
        root();
        small("a1", ROOT);
        node("granted", registerType(builder("granted_type", SkillTier.NOTABLE).requiredHullSizes(List.of(HullSize.CAPITAL_SHIP))));

        NpcTreeBuild build = generate(1, "granted");

        assertFalse(build.shipData().isAllocated("granted"));
        assertTrue(build.steps().contains(new NpcBuildStep("granted", NpcBuildStep.GRANT_SKIPPED)));
    }

    @Test
    void theTreeTagKeepsGrantsAndTheNodesBridgedThroughThem() {
        root();
        small("a1", ROOT);
        node("granted", registerType(builder("granted_type", SkillTier.SMALL)), "a1", "a2");
        small("a2", "granted");
        String tag = NpcTreeTag.encode(generate(2, "granted").shipData());

        ShipSkillData decoded = NpcTreeTag.decode(tag);

        assertTrue(decoded.isFleetGranted("granted"));
        assertTrue(decoded.isAllocated("a1") && decoded.isAllocated("a2"));
        assertEquals(2, NpcTreeTag.allocatedNodeCount(tag));
    }

    @Test
    void droppingGrantsReleasesThemAndTheNodesBridgedThroughThem() {
        root();
        small("a1", ROOT);
        node("granted", registerType(builder("granted_type", SkillTier.SMALL)), "a1", "a2");
        small("a2", "granted");
        ShipSkillData shipData = generate(2, "granted").shipData();

        List<String> released = ShipTrees.dropFleetGrants(shipData);

        assertEquals(Set.of("granted", "a2"), Set.copyOf(released));
        assertEquals(List.of(ROOT, "a1"), List.copyOf(shipData.getAllocatedNodeIds()));
    }
}
