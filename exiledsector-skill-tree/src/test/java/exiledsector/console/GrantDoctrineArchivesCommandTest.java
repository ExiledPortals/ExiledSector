package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledsector.items.DoctrineArchives;
import exiledlib.items.ItemData;
import exiledlib.items.ItemDefinitions;
import exiledsector.items.Subroutine;
import exiledsector.items.SubroutineKind;
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

class GrantDoctrineArchivesCommandTest {

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
        ItemDefinitions.register(new JSONArray()
                .put(subroutine("doctrine_archive_hegemony", SubroutineKind.DOCTRINE))
                .put(subroutine("doctrine_archive_luddic", SubroutineKind.DOCTRINE))
                .put(subroutine("volume_chip", SubroutineKind.MODSPEC_CACHE)));
        DoctrineArchives.register(new JSONArray().put(new JSONObject().put("id", "doctrine_archive_luddic").put("behaviour", "TRANSFORM")
                .put("radius", "large").put("notables", "anything")));
    }

    @AfterEach
    void tearDown() {
        DoctrineArchives.clear();
        ItemDefinitions.clear();
        consoleMock.close();
        globalMock.close();
    }

    private static JSONObject subroutine(String id, SubroutineKind kind) throws Exception {
        return new JSONObject().put("id", id).put("name", id).put("rarity", "10").put("kind", kind.id());
    }

    private List<ItemData> granted(int expectedItems) {
        ArgumentCaptor<SpecialItemData> items = ArgumentCaptor.forClass(SpecialItemData.class);
        verify(cargo, times(expectedItems)).addSpecial(items.capture(), Mockito.eq(1f));
        return items.getAllValues().stream().map(ItemData::of).toList();
    }

    @Test
    void withNoArgumentItAddsEveryDoctrineArchive() {
        assertEquals(CommandResult.SUCCESS, new GrantDoctrineArchivesCommand(new Random(3L)).runCommand("", CommandContext.CAMPAIGN_MAP));

        List<ItemData> items = granted(2);
        assertEquals(List.of("doctrine_archive_hegemony", "doctrine_archive_luddic"), items.stream().map(ItemData::definitionId).toList());
        assertInstanceOf(Subroutine.class, items.get(0).preview());
    }

    @Test
    void anArgumentPicksTheMatchingDoctrines() {
        assertEquals(CommandResult.SUCCESS, new GrantDoctrineArchivesCommand(new Random(3L)).runCommand(" Luddic ", CommandContext.CAMPAIGN_MAP));

        assertEquals(List.of("doctrine_archive_luddic"), granted(1).stream().map(ItemData::definitionId).toList());
    }

    @Test
    void anUnknownFactionIsRejectedWithoutAddingAnything() {
        assertEquals(CommandResult.BAD_SYNTAX, new GrantDoctrineArchivesCommand(new Random(3L)).runCommand("volume", CommandContext.CAMPAIGN_MAP));

        verify(cargo, never()).addSpecial(any(), anyFloat());
    }

    @Test
    void itOnlyWorksInTheCampaign() {
        assertEquals(CommandResult.WRONG_CONTEXT, new GrantDoctrineArchivesCommand().runCommand("", CommandContext.COMBAT_SIMULATION));

        verify(cargo, never()).addSpecial(any(), anyFloat());
    }
}
