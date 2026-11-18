package exiledsector.items.subroutine.doctrine;

import exiledsector.MockSector;
import exiledsector.items.HullItemRows;
import exiledsector.items.Item;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemFixtures;
import exiledsector.items.modifier.ItemModifiers;
import exiledsector.skills.ClearedSkillTreeExtension;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.npc.RealSkillData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(ClearedSkillTreeExtension.class)
class ConvertedAnchorReleaseTest {

    private final Map<String, Item> itemsById = new HashMap<>();

    @BeforeEach
    void setUp() throws Exception {
        RealSkillData.load();
        ItemFixtures.registerShippedItems();
        DoctrineArchives.register(HullItemRows.shippedRows(DoctrineArchives.DATA_PATH));
    }

    @AfterEach
    void tearDown() {
        DoctrineArchives.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }

    private String add(String itemId, String definitionId) {
        itemsById.put(itemId, Item.create(itemId, definitionId, 31L, List.of()));
        return itemId;
    }

    private static ShipSkillData withSockets(List<SkillNode> sockets) {
        ShipSkillData data = new ShipSkillData();
        for (SkillNode socket : sockets) {
            ShipTrees.allocate(data, socket, 0);
        }
        return data;
    }

    @Test
    void onlyInfestedArchivesAreUnsocketedAndEveryOtherItemStaysPut() {
        try (MockSector mockSector = new MockSector()) {
            List<SkillNode> sockets = SkillTree.getAllNodes().values().stream().filter(node -> node.getType().getTier() == SkillTier.SOCKET)
                    .sorted(Comparator.comparing(SkillNode::getId)).limit(2).toList();
            String firstSocket = sockets.get(0).getId();
            String secondSocket = sockets.get(1).getId();
            ShipSkillData first = withSockets(sockets);
            first.socketItem(firstSocket, add("threat_archive", "doctrine_archive_threat"));
            first.socketItem(secondSocket, add("luddic_archive", "doctrine_archive_luddic"));
            ShipSkillData second = withSockets(sockets);
            second.socketItem(firstSocket, add("shroud_archive", "doctrine_archive_shrouded"));
            second.socketItem(secondSocket, "missing_item");

            List<String> released = ConvertedAnchorRelease.release(Map.of("first", first, "second", second), itemsById::get);

            assertEquals(List.of("shroud_archive", "threat_archive"), released.stream().sorted().toList());
            assertNull(first.getSocketedItem(firstSocket));
            assertEquals("luddic_archive", first.getSocketedItem(secondSocket));
            assertNull(second.getSocketedItem(firstSocket));
            assertEquals("missing_item", second.getSocketedItem(secondSocket));
        }
    }
}
