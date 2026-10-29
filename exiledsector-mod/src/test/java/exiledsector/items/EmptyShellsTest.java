package exiledsector.items;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.MockSector;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.special.SpecialSkillEffect;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.AllocatedSkillEffects;
import exiledsector.skills.NodeGrants;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTreeLinks;
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
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmptyShellsTest {

    private static final String AXION = "axion";

    private MockSector mockSector;
    private SkillNode socket;
    private SkillNode nearNotable;
    private SkillNode nearSmall;
    private SkillNode farNotable;

    @BeforeEach
    void setUp() throws Exception {
        mockSector = new MockSector();
        SkillTree.clear();
        NodeRadii.register(new JSONObject("{ \"small\": 150, \"medium\": 250, \"large\": 350 }"));
        SkillTree.registerType(type("hull_notable", SkillTier.NOTABLE));
        SkillTree.registerType(type("hull_small", SkillTier.SMALL));
        SkillTree.registerType(new SkillType.Builder("socket_type", "Socket", "a.png", SkillTier.SOCKET).build());
        socket = node("socket", "socket_type", 0f, 0f);
        nearNotable = node("near_notable", "hull_notable", 100f, 0f);
        nearSmall = node("near_small", "hull_small", -100f, 0f);
        farNotable = node("far_notable", "hull_notable", 5000f, 0f);
        ItemDefinitions.register(new JSONArray().put(ItemFixtures.subroutineRow(AXION, SubroutineKind.UNIQUE).put("unique", "true")
                .put("lines", "EVERY_AFFINITY_FLAT_PER_HOLLOW_NODE:1:1").put("source", "faction:stargazer")));
        UniqueSubroutineRadii.register(new JSONArray().put(new JSONObject().put("id", AXION).put(UniqueSubroutineRadii.RADIUS_COLUMN, "medium")));
        NodeGrantLinks.install();
    }

    @AfterEach
    void tearDown() {
        NodeGrants.install(null);
        UniqueSubroutineRadii.clear();
        ItemDefinitions.clear();
        SkillTree.clear();
        NodeRadii.reset();
        mockSector.close();
    }

    private static SkillType type(String id, SkillTier tier) {
        return new SkillType.Builder(id, id, "a.png", tier).effects(List.of(new SkillTypeEffect(SkillEffect.byName("HULL_MULT"), 5f))).build();
    }

    private static SkillNode node(String id, String typeId, float x, float y) {
        SkillNode node = new SkillNode(id, SkillTree.getType(typeId), List.of(), x, y);
        SkillTree.register(node);
        return node;
    }

    @Test
    void anEmptyShellIsBornCorruptedAndGrantsEveryRegularNodeInItsRadius() {
        Item axion = ItemStore.get().add(ItemData.rolled(ItemDefinitions.get(AXION), 1L));
        ShipSkillData data = new ShipSkillData();
        ShipTrees.allocate(data, socket, 0);
        data.socketItem(socket.getId(), axion.id());

        assertTrue(axion.corrupted());
        assertTrue(EmptyShells.isEmptyShell(axion));
        assertEquals(Map.of(socket.getId(), List.of(nearNotable.getId(), nearSmall.getId())), ShipTreeLinks.grantedNodesBySocket(data));
    }

    @Test
    void hollowNodesLoseTheirOwnEffectsAndEachAddsOneToEveryAffinity() {
        Item axion = ItemStore.get().add(ItemData.rolled(ItemDefinitions.get(AXION), 1L));
        ShipSkillData data = new ShipSkillData();
        ShipTrees.allocate(data, socket, 0);
        ShipTrees.allocate(data, farNotable, 0);
        data.socketItem(socket.getId(), axion.id());
        ShipTrees.allocateGranted(data, nearNotable);
        ShipTrees.allocateGranted(data, nearSmall);

        List<SkillTypeEffect> effects = AllocatedSkillEffects.treeEffects(data, AllocatedNode.of(data), HullSize.CRUISER);

        assertEquals(Set.of(nearNotable.getId(), nearSmall.getId()), EmptyShells.hollowNodeIds(data));
        assertEquals(5f, effects.stream().filter(effect -> effect.effect() == SkillEffect.byName("HULL_MULT"))
                .map(SkillTypeEffect::magnitude).reduce(0f, Float::sum), 1e-4f);
        for (SkillEffect affinity : List.of(SpecialSkillEffect.LOW_TECH_AFFINITY_FLAT, SpecialSkillEffect.HIGH_TECH_AFFINITY_FLAT,
                SpecialSkillEffect.MIDLINE_AFFINITY_FLAT)) {
            assertEquals(2f, effects.stream().filter(effect -> effect.effect() == affinity).map(SkillTypeEffect::magnitude).reduce(0f, Float::sum), 1e-4f);
        }
    }

    @Test
    void aPaidNodeTheShellTakesOverBecomesFree() {
        Item axion = ItemStore.get().add(ItemData.rolled(ItemDefinitions.get(AXION), 1L));
        ShipSkillData data = new ShipSkillData();
        ShipTrees.allocate(data, socket, 0);
        ShipTrees.allocate(data, nearNotable, 0);
        data.socketItem(socket.getId(), axion.id());

        assertFalse(data.isFreeNode(nearNotable.getId()));
        ShipTrees.takeOverAsGranted(data, nearNotable.getId());

        assertTrue(data.isFreeNode(nearNotable.getId()));
        assertEquals(0, ShipTrees.paidNodeCount(data, List.of(nearNotable.getId())));
    }
}
