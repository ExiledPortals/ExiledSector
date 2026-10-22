package exiledsector.skills.npc;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledlib.skills.SkillTypeEffect;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledlib.skills.skilleffect.ShieldSkillEffect;
import exiledsector.skills.tags.ShipProfile;
import exiledsector.skills.tags.WeaponKind;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcSkillTreeBuilderTest {

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

    private static SkillType.Builder builder(String typeId, SkillTier tier) {
        return new SkillType.Builder(typeId, typeId + " name", "a.png", tier);
    }

    private static SkillType registerType(SkillType type) {
        SkillTree.registerType(type);
        return type;
    }

    private static SkillNode node(String id, SkillType type, List<String> tags, String... connectedTo) {
        SkillNode node = new SkillNode(id, type, List.of(connectedTo), 0f, 0f, SkillNodeDecoration.NONE, tags);
        SkillTree.register(node);
        return node;
    }

    private static SkillNode node(String id, SkillType type, String... connectedTo) {
        return node(id, type, List.of(), connectedTo);
    }

    private static void root() {
        node(ROOT, registerType(builder("root_type", SkillTier.ROOT).build()));
    }

    private static SkillNode small(String id, String... connectedTo) {
        return node(id, registerType(builder(id + "_type", SkillTier.SMALL).build()), connectedTo);
    }

    private static SkillNode notable(String id, String... connectedTo) {
        return node(id, registerType(builder(id + "_type", SkillTier.NOTABLE).build()), connectedTo);
    }

    private static void chain(String prefix, int length, String from) {
        String previous = from;
        for (int i = 1; i <= length; i++) {
            small(prefix + i, previous);
            previous = prefix + i;
        }
    }

    private static void anchorVolume(String socketId, String coreLink, String exitId, String volume) {
        SkillType socket = SkillTree.getType("socket") != null ? SkillTree.getType("socket") : registerType(builder("socket", SkillTier.SOCKET).build());
        SkillType exit = SkillTree.getType("wormhole_exit") != null ? SkillTree.getType("wormhole_exit")
                : registerType(builder("wormhole_exit", SkillTier.WORMHOLE_EXIT).build());
        if (socketId != null) {
            node(socketId, socket, List.of("core"), coreLink);
        }
        node(exitId, exit, List.of(volume));
    }

    private static void wormholePair(String core, String far, String coreRegion, String farRegion, String coreLink) {
        SkillType wormhole = SkillTree.getType("wormhole") != null ? SkillTree.getType("wormhole")
                : registerType(builder("wormhole", SkillTier.WORMHOLE).build());
        SkillTree.register(new SkillNode(core, wormhole, List.of(coreLink, far), 0f, 0f,
                new SkillNodeDecoration(null, null, null, null, far), List.of(coreRegion)));
        SkillTree.register(new SkillNode(far, wormhole, List.of(core), 0f, 0f,
                new SkillNodeDecoration(null, null, null, null, core), List.of(farRegion)));
    }

    private static NpcTreeBuild generate(int nodeCount) {
        return generate(nodeCount, null, NpcHullMods.NONE, NpcFreedOp.NONE, 1L);
    }

    private static NpcTreeBuild generate(int nodeCount, String factionRegion, NpcHullMods hullMods, NpcFreedOp freedOp, long seed) {
        return generate(BALLISTIC_FRIGATE, null, nodeCount, factionRegion, hullMods, freedOp, seed);
    }

    private static NpcTreeBuild generate(ShipProfile profile, String designType, int nodeCount, String factionRegion,
                                         NpcHullMods hullMods, NpcFreedOp freedOp, long seed) {
        return NpcSkillTreeBuilder.generate(new NpcBuildRequest(profile, designType, factionRegion, hullMods, freedOp, nodeCount,
                NpcTestAnchors.idFor(factionRegion)),
                new Random(seed));
    }

    private static List<String> allocated(NpcTreeBuild build) {
        return List.copyOf(build.shipData().getAllocatedNodeIds());
    }

    @Test
    void theBuilderNeverAimsForASocketOfItsOwn() {
        root();
        SkillType socket = registerType(builder("socket", SkillTier.SOCKET).build());
        node("lonely_socket", socket, ROOT);
        chain("a", 3, ROOT);

        NpcTreeBuild build = generate(3);

        assertFalse(allocated(build).contains("lonely_socket"));
    }

    @Test
    void theRootMatchesTheHullsDesignType() {
        for (String type : List.of("root_low_tech", "root_midline", "root_high_tech")) {
            node(type + "_1", registerType(builder(type, SkillTier.ROOT).build()));
        }

        assertEquals("root_midline_1", allocated(generate(BALLISTIC_FRIGATE, "Midline", 0, null, NpcHullMods.NONE,
                NpcFreedOp.NONE, 1L)).get(0));
        assertEquals("root_low_tech_1", allocated(generate(BALLISTIC_FRIGATE, "Low Tech", 0, null, NpcHullMods.NONE,
                NpcFreedOp.NONE, 1L)).get(0));
        assertEquals("root_high_tech_1", allocated(generate(BALLISTIC_FRIGATE, "High Tech", 0, null, NpcHullMods.NONE,
                NpcFreedOp.NONE, 1L)).get(0));
    }

    @Test
    void anUnknownDesignTypeStartsAtARandomRootThatTheSeedDecides() {
        for (String type : List.of("root_low_tech", "root_midline", "root_high_tech")) {
            node(type + "_1", registerType(builder(type, SkillTier.ROOT).build()));
        }

        String first = allocated(generate(BALLISTIC_FRIGATE, "Pirate", 0, null, NpcHullMods.NONE, NpcFreedOp.NONE, 7L)).get(0);
        String again = allocated(generate(BALLISTIC_FRIGATE, "Pirate", 0, null, NpcHullMods.NONE, NpcFreedOp.NONE, 7L)).get(0);

        assertEquals(first, again);
        assertTrue(first.startsWith("root_"));
    }

    @Test
    void npcsPassThroughASocketOnTheWayButNeverAimForOne() {
        root();
        SkillType socket = registerType(builder("socket", SkillTier.SOCKET).build());
        node("socket_1", socket, ROOT);
        notable("behind_socket", "socket_1");
        node("dead_end_socket", socket, ROOT);
        chain("n", 3, ROOT);

        List<String> allocated = allocated(generate(10));

        assertTrue(allocated.contains("socket_1") && allocated.contains("behind_socket"));
        assertFalse(allocated.contains("dead_end_socket"));
        assertTrue(allocated.contains("n3"));
    }

    @Test
    void theRootIsFreeAndTheTreeReachesTheRolledCount() {
        root();
        chain("n", 10, ROOT);

        NpcTreeBuild build = generate(7);

        assertEquals(8, build.shipData().getAllocatedNodeIds().size());
        assertEquals(7, build.shipData().getLevel());
        assertEquals(0, ShipTrees.getSpentOp(build.shipData(), 1));
    }

    @Test
    void theNodeCountIsCappedAtTheMaximum() {
        root();
        chain("n", 80, ROOT);

        assertEquals(NpcSkillTreeBuilder.MAX_NODE_COUNT + 1, generate(500).shipData().getAllocatedNodeIds().size());
    }

    @Test
    void strippingAHullmodTheTreeReplacesBuysExtraNodesWithItsOp() {
        root();
        node("armor_node", registerType(builder("armor_node_type", SkillTier.NOTABLE).exclusiveHullModIds(List.of("heavyarmor")).build()), ROOT);
        chain("n", 10, "armor_node");
        NpcFreedOp freedOp = new NpcFreedOp(2, Map.of("heavyarmor", 6), 60);

        NpcTreeBuild build = generate(1, null, new NpcHullMods(Set.of("heavyarmor"), Set.of()), freedOp, 1L);

        assertEquals(List.of("heavyarmor"), build.strippedHullModIds());
        assertEquals(List.of(ROOT, "armor_node", "n1", "n2", "n3"), allocated(build));
        assertEquals(6, ShipTrees.getSpentOp(build.shipData(), 2));
        assertTrue(build.steps().get(0).outcome().startsWith(NpcBuildStep.CONVERTED_HULLMOD));
    }

    @Test
    void aHullmodWhoseNodeIsOutOfReachIsPutBackAndItsOpIsNotSpent() {
        root();
        chain("n", 5, ROOT);
        node("armor_node", registerType(builder("armor_node_type", SkillTier.NOTABLE).exclusiveHullModIds(List.of("heavyarmor")).build()), "n5");
        NpcFreedOp freedOp = new NpcFreedOp(2, Map.of("heavyarmor", 2), 60);

        NpcTreeBuild build = generate(2, null, new NpcHullMods(Set.of("heavyarmor"), Set.of()), freedOp, 1L);

        assertTrue(build.strippedHullModIds().isEmpty());
        assertTrue(build.steps().stream().anyMatch(step -> step.outcome().equals(NpcBuildStep.HULLMOD_KEPT + "heavyarmor")));
        assertEquals(3, build.shipData().getAllocatedNodeIds().size());
        assertEquals(0, ShipTrees.getSpentOp(build.shipData(), 2));
    }

    @Test
    void aShortBudgetGoesToTheNearestChosenNotablesWithoutSmallDetours() {
        root();
        small("s1", ROOT);
        notable("near", "s1");
        small("t1", ROOT);
        small("t2", "t1");
        notable("far", "t2");
        small("leaf", ROOT);

        for (long seed = 1; seed <= 20; seed++) {
            NpcTreeBuild build = generate(5, null, NpcHullMods.NONE, NpcFreedOp.NONE, seed);

            assertEquals(List.of(ROOT, "s1", "near", "t1", "t2", "far"), allocated(build), "seed " + seed);
        }
    }

    @Test
    void smallNodesFillTheBudgetOnlyWhenNoNotableIsAffordable() {
        root();
        small("s1", ROOT);
        notable("notable", "s1");
        small("leaf", ROOT);

        NpcTreeBuild build = generate(1);

        assertEquals(2, build.shipData().getAllocatedNodeIds().size());
        assertFalse(build.shipData().isAllocated("notable"));
    }

    @Test
    void anotherRootIsBoughtAsAPaidNodeOnTheWayToANotableAndSurvivesTheTag() {
        root();
        node("other_root", registerType(builder("other_root_type", SkillTier.ROOT).build()), ROOT);
        notable("notable", "other_root");

        NpcTreeBuild build = generate(2);
        ShipSkillData decoded = NpcTreeTag.decode(NpcTreeTag.encode(build.shipData()));

        assertEquals(List.of(ROOT, "other_root", "notable"), allocated(build));
        assertEquals(ROOT, ShipTrees.resolveStartingRootId(build.shipData()));
        assertEquals(allocated(build), List.copyOf(decoded.getAllocatedNodeIds()));
        assertEquals(ROOT, ShipTrees.resolveStartingRootId(decoded));
    }

    @Test
    void anotherRootCanFillTheBudgetLikeASmallNode() {
        root();
        node("other_root", registerType(builder("other_root_type", SkillTier.ROOT).build()), ROOT);

        assertEquals(List.of(ROOT, "other_root"), allocated(generate(1)));
    }


    @Test
    void theFactionNotableIsSkippedWhenTheBudgetCannotReachIt() {
        root();
        small("s1", ROOT);
        anchorVolume("socket", "s1", "gate_far", "hegemony");
        node("pride", registerType(builder("pride_type", SkillTier.NOTABLE).build()), List.of("hegemony"), "gate_far");
        notable("core_notable", ROOT);

        NpcTreeBuild build = generate(1, "hegemony", NpcHullMods.NONE, NpcFreedOp.NONE, 1L);

        assertFalse(build.shipData().isAllocated("pride"));
        assertTrue(build.shipData().isAllocated("core_notable"));
    }

    @Test
    void shipsOnlyEverOpenTheirOwnFactionsVolume() {
        root();
        chain("c", 3, ROOT);
        anchorVolume("socket", "c1", "heg_far", "hegemony");
        node("heg_notable", registerType(builder("heg_type", SkillTier.NOTABLE).build()), List.of("hegemony"), "heg_far");
        anchorVolume(null, null, "tt_far", "tritachyon");
        node("tt_notable", registerType(builder("tt_type", SkillTier.NOTABLE).build()), List.of("tritachyon"), "tt_far");
        wormholePair("short_a", "short_b", "core", "core", "c3");
        node("beyond_shortcut", registerType(builder("beyond_type", SkillTier.NOTABLE).build()), List.of("core"), "short_b");
        anchorVolume(null, null, "lost_far", "enigma");
        node("enigma_notable", registerType(builder("enigma_type", SkillTier.NOTABLE).build()), List.of("enigma"), "lost_far");

        NpcTreeBuild hegemony = generate(30, "hegemony", NpcHullMods.NONE, NpcFreedOp.NONE, 1L);
        NpcTreeBuild independent = generate(30, null, NpcHullMods.NONE, NpcFreedOp.NONE, 1L);

        assertTrue(hegemony.shipData().isAllocated("heg_notable"));
        for (String forbidden : List.of("tt_far", "tt_notable", "short_a", "beyond_shortcut", "lost_far", "enigma_notable")) {
            assertFalse(hegemony.shipData().isAllocated(forbidden), forbidden);
        }
        for (String forbidden : List.of("heg_far", "heg_notable", "tt_far", "short_a")) {
            assertFalse(independent.shipData().isAllocated(forbidden), forbidden);
        }
        assertTrue(independent.shipData().getSocketedItems().isEmpty());
    }

    @Test
    void nodesTheShipCannotUseAreNeverTaken() {
        root();
        node("missile_notable", registerType(builder("missile_notable_type", SkillTier.NOTABLE).tags(List.of("req_missile")).build()), ROOT);
        node("first", registerType(builder("first_type", SkillTier.NOTABLE).exclusiveSkillTypeIds(List.of("second_type")).build()), ROOT);
        node("second", registerType(builder("second_type", SkillTier.NOTABLE).build()), ROOT);

        NpcTreeBuild build = generate(10);

        assertFalse(build.shipData().isAllocated("missile_notable"));
        assertTrue(build.shipData().isAllocated("first") ^ build.shipData().isAllocated("second"));
    }

    @Test
    void optionalNodesOnlyPickOptionsTheShipCanUse() {
        root();
        registerType(builder("missile_option", SkillTier.SMALL).tags(List.of("req_missile")).build());
        registerType(builder("hull_option", SkillTier.SMALL).build());
        node("optional", registerType(builder("optional_type", SkillTier.SMALL)
                .optionalOptionIds(List.of("missile_option", "hull_option")).build()), ROOT);

        for (long seed = 1; seed <= 10; seed++) {
            ShipSkillData data = generate(1, null, NpcHullMods.NONE, NpcFreedOp.NONE, seed).shipData();

            assertEquals("hull_option", data.getOptionalSelection("optional"));
        }
    }

    private static NpcTreeBuild generateFocused(int nodeCount, Map<String, Float> focus, long seed) {
        return NpcSkillTreeBuilder.generate(new NpcBuildRequest(BALLISTIC_FRIGATE, null, null, NpcHullMods.NONE, NpcFreedOp.NONE, nodeCount,
                null, null, null, focus), new Random(seed));
    }

    private static int countSelections(String nodeId, String optionId, Map<String, Float> focus) {
        int selections = 0;
        for (long seed = 1; seed <= 60; seed++) {
            if (optionId.equals(generateFocused(1, focus, seed).shipData().getOptionalSelection(nodeId))) {
                selections++;
            }
        }
        return selections;
    }

    @Test
    void factionFocusSteersWhichOptionAnOptionalNodeTakes() {
        root();
        registerType(builder("armour_option", SkillTier.SMALL).tags(List.of("armour")).build());
        registerType(builder("flux_option", SkillTier.SMALL).tags(List.of("flux")).build());
        node("optional", registerType(builder("optional_type", SkillTier.SMALL)
                .optionalOptionIds(List.of("armour_option", "flux_option")).build()), ROOT);

        int unfocused = countSelections("optional", "armour_option", Map.of());
        int focused = countSelections("optional", "armour_option", Map.of("armour", 30f));

        assertTrue(unfocused < 30, "unfocused " + unfocused);
        assertTrue(focused > 45, "focused " + focused);
    }

    @Test
    void factionFocusWeighsAnOptionalNotableByTheOptionItWouldTake() {
        root();
        registerType(builder("armour_notable_option", SkillTier.NOTABLE).tags(List.of("armour")).build());
        registerType(builder("missile_notable_option", SkillTier.NOTABLE).tags(List.of("armour", "missile", "req_missile")).build());
        node("optional_notable", registerType(builder("optional_notable_type", SkillTier.NOTABLE)
                .optionalOptionIds(List.of("armour_notable_option", "missile_notable_option")).build()), ROOT);
        node("flux_notable", registerType(builder("flux_notable_type", SkillTier.NOTABLE).tags(List.of("flux")).build()), ROOT);

        int unfocused = 0;
        int focused = 0;
        for (long seed = 1; seed <= 60; seed++) {
            unfocused += generateFocused(1, Map.of(), seed).shipData().isAllocated("optional_notable") ? 1 : 0;
            ShipSkillData focusedData = generateFocused(1, Map.of("armour", 30f), seed).shipData();
            if (focusedData.isAllocated("optional_notable")) {
                focused++;
                assertEquals("armour_notable_option", focusedData.getOptionalSelection("optional_notable"));
            }
        }

        assertTrue(unfocused < 30, "unfocused " + unfocused);
        assertTrue(focused > 45, "focused " + focused);
    }

    @Test
    void focusMultipliesTheShipsOwnTagWeights() {
        NpcRelevance plain = NpcRelevance.of(BALLISTIC_FRIGATE, Set.of());
        NpcRelevance focused = NpcRelevance.of(BALLISTIC_FRIGATE, Set.of(), Map.of("ballistic", 2f, "fighter", 3f));

        assertEquals(plain.of(List.of("ballistic")) * 2f, focused.of(List.of("ballistic")), 0.001f);
        assertEquals(plain.of(List.of("fighter")) * 3f, focused.of(List.of("fighter")), 0.001f);
        assertEquals(plain.of(List.of("flux")), focused.of(List.of("flux")), 0.001f);
    }

    @Test
    void lightlyArmouredHullsRarelyChaseArmour() {
        ShipProfile lightFrigate = new ShipProfile(HullSize.FRIGATE, ShieldType.FRONT, 0, Set.of(WeaponKind.ENERGY), false, 100f, false);
        ShipProfile midFrigate = new ShipProfile(HullSize.FRIGATE, ShieldType.FRONT, 0, Set.of(WeaponKind.ENERGY), false, 200f, false);

        assertEquals(NpcRelevance.LIGHT_ARMOUR_WEIGHT, NpcRelevance.of(lightFrigate, Set.of()).of(List.of("armour")), 0.001f);
        assertTrue(NpcRelevance.of(midFrigate, Set.of()).of(List.of("armour")) > NpcRelevance.LIGHT_ARMOUR_WEIGHT);
    }

    @Test
    void theSameSeedAlwaysGrowsTheSameTree() {
        root();
        for (int branch = 1; branch <= 4; branch++) {
            chain("b" + branch + "_", 3, ROOT);
            notable("goal" + branch, "b" + branch + "_3");
        }

        assertEquals(allocated(generate(9, null, NpcHullMods.NONE, NpcFreedOp.NONE, 42L)),
                allocated(generate(9, null, NpcHullMods.NONE, NpcFreedOp.NONE, 42L)));
    }

    @Test
    void relevanceFavoursNodesThatMatchTheShip() {
        NpcRelevance relevance = NpcRelevance.of(BALLISTIC_FRIGATE, Set.of());

        assertTrue(relevance.of(List.of("ballistic")) > relevance.of(List.of("missile")));
        assertTrue(relevance.of(List.of("shield")) > relevance.of(List.of("fighter")));
        assertEquals(NpcRelevance.UNTHEMED, relevance.of(List.of("core")));
    }

    @Test
    void aShipNeverPairsShieldShuntWithShieldNodes() {
        root();
        node("shunt", registerType(builder("shunt_type", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(ShieldSkillEffect.REMOVE_SHIELD, 1f))).build()), ROOT);
        node("emitter", registerType(builder("emitter_type", SkillTier.NOTABLE).tags(List.of("shield", "req_shields")).build()), ROOT);
        small("arc", "emitter");
        node("raise", registerType(builder("raise_type", SkillTier.SMALL).tags(List.of("req_shields")).build()), "shunt");

        for (long seed = 1; seed <= 30; seed++) {
            ShipSkillData data = generate(4, null, NpcHullMods.NONE, NpcFreedOp.NONE, seed).shipData();

            assertFalse(data.isAllocated("shunt") && (data.isAllocated("emitter") || data.isAllocated("raise")), "seed " + seed);
        }
    }

    @Test
    void aShipWithConvertedHangarCanTradeItForTheNoFighterBaysNode() {
        root();
        node("hangar", registerType(builder("hangar_type", SkillTier.KEYSTONE).tags(List.of("fighter", "req_no_fighter_bays"))
                .exclusiveHullModIds(List.of("converted_hangar")).build()), ROOT);
        ShipProfile hangarShip = new ShipProfile(HullSize.DESTROYER, ShieldType.FRONT, 1, Set.of(WeaponKind.BALLISTIC), false, 0f, false);
        NpcHullMods hullMods = new NpcHullMods(Set.of("converted_hangar"), Set.of());

        NpcTreeBuild counted = generate(hangarShip, null, 1, null, hullMods,
                new NpcFreedOp(2, Map.of("converted_hangar", 10), Map.of("converted_hangar", 1), 60), 1L);
        NpcTreeBuild unknown = generate(hangarShip, null, 1, null, hullMods, new NpcFreedOp(2, Map.of("converted_hangar", 10), 60), 1L);

        assertTrue(counted.shipData().isAllocated("hangar"));
        assertEquals(List.of("converted_hangar"), counted.strippedHullModIds());
        assertFalse(unknown.shipData().isAllocated("hangar"));
    }

    @Test
    void theFactionPickCanLandOnAKeystone() {
        root();
        anchorVolume("socket", ROOT, "gate_far", "sindrian_dictat");
        node("lions_gaze", registerType(builder("lions_gaze_type", SkillTier.KEYSTONE).build()), List.of("sindrian_dictat"), "gate_far");

        NpcTreeBuild build = generate(2, "sindrian_dictat", NpcHullMods.NONE, NpcFreedOp.NONE, 1L);

        assertTrue(build.shipData().isAllocated("socket"));

        assertTrue(build.shipData().isAllocated("lions_gaze"));
        assertTrue(build.steps().stream().anyMatch(step -> step.outcome().equals(NpcBuildStep.FACTION_GOAL)));
    }
}
