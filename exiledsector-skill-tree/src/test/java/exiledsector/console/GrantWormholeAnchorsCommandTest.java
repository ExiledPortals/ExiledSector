package exiledsector.console;

import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.WormholeAnchor;
import exiledsector.items.WormholeAnchorDefinitions;
import exiledsector.skills.SkillTree;
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

class GrantWormholeAnchorsCommandTest {

    private ConsoleFixture fixture;

    @BeforeEach
    void setUp() throws Exception {
        fixture = new ConsoleFixture();
        SkillTree.clearNodes();
        WormholeAnchorDefinitions.register(new JSONArray()
                .put(anchor("wormhole_anchor_hegemony", "hegemony", "hegemony_2"))
                .put(anchor("wormhole_anchor_abyss", "abyss", "rat_abyss_wormhole_1")));
    }

    @AfterEach
    void tearDown() {
        WormholeAnchorDefinitions.clear();
        ItemDefinitions.clear();
        fixture.close();
    }

    private static JSONObject anchor(String id, String volume, String exitNode) throws Exception {
        return new JSONObject().put("id", id).put("volume", volume).put("exit_node", exitNode).put("name", id);
    }

    @Test
    void withNoVolumeItAddsOneAnchorForEveryVolume() {
        assertEquals(CommandResult.SUCCESS, new GrantWormholeAnchorsCommand(new Random(3L)).runCommand("", CommandContext.CAMPAIGN_MAP));

        List<ItemData> items = fixture.granted(2);
        assertEquals(List.of("wormhole_anchor_hegemony", "wormhole_anchor_abyss"), items.stream().map(ItemData::definitionId).toList());
        assertInstanceOf(WormholeAnchor.class, items.get(0).preview());
    }

    @Test
    void aVolumeNamesTheOneAnchorToAdd() {
        assertEquals(CommandResult.SUCCESS, new GrantWormholeAnchorsCommand(new Random(3L)).runCommand(" Abyss ", CommandContext.CAMPAIGN_MAP));

        assertEquals(List.of("wormhole_anchor_abyss"), fixture.granted(1).stream().map(ItemData::definitionId).toList());
    }

    @Test
    void anUnknownVolumeIsRejectedWithoutAddingAnything() {
        assertEquals(CommandResult.BAD_SYNTAX, new GrantWormholeAnchorsCommand(new Random(3L)).runCommand("nowhere", CommandContext.CAMPAIGN_MAP));

        fixture.verifyNothingGranted();
    }

    @Test
    void itOnlyWorksInTheCampaign() {
        assertEquals(CommandResult.WRONG_CONTEXT, new GrantWormholeAnchorsCommand().runCommand("", CommandContext.COMBAT_SIMULATION));

        fixture.verifyNothingGranted();
    }
}
