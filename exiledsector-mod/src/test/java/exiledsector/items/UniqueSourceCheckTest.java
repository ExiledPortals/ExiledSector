package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import exiledlib.items.ItemDefinitions;
import exiledlib.items.ItemModifiers;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UniqueSourceCheckTest {

    @BeforeEach
    void setUp() throws Exception {
        ItemDefinitions.register(new JSONArray()
                .put(uniqueRow("fleet_relic", "faction:pirates; faction:missing_faction"))
                .put(uniqueRow("site_relic", "site:station; site:probe; site:nowhere"))
                .put(ItemFixtures.row("hull_relic", "reactor", "HULL_MULT:4:6").put("unique", "TRUE")));
        ItemDrops.register(new JSONArray()
                .put(new JSONObject().put("site", "station").put("chances", "1").put("uniqueChance", "0.2"))
                .put(new JSONObject().put("site", "probe").put("chances", "0.5").put("uniqueChance", "0")));
    }

    @AfterEach
    void tearDown() {
        ItemDrops.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }

    private static JSONObject uniqueRow(String id, String source) throws JSONException {
        return ItemFixtures.subroutineRow(id, SubroutineKind.UNIQUE).put("unique", "true").put(SubroutineDefinitions.UNIQUE_LINES_COLUMN, "HULL_MULT:1:1")
                .put(SubroutineDefinitions.UNIQUE_SOURCE_COLUMN, source);
    }

    @Test
    void sourcesThatCanNeverDropAreReportedAndReachableOnesAreNot() {
        List<String> warnings = UniqueSourceCheck.unreachableSources(ItemDefinitions.all(), Set.of("pirates", "hegemony"), ItemDrops::hasUniqueRoll);

        assertEquals(List.of(
                "fleet_relic drops from faction:missing_faction, but no faction with that id is loaded, so it can never drop there",
                "site_relic drops from site:probe, but " + ItemDrops.DATA_PATH + " has no row for that site with a uniqueChance above zero, so it can never drop there",
                "site_relic drops from site:nowhere, but " + ItemDrops.DATA_PATH + " has no row for that site with a uniqueChance above zero, so it can never drop there"),
                warnings);
    }

    @Test
    void withoutAnyLoadedFactionSpecsTheFactionCheckIsSkipped() {
        assertEquals(List.of(), UniqueSourceCheck.unreachableSources(List.of(ItemDefinitions.get("fleet_relic")), Set.of(), ItemDrops::hasUniqueRoll));
    }

    @Test
    void theShippedUniquesAllHaveAReachableSource() throws Exception {
        ItemFixtures.registerShippedItems();
        ItemDrops.register(ItemFixtures.shippedRows(ItemDrops.DATA_PATH));

        assertEquals(List.of(), UniqueSourceCheck.unreachableSources(ItemDefinitions.all(), Set.of("pirates", "tritachyon"), ItemDrops::hasUniqueRoll));
    }

    @Test
    void theLoadCheckReadsTheFactionSpecsFromTheSettingsAndOnlyWarns() {
        FactionSpecAPI pirates = mock(FactionSpecAPI.class);
        when(pirates.getId()).thenReturn("pirates");
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getAllFactionSpecs()).thenReturn(List.of(pirates));
        List<String> logged = new ArrayList<>();
        AppenderSkeleton appender = new AppenderSkeleton() {
            @Override
            protected void append(LoggingEvent event) {
                logged.add(String.valueOf(event.getMessage()));
            }

            @Override
            public void close() {
            }

            @Override
            public boolean requiresLayout() {
                return false;
            }
        };
        Logger logger = Logger.getLogger(UniqueSourceCheck.class);
        logger.setLevel(Level.WARN);
        logger.addAppender(appender);
        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class)) {
            globalMock.when(Global::getSettings).thenReturn(settings);

            UniqueSourceCheck.logUnreachableSources();
        } finally {
            logger.removeAppender(appender);
            logger.setLevel(null);
        }

        assertEquals(3, logged.size(), logged.toString());
        assertEquals(3, ItemDefinitions.all().size());
    }
}
