package exiledsector.skills.npc;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.skilleffect.ShieldSkillEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.tags.ShipProfile;
import exiledsector.skills.tags.WeaponKind;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcSkillTreeBuilderTest {

    private static final ShipProfile SHIELDED_BALLISTIC_FRIGATE =
            new ShipProfile(HullSize.FRIGATE, ShieldType.FRONT, 0, Set.of(WeaponKind.BALLISTIC), false);
    private static final ShipProfile OMNI_SHIELDED_FRIGATE =
            new ShipProfile(HullSize.FRIGATE, ShieldType.OMNI, 0, Set.of(WeaponKind.BALLISTIC), false);
    private static final String ROOT = "root";

    @BeforeEach
    void setUp() {
        RealSkillData.clear();
        register(new SkillNode(ROOT, registerType(builder("root_type", SkillTier.ROOT).build()), List.of(), 0f, 0f));
    }

    @AfterEach
    void tearDown() {
        RealSkillData.clear();
    }

    private static SkillType.Builder builder(String typeId, SkillTier tier) {
        return new SkillType.Builder(typeId, typeId + " name", "a.png", tier);
    }

    private static SkillType registerType(SkillType type) {
        SkillTree.registerType(type);
        return type;
    }

    private static SkillNode register(SkillNode node) {
        SkillTree.register(node);
        return node;
    }

    private static SkillNode node(String id, SkillType type, String... connectedTo) {
        return register(new SkillNode(id, type, List.of(connectedTo), 0f, 0f));
    }

    private static SkillNode small(String id, String... connectedTo) {
        return node(id, registerType(builder(id + "_type", SkillTier.SMALL).build()), connectedTo);
    }

    private static SkillType effectType(String typeId, SkillEffect effect) {
        return registerType(builder(typeId, SkillTier.NOTABLE).effects(List.of(new SkillTypeEffect(effect, 100f))).build());
    }

    private static NpcLayoutEntry entry(String nodeId) {
        return new NpcLayoutEntry(nodeId, null);
    }

    private static NpcLayoutEntry entry(String nodeId, String optionTypeId) {
        return new NpcLayoutEntry(nodeId, optionTypeId);
    }

    private static NpcLayout layout(NpcLayoutEntry... entries) {
        return layoutWithRoot(ROOT, entries);
    }

    private static NpcLayout layoutWithRoot(String rootNodeId, NpcLayoutEntry... entries) {
        return new NpcLayout("layout", "Layout", rootNodeId, List.of(), "", List.of(entries));
    }

    private static NpcTreeBuild build(NpcLayout layout, int nodeCount) {
        return NpcSkillTreeBuilder.build(layout, nodeCount, SHIELDED_BALLISTIC_FRIGATE, NpcHullMods.NONE);
    }

    private static NpcHullMods permanent(String... hullModIds) {
        return new NpcHullMods(Set.of(), Set.of(hullModIds));
    }

    private static NpcHullMods removable(String... hullModIds) {
        return new NpcHullMods(Set.of(hullModIds), Set.of());
    }

    private static NpcTreeBuild buildWith(NpcLayout layout, int nodeCount, NpcHullMods hullMods) {
        return NpcSkillTreeBuilder.build(layout, nodeCount, SHIELDED_BALLISTIC_FRIGATE, hullMods);
    }

    private static SkillNode hullModNode(String id, String hullModId, String... connectedTo) {
        return node(id, registerType(builder(hullModId + "_node", SkillTier.NOTABLE).exclusiveHullModIds(List.of(hullModId)).build()),
                connectedTo);
    }

    private static List<String> outcomes(NpcTreeBuild build) {
        return build.steps().stream().map(NpcBuildStep::outcome).toList();
    }

    private static void chain(int length) {
        String previous = ROOT;
        for (int i = 1; i <= length; i++) {
            small("n" + i, previous);
            previous = "n" + i;
        }
    }

    private static NpcLayout chainLayout(int length) {
        List<NpcLayoutEntry> entries = new ArrayList<>();
        for (int i = 1; i <= length; i++) {
            entries.add(entry("n" + i));
        }
        return layout(entries.toArray(new NpcLayoutEntry[0]));
    }

    @Test
    void theRootIsAllocatedForFreeOnTopOfTheNodeCount() {
        chain(3);

        NpcTreeBuild build = build(chainLayout(3), 2);

        assertEquals(List.of(ROOT, "n1", "n2"), List.copyOf(build.data().getAllocatedNodeIds()));
        assertEquals(2, build.allocatedCount());
        assertEquals(0, build.data().getSpentOp(1));
    }

    @Test
    void stopsOnceTheNodeCountIsReached() {
        chain(4);

        NpcTreeBuild build = build(chainLayout(4), 2);

        assertEquals(List.of("allocated", "allocated", "count reached", "count reached"), outcomes(build));
        assertFalse(build.data().isAllocated("n3"));
    }

    @Test
    void everyAllocatedNonRootNodeIsFree() {
        chain(3);

        ShipSkillData data = build(chainLayout(3), 3).data();

        assertTrue(data.isFreeNode("n1"));
        assertTrue(data.isFreeNode("n2"));
        assertTrue(data.isFreeNode("n3"));
        assertEquals(0, data.getSpentOp(1));
        assertEquals(0, data.getBankedFreeAllocations());
    }

    @Test
    void theLevelEqualsTheNodeCountEvenWhenTheLayoutRunsOut() {
        chain(3);

        NpcTreeBuild build = build(chainLayout(3), 5);

        assertEquals(5, build.data().getLevel());
        assertEquals(3, build.allocatedCount());
    }

    @Test
    void theNodeCountIsCappedAtSixty() {
        chain(70);

        NpcTreeBuild build = build(chainLayout(70), 75);

        assertEquals(NpcSkillTreeBuilder.MAX_NODE_COUNT, build.allocatedCount());
        assertEquals(NpcSkillTreeBuilder.MAX_NODE_COUNT, build.data().getLevel());
    }

    @Test
    void aNegativeNodeCountAllocatesOnlyTheRoot() {
        chain(2);

        NpcTreeBuild build = build(chainLayout(2), -3);

        assertEquals(List.of(ROOT), List.copyOf(build.data().getAllocatedNodeIds()));
        assertEquals(0, build.data().getLevel());
        assertEquals(List.of("count reached", "count reached"), outcomes(build));
    }

    @Test
    void skipsUnknownNodesWithoutUsingUpTheCount() {
        chain(2);

        NpcTreeBuild build = build(layout(entry("ghost"), entry("n1"), entry("n2")), 2);

        assertEquals(List.of("unknown node", "allocated", "allocated"), outcomes(build));
    }

    @Test
    void skipsTheRootAndRepeatedEntries() {
        chain(1);

        NpcTreeBuild build = build(layout(entry(ROOT), entry("n1"), entry("n1")), 5);

        assertEquals(List.of("already allocated", "allocated", "already allocated"), outcomes(build));
    }

    @Test
    void skipsWormholeNodes() {
        node("wormhole_1", registerType(builder("wormhole", SkillTier.WORMHOLE).build()), ROOT);

        assertEquals(List.of("wormhole"), outcomes(build(layout(entry("wormhole_1")), 5)));
    }

    @Test
    void skipsOtherRootNodes() {
        node("other_root", registerType(builder("other_root_type", SkillTier.ROOT).build()), ROOT);

        NpcTreeBuild build = build(layout(entry("other_root")), 5);

        assertEquals(List.of("root node"), outcomes(build));
        assertFalse(build.data().isAllocated("other_root"));
    }

    @Test
    void skipsNodesWhoseNodeTagsHaveAnUnmetRequirement() {
        register(new SkillNode("phase_1", registerType(builder("phase", SkillTier.SMALL).build()), List.of(ROOT), 0f, 0f,
                SkillNodeDecoration.NONE, List.of("inner", "req_phase")));

        assertEquals(List.of("unmet requirement: req_phase"), outcomes(build(layout(entry("phase_1")), 5)));
    }

    @Test
    void skipsNodesWhoseTypeTagsHaveAnUnmetRequirement() {
        node("missile_1", registerType(builder("missile", SkillTier.SMALL).tags(List.of("missile", "req_missile")).build()), ROOT);

        assertEquals(List.of("unmet requirement: req_missile"), outcomes(build(layout(entry("missile_1")), 5)));
    }

    @Test
    void skipsOptionalNodesWhoseChosenOptionHasAnUnmetRequirement() {
        registerType(builder("fighter_option", SkillTier.SMALL).tags(List.of("req_fighter_bays")).build());
        registerType(builder("hull_option", SkillTier.SMALL).build());
        node("optional_1", registerType(builder("optional", SkillTier.SMALL)
                .optionalOptionIds(List.of("fighter_option", "hull_option")).build()), ROOT);

        NpcTreeBuild blocked = build(layout(entry("optional_1", "fighter_option")), 5);
        NpcTreeBuild allowed = build(layout(entry("optional_1", "hull_option")), 5);

        assertEquals(List.of("unmet requirement: req_fighter_bays"), outcomes(blocked));
        assertEquals(List.of("allocated"), outcomes(allowed));
    }

    @Test
    void skipsNodesRestrictedToOtherHullSizes() {
        node("escort_1", registerType(builder("escort", SkillTier.SMALL)
                .requiredHullSizes(List.of(HullSize.DESTROYER, HullSize.CRUISER)).build()), ROOT);
        node("frigate_1", registerType(builder("frigate_only", SkillTier.SMALL)
                .requiredHullSizes(List.of(HullSize.FRIGATE)).build()), ROOT);

        assertEquals(List.of(NpcBuildStep.WRONG_HULL_SIZE, NpcBuildStep.ALLOCATED),
                outcomes(build(layout(entry("escort_1"), entry("frigate_1")), 5)));
    }

    @Test
    void skipsOptionalNodesWhoseChosenOptionIsRestrictedToOtherHullSizes() {
        registerType(builder("capital_option", SkillTier.SMALL).requiredHullSizes(List.of(HullSize.CAPITAL_SHIP)).build());
        node("optional_1", registerType(builder("optional", SkillTier.SMALL)
                .optionalOptionIds(List.of("capital_option")).build()), ROOT);

        assertEquals(List.of(NpcBuildStep.WRONG_HULL_SIZE), outcomes(build(layout(entry("optional_1", "capital_option")), 5)));
    }

    @Test
    void skipsNodesThatConflictWithAPermanentHullmod() {
        node("so_1", registerType(builder("so", SkillTier.SMALL).exclusiveHullModIds(List.of("safetyoverrides")).build()), ROOT);
        node("armor_1", registerType(builder("armor", SkillTier.SMALL).vanillaHullModId("heavyarmor").build()), ROOT);
        NpcLayout layout = layout(entry("so_1"), entry("armor_1"));

        NpcTreeBuild build = NpcSkillTreeBuilder.build(layout, 5, SHIELDED_BALLISTIC_FRIGATE,
                permanent("safetyoverrides", "heavyarmor"));

        assertEquals(List.of("conflicts with installed hullmod: safetyoverrides", "conflicts with installed hullmod: heavyarmor"),
                outcomes(build));
    }

    @Test
    void skipsOptionalNodesWhoseChosenOptionConflictsWithAPermanentHullmod() {
        registerType(builder("cargo_option", SkillTier.SMALL).installedHullModIds(List.of("expanded_cargo_holds")).build());
        node("optional_1", registerType(builder("optional", SkillTier.SMALL).optionalOptionIds(List.of("cargo_option")).build()), ROOT);

        NpcTreeBuild build = NpcSkillTreeBuilder.build(layout(entry("optional_1", "cargo_option")), 5,
                SHIELDED_BALLISTIC_FRIGATE, permanent("expanded_cargo_holds"));

        assertEquals(List.of("conflicts with installed hullmod: expanded_cargo_holds"), outcomes(build));
    }

    @Test
    void nullHullmodsAreTreatedAsNone() {
        node("so_1", registerType(builder("so", SkillTier.SMALL).exclusiveHullModIds(List.of("safetyoverrides")).build()), ROOT);

        NpcTreeBuild build = NpcSkillTreeBuilder.build(layout(entry("so_1")), 5, SHIELDED_BALLISTIC_FRIGATE, null);

        assertEquals(List.of("allocated"), outcomes(build));
    }

    @Test
    void skipsANodeWhoseTypeExcludesAnAllocatedType() {
        node("first_1", registerType(builder("first", SkillTier.SMALL).build()), ROOT);
        node("second_1", registerType(builder("second", SkillTier.SMALL).exclusiveSkillTypeIds(List.of("first")).build()), ROOT);

        NpcTreeBuild build = build(layout(entry("first_1"), entry("second_1")), 5);

        assertEquals(List.of("allocated", "exclusive with allocated type: first"), outcomes(build));
    }

    @Test
    void skipsANodeWhoseTypeIsExcludedByAnAllocatedType() {
        node("first_1", registerType(builder("first", SkillTier.SMALL).exclusiveSkillTypeIds(List.of("second")).build()), ROOT);
        node("second_1", registerType(builder("second", SkillTier.SMALL).build()), ROOT);

        NpcTreeBuild build = build(layout(entry("first_1"), entry("second_1")), 5);

        assertEquals(List.of("allocated", "exclusive with allocated type: first"), outcomes(build));
        assertFalse(build.data().isAllocated("second_1"));
    }

    @Test
    void exclusivityConsidersTheOptionChosenOnAnAllocatedOptionalNode() {
        registerType(builder("vents_option", SkillTier.SMALL).exclusiveSkillTypeIds(List.of("second")).build());
        node("optional_1", registerType(builder("optional", SkillTier.SMALL).optionalOptionIds(List.of("vents_option")).build()), ROOT);
        node("second_1", registerType(builder("second", SkillTier.SMALL).build()), ROOT);

        NpcTreeBuild build = build(layout(entry("optional_1", "vents_option"), entry("second_1")), 5);

        assertEquals(List.of("allocated", "exclusive with allocated type: vents_option"), outcomes(build));
    }

    @Test
    void exclusivityConsidersTheOptionChosenOnTheCandidate() {
        node("first_1", registerType(builder("first", SkillTier.SMALL).build()), ROOT);
        registerType(builder("vents_option", SkillTier.SMALL).exclusiveSkillTypeIds(List.of("first")).build());
        node("optional_1", registerType(builder("optional", SkillTier.SMALL).optionalOptionIds(List.of("vents_option")).build()), ROOT);

        NpcTreeBuild build = build(layout(entry("first_1"), entry("optional_1", "vents_option")), 5);

        assertEquals(List.of("allocated", "exclusive with allocated type: first"), outcomes(build));
    }

    @Test
    void skipsNodesThatAreNotConnectedYetAndNeverRevisitsThem() {
        small("a", ROOT);
        small("b", "a");

        NpcTreeBuild build = build(layout(entry("b"), entry("a")), 5);

        assertEquals(List.of("not connected", "allocated"), outcomes(build));
        assertFalse(build.data().isAllocated("b"));
    }

    @Test
    void connectivityOnlyFollowsTheCandidatesOwnConnections() {
        small("a", ROOT, "x");
        small("x", "elsewhere");

        NpcTreeBuild build = build(layout(entry("a"), entry("x")), 5);

        assertEquals(List.of("allocated", "not connected"), outcomes(build));
    }

    @Test
    void selectsTheChosenOptionOnOptionalNodes() {
        registerType(builder("caps", SkillTier.SMALL).build());
        registerType(builder("vents", SkillTier.SMALL).build());
        node("optional_1", registerType(builder("optional", SkillTier.SMALL).optionalOptionIds(List.of("caps", "vents")).build()), ROOT);

        ShipSkillData data = build(layout(entry("optional_1", "vents")), 5).data();

        assertTrue(data.isAllocated("optional_1"));
        assertEquals("vents", data.getOptionalSelection("optional_1"));
        assertTrue(data.isFreeNode("optional_1"));
    }

    @Test
    void skipsOptionalNodesWithAMissingOrInvalidOption() {
        registerType(builder("caps", SkillTier.SMALL).build());
        registerType(builder("stranger", SkillTier.SMALL).build());
        node("optional_1", registerType(builder("optional", SkillTier.SMALL).optionalOptionIds(List.of("caps", "ghost")).build()), ROOT);

        NpcTreeBuild build = build(layout(entry("optional_1"), entry("optional_1", "stranger"), entry("optional_1", "ghost")), 5);

        assertEquals(List.of("missing option", "invalid option: stranger", "invalid option: ghost"), outcomes(build));
    }

    @Test
    void skipsAnOptionGivenForANonOptionalNode() {
        registerType(builder("caps", SkillTier.SMALL).build());
        small("a", ROOT);

        assertEquals(List.of("unexpected option: caps"), outcomes(build(layout(entry("a", "caps")), 5)));
    }

    @Test
    void anUnknownOrNonRootRootAllocatesNothing() {
        small("a", ROOT);

        NpcTreeBuild unknownRoot = build(layoutWithRoot("ghost", entry("a")), 5);
        NpcTreeBuild smallRoot = build(layoutWithRoot("a", entry("a")), 5);

        assertEquals(List.of("invalid root: ghost"), outcomes(unknownRoot));
        assertTrue(unknownRoot.data().getAllocatedNodeIds().isEmpty());
        assertEquals(List.of("invalid root: a"), outcomes(smallRoot));
        assertTrue(smallRoot.data().getAllocatedNodeIds().isEmpty());
    }

    @Test
    void anInvalidRootBuildsTheSameEmptyTreeItsTagDecodesTo() {
        small("a", ROOT);

        ShipSkillData built = build(layoutWithRoot("ghost", entry("a")), 5).data();
        ShipSkillData decoded = NpcTreeTag.decode(NpcTreeTag.encode("layout", built));

        assertEquals(0, built.getLevel());
        assertEquals(built.getLevel(), decoded.getLevel());
        assertEquals(built.getBankedFreeAllocations(), decoded.getBankedFreeAllocations());
        assertTrue(built.isNpcBuild());
        assertTrue(decoded.isNpcBuild());
    }

    @Test
    void recordsEveryEntryInOrder() {
        chain(2);

        NpcTreeBuild build = build(layout(entry("n2"), entry("ghost"), entry("n1"), entry("n2")), 5);

        assertEquals(List.of("n2", "ghost", "n1", "n2"), build.steps().stream().map(NpcBuildStep::nodeId).toList());
        assertEquals(List.of("not connected", "unknown node", "allocated", "allocated"), outcomes(build));
        assertEquals(List.of("n1", "n2"), build.allocatedNodeIds());
    }

    @Test
    void theBuiltDataIsMarkedAsAnNpcBuild() {
        chain(1);

        assertTrue(build(chainLayout(1), 1).data().isNpcBuild());
    }

    @Test
    void skipsAFrontShieldConversionOnAShipThatAlreadyHasFrontShields() {
        node("front_1", effectType("front", ShieldSkillEffect.CONVERT_SHIELD_TO_FRONT), ROOT);

        NpcTreeBuild frontShielded = build(layout(entry("front_1")), 5);
        NpcTreeBuild omniShielded = NpcSkillTreeBuilder.build(layout(entry("front_1")), 5, OMNI_SHIELDED_FRIGATE, NpcHullMods.NONE);

        assertEquals(List.of("blocked by ship state: Ship already has front shields."), outcomes(frontShielded));
        assertFalse(frontShielded.data().isAllocated("front_1"));
        assertEquals(List.of("allocated"), outcomes(omniShielded));
    }

    @Test
    void shieldStateBlocksFollowConversionsAllocatedEarlierInTheBuild() {
        node("omni_1", effectType("omni", ShieldSkillEffect.CONVERT_SHIELD_TO_OMNI), ROOT);
        node("front_1", effectType("front", ShieldSkillEffect.CONVERT_SHIELD_TO_FRONT), ROOT);
        node("front_2", effectType("front_again", ShieldSkillEffect.CONVERT_SHIELD_TO_FRONT), ROOT);

        NpcTreeBuild build = NpcSkillTreeBuilder.build(layout(entry("omni_1"), entry("front_1"), entry("front_2")), 5,
                OMNI_SHIELDED_FRIGATE, NpcHullMods.NONE);

        assertEquals(List.of("blocked by ship state: Ship already has omni-directional shields.", "allocated",
                "blocked by ship state: Ship already has front shields."), outcomes(build));
    }

    @Test
    void shieldStateBlocksApplyToTheChosenOptionOfAnOptionalNode() {
        effectType("front_option", ShieldSkillEffect.CONVERT_SHIELD_TO_FRONT);
        registerType(builder("hull_option", SkillTier.SMALL).build());
        node("optional_1", registerType(builder("optional", SkillTier.SMALL)
                .optionalOptionIds(List.of("front_option", "hull_option")).build()), ROOT);

        NpcTreeBuild blocked = build(layout(entry("optional_1", "front_option")), 5);
        NpcTreeBuild allowed = build(layout(entry("optional_1", "hull_option")), 5);

        assertEquals(List.of("blocked by ship state: Ship already has front shields."), outcomes(blocked));
        assertEquals(List.of("allocated"), outcomes(allowed));
    }

    @Test
    void buildsAreDeterministic() {
        chain(10);
        small("side", "n3");
        NpcLayout layout = layout(entry("n1"), entry("side"), entry("n2"), entry("n3"), entry("side"), entry("n4"));

        NpcTreeBuild first = build(layout, 4);
        NpcTreeBuild second = build(layout, 4);

        assertEquals(first.steps(), second.steps());
        assertEquals(List.copyOf(first.data().getAllocatedNodeIds()), List.copyOf(second.data().getAllocatedNodeIds()));
    }

    @Test
    void aRemovableHullmodIsConvertedByPathingToItsEquivalentNodeAndStripped() {
        small("a", ROOT);
        hullModNode("armor_1", "heavyarmor", "a");
        small("b", ROOT);

        NpcTreeBuild build = buildWith(layout(entry("b"), entry("armor_1")), 4, removable("heavyarmor"));

        assertEquals(List.of("allocated: path to converted hullmod heavyarmor", "allocated: converts hullmod heavyarmor",
                "allocated", "already allocated"), outcomes(build));
        assertEquals(List.of("heavyarmor"), build.strippedHullModIds());
        assertTrue(build.data().isAllocated("armor_1"));
        assertTrue(build.data().isFreeNode("armor_1"));
        assertEquals(3, build.allocatedCount());
    }

    @Test
    void aHullmodSplitAcrossSeveralNotablesConvertsIntoTheHalfTheShipFits() {
        SkillType energyHalf = registerType(builder("magazines_energy", SkillTier.NOTABLE)
                .exclusiveHullModIds(List.of("magazines")).tags(List.of("req_energy")).build());
        SkillType ballisticHalf = registerType(builder("magazines_ballistic", SkillTier.NOTABLE)
                .exclusiveHullModIds(List.of("magazines")).tags(List.of("req_ballistic")).build());
        node("energy_1", energyHalf, ROOT);
        small("a", ROOT);
        node("ballistic_1", ballisticHalf, "a");

        NpcTreeBuild build = buildWith(layout(), 4, removable("magazines"));

        assertTrue(build.data().isAllocated("ballistic_1"));
        assertFalse(build.data().isAllocated("energy_1"));
        assertEquals(List.of("magazines"), build.strippedHullModIds());
    }

    @Test
    void conversionPathsSpendTheNodeBudgetBeforeTheLayout() {
        small("a", ROOT);
        hullModNode("armor_1", "heavyarmor", "a");
        chain(3);

        NpcTreeBuild build = buildWith(chainLayout(3), 3, removable("heavyarmor"));

        assertEquals(List.of("a", "armor_1", "n1"), build.allocatedNodeIds());
        assertEquals(List.of("allocated: path to converted hullmod heavyarmor", "allocated: converts hullmod heavyarmor",
                "allocated", "count reached", "count reached"), outcomes(build));
    }

    @Test
    void aHullmodWhoseEquivalentNodeIsOutOfReachIsKeptAndItsNodeStaysBlocked() {
        small("a", ROOT);
        small("b", "a");
        hullModNode("armor_1", "heavyarmor", "b");

        NpcTreeBuild build = buildWith(layout(entry("a"), entry("b"), entry("armor_1")), 2, removable("heavyarmor"));

        assertEquals(List.of("hullmod kept, equivalent node out of reach: heavyarmor", "allocated", "allocated", "count reached"),
                outcomes(build));
        assertTrue(build.strippedHullModIds().isEmpty());
        assertFalse(build.data().isAllocated("armor_1"));
    }

    private static NpcTreeBuild buildWithFreedOp(NpcLayout layout, int nodeCount, NpcHullMods hullMods, NpcFreedOp freedOp) {
        return NpcSkillTreeBuilder.build(layout, nodeCount, SHIELDED_BALLISTIC_FRIGATE, hullMods, freedOp);
    }

    private static NpcLayout armorThenChain() {
        small("a", ROOT);
        hullModNode("armor_1", "heavyarmor", "a");
        small("b", "armor_1");
        small("c", "b");
        small("d", "c");
        return layout(entry("b"), entry("c"), entry("d"));
    }

    @Test
    void opFreedByAStrippedHullmodBuysExtraLayoutNodesChargedAtThePerNodeCost() {
        NpcTreeBuild build = buildWithFreedOp(armorThenChain(), 2, removable("heavyarmor"),
                new NpcFreedOp(3, Map.of("heavyarmor", 7), 60));

        assertEquals(List.of("allocated: path to converted hullmod heavyarmor", "allocated: converts hullmod heavyarmor",
                NpcBuildStep.ALLOCATED_WITH_FREED_OP, NpcBuildStep.ALLOCATED_WITH_FREED_OP, "count reached"), outcomes(build));
        ShipSkillData data = build.data();
        assertEquals(6, data.getSpentOp(3));
        assertEquals(0, data.getBankedFreeAllocations());
        assertTrue(data.isFreeNode("armor_1"));
        assertFalse(data.isFreeNode("b"));
        assertFalse(data.isFreeNode("c"));
        assertEquals(2, data.getLevel());
    }

    @Test
    void extraNodesFromFreedOpStopAtTheMaxNodeCount() {
        NpcTreeBuild build = buildWithFreedOp(armorThenChain(), 2, removable("heavyarmor"),
                new NpcFreedOp(1, Map.of("heavyarmor", 10), 3));

        assertEquals(1, build.steps().stream().filter(step -> NpcBuildStep.ALLOCATED_WITH_FREED_OP.equals(step.outcome())).count());
        assertEquals(1, build.data().getSpentOp(1));
    }

    @Test
    void aHullmodThatIsKeptFreesNoOp() {
        NpcTreeBuild build = buildWithFreedOp(armorThenChain(), 1, removable("heavyarmor"),
                new NpcFreedOp(1, Map.of("heavyarmor", 10), 60));

        assertTrue(build.strippedHullModIds().isEmpty());
        assertEquals(0, build.data().getSpentOp(1));
        assertFalse(outcomes(build).contains(NpcBuildStep.ALLOCATED_WITH_FREED_OP));
    }

    @Test
    void permanentHullmodsAreNeverConverted() {
        hullModNode("armor_1", "heavyarmor", ROOT);

        NpcTreeBuild build = buildWith(layout(entry("armor_1")), 5, permanent("heavyarmor"));

        assertEquals(List.of("conflicts with installed hullmod: heavyarmor"), outcomes(build));
        assertTrue(build.strippedHullModIds().isEmpty());
    }

    @Test
    void theNearestConversionsAreMadeFirstWhenTheBudgetCannotCoverAll() {
        small("a", ROOT);
        small("b", "a");
        hullModNode("far_1", "farmod", "b");
        hullModNode("near_1", "nearmod", ROOT);

        NpcTreeBuild build = buildWith(layout(), 2, removable("farmod", "nearmod"));

        assertEquals(List.of("nearmod"), build.strippedHullModIds());
        assertTrue(build.data().isAllocated("near_1"));
        assertFalse(build.data().isAllocated("far_1"));
        assertEquals("hullmod kept, equivalent node out of reach: farmod", outcomes(build).get(1));
    }

    @Test
    void conversionPathsReuseNodesAlreadyAllocatedByEarlierConversions() {
        small("a", ROOT);
        hullModNode("first_1", "firstmod", "a");
        hullModNode("second_1", "secondmod", "a");

        NpcTreeBuild build = buildWith(layout(), 3, removable("firstmod", "secondmod"));

        assertEquals(List.of("firstmod", "secondmod"), build.strippedHullModIds());
        assertEquals(List.of("a", "first_1", "second_1"), build.allocatedNodeIds());
    }

    @Test
    void conversionPathsAvoidNodesTheShipCannotTake() {
        node("shield_1", registerType(builder("shield_type", SkillTier.SMALL).tags(List.of("req_no_shields")).build()), ROOT);
        hullModNode("armor_1", "heavyarmor", "shield_1");
        small("a", ROOT);
        small("b", "a");
        register(new SkillNode("armor_2", SkillTree.getType("heavyarmor_node"), List.of("b"), 0f, 0f));

        NpcTreeBuild build = buildWith(layout(), 5, removable("heavyarmor"));

        assertEquals(List.of("a", "b", "armor_2"), build.allocatedNodeIds());
        assertEquals(List.of("heavyarmor"), build.strippedHullModIds());
    }

    @Test
    void conversionPathsUseTheLayoutsOptionForOptionalNodes() {
        registerType(builder("hull_option", SkillTier.SMALL).build());
        registerType(builder("flux_option", SkillTier.SMALL).build());
        node("optional_1", registerType(builder("optional", SkillTier.SMALL)
                .optionalOptionIds(List.of("flux_option", "hull_option")).build()), ROOT);
        hullModNode("armor_1", "heavyarmor", "optional_1");

        NpcTreeBuild withLayoutOption = buildWith(layout(entry("optional_1", "hull_option")), 5, removable("heavyarmor"));
        NpcTreeBuild withoutLayoutOption = buildWith(layout(), 5, removable("heavyarmor"));

        assertEquals("hull_option", withLayoutOption.data().getOptionalSelection("optional_1"));
        assertEquals("flux_option", withoutLayoutOption.data().getOptionalSelection("optional_1"));
    }

    @Test
    void theVanillaHullmodIdTakesPriorityAsTheEquivalentHullmod() {
        node("rangefinder_1", registerType(builder("rangefinder", SkillTier.KEYSTONE)
                .vanillaHullModId("ballistic_rangefinder").exclusiveHullModIds(List.of("other")).build()), ROOT);

        NpcTreeBuild viaVanillaId = buildWith(layout(), 5, removable("ballistic_rangefinder"));
        NpcTreeBuild viaExclusiveId = buildWith(layout(entry("rangefinder_1")), 5, removable("other"));

        assertEquals(List.of("ballistic_rangefinder"), viaVanillaId.strippedHullModIds());
        assertTrue(viaExclusiveId.strippedHullModIds().isEmpty());
        assertEquals(List.of("conflicts with installed hullmod: other"), outcomes(viaExclusiveId));
    }

    @Test
    void conversionIsDeterministic() {
        small("a", ROOT);
        hullModNode("first_1", "firstmod", "a");
        hullModNode("second_1", "secondmod", ROOT);

        NpcTreeBuild first = buildWith(layout(), 2, removable("secondmod", "firstmod"));
        NpcTreeBuild second = buildWith(layout(), 2, removable("firstmod", "secondmod"));

        assertEquals(first.steps(), second.steps());
        assertEquals(first.strippedHullModIds(), second.strippedHullModIds());
    }
}
