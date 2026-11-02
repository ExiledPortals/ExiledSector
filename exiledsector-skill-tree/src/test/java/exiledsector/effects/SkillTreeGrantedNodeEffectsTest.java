package exiledsector.effects;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.skills.EffectContribution;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeSetting;
import exiledsector.skills.SkillType;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.npc.NpcSkillTreeBuilder;
import exiledsector.skills.npc.RealSkillData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SkillTreeGrantedNodeEffectsTest {

    private static final String FREEBOOTERS_CHARGE = "reckless_assault_1";
    private static final String MOD_ID_PREFIX = "test_grant_";

    @BeforeEach
    void setUp() throws Exception {
        RealSkillData.load();
    }

    @AfterEach
    void tearDown() {
        SkillTreeSetting.setEnabledForTests(true);
        RealSkillData.clear();
    }

    private static ShipVariantAPI destroyer() {
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(hullSpec.getHullSize()).thenReturn(HullSize.DESTROYER);
        when(hullSpec.getShieldType()).thenReturn(ShieldType.FRONT);
        when(hullSpec.getHullId()).thenReturn("test_destroyer");
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getHullSpec()).thenReturn(hullSpec);
        when(variant.getHullSize()).thenReturn(HullSize.DESTROYER);
        return variant;
    }

    private static List<EffectContribution> grantTo(ShipSkillData shipData, String... nodeIds) {
        ShipEffectContext context = new ShipEffectContext(shipData, null, destroyer(), HullSize.DESTROYER);
        return SkillTreeGrantedNodeEffects.INSTANCE.withoutTree(List.of(nodeIds), context, MOD_ID_PREFIX);
    }

    private static void registerGrant(String nodeId, List<String> exclusiveTypeIds) {
        SkillType type = new SkillType.Builder(nodeId + "_type", nodeId, "a.png", SkillTier.NOTABLE)
                .effects(SkillTree.get(FREEBOOTERS_CHARGE).getType().getEffects()).exclusiveSkillTypeIds(exclusiveTypeIds).build();
        SkillTree.registerType(type);
        SkillTree.register(new SkillNode(nodeId, type, List.of(), 0f, 0f, SkillNodeDecoration.NONE, List.of()));
    }

    private static ShipSkillData npcTreeHolding(String nodeId) {
        ShipSkillData shipData = NpcSkillTreeBuilder.emptyTree();
        ShipTrees.allocateFleetGrant(shipData, SkillTree.get(nodeId));
        return shipData;
    }

    @Test
    void aShipWithoutATreeGetsTheNodesOwnEffectsUnscaled() {
        List<EffectContribution> contributions = grantTo(new ShipSkillData(), FREEBOOTERS_CHARGE);

        assertEquals(1, contributions.size());
        EffectContribution contribution = contributions.get(0);
        List<SkillTypeEffect> expected = SkillTree.get(FREEBOOTERS_CHARGE).getType().effectsFor(HullSize.DESTROYER).stream()
                .filter(effect -> effect.effect().appliesToNpcShips()).toList();
        assertEquals(expected, contribution.effects());
        assertEquals(MOD_ID_PREFIX + FREEBOOTERS_CHARGE + "_", contribution.modIdPrefix());
        assertEquals(new GrantedNodeOrigin(FREEBOOTERS_CHARGE), contribution.origin());
        assertEquals(false, contribution.bonusScaled());
    }

    @Test
    void aTreeThatAlreadyHoldsTheNodeAppliesItItself() {
        assertTrue(grantTo(npcTreeHolding(FREEBOOTERS_CHARGE), FREEBOOTERS_CHARGE).isEmpty());
    }

    @Test
    void withTheSkillTreeSwitchedOffOnlyTheEffectsFlowThrough() {
        SkillTreeSetting.setEnabledForTests(false);

        assertEquals(1, grantTo(npcTreeHolding(FREEBOOTERS_CHARGE), FREEBOOTERS_CHARGE).size());
    }

    @Test
    void anUnknownNodeGrantsNothing() {
        assertTrue(grantTo(new ShipSkillData(), "no_such_node").isEmpty());
    }

    @Test
    void theDescriptionListsTheNodesEffectsThenItsFlavour() {
        List<StyledText> lines = SkillTreeGrantedNodeEffects.INSTANCE.descriptionLines(FREEBOOTERS_CHARGE);
        SkillType type = SkillTree.get(FREEBOOTERS_CHARGE).getType();

        assertEquals(SkillNode.describeMechanics(type, null).all().size() + 1, lines.size());
        assertEquals(StyledText.styled(type.getFlavourText(), Style.MUTED), lines.get(lines.size() - 1));
        assertTrue(SkillTreeGrantedNodeEffects.INSTANCE.descriptionLines("no_such_node").isEmpty());
    }

    @Test
    void ofTwoConflictingGrantsOnlyTheFirstApplies() {
        registerGrant("grant_first", List.of("grant_second_type"));
        registerGrant("grant_second", List.of());

        List<EffectContribution> contributions = grantTo(new ShipSkillData(), "grant_first", "grant_second", "grant_first");

        assertEquals(List.of(new GrantedNodeOrigin("grant_first")), contributions.stream().map(EffectContribution::origin).toList());
    }
}
