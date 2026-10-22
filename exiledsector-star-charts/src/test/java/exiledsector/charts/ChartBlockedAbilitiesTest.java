package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartBlockedAbilitiesTest {

    private MockedStatic<Global> globalMock;
    private SettingsAPI settings;

    @BeforeEach
    void setUp() {
        settings = mock(SettingsAPI.class);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        ChartBlockedAbilities.resetForTests();
    }

    private void csvRows(String json) throws Exception {
        when(settings.getMergedSpreadsheetDataForMod(ChartBlockedAbilities.ID_COLUMN, ChartBlockedAbilities.PATH, "exiledSector"))
                .thenReturn(new JSONArray(json));
    }

    @Test
    void loadsTheMergedListAndAlwaysKeepsTransverseJumpFirst() throws Exception {
        csvRows("[{\"ability_id\": \"other_mod_jump\", \"note\": \"teleports\"}, {\"ability_id\": \"\"},"
                + " {\"ability_id\": \"fracture_jump\"}, {\"ability_id\": \" spaced_jump \"}]");

        ChartBlockedAbilities.load();

        assertEquals(List.of(Abilities.TRANSVERSE_JUMP, "other_mod_jump", "spaced_jump"), ChartBlockedAbilities.abilityIds());
    }

    @Test
    void theShippedCsvListsTransverseJumpUnderTheIdColumn() throws Exception {
        List<String> lines = java.nio.file.Files.readAllLines(java.nio.file.Path.of(ChartBlockedAbilities.PATH));

        assertEquals(ChartBlockedAbilities.ID_COLUMN + ",note", lines.get(0));
        assertTrue(lines.stream().anyMatch(line -> line.startsWith(Abilities.TRANSVERSE_JUMP + ",")));
    }

    @Test
    void aMissingOrBrokenFileStillBlocksTransverseJump() throws Exception {
        when(settings.getMergedSpreadsheetDataForMod(ChartBlockedAbilities.ID_COLUMN, ChartBlockedAbilities.PATH, "exiledSector"))
                .thenThrow(new IOException("missing"));

        ChartBlockedAbilities.load();

        assertEquals(List.of(Abilities.TRANSVERSE_JUMP), ChartBlockedAbilities.abilityIds());
    }

    @Test
    void theShippedFileIsReadByItsIdColumnAndTheWatchdogBlocksEveryListedAbility() throws Exception {
        csvRows("[{\"ability_id\": \"fracture_jump\"}, {\"ability_id\": \"broken\"}, {\"ability_id\": \"mod_teleport\"},"
                + " {\"ability_id\": \"not_installed\"}]");
        ChartBlockedAbilities.load();
        CampaignFleetAPI playerFleet = mock(CampaignFleetAPI.class);
        AbilityPlugin transverseJump = mock(AbilityPlugin.class);
        AbilityPlugin modTeleport = mock(AbilityPlugin.class);
        AbilityPlugin broken = mock(AbilityPlugin.class);
        when(playerFleet.getAbility(Abilities.TRANSVERSE_JUMP)).thenReturn(transverseJump);
        when(playerFleet.getAbility("mod_teleport")).thenReturn(modTeleport);
        when(playerFleet.getAbility("broken")).thenReturn(broken);
        when(modTeleport.isActiveOrInProgress()).thenReturn(true);
        when(broken.isActiveOrInProgress()).thenReturn(true);
        doThrow(new IllegalStateException("mod bug")).when(broken).deactivate();

        ChartRunScript.blockEscapeAbilities(playerFleet);

        verify(transverseJump).forceDisable();
        verify(transverseJump, never()).deactivate();
        verify(modTeleport).deactivate();
        verify(modTeleport).forceDisable();
        verify(playerFleet).getAbility("not_installed");
    }
}
