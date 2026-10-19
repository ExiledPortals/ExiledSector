package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.SubroutineKind;
import exiledsector.items.VolumeSubroutines;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrantVolumeSubroutinesCommandTest {

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
        JSONArray rows = new JSONArray()
                .put(subroutine("volume_subroutine_core", SubroutineKind.VOLUME).put("volume", "core"))
                .put(subroutine("volume_subroutine_hegemony", SubroutineKind.VOLUME).put("volume", "hegemony"))
                .put(subroutine("doctrine_archive_hegemony", SubroutineKind.DOCTRINE));
        ItemDefinitions.register(rows);
        VolumeSubroutines.register(rows);
    }

    @AfterEach
    void tearDown() {
        VolumeSubroutines.clear();
        ItemDefinitions.clear();
        consoleMock.close();
        globalMock.close();
    }

    private static JSONObject subroutine(String id, SubroutineKind kind) throws Exception {
        return new JSONObject().put("id", id).put("name", id).put("rarity", "10").put("kind", kind.id());
    }

    private List<String> grantedIds(int expectedItems) {
        ArgumentCaptor<SpecialItemData> items = ArgumentCaptor.forClass(SpecialItemData.class);
        verify(cargo, times(expectedItems)).addSpecial(items.capture(), Mockito.eq(1f));
        return items.getAllValues().stream().map(ItemData::of).map(ItemData::definitionId).toList();
    }

    @Test
    void withNoArgumentItAddsEveryVolumeSubroutine() {
        assertEquals(CommandResult.SUCCESS, new GrantVolumeSubroutinesCommand(new Random(3L)).runCommand("", CommandContext.CAMPAIGN_MAP));

        assertEquals(List.of("volume_subroutine_core", "volume_subroutine_hegemony"), grantedIds(2));
    }

    @Test
    void aVolumeNamePicksItsSubroutine() {
        assertEquals(CommandResult.SUCCESS, new GrantVolumeSubroutinesCommand(new Random(3L)).runCommand(" Hegemony ", CommandContext.CAMPAIGN_MAP));

        assertEquals(List.of("volume_subroutine_hegemony"), grantedIds(1));
    }

    @Test
    void anUnknownVolumeIsRejectedWithoutAddingAnything() {
        assertEquals(CommandResult.BAD_SYNTAX, new GrantVolumeSubroutinesCommand(new Random(3L)).runCommand("ocean", CommandContext.CAMPAIGN_MAP));

        verify(cargo, never()).addSpecial(any(), anyFloat());
    }

    @Test
    void itOnlyWorksInTheCampaign() {
        assertEquals(CommandResult.WRONG_CONTEXT, new GrantVolumeSubroutinesCommand().runCommand("", CommandContext.COMBAT_SIMULATION));

        verify(cargo, never()).addSpecial(any(), anyFloat());
    }
}
