package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemData;
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

class GrantUniqueHullItemsCommandTest {

    private MockedStatic<Global> globalMock;
    private MockedStatic<Console> consoleMock;
    private CargoAPI cargo;

    @BeforeEach
    void setUp() {
        globalMock = Mockito.mockStatic(Global.class);
        consoleMock = Mockito.mockStatic(Console.class);
        SectorAPI sector = mock(SectorAPI.class);
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        cargo = mock(CargoAPI.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        when(sector.getPlayerFleet()).thenReturn(fleet);
        when(fleet.getCargo()).thenReturn(cargo);
    }

    @AfterEach
    void tearDown() {
        ItemDefinitions.clear();
        consoleMock.close();
        globalMock.close();
    }

    private static JSONObject definition(String id, boolean unique) throws Exception {
        return new JSONObject().put("id", id).put("section", "crew_quarters").put("prefixes", "HULL_MULT:4:6")
                .put("suffixes", "ARMOR_PERCENT:6:9").put("unique", Boolean.toString(unique));
    }

    @Test
    void itAddsOneCopyOfEachUniqueAndNothingElse() throws Exception {
        ItemDefinitions.register(new JSONArray()
                .put(definition("basic", false))
                .put(definition("first_unique", true))
                .put(definition("second_unique", true)));

        assertEquals(CommandResult.SUCCESS, new GrantUniqueHullItemsCommand(new Random(3L)).runCommand("", CommandContext.CAMPAIGN_MAP));

        ArgumentCaptor<SpecialItemData> items = ArgumentCaptor.forClass(SpecialItemData.class);
        verify(cargo, times(2)).addSpecial(items.capture(), Mockito.eq(1f));
        assertEquals(List.of("first_unique", "second_unique"),
                items.getAllValues().stream().map(ItemData::of).map(ItemData::definitionId).sorted().toList());
    }

    @Test
    void withNoUniquesDefinedItReportsAnError() throws Exception {
        ItemDefinitions.register(new JSONArray().put(definition("basic", false)));

        assertEquals(CommandResult.ERROR, new GrantUniqueHullItemsCommand().runCommand("", CommandContext.CAMPAIGN_MAP));
        verify(cargo, never()).addSpecial(any(), anyFloat());
    }

    @Test
    void argumentsAndCombatAreRejected() {
        GrantUniqueHullItemsCommand command = new GrantUniqueHullItemsCommand();

        assertEquals(CommandResult.BAD_SYNTAX, command.runCommand("5", CommandContext.CAMPAIGN_MAP));
        assertEquals(CommandResult.WRONG_CONTEXT, command.runCommand("", CommandContext.COMBAT_SIMULATION));
        verify(cargo, never()).addSpecial(any(), anyFloat());
    }
}
