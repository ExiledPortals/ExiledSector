package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;
import exiledsector.ExiledSectorModPlugin;
import exiledsector.charts.objective.ChartObjectiveIntel;
import exiledsector.charts.ChartRun;
import exiledsector.charts.ChartRunState;
import exiledsector.charts.ChartWormholePlugin;
import exiledsector.charts.WormholeGateway;
import exiledsector.charts.atlas.AtlasAnomalyRecord;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.skills.ShipSkillData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SaveFixtureTest {

    private static final Path FIXTURE = Path.of("exiledsector-mod/src/test/resources/save_fixture.xml");
    private static final Path ATLAS_BEFORE_ALIAS = Path.of("exiledsector-mod/src/test/resources/save_fixture_atlas_before_alias.xml");
    private static final Pattern JAVA_CLASS_NAME = Pattern.compile("exiledsector\\.[\\w.$]+");

    @BeforeEach
    void setUp() throws Exception {
        ItemFixtures.registerShippedItems();
        StarChartFixtures.registerShipped();
        WormholeAnchorDefinitions.register(HullItemRows.shippedRows(WormholeAnchorDefinitions.DATA_PATH));
        ItemIdAliases.register(HullItemRows.shippedRows(ItemIdAliases.dataPath()));
    }

    @AfterEach
    void tearDown() {
        StarChartFixtures.clear();
        ItemIdAliases.clear();
        WormholeAnchorDefinitions.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }

    private static XStream xstream() {
        XStream xstream = new XStream(new DomDriver());
        XStream.setupDefaultSecurity(xstream);
        xstream.addPermission(AnyTypePermission.ANY);
        new ExiledSectorModPlugin().configureXStream(xstream);
        return xstream;
    }

    // the fixture is a Map<String, Object> keyed like the sector's persistent data
    @SuppressWarnings("unchecked")
    private static Map<String, Object> load() throws IOException {
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getIndustrySpec(WormholeGateway.INDUSTRY_ID)).thenReturn(mock(IndustrySpecAPI.class));
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getSettings).thenReturn(settings);
            return (Map<String, Object>) xstream().fromXML(Files.readString(FIXTURE, StandardCharsets.UTF_8));
        }
    }

    @Test
    void aSaveWrittenBeforeTheDomainSplitStillLoads() throws IOException {
        Map<String, Object> saved = load();

        // the ship records were saved as a Map<String, ShipSkillData>
        @SuppressWarnings("unchecked")
        ShipSkillData ship = ((Map<String, ShipSkillData>) saved.get("exiledSector_shipSkillData")).get("fixture_ship");
        assertEquals(1, ship.getLevel());
        assertEquals(250f, ship.getXp());
        assertEquals(Set.of("fixture_root", "fixture_small"), ship.getAllocatedNodeIds());
        assertTrue(ship.isSocketTypeUnlocked("bridge"));
        assertEquals("socketable_1", ship.getSectionItem("bridge"));

        List<Item> items = ((ItemStore) saved.get("exiledSector_socketables")).owned();
        assertEquals(List.of(HullItem.class, Subroutine.class, WormholeAnchor.class), items.stream().map(Object::getClass).toList());
        assertEquals(List.of("socketable_1", "socketable_2", "socketable_3"), items.stream().map(Item::id).toList());
        assertEquals("unique_coatl_gunnery_team", items.get(0).definitionId());

        ItemStore charts = (ItemStore) saved.get("exiledSector_starCharts");
        assertTrue(charts.holdsStarCharts());
        assertInstanceOf(StarChart.class, charts.owned().get(0));

        ChartRun run = (ChartRun) saved.get("exiledSector_chartRun");
        assertEquals("fixture_run", run.runId());
        assertEquals(ChartRunState.OPEN, run.runState());
        assertInstanceOf(ChartObjectiveIntel.class, saved.get("intel"));
        assertInstanceOf(ChartWormholePlugin.class, saved.get("customEntityPlugin"));
        assertInstanceOf(WormholeGateway.class, saved.get("industry"));
    }

    @Test
    void noClassIsSavedUnderItsJavaNameSoClassesCanChangePackage() throws IOException {
        String written = Files.readString(FIXTURE, StandardCharsets.UTF_8);
        String resaved = xstream().toXML(load());

        assertEquals(List.of(), javaClassNames(written));
        assertEquals(List.of(), javaClassNames(resaved));
    }

    @Test
    void anAtlasSavedBeforeItHadAnAliasStillLoadsAndIsResavedUnderTheAlias() throws IOException {
        String written = Files.readString(ATLAS_BEFORE_ALIAS, StandardCharsets.UTF_8);
        assertEquals(List.of("exiledsector.charts.atlas.AtlasState"), javaClassNames(written));

        Map<?, ?> saved = (Map<?, ?>) xstream().fromXML(written);
        AtlasState atlas = (AtlasState) saved.get(AtlasState.PERSISTENT_KEY);
        assertEquals(424242L, atlas.seed());
        assertTrue(atlas.isCompleted(new AtlasCell(1, 0)));
        assertTrue(atlas.has(new AtlasCell(2, -1), AtlasState.FAILED));
        assertTrue(atlas.has(new AtlasCell(-1, 3), AtlasState.MARKERS_SPENT));
        assertEquals(Set.of(1, 2), atlas.clearedTiers());
        assertEquals(5, atlas.atlasPoints());
        assertEquals(1, atlas.keyFragments());
        assertEquals("fixture_region", atlas.recordedRegion(77L));
        assertEquals(Boolean.TRUE, atlas.unlockedAtReveal(0L, "fixture_unlock"));
        assertEquals("pirates", atlas.recordedFaction("0:0"));
        assertEquals("hegemony", atlas.capturedBy(new AtlasCell(1, 0)));
        assertEquals(List.of("spatial_breach"), atlas.anomalyRecords(new AtlasCell(2, -1)).stream().map(AtlasAnomalyRecord::anomalyId).toList());

        String resaved = xstream().toXML(saved);
        assertEquals(List.of(), javaClassNames(resaved));
        assertTrue(resaved.contains("<exiledSector.AtlasState>"), resaved);
    }

    private static List<String> javaClassNames(String xml) {
        return JAVA_CLASS_NAME.matcher(xml).results().map(MatchResult::group).distinct().sorted().toList();
    }
}
