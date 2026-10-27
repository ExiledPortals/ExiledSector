package exiledsector.charts.content;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import exiledlib.ProjectFiles;
import exiledsector.charts.anomaly.convoy.ConvoyRules;
import exiledsector.charts.anomaly.debris.DormantFleets;
import exiledsector.charts.anomaly.skirmish.SkirmishFactions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void ourOwnRunOnlyFactionsAreNeverFoundEvenIfTheirConfigSaysPlayable() throws Exception {
        when(settings.getMergedSpreadsheetData(ModdedChartFactions.FACTION_COLUMN, ModdedChartFactions.MOD_FACTIONS_PATH)).thenReturn(new JSONArray()
                .put(row(DormantFleets.FACTION_ID)).put(row(SkirmishFactions.ALLY_FACTION_ID)));
        for (String factionId : List.of(DormantFleets.FACTION_ID, SkirmishFactions.ALLY_FACTION_ID)) {
            when(settings.getFactionSpec(factionId)).thenReturn(mock(FactionSpecAPI.class));
            config(factionId, new JSONObject().put(ModdedChartFactions.PLAYABLE_KEY, true));
        }

        assertEquals(List.of(), ModdedChartFactions.detect(Set.of()));
    }

    @Test
    void nexerelinReadsOurRunOnlyFactionsAsUnplayableAndOutOfDiplomacy() throws Exception {
        java.nio.file.Path folder = exiledlib.ProjectFiles.projectRoot().resolve(ModdedChartFactions.CONFIG_FOLDER);
        String listed = java.nio.file.Files.readString(folder.resolve("mod_factions.csv"));
        for (String factionId : List.of(DormantFleets.FACTION_ID, SkirmishFactions.ALLY_FACTION_ID)) {
            org.junit.jupiter.api.Assertions.assertTrue(listed.lines().anyMatch(factionId::equals), factionId);
            String config = java.nio.file.Files.readString(folder.resolve(factionId + ".json")).replaceAll("\\s", "");
            for (String flag : List.of("\"playableFaction\":false", "\"noRandomizeRelations\":true", "\"noSyncRelations\":true",
                    "\"disableDiplomacy\":true")) {
                org.junit.jupiter.api.Assertions.assertTrue(config.contains(flag), factionId + " " + flag);
            }
        }
    }

    @Test
    void withoutAnyModFactionListNothingIsFound() throws Exception {
        when(settings.getMergedSpreadsheetData(anyString(), anyString())).thenThrow(new IOException("missing"));

        assertEquals(List.of(), ModdedChartFactions.detect(Set.of()));
    }

    @Test
    void ourRunOnlyFactionsAreListedForNexerelinButNeverTakenAsChartFactions() throws Exception {
        List<String> listed = new ArrayList<>(Files.readAllLines(ProjectFiles.projectRoot().resolve(ModdedChartFactions.MOD_FACTIONS_PATH)));
        assertEquals(ModdedChartFactions.FACTION_COLUMN, listed.remove(0).trim());
        assertEquals(List.of(ConvoyRules.FACTION_ID, DormantFleets.FACTION_ID, SkirmishFactions.ALLY_FACTION_ID), listed.stream().map(String::trim).toList());
        JSONArray rows = new JSONArray();
        for (String factionId : listed) {
            rows.put(row(factionId.trim()));
            JSONObject config = ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(ModdedChartFactions.CONFIG_FOLDER + factionId.trim() + ".json"));
            for (String flag : List.of("disableDiplomacy", "noSyncRelations", "noRandomizeRelations")) {
                assertTrue(config.getBoolean(flag), factionId + " " + flag);
            }
            when(settings.getFactionSpec(factionId.trim())).thenReturn(mock(FactionSpecAPI.class));
            config(factionId.trim(), config);
        }
        when(settings.getMergedSpreadsheetData(ModdedChartFactions.FACTION_COLUMN, ModdedChartFactions.MOD_FACTIONS_PATH)).thenReturn(rows);

        assertEquals(List.of(), ModdedChartFactions.detect(Set.of()));
    }

    private void config(String factionId, JSONObject config) throws Exception {
        Mockito.doReturn(config).when(settings).getMergedJSON(ModdedChartFactions.CONFIG_FOLDER + factionId + ".json");
    }

    private static JSONObject row(String factionId) throws Exception {
        return new JSONObject().put(ModdedChartFactions.FACTION_COLUMN, factionId);
    }
}
