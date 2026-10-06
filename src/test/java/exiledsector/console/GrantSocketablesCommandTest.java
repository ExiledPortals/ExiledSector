package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledsector.socketables.SocketableDefinitions;
import exiledsector.socketables.SocketableItemData;
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrantSocketablesCommandTest {

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
        SocketableDefinitions.register(new JSONArray()
                .put(definition("first_subroutine"))
                .put(definition("second_subroutine")));
    }

    @AfterEach
    void tearDown() {
        SocketableDefinitions.clear();
        consoleMock.close();
        globalMock.close();
    }

    private static JSONObject definition(String id) throws Exception {
        return new JSONObject().put("id", id).put("kind", "subroutine").put("prefixes", "HULL_MULT:4:6").put("suffixes", "ARMOR_PERCENT:6:9");
    }

    private List<SocketableItemData> granted(int expectedItems) {
        ArgumentCaptor<SpecialItemData> items = ArgumentCaptor.forClass(SpecialItemData.class);
        verify(cargo, times(expectedItems)).addSpecial(items.capture(), Mockito.eq(1f));
        return items.getAllValues().stream().map(SocketableItemData::of).toList();
    }

    @Test
    void withNoNumberItAddsFiveCopiesOfEveryTypeEachWithItsOwnRoll() {
        CommandResult result = new GrantSocketablesCommand(new Random(3L)).runCommand("", CommandContext.CAMPAIGN_MAP);

        assertEquals(CommandResult.SUCCESS, result);
        List<SocketableItemData> items = granted(10);
        Map<String, Long> perType = items.stream().collect(Collectors.groupingBy(SocketableItemData::definitionId, Collectors.counting()));
        assertEquals(Map.of("first_subroutine", 5L, "second_subroutine", 5L), perType);
        Set<Long> seeds = new HashSet<>();
        items.forEach(item -> seeds.add(item.seed()));
        assertEquals(10, seeds.size());
    }

    @Test
    void aNumberSetsHowManyCopiesOfEachType() {
        assertEquals(CommandResult.SUCCESS, new GrantSocketablesCommand(new Random(3L)).runCommand(" 2 ", CommandContext.CAMPAIGN_MAP));

        assertEquals(4, granted(4).size());
    }

    @Test
    void aBadOrNonPositiveNumberIsRejectedWithoutAddingAnything() {
        GrantSocketablesCommand command = new GrantSocketablesCommand(new Random(3L));

        assertEquals(CommandResult.BAD_SYNTAX, command.runCommand("lots", CommandContext.CAMPAIGN_MAP));
        assertEquals(CommandResult.BAD_SYNTAX, command.runCommand("0", CommandContext.CAMPAIGN_MAP));
        verify(cargo, never()).addSpecial(any(), anyFloat());
    }

    @Test
    void moreThanTwoHundredCopiesIsRejectedWithoutAddingAnything() {
        GrantSocketablesCommand command = new GrantSocketablesCommand(new Random(3L));

        assertEquals(CommandResult.BAD_SYNTAX, command.runCommand("201", CommandContext.CAMPAIGN_MAP));
        assertEquals(CommandResult.BAD_SYNTAX, command.runCommand("1000000", CommandContext.CAMPAIGN_MAP));
        verify(cargo, never()).addSpecial(any(), anyFloat());
        assertEquals(CommandResult.SUCCESS, command.runCommand("200", CommandContext.CAMPAIGN_MAP));
        assertEquals(400, granted(400).size());
    }

    @Test
    void itOnlyWorksInTheCampaign() {
        assertEquals(CommandResult.WRONG_CONTEXT, new GrantSocketablesCommand().runCommand("", CommandContext.COMBAT_SIMULATION));

        verify(cargo, never()).addSpecial(any(), anyFloat());
    }
}
