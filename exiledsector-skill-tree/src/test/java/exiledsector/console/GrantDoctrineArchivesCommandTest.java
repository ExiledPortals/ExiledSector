package exiledsector.console;

import exiledsector.items.DoctrineArchives;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.Subroutine;
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class GrantDoctrineArchivesCommandTest {

    private ConsoleFixture fixture;

    @BeforeEach
    void setUp() throws Exception {
        fixture = new ConsoleFixture();
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
        fixture.close();
    }

    private static JSONObject subroutine(String id, SubroutineKind kind) throws Exception {
        return new JSONObject().put("id", id).put("name", id).put("rarity", "10").put("kind", kind.id());
    }

    @Test
    void withNoArgumentItAddsEveryDoctrineArchive() {
        assertEquals(CommandResult.SUCCESS, new GrantDoctrineArchivesCommand(new Random(3L)).runCommand("", CommandContext.CAMPAIGN_MAP));

        List<ItemData> items = fixture.granted(2);
        assertEquals(List.of("doctrine_archive_hegemony", "doctrine_archive_luddic"), items.stream().map(ItemData::definitionId).toList());
        assertInstanceOf(Subroutine.class, items.get(0).preview());
    }

    @Test
    void anArgumentPicksTheMatchingDoctrines() {
        assertEquals(CommandResult.SUCCESS, new GrantDoctrineArchivesCommand(new Random(3L)).runCommand(" Luddic ", CommandContext.CAMPAIGN_MAP));

        assertEquals(List.of("doctrine_archive_luddic"), fixture.granted(1).stream().map(ItemData::definitionId).toList());
    }

    @Test
    void anUnknownFactionIsRejectedWithoutAddingAnything() {
        assertEquals(CommandResult.BAD_SYNTAX, new GrantDoctrineArchivesCommand(new Random(3L)).runCommand("volume", CommandContext.CAMPAIGN_MAP));

        fixture.verifyNothingGranted();
    }

    @Test
    void itOnlyWorksInTheCampaign() {
        assertEquals(CommandResult.WRONG_CONTEXT, new GrantDoctrineArchivesCommand().runCommand("", CommandContext.COMBAT_SIMULATION));

        fixture.verifyNothingGranted();
    }
}
