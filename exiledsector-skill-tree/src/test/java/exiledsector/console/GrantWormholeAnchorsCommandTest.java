package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.WormholeAnchor;
import exiledsector.items.WormholeAnchorDefinitions;
import exiledsector.skills.SkillTree;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.Console;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrantWormholeAnchorsCommandTest {

    private MockedStatic<Global> globalMock;
    private MockedStatic<Console> consoleMock;
    private CargoAPI cargo;

    @BeforeEach
    void setUp() throws Exception {
        globalMock = Mockito.mockStatic(Global.class);
        consoleMock = Mockito.mockStatic(Console.class);
        SectorAPI sector = mock(SectorAPI.class);
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        cargo = mock(CargoAPI.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        when(sector.getPlayerFleet()).thenReturn(fleet);
        when(fleet.getCargo()).thenReturn(cargo);
        SkillTree.clearNodes();
        WormholeAnchorDefinitions.register(new JSONArray()
                .put(anchor("wormhole_anchor_hegemony", "hegemony", "hegemony_2"))
                .put(anchor("wormhole_anchor_abyss", "abyss", "rat_abyss_wormhole_1")));
    }

    @AfterEach
    void tearDown() {
        WormholeAnchorDefinitions.clear();
        ItemDefinitions.clear();
        consoleMock.close();
        globalMock.close();
    }

    private static JSONObject anchor(String id, String volume, String exitNode) throws Exception {
        return new JSONObject().put("id", id).put("volume", volume).put("exit_node", exitNode).put("name", id);
    }

    private List<ItemData> granted(int expectedItems) {
        ArgumentCaptor<SpecialItemData> items = ArgumentCaptor.forClass(SpecialItemData.class);
        verify(cargo, times(expectedItems)).addSpecial(items.capture(), Mockito.eq(1f));
        return items.getAllValues().stream().map(ItemData::of).toList();
    }

    @Test
    void withNoVolumeItAddsOneAnchorForEveryVolume() {
        assertEquals(CommandResult.SUCCESS, new GrantWormholeAnchorsCommand(new Random(3L)).runCommand("", CommandContext.CAMPAIGN_MAP));

        List<ItemData> items = granted(2);
        assertEquals(List.of("wormhole_anchor_hegemony", "wormhole_anchor_abyss"), items.stream().map(ItemData::definitionId).toList());
        assertInstanceOf(WormholeAnchor.class, items.get(0).preview());
    }

    @Test
    void aVolumeNamesTheOneAnchorToAdd() {
        assertEquals(CommandResult.SUCCESS, new GrantWormholeAnchorsCommand(new Random(3L)).runCommand(" Abyss ", CommandContext.CAMPAIGN_MAP));

        assertEquals(List.of("wormhole_anchor_abyss"), granted(1).stream().map(ItemData::definitionId).toList());
    }

    @Test
    void anUnknownVolumeIsRejectedWithoutAddingAnything() {
        assertEquals(CommandResult.BAD_SYNTAX, new GrantWormholeAnchorsCommand(new Random(3L)).runCommand("nowhere", CommandContext.CAMPAIGN_MAP));

        verify(cargo, never()).addSpecial(any(), anyFloat());
    }

    @Test
    void itOnlyWorksInTheCampaign() {
        assertEquals(CommandResult.WRONG_CONTEXT, new GrantWormholeAnchorsCommand().runCommand("", CommandContext.COMBAT_SIMULATION));

        verify(cargo, never()).addSpecial(any(), anyFloat());
    }
}
