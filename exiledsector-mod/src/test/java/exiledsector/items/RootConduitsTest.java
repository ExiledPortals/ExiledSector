package exiledsector.items;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.MockSector;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.AllocatedSkillEffects;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RootConduitsTest {

    private MockSector mockSector;
    private SkillNode root;
    private SkillNode socket;
    private final List<SkillNode> chain = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        mockSector = new MockSector();
        SkillTree.clear();
        SkillTree.registerType(new SkillType.Builder("root_type", "Root", "a.png", SkillTier.ROOT).build());
        SkillTree.registerType(new SkillType.Builder("small_type", "Small", "a.png", SkillTier.SMALL).build());
        SkillTree.registerType(new SkillType.Builder("socket_type", "Socket", "a.png", SkillTier.SOCKET).build());
        root = node("root", "root_type", List.of());
        String previous = root.getId();
        for (int index = 0; index < 6; index++) {
            SkillNode link = node("link_" + index, "small_type", List.of(previous));
            chain.add(link);
            previous = link.getId();
        }
        socket = node("socket", "socket_type", List.of(previous));
        node("shortcut_bait", "small_type", List.of("link_2"));
        ItemDefinitions.register(new JSONArray().put(ItemFixtures.subroutineRow("conduit", SubroutineKind.ROOT_CONDUIT)));
    }

    @AfterEach
    void tearDown() {
        ItemDefinitions.clear();
        SkillTree.clear();
        mockSector.close();
    }

    private static SkillNode node(String id, String typeId, List<String> connections) {
        SkillNode node = new SkillNode(id, SkillTree.getType(typeId), connections, 0f, 0f);
        SkillTree.register(node);
        return node;
    }

    private ShipSkillData shipWithConduit(Item conduit) {
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        ShipTrees.allocate(data, socket, 0);
        data.socketItem(socket.getId(), conduit.id());
        return data;
    }

    @Test
    void aConduitIsBornCorruptedWithOneFrozenAffinityAndCountsTheShortestPathToTheRoot() {
        Subroutine conduit = (Subroutine) ItemStore.get().add(ItemData.rolled(ItemDefinitions.get("conduit"), 9L));
        ShipSkillData data = shipWithConduit(conduit);

        RootConduits.Conduit found = RootConduits.of(data);

        assertTrue(conduit.corrupted());
        assertEquals(conduit.conduitAffinity(), found.affinity());
        assertEquals(8, found.path().size());
        assertEquals(6, found.nodesBetween());
        assertEquals(5f, found.affinityGranted(), 1e-4f);
        assertTrue(found.connectorKeys().contains(RootConduits.connectorKey("link_5", "socket")));
        assertEquals(Set.copyOf(found.path()).size(), found.path().size());
        assertTrue(found.path() == RootConduits.of(data).path());
    }

    @Test
    void theSocketGrantsTheConduitAffinityAndAShipWithoutOneGetsNothing() {
        Subroutine conduit = (Subroutine) ItemStore.get().add(ItemData.rolled(ItemDefinitions.get("conduit"), 9L));
        ShipSkillData data = shipWithConduit(conduit);

        float granted = AllocatedSkillEffects.treeEffects(data, AllocatedNode.of(data), HullSize.CRUISER).stream()
                .filter(effect -> effect.effect() == conduit.conduitAffinity()).map(SkillTypeEffect::magnitude).reduce(0f, Float::sum);

        assertEquals(5f, granted, 1e-4f);
        assertNull(RootConduits.of(new ShipSkillData()));
    }
}
