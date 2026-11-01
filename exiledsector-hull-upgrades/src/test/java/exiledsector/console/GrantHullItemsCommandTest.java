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
                .put(definition("first_bridge_item", "bridge", false))
                .put(definition("second_bridge_item", "bridge", false))
                .put(definition("reactor_item", "reactor", false))
                .put(definition("unique_bridge_item", "bridge", true)));
    }

    @AfterEach
    void tearDown() {
        ItemDefinitions.clear();
        fixture.close();
    }

    private static JSONObject definition(String id, String socketType, boolean unique) throws Exception {
        return new JSONObject().put("id", id).put("section", socketType).put("prefixes", "HULL_MULT:4:6").put("suffixes", "ARMOR_PERCENT:6:9")
                .put("unique", Boolean.toString(unique));
    }

    private static Set<String> grantedIds(List<ItemData> items) {
        return items.stream().map(ItemData::definitionId).collect(Collectors.toSet());
    }

    @Test
    void withNoArgumentsItAddsOneOfEveryNonUniqueHullItem() {
        CommandResult result = new GrantHullItemsCommand(new Random(3L)).runCommand("", CommandContext.CAMPAIGN_MAP);

        assertEquals(CommandResult.SUCCESS, result);
        assertEquals(Set.of("first_bridge_item", "second_bridge_item", "reactor_item"), grantedIds(fixture.granted(3)));
    }

    @Test
    void aSocketTypeAddsOneOfEachNonUniqueHullItemForThatSocket() {
        assertEquals(CommandResult.SUCCESS, new GrantHullItemsCommand(new Random(3L)).runCommand(" BRIDGE ", CommandContext.CAMPAIGN_MAP));

        assertEquals(Set.of("first_bridge_item", "second_bridge_item"), grantedIds(fixture.granted(2)));
    }

    @Test
    void anAmountSetsHowManyOfEachAreAddedEachWithItsOwnRoll() {
        assertEquals(CommandResult.SUCCESS, new GrantHullItemsCommand(new Random(3L)).runCommand("bridge 3", CommandContext.CAMPAIGN_MAP));

        List<ItemData> items = fixture.granted(6);
        Map<String, Long> perType = items.stream().collect(Collectors.groupingBy(ItemData::definitionId, Collectors.counting()));
        assertEquals(Map.of("first_bridge_item", 3L, "second_bridge_item", 3L), perType);
        Set<Long> seeds = new HashSet<>();
        items.forEach(item -> seeds.add(item.seed()));
        assertEquals(6, seeds.size());
    }

    @Test
    void subroutinesAreNotGranted() {
        List<ItemDefinition> definitions = new ArrayList<>(ItemDefinitions.all());
        definitions.add(new ItemDefinition("a_subroutine", ItemKind.SUBROUTINE, "Subroutine", ItemDefinition.FALLBACK_ICON, 1f, false, "",
                List.of(), List.of(), ""));
        ItemDefinitions.register(definitions);

        assertEquals(CommandResult.SUCCESS, new GrantHullItemsCommand(new Random(3L)).runCommand("", CommandContext.CAMPAIGN_MAP));

        assertEquals(Set.of("first_bridge_item", "second_bridge_item", "reactor_item"), grantedIds(fixture.granted(3)));
    }

    @Test
    void anUnknownOrNonHullSocketTypeIsRejectedWithoutAddingAnything() {
        GrantHullItemsCommand command = new GrantHullItemsCommand(new Random(3L));

        assertEquals(CommandResult.BAD_SYNTAX, command.runCommand("hangar", CommandContext.CAMPAIGN_MAP));
        assertEquals(CommandResult.BAD_SYNTAX, command.runCommand("subroutine", CommandContext.CAMPAIGN_MAP));
        assertEquals(CommandResult.BAD_SYNTAX, command.runCommand("5", CommandContext.CAMPAIGN_MAP));
        fixture.verifyNothingGranted();
    }

    @Test
    void aSocketTypeWithNoNonUniqueHullItemsReportsAnError() {
        assertEquals(CommandResult.ERROR, new GrantHullItemsCommand(new Random(3L)).runCommand("flight_deck", CommandContext.CAMPAIGN_MAP));

        fixture.verifyNothingGranted();
    }

    @Test
    void aBadAmountOrExtraArgumentsAreRejectedWithoutAddingAnything() {
        GrantHullItemsCommand command = new GrantHullItemsCommand(new Random(3L));

        assertEquals(CommandResult.BAD_SYNTAX, command.runCommand("bridge lots", CommandContext.CAMPAIGN_MAP));
        assertEquals(CommandResult.BAD_SYNTAX, command.runCommand("bridge 0", CommandContext.CAMPAIGN_MAP));
        assertEquals(CommandResult.BAD_SYNTAX, command.runCommand("bridge 201", CommandContext.CAMPAIGN_MAP));
        assertEquals(CommandResult.BAD_SYNTAX, command.runCommand("bridge 2 3", CommandContext.CAMPAIGN_MAP));
        fixture.verifyNothingGranted();
        assertEquals(CommandResult.SUCCESS, command.runCommand("bridge 200", CommandContext.CAMPAIGN_MAP));
        assertEquals(400, fixture.granted(400).size());
    }

    @Test
    void itOnlyWorksInTheCampaign() {
        assertEquals(CommandResult.WRONG_CONTEXT, new GrantHullItemsCommand().runCommand("", CommandContext.COMBAT_SIMULATION));

        fixture.verifyNothingGranted();
    }
}
