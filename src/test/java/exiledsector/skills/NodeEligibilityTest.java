package exiledsector.skills;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.npc.RealSkillData;
import exiledsector.skills.skilleffect.DefenseSkillEffect;
import exiledsector.skills.skilleffect.ShieldSkillEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NodeEligibilityTest {

    private static final ShipFacts FRONT_SHIELDED_FRIGATE = facts(ShieldType.FRONT, Set.of());

    private ShipSkillData data;

    @BeforeEach
    void setUp() {
        RealSkillData.clear();
        SkillNode root = node("root", type("root_type", SkillTier.ROOT).build());
        data = new ShipSkillData();
        data.chooseStartingRoot(root);
    }

    @AfterEach
    void tearDown() {
        RealSkillData.clear();
    }

    private static ShipFacts facts(ShieldType shieldType, Set<String> installed) {
        return new ShipFacts(HullSize.FRIGATE, shieldType, shieldType == ShieldType.PHASE, 100f, installed::contains);
    }

    private static SkillType.Builder type(String id, SkillTier tier) {
        return new SkillType.Builder(id, id + " name", "a.png", tier);
    }

    private static SkillType registered(SkillType type) {
        SkillTree.registerType(type);
        return type;
    }

    private static SkillNode node(String id, SkillType type) {
        SkillNode node = new SkillNode(id, type, List.of("root"), 0f, 0f);
        SkillTree.register(node);
        return node;
    }

    private static SkillType effectType(String id, SkillEffect effect) {
        return registered(type(id, SkillTier.NOTABLE).effects(List.of(new SkillTypeEffect(effect, 1f))).build());
    }

    private static SkillNode optionalNode(String id, SkillType.Builder container, SkillType... options) {
        return node(id, registered(container.optionalOptionIds(List.of(options).stream().map(SkillType::getId).toList()).build()));
    }

    private NodeEligibility.Block check(SkillNode node, SkillType option, ShipFacts ship) {
        return NodeEligibility.check(node, option, data, ship);
    }

    @Test
    void aContainersExclusiveHullModBlocksEveryOptionAndIsStrippedForThem() {
        SkillType hull = registered(type("hull", SkillTier.SMALL).build());
        SkillNode slot = optionalNode("slot", type("slot_type", SkillTier.SMALL).exclusiveHullModIds(List.of("heavyarmor")), hull);

        NodeEligibility.Block block = check(slot, hull, facts(ShieldType.FRONT, Set.of("heavyarmor")));

        assertEquals(NodeEligibility.Kind.HULL_MOD_CONFLICT, block.kind());
        assertEquals("heavyarmor", block.detail());
        assertEquals(Set.of("heavyarmor"), AllocatedNode.planned(slot, hull).exclusiveHullModIds());
    }

    @Test
    void aContainerRestrictedToOtherHullSizesBlocksEveryOption() {
        SkillType hull = registered(type("hull", SkillTier.SMALL).build());
        SkillNode slot = optionalNode("slot", type("slot_type", SkillTier.SMALL).requiredHullSizes(List.of(HullSize.CAPITAL_SHIP)), hull);

        assertEquals(NodeEligibility.Kind.WRONG_HULL_SIZE, check(slot, hull, FRONT_SHIELDED_FRIGATE).kind());
    }

    @Test
    void aHullRequirementTagOnTheTypeOrTheChosenOptionBlocksShipsThatDoNotMeetIt() {
        SkillNode militarized = node("militarized", registered(type("militarized_type", SkillTier.NOTABLE)
                .tags(List.of("logistics", "req_civilian_hull")).build()));
        SkillType civilianOption = registered(type("civilian_option", SkillTier.SMALL).tags(List.of("req_civilian_hull")).build());
        SkillNode slot = optionalNode("slot", type("slot_type", SkillTier.SMALL), civilianOption);
        ShipFacts civilian = facts(ShieldType.FRONT, Set.of("civgrade"));

        NodeEligibility.Block block = check(militarized, null, FRONT_SHIELDED_FRIGATE);

        assertEquals(NodeEligibility.Kind.UNMET_HULL_REQUIREMENT, block.kind());
        assertEquals("req_civilian_hull", block.detail());
        assertEquals(NodeEligibility.Kind.UNMET_HULL_REQUIREMENT, check(slot, civilianOption, FRONT_SHIELDED_FRIGATE).kind());
        assertNull(check(militarized, null, civilian));
        assertNull(check(slot, civilianOption, civilian));
    }

    @Test
    void anExclusivityDeclaredAgainstTheCandidatesContainerBlocksItsOptions() {
        SkillNode guard = node("guard", registered(type("guard_type", SkillTier.SMALL)
                .exclusiveSkillTypeIds(List.of("slot_type")).build()));
        SkillType hull = registered(type("hull", SkillTier.SMALL).build());
        SkillNode slot = optionalNode("slot", type("slot_type", SkillTier.SMALL), hull);
        data.allocate(guard, 0);

        NodeEligibility.Block block = check(slot, hull, FRONT_SHIELDED_FRIGATE);

        assertEquals(NodeEligibility.Kind.TYPE_CONFLICT, block.kind());
        assertEquals("guard_type", block.detail());
    }

    @Test
    void anExclusivityOnAnAllocatedContainerBlocksTheTypeItNames() {
        SkillType hull = registered(type("hull", SkillTier.SMALL).build());
        SkillNode slot = optionalNode("slot", type("slot_type", SkillTier.SMALL).exclusiveSkillTypeIds(List.of("later_type")), hull);
        SkillNode later = node("later", registered(type("later_type", SkillTier.SMALL).build()));
        data.selectOption(slot, hull, 0);

        assertEquals(NodeEligibility.Kind.TYPE_CONFLICT, check(later, null, FRONT_SHIELDED_FRIGATE).kind());
    }

    @Test
    void effectBlocksSeeTheShieldLeftByNodesAlreadyAllocated() {
        SkillNode makeshift = node("makeshift", effectType("makeshift_type", ShieldSkillEffect.CREATE_FRONT_SHIELD_IF_NONE));
        SkillNode convert = node("convert", effectType("convert_type", ShieldSkillEffect.CONVERT_SHIELD_TO_FRONT));
        ShipFacts unshielded = facts(ShieldType.NONE, Set.of());

        assertNull(check(convert, null, unshielded));
        data.allocate(makeshift, 0);

        NodeEligibility.Block block = check(convert, null, unshielded);
        assertEquals(NodeEligibility.Kind.EFFECT_BLOCK, block.kind());
        assertEquals("Ship already has front shields.", block.detail());
    }

    @Test
    void hullFactBlocksApplyToEveryCallerIncludingNpcProfiles() {
        SkillNode citadel = node("citadel", effectType("citadel_type", DefenseSkillEffect.ARMOR_FLAT_FOR_LOW_BASE_ARMOR));
        ShipFacts heavilyArmoured = new ShipFacts(HullSize.FRIGATE, ShieldType.FRONT, false, 2000f, id -> false);

        assertNull(check(citadel, null, FRONT_SHIELDED_FRIGATE));
        assertEquals(NodeEligibility.Kind.EFFECT_BLOCK, check(citadel, null, heavilyArmoured).kind());
    }
}
