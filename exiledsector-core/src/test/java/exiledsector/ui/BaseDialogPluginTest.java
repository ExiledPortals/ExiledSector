package exiledsector.ui;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CharacterDataAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.MockSector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BaseDialogPluginTest {

    private final MemoryAPI globalMemory = mock(MemoryAPI.class);
    private final MemoryAPI playerMemory = mock(MemoryAPI.class);
    private MockSector mockSector;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        when(mockSector.sector.getMemoryWithoutUpdate()).thenReturn(globalMemory);
        CharacterDataAPI characterData = mock(CharacterDataAPI.class);
        when(characterData.getMemoryWithoutUpdate()).thenReturn(playerMemory);
        when(mockSector.sector.getCharacterData()).thenReturn(characterData);
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    @Test
    void initGivesRuleCommandsTheTargetsMemories() {
        CampaignFleetAPI target = mock(CampaignFleetAPI.class);
        MemoryAPI targetMemory = mock(MemoryAPI.class);
        when(target.getMemoryWithoutUpdate()).thenReturn(targetMemory);
        FactionAPI faction = mock(FactionAPI.class);
        MemoryAPI factionMemory = mock(MemoryAPI.class);
        when(faction.getMemoryWithoutUpdate()).thenReturn(factionMemory);
        when(target.getFaction()).thenReturn(faction);

        Map<String, MemoryAPI> memoryMap = openOn(target).getMemoryMap();

        assertSame(targetMemory, memoryMap.get(MemKeys.LOCAL));
        assertSame(factionMemory, memoryMap.get(MemKeys.FACTION));
        assertSame(globalMemory, memoryMap.get(MemKeys.GLOBAL));
        assertSame(playerMemory, memoryMap.get(MemKeys.PLAYER));
    }

    @Test
    void initWithoutTargetLeavesTheMapEmpty() {
        assertTrue(openOn(null).getMemoryMap().isEmpty());
    }

    private static BaseDialogPlugin openOn(SectorEntityToken target) {
        InteractionDialogAPI dialog = mock(InteractionDialogAPI.class);
        when(dialog.getInteractionTarget()).thenReturn(target);
        BaseDialogPlugin plugin = new BaseDialogPlugin() {
            @Override
            protected void open() {
            }

            @Override
            public void optionSelected(String optionText, Object optionData) {
            }
        };
        plugin.init(dialog);
        return plugin;
    }
}
