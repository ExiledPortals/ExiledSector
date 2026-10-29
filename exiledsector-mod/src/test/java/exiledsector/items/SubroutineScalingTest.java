package exiledsector.items;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.MockSector;
import exiledsector.skills.CombatGate;
import exiledsector.skills.EffectContribution;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.SocketedItemScaling;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.defense.DefenseSkillEffect;
import exiledsector.skills.skilleffect.shield.ShieldSkillEffect;
import exiledsector.skills.skilleffect.special.SpecialSkillEffect;
import exiledsector.effects.SkillTreeEffectSource;
import exiledsector.skills.NodeGrants;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.radius.NodeRadii;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubroutineScalingTest {

    private static final String RELIC = "relic";
    private static final String PHOENIX = "phoenix";
    private static final String AXION = "axion";
    private static final String AUGMENT = "doctrine_augment";
    private static final Predicate<EffectContribution> PERMANENT = EffectContribution::permanent;

    private MockSector mockSector;
    private SkillNode socket;
    private SkillNode near;
    private SkillNode booster;
    private SkillNode plating;

    @BeforeEach
    void setUp() throws Exception {
        mockSector = new MockSector();
        SkillTree.clear();
        NodeRadii.register(new JSONObject("{ \"small\": 150, \"medium\": 250, \"large\": 350 }"));
        SkillTree.registerType(type("socket_type", SkillTier.SOCKET, DefenseSkillEffect.HULL_PERCENT, 2f));
        SkillTree.registerType(type("mixed", SkillTier.SMALL, ShieldSkillEffect.SHIELD_ABSORPTION_PERCENT, -10f));
        SkillTree.registerType(type("booster", SkillTier.SMALL, SpecialSkillEffect.SOCKETED_SUBROUTINE_EFFECT_PERCENT, 100f));
        SkillTree.registerType(type("plating", SkillTier.NOTABLE, DefenseSkillEffect.HULL_PERCENT, 5f));
        socket = node("socket", "socket_type", 0f, 0f);
        near = node("near", "mixed", 100f, 0f);
        booster = node("booster", "booster", 9000f, 0f);
        plating = node("plating", "plating", 5000f, 0f);
        ItemModifiers.register(new JSONArray().put(HullItemRows.modifierRow("HULL_PERCENT_TEST_DOCTRINE", "prefix", "doctrine", "HULL_PERCENT:2:2", "")));
        ItemDefinitions.register(new JSONArray()
                .put(unique(RELIC, "HULL_PERCENT:10:10"))
                .put(unique(PHOENIX, "GATES_RADIUS_BELOW_HALF_HULL:3:3"))
                .put(unique(AXION, "EVERY_AFFINITY_FLAT_PER_HOLLOW_NODE:1:1"))
                .put(ItemFixtures.subroutineRow(AUGMENT, SubroutineKind.DOCTRINE)));
        UniqueSubroutineRadii.register(new JSONArray()
                .put(new JSONObject().put("id", PHOENIX).put(UniqueSubroutineRadii.RADIUS_COLUMN, "medium"))
                .put(new JSONObject().put("id", AXION).put(UniqueSubroutineRadii.RADIUS_COLUMN, "medium")));
        DoctrineArchives.register(new JSONArray().put(new JSONObject().put("id", AUGMENT).put("behaviour", "AUGMENT").put("radius", "medium")
                .put("modifiers", "@HULL_PERCENT_TEST_DOCTRINE").put("min_lines", "1").put("max_lines", "1")));
        NodeGrantLinks.install();
    }

    @AfterEach
    void tearDown() {
        NodeGrants.install(null);
        DoctrineOverrides.clearCache();
        DoctrineArchives.clear();
        UniqueSubroutineRadii.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
        SkillTree.clear();
        NodeRadii.reset();
        mockSector.close();
    }

    private static SkillType type(String id, SkillTier tier, SkillEffect effect, float magnitude) {
        return new SkillType.Builder(id, id, "a.png", tier).effects(List.of(new SkillTypeEffect(effect, magnitude))).build();
    }

    private static SkillNode node(String id, String typeId, float x, float y) {
        SkillNode node = new SkillNode(id, SkillTree.getType(typeId), List.of(), x, y);
        SkillTree.register(node);
        return node;
    }

    private static JSONObject unique(String id, String lines) throws Exception {
        return ItemFixtures.subroutineRow(id, SubroutineKind.UNIQUE).put("unique", "true").put("lines", lines).put("source", "faction:pirates");
    }

    private ShipSkillData shipWith(String definitionId, boolean boosted, SkillNode... allocated) {
        ShipSkillData data = new ShipSkillData();
        ShipTrees.allocate(data, socket, 0);
        if (boosted) {
            ShipTrees.allocate(data, booster, 0);
        }
        for (SkillNode node : allocated) {
            ShipTrees.allocate(data, node, 0);
        }
        Item item = ItemStore.get().add(ItemData.rolled(ItemDefinitions.get(definitionId), 1L));
        data.socketItem(socket.getId(), item.id());
        return data;
    }

    private static float total(ShipSkillData data, SkillEffect effect, Predicate<EffectContribution> included) {
        return SocketedItemScaling.withScaledItems(SkillTreeEffectSource.contributions(data, HullSize.CRUISER)).stream().filter(included)
                .flatMap(contribution -> contribution.effects().stream()).filter(applied -> applied.effect() == effect)
                .map(SkillTypeEffect::magnitude).reduce(0f, Float::sum);
    }

    @Test
    void aSubroutinesLinesScaleWhileItsSocketsOwnEffectStays() {
        assertEquals(12f, total(shipWith(RELIC, false), DefenseSkillEffect.HULL_PERCENT, PERMANENT), 1e-4f);
        assertEquals(22f, total(shipWith(RELIC, true), DefenseSkillEffect.HULL_PERCENT, PERMANENT), 1e-4f);
    }

    @Test
    void aSubroutinesLinesGetTheirOwnContributionMarkedAsTheSubroutine() {
        List<EffectContribution> contributions = SkillTreeEffectSource.contributions(shipWith(RELIC, false), HullSize.CRUISER);

        assertTrue(contributions.stream().anyMatch(contribution -> contribution.origin() instanceof SubroutineOrigin origin
                && origin.slotId().equals(socket.getId()) && contribution.effects().equals(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)))));
    }

    @Test
    void aGrantedNodeKeepsItsOwnValue() {
        ShipSkillData data = shipWith(RELIC, true);
        ShipTrees.allocateGranted(data, plating);

        assertEquals(27f, total(data, DefenseSkillEffect.HULL_PERCENT, PERMANENT), 1e-4f);
    }

    @Test
    void theGatingStrengthScalesWithTheSubroutine() {
        Predicate<EffectContribution> lowHull = contribution -> contribution.gate() == CombatGate.LOW_HULL;

        assertEquals(-30f, total(shipWith(PHOENIX, false, near), ShieldSkillEffect.SHIELD_ABSORPTION_PERCENT, lowHull), 1e-4f);
        assertEquals(-60f, total(shipWith(PHOENIX, true, near), ShieldSkillEffect.SHIELD_ABSORPTION_PERCENT, lowHull), 1e-4f);
    }

    @Test
    void theAffinityPerHollowNodeScales() {
        ShipSkillData plain = shipWith(AXION, false);
        ShipTrees.allocateGranted(plain, near);
        ShipSkillData boosted = shipWith(AXION, true);
        ShipTrees.allocateGranted(boosted, near);

        assertEquals(1f, total(plain, SpecialSkillEffect.LOW_TECH_AFFINITY_FLAT, PERMANENT), 1e-4f);
        assertEquals(2f, total(boosted, SpecialSkillEffect.LOW_TECH_AFFINITY_FLAT, PERMANENT), 1e-4f);
    }

    @Test
    void anAugmentingArchivesAddedLinesScaleButTheNodesOwnEffectsDoNot() {
        assertEquals(4f, total(shipWith(AUGMENT, false, near), DefenseSkillEffect.HULL_PERCENT, PERMANENT), 1e-4f);
        assertEquals(-10f, total(shipWith(AUGMENT, true, near), ShieldSkillEffect.SHIELD_ABSORPTION_PERCENT, PERMANENT), 1e-4f);
        assertEquals(6f, total(shipWith(AUGMENT, true, near), DefenseSkillEffect.HULL_PERCENT, PERMANENT), 1e-4f);
    }
}
