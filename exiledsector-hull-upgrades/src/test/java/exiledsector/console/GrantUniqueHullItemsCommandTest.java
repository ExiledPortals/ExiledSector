package exiledsector.console;

import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemData;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GrantUniqueHullItemsCommandTest {

    private ConsoleFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new ConsoleFixture();
    }

    @AfterEach
    void tearDown() {
        ItemDefinitions.clear();
        fixture.close();
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

        assertEquals(List.of("first_unique", "second_unique"), fixture.granted(2).stream().map(ItemData::definitionId).sorted().toList());
    }

    @Test
    void withNoUniquesDefinedItReportsAnError() throws Exception {
        ItemDefinitions.register(new JSONArray().put(definition("basic", false)));

        assertEquals(CommandResult.ERROR, new GrantUniqueHullItemsCommand().runCommand("", CommandContext.CAMPAIGN_MAP));
        fixture.verifyNothingGranted();
    }

    @Test
    void argumentsAndCombatAreRejected() {
        GrantUniqueHullItemsCommand command = new GrantUniqueHullItemsCommand();

        assertEquals(CommandResult.BAD_SYNTAX, command.runCommand("5", CommandContext.CAMPAIGN_MAP));
        assertEquals(CommandResult.WRONG_CONTEXT, command.runCommand("", CommandContext.COMBAT_SIMULATION));
        fixture.verifyNothingGranted();
    }
}
