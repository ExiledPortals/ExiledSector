package exiledsector.items;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import exiledlib.MockSector;
import exiledlib.skills.CombatGate;
import exiledlib.skills.EffectContribution;
import exiledlib.skills.EffectTotals;
import exiledlib.skills.SkillTypeEffect;
import exiledlib.skills.skilleffect.defense.DefenseSkillEffect;
import exiledlib.skills.skilleffect.shield.ShieldSkillEffect;
import exiledsector.effects.SkillTreeEffectSource;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatedSubroutinesTest {

    private MockSector mockSector;
    private SkillNode socket;
    private SkillNode nearNode;
    private SkillNode farNode;

    @BeforeEach
    void setUp() throws Exception {
        mockSector = new MockSector();
        SkillTree.clear();
        NodeRadii.register(new JSONObject("{ \"small\": 150, \"medium\": 250, \"large\": 350 }"));
        SkillTree.registerType(new SkillType.Builder("socket_type", "Socket", "a.png", SkillTier.SOCKET).build());
        SkillTree.registerType(new SkillType.Builder("mixed", "Mixed", "a.png", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(ShieldSkillEffect.SHIELD_ABSORPTION_PERCENT, -10f), new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 5f)))
                .build());
        socket = node("socket", "socket_type", 0f, 0f);
        nearNode = node("near", "mixed", 100f, 0f);
        farNode = node("far", "mixed", 5000f, 0f);
        ItemDefinitions.register(new JSONArray().put(ItemFixtures.subroutineRow("phoenix", SubroutineKind.UNIQUE).put("unique", "true")
                .put("lines", "GATES_RADIUS_BELOW_HALF_HULL:3:3").put("source", "faction:navarchy_of_tania")));
        UniqueSubroutineRadii.register(new JSONArray().put(new JSONObject().put("id", "phoenix").put(UniqueSubroutineRadii.RADIUS_COLUMN, "medium")));
    }

    @AfterEach
    void tearDown() {
        UniqueSubroutineRadii.clear();
        ItemDefinitions.clear();
        SkillTree.clear();
        NodeRadii.reset();
        mockSector.close();
    }

    private static SkillNode node(String id, String typeId, float x, float y) {
        SkillNode node = new SkillNode(id, SkillTree.getType(typeId), List.of(), x, y);
        SkillTree.register(node);
        return node;
    }

    private static EffectContribution contributionFor(List<EffectContribution> contributions, SkillNode node) {
        return contributions.stream().filter(contribution -> contribution.modIdPrefix().contains("_" + node.getId() + "_")).findFirst().orElseThrow();
    }

    @Test
    void nodesInAGatingRadiusKeepOnlyLiveEffectsAtTripleStrengthBehindTheGate() {
        Item phoenix = ItemStore.get().add(ItemData.rolled(ItemDefinitions.get("phoenix"), 1L));
        ShipSkillData data = new ShipSkillData();
        ShipTrees.allocate(data, socket, 0);
        ShipTrees.allocate(data, nearNode, 0);
        ShipTrees.allocate(data, farNode, 0);
        data.socketItem(socket.getId(), phoenix.id());

        List<EffectContribution> contributions = SkillTreeEffectSource.contributions(data, HullSize.CRUISER);
        EffectContribution near = contributionFor(contributions, nearNode);
        EffectContribution far = contributionFor(contributions, farNode);

        assertEquals(CombatGate.LOW_HULL, near.gate());
        assertFalse(near.permanent());
        assertEquals(List.of(new SkillTypeEffect(ShieldSkillEffect.SHIELD_ABSORPTION_PERCENT, -30f)), near.effects());
        assertNull(far.gate());
        EffectTotals totals = EffectTotals.of(contributions, 1f);
        assertTrue(totals.groups().stream().anyMatch(group -> group.gate() == CombatGate.LOW_HULL));
        assertEquals(-10f, totals.groups().stream().filter(group -> group.gate() == null && group.temporarySeconds() == null).findFirst().orElseThrow()
                .totals().get(ShieldSkillEffect.SHIELD_ABSORPTION_PERCENT), 1e-4f);
    }

    @Test
    void eachGateReadsItsOwnCombatCondition() {
        ShipAPI ship = mock(ShipAPI.class);
        ShipSystemAPI system = mock(ShipSystemAPI.class);
        when(ship.getSystem()).thenReturn(system);
        when(ship.getHullLevel()).thenReturn(0.49f);
        when(system.isActive()).thenReturn(true);

        assertTrue(CombatGate.LOW_HULL.holds(ship));
        assertTrue(CombatGate.SYSTEM_ACTIVE.holds(ship));
        assertFalse(CombatGate.PHASED.holds(ship));
        when(ship.getHullLevel()).thenReturn(0.5f);
        assertFalse(CombatGate.LOW_HULL.holds(ship));
    }
}
