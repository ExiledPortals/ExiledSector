package exiledsector.console;

import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemData;
import exiledsector.items.ItemKind;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GrantHullItemsCommandTest {

    private ConsoleFixture fixture;

    @BeforeEach
    void setUp() throws Exception {
        fixture = new ConsoleFixture();
        ItemDefinitions.register(new JSONArray()
                .put(definition("first_hull_item"))
                .put(definition("second_hull_item")));
    }

    @AfterEach
    void tearDown() {
        ItemDefinitions.clear();
        fixture.close();
    }

    private static JSONObject definition(String id) throws Exception {
        return new JSONObject().put("id", id).put("section", "bridge").put("prefixes", "HULL_MULT:4:6").put("suffixes", "ARMOR_PERCENT:6:9");
    }

    @Test
    void withNoNumberItAddsFiveCopiesOfEveryTypeEachWithItsOwnRoll() {
        CommandResult result = new GrantHullItemsCommand(new Random(3L)).runCommand("", CommandContext.CAMPAIGN_MAP);

        assertEquals(CommandResult.SUCCESS, result);
        List<ItemData> items = fixture.granted(10);
        Map<String, Long> perType = items.stream().collect(Collectors.groupingBy(ItemData::definitionId, Collectors.counting()));
        assertEquals(Map.of("first_hull_item", 5L, "second_hull_item", 5L), perType);
        Set<Long> seeds = new HashSet<>();
        items.forEach(item -> seeds.add(item.seed()));
        assertEquals(10, seeds.size());
    }

    @Test
    void aNumberSetsHowManyCopiesOfEachType() {
        assertEquals(CommandResult.SUCCESS, new GrantHullItemsCommand(new Random(3L)).runCommand(" 2 ", CommandContext.CAMPAIGN_MAP));

        assertEquals(4, fixture.granted(4).size());
    }

    @Test
    void subroutinesAreNotGranted() {
        List<ItemDefinition> definitions = new ArrayList<>(ItemDefinitions.all());
        definitions.add(new ItemDefinition("a_subroutine", ItemKind.SUBROUTINE, "Subroutine", ItemDefinition.FALLBACK_ICON, 1f, false, "",
                List.of(), List.of(), ""));
        ItemDefinitions.register(definitions);

        assertEquals(CommandResult.SUCCESS, new GrantHullItemsCommand(new Random(3L)).runCommand("1", CommandContext.CAMPAIGN_MAP));

        assertEquals(Set.of("first_hull_item", "second_hull_item"), fixture.granted(2).stream().map(ItemData::definitionId).collect(Collectors.toSet()));
    }

    @Test
    void aBadOrNonPositiveNumberIsRejectedWithoutAddingAnything() {
        GrantHullItemsCommand command = new GrantHullItemsCommand(new Random(3L));

        assertEquals(CommandResult.BAD_SYNTAX, command.runCommand("lots", CommandContext.CAMPAIGN_MAP));
        assertEquals(CommandResult.BAD_SYNTAX, command.runCommand("0", CommandContext.CAMPAIGN_MAP));
        fixture.verifyNothingGranted();
    }

    @Test
    void moreThanTwoHundredCopiesIsRejectedWithoutAddingAnything() {
        GrantHullItemsCommand command = new GrantHullItemsCommand(new Random(3L));

        assertEquals(CommandResult.BAD_SYNTAX, command.runCommand("201", CommandContext.CAMPAIGN_MAP));
        assertEquals(CommandResult.BAD_SYNTAX, command.runCommand("1000000", CommandContext.CAMPAIGN_MAP));
        fixture.verifyNothingGranted();
        assertEquals(CommandResult.SUCCESS, command.runCommand("200", CommandContext.CAMPAIGN_MAP));
        assertEquals(400, fixture.granted(400).size());
    }

    @Test
    void itOnlyWorksInTheCampaign() {
        assertEquals(CommandResult.WRONG_CONTEXT, new GrantHullItemsCommand().runCommand("", CommandContext.COMBAT_SIMULATION));

        fixture.verifyNothingGranted();
    }
}
