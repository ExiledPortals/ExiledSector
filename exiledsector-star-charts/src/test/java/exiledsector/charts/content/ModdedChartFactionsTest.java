package exiledsector.charts.content;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModdedChartFactionsTest {

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
    }

    @Test
    void onlyLoadedPlayableFactionsThatTheChartsDoNotListAreFound() throws Exception {
        when(settings.getMergedSpreadsheetData(ModdedChartFactions.FACTION_COLUMN, ModdedChartFactions.MOD_FACTIONS_PATH)).thenReturn(new JSONArray()
                .put(row("roider")).put(row("kesteven")).put(row("rat_abyssals")).put(row("famous_bounty")).put(row("pirates"))
                .put(row("no_config")).put(row("")));
        for (String factionId : List.of("roider", "kesteven", "rat_abyssals", "pirates", "no_config")) {
            when(settings.getFactionSpec(factionId)).thenReturn(mock(FactionSpecAPI.class));
        }
        when(settings.getMergedJSON(anyString())).thenThrow(new RuntimeException("missing"));
        config("roider", new JSONObject().put(ModdedChartFactions.PLAYABLE_KEY, true));
        config("kesteven", new JSONObject());
        config("rat_abyssals", new JSONObject().put(ModdedChartFactions.PLAYABLE_KEY, false));
        config("famous_bounty", new JSONObject().put(ModdedChartFactions.PLAYABLE_KEY, true));
        config("pirates", new JSONObject().put(ModdedChartFactions.PLAYABLE_KEY, true));

        assertEquals(List.of("kesteven", "roider"), ModdedChartFactions.detect(Set.of("pirates")));
    }

    @Test
    void withoutAnyModFactionListNothingIsFound() throws Exception {
        when(settings.getMergedSpreadsheetData(anyString(), anyString())).thenThrow(new IOException("missing"));

        assertEquals(List.of(), ModdedChartFactions.detect(Set.of()));
    }

    private void config(String factionId, JSONObject config) throws Exception {
        Mockito.doReturn(config).when(settings).getMergedJSON(ModdedChartFactions.CONFIG_FOLDER + factionId + ".json");
    }

    private static JSONObject row(String factionId) throws Exception {
        return new JSONObject().put(ModdedChartFactions.FACTION_COLUMN, factionId);
    }
}
