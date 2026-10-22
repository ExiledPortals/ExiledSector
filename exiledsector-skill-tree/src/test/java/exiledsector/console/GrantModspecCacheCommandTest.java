package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledlib.items.ItemData;
import exiledlib.items.ItemDefinitions;
import exiledsector.items.ModspecCaches;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrantModspecCacheCommandTest {

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
                .put(subroutine("contraband_modspec_cache", SubroutineKind.MODSPEC_CACHE).put("node_count_chances", "0.9;0.09;0.01"))
                .put(subroutine("doctrine_archive_hegemony", SubroutineKind.DOCTRINE));
        ItemDefinitions.register(rows);
        ModspecCaches.register(rows);
    }

    @AfterEach
    void tearDown() {
        ModspecCaches.clear();
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
    void withNoArgumentItAddsOneCache() {
        assertEquals(CommandResult.SUCCESS, new GrantModspecCacheCommand(new Random(3L)).runCommand("", CommandContext.CAMPAIGN_MAP));

        assertEquals(List.of("contraband_modspec_cache"), granted(1).stream().map(ItemData::definitionId).toList());
    }

    @Test
    void aNodeCountPicksASeedThatRollsThatManyNodes() {
        assertEquals(CommandResult.SUCCESS, new GrantModspecCacheCommand(new Random(3L)).runCommand(" 3 ", CommandContext.CAMPAIGN_MAP));

        ItemData cache = granted(1).get(0);
        assertEquals(3, ModspecCaches.base(cache.definitionId()).nodeCount(cache.seed()));
    }

    @Test
    void aNodeCountNoCacheCanRollAddsNothing() {
        assertEquals(CommandResult.ERROR, new GrantModspecCacheCommand(new Random(3L)).runCommand("4", CommandContext.CAMPAIGN_MAP));

        verify(cargo, never()).addSpecial(any(), anyFloat());
    }

    @Test
    void aBadNodeCountIsRejectedWithoutAddingAnything() {
        assertEquals(CommandResult.BAD_SYNTAX, new GrantModspecCacheCommand(new Random(3L)).runCommand("lots", CommandContext.CAMPAIGN_MAP));
        assertEquals(CommandResult.BAD_SYNTAX, new GrantModspecCacheCommand(new Random(3L)).runCommand("0", CommandContext.CAMPAIGN_MAP));

        verify(cargo, never()).addSpecial(any(), anyFloat());
    }

    @Test
    void itOnlyWorksInTheCampaign() {
        assertEquals(CommandResult.WRONG_CONTEXT, new GrantModspecCacheCommand().runCommand("", CommandContext.COMBAT_SIMULATION));

        verify(cargo, never()).addSpecial(any(), anyFloat());
    }
}
