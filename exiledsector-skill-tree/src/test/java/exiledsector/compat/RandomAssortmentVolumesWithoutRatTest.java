package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.MockSector;
import exiledsector.ProjectFiles;
import exiledsector.skills.DescriptionLine;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.npc.RealSkillData;
import exiledsector.skills.tags.VolumeToggles;
import exiledsector.skills.unlock.NodeGates;
import exiledsector.skills.unlock.SkillTypeUnlockStatus;
import lunalib.lunaSettings.LunaSettings;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RandomAssortmentVolumesWithoutRatTest {

    private MockedStatic<LunaSettings> lunaSettingsMock;
    private MockSector mockSector;

    @BeforeEach
    void setUp() throws Exception {
        RandomAssortmentCompat.clearCachedLookups();
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class, invocation -> null);
        SettingsAPI settings = mock(SettingsAPI.class);
        ModManagerAPI modManager = mock(ModManagerAPI.class);
        when(settings.getModManager()).thenReturn(modManager);
        when(modManager.isModEnabled(anyString())).thenReturn(false);
        when(settings.getCommoditySpec(anyString())).thenAnswer(invocation ->
                ((String) invocation.getArgument(0)).startsWith("rat_") ? null : mock(CommoditySpecAPI.class));
        mockSector = new MockSector();
        MemoryAPI sectorMemory = mock(MemoryAPI.class);
        when(sectorMemory.getBoolean("$playerCanUseGates")).thenReturn(true);
        when(mockSector.sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
        mockSector.global.when(Global::getSettings).thenReturn(settings);

        RealSkillData.load();
        NodeGates.register(ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(NodeGates.DATA_PATH)));
    }

    @AfterEach
    void tearDown() throws Exception {
        RealSkillData.clear();
        NodeGates.register(new JSONObject("{}"));
        mockSector.close();
        lunaSettingsMock.close();
        RandomAssortmentCompat.clearCachedLookups();
    }

    private static List<SkillNode> ratVolumeNodes() {
        List<SkillNode> nodes = new ArrayList<>();
        for (SkillNode node : SkillTree.getAllNodes().values()) {
            boolean inVolume = VolumeToggles.RANDOM_ASSORTMENT_VOLUMES.contains(node.getVolume());
            SkillNode paired = node.getPairedNodeId() == null ? null : SkillTree.get(node.getPairedNodeId());
            boolean leadsIn = paired != null && VolumeToggles.RANDOM_ASSORTMENT_VOLUMES.contains(paired.getVolume());
            if (inVolume || leadsIn) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    @Test
    void noRatVolumeNodeIsLockedOrHiddenWithoutRat() {
        List<String> blocked = ratVolumeNodes().stream()
                .filter(node -> SkillTypeUnlockStatus.isLocked(node.getType(), null) || NodeGates.isLocked(node, null))
                .map(SkillNode::getId).toList();

        assertTrue(blocked.isEmpty(), "Locked without RAT: " + blocked);
        assertTrue(ratVolumeNodes().size() > 30);
    }

    @Test
    void abyssalMatterCostsAreWaivedWithoutRat() {
        List<String> stillCosting = ratVolumeNodes().stream()
                .filter(node -> node.getType().getItemCost() != null)
                .map(SkillNode::getId).toList();

        assertTrue(stillCosting.isEmpty(), "Costs a commodity that doesn't exist: " + stillCosting);
    }

    @Test
    void noRatVolumeTooltipShowsARawRatId() {
        List<String> leaks = new ArrayList<>();
        for (SkillNode node : ratVolumeNodes()) {
            for (HullSize hullSize : new HullSize[]{null, HullSize.FRIGATE, HullSize.CAPITAL_SHIP}) {
                for (DescriptionLine line : SkillNode.describeMechanics(node.getType(), hullSize).all()) {
                    if (line.text().plain().contains("rat_")) {
                        leaks.add(node.getId() + " (" + hullSize + "): " + line.text().plain());
                    }
                }
            }
        }

        assertTrue(leaks.isEmpty(), "Tooltips mentioning raw RAT ids: " + leaks);
    }
}
