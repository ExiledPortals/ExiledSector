package exiledsector.console;

import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ModspecCaches;
import exiledsector.items.SubroutineKind;
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

class GrantModspecCacheCommandTest {

    private ConsoleFixture fixture;

    @BeforeEach
    void setUp() throws Exception {
        fixture = new ConsoleFixture();
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
        fixture.close();
    }

    private static JSONObject subroutine(String id, SubroutineKind kind) throws Exception {
        return new JSONObject().put("id", id).put("name", id).put("rarity", "10").put("kind", kind.id());
    }

    @Test
    void withNoArgumentItAddsOneCache() {
        assertEquals(CommandResult.SUCCESS, new GrantModspecCacheCommand(new Random(3L)).runCommand("", CommandContext.CAMPAIGN_MAP));

        assertEquals(List.of("contraband_modspec_cache"), fixture.granted(1).stream().map(ItemData::definitionId).toList());
    }

    @Test
    void aNodeCountPicksASeedThatRollsThatManyNodes() {
        assertEquals(CommandResult.SUCCESS, new GrantModspecCacheCommand(new Random(3L)).runCommand(" 3 ", CommandContext.CAMPAIGN_MAP));

        ItemData cache = fixture.granted(1).get(0);
        assertEquals(3, ModspecCaches.base(cache.definitionId()).nodeCount(cache.seed()));
    }

    @Test
    void aNodeCountNoCacheCanRollAddsNothing() {
        assertEquals(CommandResult.ERROR, new GrantModspecCacheCommand(new Random(3L)).runCommand("4", CommandContext.CAMPAIGN_MAP));

        fixture.verifyNothingGranted();
    }

    @Test
    void aBadNodeCountIsRejectedWithoutAddingAnything() {
        assertEquals(CommandResult.BAD_SYNTAX, new GrantModspecCacheCommand(new Random(3L)).runCommand("lots", CommandContext.CAMPAIGN_MAP));
        assertEquals(CommandResult.BAD_SYNTAX, new GrantModspecCacheCommand(new Random(3L)).runCommand("0", CommandContext.CAMPAIGN_MAP));

        fixture.verifyNothingGranted();
    }

    @Test
    void itOnlyWorksInTheCampaign() {
        assertEquals(CommandResult.WRONG_CONTEXT, new GrantModspecCacheCommand().runCommand("", CommandContext.COMBAT_SIMULATION));

        fixture.verifyNothingGranted();
    }
}
