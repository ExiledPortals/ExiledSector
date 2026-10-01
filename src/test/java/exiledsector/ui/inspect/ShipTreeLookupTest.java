package exiledsector.ui.inspect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.SkillDataResolver;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.npc.NpcLayout;
import exiledsector.skills.npc.NpcLayoutEntry;
import exiledsector.skills.npc.NpcLayouts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShipTreeLookupTest {

    private static final String NPC_TAG = "exiledSector_npcTree|bulwark|2|root_1,a_1";

    private MockedStatic<Global> globalMock;
    private Map<String, Object> persistentData;

    @BeforeEach
    void setUp() {
        SkillDataResolver.clearCache();
        SkillTree.clearNodes();
        SkillTree.clearTypes();
        SkillTree.register(new SkillNode("root_1", new SkillType.Builder("root", "Root", "a.png", SkillTier.ROOT).build(), List.of(), 0f, 0f));
        SkillTree.register(new SkillNode("a_1", new SkillType.Builder("a", "A", "a.png", SkillTier.SMALL).build(), List.of("root_1"), 0f, 0f));
        NpcLayouts.register(Map.of("bulwark", new NpcLayout("bulwark", "Bulwark", "root_1", List.of(), "",
                List.of(new NpcLayoutEntry("a_1", null)))));
        persistentData = new HashMap<>();
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        NpcLayouts.register(Map.of());
        SkillDataResolver.clearCache();
        SkillTree.clearNodes();
        SkillTree.clearTypes();
    }

    private static FleetMemberAPI member(String id, boolean playerFleet, String... tags) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn(id);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getTags()).thenReturn(List.of(tags));
        when(member.getVariant()).thenReturn(variant);
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        when(fleet.isPlayerFleet()).thenReturn(playerFleet);
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(fleetData.getFleet()).thenReturn(fleet);
        when(member.getFleetData()).thenReturn(fleetData);
        return member;
    }

    @Test
    void aLevelledNpcShowsItsTaggedTreeAndLayoutName() {
        ShipTreeLookup.ShipTree tree = ShipTreeLookup.find(member("npc", false, NPC_TAG));

        assertNotNull(tree);
        assertEquals("Bulwark", tree.layoutName());
        assertEquals(List.of("root_1", "a_1"), List.copyOf(tree.data().getAllocatedNodeIds()));
        assertTrue(persistentData.isEmpty());
    }

    @Test
    void anUnlevelledNpcHasNothingToShowAndLeavesTheSaveUntouched() {
        assertNull(ShipTreeLookup.find(member("npc", false)));
        assertTrue(persistentData.isEmpty());
    }

    @Test
    void aPlayerShipShowsItsSavedTreeOnceItHasOne() {
        FleetMemberAPI ship = member("mine", true);
        assertNull(ShipTreeLookup.find(ship));

        ShipSkillDataManager.get("mine").incrementLevel();
        ShipTreeLookup.ShipTree tree = ShipTreeLookup.find(ship);

        assertNotNull(tree);
        assertNull(tree.layoutName());
        assertEquals(1, tree.data().getLevel());
    }

    @Test
    void onlyNpcTaggedShipsCountAsLevelledNpcs() {
        assertTrue(ShipTreeLookup.isLevelledNpc(member("npc", false, NPC_TAG)));
        assertFalse(ShipTreeLookup.isLevelledNpc(member("npc", false)));
        assertFalse(ShipTreeLookup.isLevelledNpc(null));
    }
}
