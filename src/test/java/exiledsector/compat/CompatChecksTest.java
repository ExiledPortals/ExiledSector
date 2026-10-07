package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import exiledsector.skills.npc.RealSkillData;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompatChecksTest {

    private MockedStatic<Global> globalMock;
    private SettingsAPI settings;

    @BeforeEach
    void setUp() {
        settings = mock(SettingsAPI.class);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        SecondInCommandCompat.clearCachedLookups();
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        SecondInCommandCompat.clearCachedLookups();
    }

    private static CompatTarget target(List<String> missing) {
        return new CompatTarget("Example", List.of("example", "example.next"), List.of("1.0"), () -> missing);
    }

    private static List<CompatChecks.Finding> check(CompatTarget target, Map<String, String> installed) {
        return CompatChecks.check(target, installed::get);
    }

    @Test
    void aModThatIsNotInstalledOnlyNotesThatItsCompatIsIdle() {
        List<CompatChecks.Finding> findings = check(target(List.of("the hull mod x")), Map.of());

        assertEquals(1, findings.size());
        assertEquals(CompatChecks.Level.INFO, findings.get(0).level());
        assertTrue(findings.get(0).message().contains("isn't installed"));
    }

    @Test
    void theTestedVersionWithEverythingPresentLogsOneReassuringLine() {
        List<CompatChecks.Finding> findings = check(target(List.of()), Map.of("example", "1.0"));

        assertEquals(1, findings.size());
        assertEquals(CompatChecks.Level.INFO, findings.get(0).level());
        assertTrue(findings.get(0).message().contains("Example 1.0 matches"));
    }

    @Test
    void anUntestedVersionWarnsButStillChecksWhatIsMissing() {
        List<CompatChecks.Finding> findings = check(target(List.of("the hull mod x")), Map.of("example.next", "2.0"));

        assertEquals(2, findings.size());
        assertTrue(findings.stream().allMatch(finding -> finding.level() == CompatChecks.Level.WARN));
        assertTrue(findings.get(0).message().contains("Example 2.0 is installed") && findings.get(0).message().contains("1.0"));
        assertTrue(findings.get(1).message().contains("no longer provides the hull mod x"));
    }

    @Test
    void aSelfCheckThatThrowsIsReportedInsteadOfStoppingTheLoad() {
        CompatTarget broken = new CompatTarget("Example", List.of("example"), List.of("1.0"), () -> {
            throw new IllegalStateException("boom");
        });

        List<CompatChecks.Finding> findings = check(broken, Map.of("example", "1.0"));

        assertEquals(CompatChecks.Level.WARN, findings.get(0).level());
        assertTrue(findings.get(0).message().contains("boom"));
    }

    @Test
    void everyCompatTargetNamesItsModAndTheVersionsItWasTestedAgainst() {
        List<String> names = new ArrayList<>();
        for (CompatTarget target : CompatChecks.targets()) {
            names.add(target.displayName());
            assertFalse(target.modIds().isEmpty(), target.displayName());
            assertFalse(target.testedVersions().isEmpty(), target.displayName());
        }
        assertEquals(List.of("Lost Sector", "Second-in-Command", "MagicLib"), names.subList(0, 3));
        assertEquals(3 + SalvageSiteCompat.SOURCES.size(), names.size());
        assertTrue(names.contains("Random Assortment of Things salvage sites"));
    }

    @Test
    void lostSectorReportsEachRequiredHullModThatIsGone() {
        when(settings.getHullModSpec(anyString())).thenReturn(mock(HullModSpecAPI.class));
        when(settings.getHullModSpec("nskr_bigBats")).thenReturn(null);

        assertEquals(List.of("the hull mod nskr_bigBats"), LostSectorCompat.TARGET.missingFeatures().get());
    }

    @Test
    void magicLibProvidesEverythingExiledSectorUses() {
        when(settings.getHullModSpec(MagicLibCompat.WARNING_HULLMOD_ID)).thenReturn(mock(HullModSpecAPI.class));

        assertEquals(List.of(), MagicLibCompat.TARGET.missingFeatures().get());
    }

    @Test
    void secondInCommandProvidesEverythingExiledSectorUses() {
        when(settings.getHullModSpec(SecondInCommandCompat.CONTROLLER_HULLMOD_ID)).thenReturn(mock(HullModSpecAPI.class));
        when(settings.getScriptClassLoader()).thenReturn(CompatChecksTest.class.getClassLoader());

        assertEquals(List.of(), SecondInCommandCompat.TARGET.missingFeatures().get());
    }

    @Test
    void secondInCommandReportsItsSkillApiWhenTheClassesCannotBeFound() {
        when(settings.getHullModSpec(SecondInCommandCompat.CONTROLLER_HULLMOD_ID)).thenReturn(mock(HullModSpecAPI.class));
        when(settings.getScriptClassLoader()).thenReturn(new ClassLoader(null) {
        });

        List<String> missing = SecondInCommandCompat.TARGET.missingFeatures().get();

        assertEquals(1, missing.size());
        assertTrue(missing.get(0).contains("SCData.isSkillActive"));
    }

    @Test
    void everyLostSectorHullModANodeStandsInForIsInTheSelfCheck() throws Exception {
        JSONArray types = RealSkillData.readJson(RealSkillData.projectRoot().resolve(RealSkillData.TYPES_FILE))
                .getJSONArray("skillTypes");
        List<String> unchecked = new ArrayList<>();
        for (int i = 0; i < types.length(); i++) {
            JSONObject type = types.getJSONObject(i);
            JSONArray phantoms = type.optJSONArray("phantomHullMods");
            for (int j = 0; phantoms != null && j < phantoms.length(); j++) {
                String hullModId = phantoms.getString(j);
                if (hullModId.startsWith("nskr_") && !LostSectorCompat.REQUIRED_HULL_MOD_IDS.contains(hullModId)) {
                    unchecked.add(type.getString("id") + " -> " + hullModId);
                }
            }
        }

        assertTrue(unchecked.isEmpty(), String.join("\n", unchecked));
    }
}
