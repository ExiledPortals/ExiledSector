package exiledsector.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemStore;
import exiledsector.items.RolledEffect;
import exiledsector.items.RolledModifier;
import exiledsector.modifiers.ModifierSlot;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SocketedEffectsTest {

    private final Map<String, Object> persistentData = new HashMap<>();
    private MockedStatic<Global> globalMock;
    private SkillNode socket;

    @BeforeEach
    void setUp() throws Exception {
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        SkillTree.clearNodes();
        socket = new SkillNode("socket", new SkillType.Builder("socket_type", "Socket", "", SkillTier.SOCKET).build(), List.of(), 0f, 0f);
        SkillTree.register(socket);
        ItemDefinitions.register(new JSONArray().put(new JSONObject().put("id", "chip").put("kind", "UNIQUE").put("unique", "true")));
    }

    @AfterEach
    void tearDown() {
        SkillTree.clearNodes();
        ItemDefinitions.clear();
        globalMock.close();
    }

    @Test
    void anInstalledItemsEffectsFlowThroughItsSocketNode() {
        Item item = ItemStore.get().add(new ItemData("chip", 4L, List.of(RolledModifier.single(ModifierSlot.PREFIX, new RolledEffect("HULL_MULT", 5f))), null));
        ShipSkillData data = new ShipSkillData();
        data.allocate(socket, 3);
        AllocatedNode allocated = AllocatedNode.of(data).get(0);
        assertTrue(AllocatedSkillEffects.appliedEffects(data, allocated, null).isEmpty());

        data.socketItem("socket", item.id());

        assertEquals(item.skillEffects(), AllocatedSkillEffects.appliedEffects(data, allocated, null));
        assertEquals(item.skillEffects().stream().map(SkillTypeEffect::effect).toList(), AllocatedSkillEffects.forData(data, null));
    }

    @Test
    void aDestroyedItemNoLongerContributesAnything() {
        Item item = ItemStore.get().add(new ItemData("chip", 4L, List.of(RolledModifier.single(ModifierSlot.PREFIX, new RolledEffect("HULL_MULT", 5f))), null));
        ShipSkillData data = new ShipSkillData();
        data.allocate(socket, 3);
        data.socketItem("socket", item.id());

        ItemStore.get().remove(item);

        assertTrue(AllocatedSkillEffects.appliedEffects(data, AllocatedNode.of(data).get(0), null).isEmpty());
    }
}
