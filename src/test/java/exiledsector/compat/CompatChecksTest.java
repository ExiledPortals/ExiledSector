package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
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
    void theEngineCheckLooksForTheFleetManagersDeploymentRecord() {
        assertEquals(List.of("com.fs.starfarer.combat.CombatFleetManager.getAllEverDeployed"),
                CompatChecks.ENGINE_MEMBERS.stream().map(ApiMember::displayName).toList());
    }

    @Test
    void anEngineThatStillHasEveryMethodLogsOneReassuringLine() {
        List<CompatChecks.Finding> findings = CompatChecks.checkEngine(
                List.of(ApiMember.instanceMethod("java.util.ArrayList", "subList", "java.util.List", ApiMember.INT, ApiMember.INT)),
                CompatChecksTest.class.getClassLoader());

        assertEquals(1, findings.size());
        assertEquals(CompatChecks.Level.INFO, findings.get(0).level());
    }

    @Test
    void anEngineMissingAMethodOrClassWarnsAboutEachOne() {
        List<CompatChecks.Finding> findings = CompatChecks.checkEngine(List.of(
                ApiMember.instanceMethod("java.util.ArrayList", "getAllEverDeployed", "java.util.List"),
                ApiMember.instanceMethod("com.fs.starfarer.combat.RenamedFleetManager", "getAllEverDeployed", "java.util.List")),
                CompatChecksTest.class.getClassLoader());

        assertEquals(2, findings.size());
        assertTrue(findings.stream().allMatch(finding -> finding.level() == CompatChecks.Level.WARN));
        assertTrue(findings.get(0).message().contains("java.util.ArrayList.getAllEverDeployed"));
        assertTrue(findings.get(1).message().contains("Coalition Flight Doctrine"));
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
        assertEquals(List.of("Lost Sector", "Second-in-Command", "MagicLib", "Exotica Technologies",
                "Random Assortment of Things areas", "Nightcross Armory area", "GraphicsLib"), names.subList(0, 7));
        assertEquals(7 + SalvageSiteCompat.SOURCES.size(), names.size());
        assertTrue(names.contains("Random Assortment of Things salvage sites"));
    }

    @Test
    void aTargetIsEnabledWhenAnyOfItsModIdsIsEnabled() {
        ModManagerAPI modManager = mock(ModManagerAPI.class);
        when(settings.getModManager()).thenReturn(modManager);

        assertFalse(target(List.of()).isEnabled());
        when(modManager.isModEnabled("example.next")).thenReturn(true);
        assertTrue(target(List.of()).isEnabled());
    }

    @Test
    void aTargetIsNotEnabledWithoutAModManager() {
        assertFalse(target(List.of()).isEnabled());
        assertFalse(SecondInCommandCompat.isModEnabled());
    }

    @Test
    void lostSectorReportsEachRequiredHullModThatIsGone() {
        when(settings.getHullModSpec(anyString())).thenReturn(mock(HullModSpecAPI.class));
        when(settings.getHullModSpec("nskr_bigBats")).thenReturn(null);

        assertEquals(List.of("the hull mod nskr_bigBats"), LostSectorCompat.TARGET.missingFeatures().get());
    }

    @Test
    void randomAssortmentReportsEachExotechHullModThatIsGone() {
        when(settings.getHullModSpec(anyString())).thenReturn(mock(HullModSpecAPI.class));
        when(settings.getHullModSpec("rat_hypatia_hullmod")).thenReturn(null);
        when(settings.getScriptClassLoader()).thenReturn(CompatChecksTest.class.getClassLoader());
        when(settings.getHullSpec("rat_genesis")).thenReturn(mock(ShipHullSpecAPI.class));
        when(settings.getSkillSpec("rat_abyssal_requiem")).thenReturn(mock(SkillSpecAPI.class));
        when(settings.doesVariantExist("rat_genesis_Hull")).thenReturn(true);
        when(settings.getCommoditySpec(anyString())).thenReturn(mock(CommoditySpecAPI.class));
        RandomAssortmentCompat.clearCachedLookups();

        assertEquals(List.of("the hull mod rat_hypatia_hullmod"), RandomAssortmentCompat.TARGET.missingFeatures().get());
    }

    @Test
    void randomAssortmentReportsTheAbyssCurrenciesAndTheGenesisWreckVariantWhenTheyAreGone() {
        when(settings.getHullModSpec(anyString())).thenReturn(mock(HullModSpecAPI.class));
        when(settings.getScriptClassLoader()).thenReturn(CompatChecksTest.class.getClassLoader());
        when(settings.getHullSpec("rat_genesis")).thenReturn(mock(ShipHullSpecAPI.class));
        when(settings.getSkillSpec("rat_abyssal_requiem")).thenReturn(mock(SkillSpecAPI.class));
        when(settings.getCommoditySpec("rat_abyssal_matter")).thenReturn(mock(CommoditySpecAPI.class));
        RandomAssortmentCompat.clearCachedLookups();

        List<String> missing = RandomAssortmentCompat.TARGET.missingFeatures().get();

        assertEquals(2, missing.size(), missing.toString());
        assertTrue(missing.stream().anyMatch(feature -> feature.contains("rat_genesis_Hull")), missing.toString());
        assertTrue(missing.stream().anyMatch(feature -> feature.contains("rat_chronos_core")), missing.toString());
    }

    @Test
    void randomAssortmentReportsWhenTheSingularityHullOrRequiemSkillIsRenamed() {
        when(settings.getHullModSpec(anyString())).thenReturn(mock(HullModSpecAPI.class));
        when(settings.getScriptClassLoader()).thenReturn(CompatChecksTest.class.getClassLoader());
        when(settings.getHullSpec("rat_genesis")).thenThrow(new RuntimeException("no such hull"));
        RandomAssortmentCompat.clearCachedLookups();

        List<String> missing = RandomAssortmentCompat.TARGET.missingFeatures().get();

        assertTrue(missing.stream().anyMatch(feature -> feature.contains("rat_genesis")), missing.toString());
        assertTrue(missing.stream().anyMatch(feature -> feature.contains("rat_abyssal_requiem")), missing.toString());
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
    void nightcrossNodesExcludeExactlyTheHullModsTheSelfCheckWatches() throws Exception {
        JSONArray types = RealSkillData.readJson(RealSkillData.projectRoot().resolve(RealSkillData.TYPES_FILE)).getJSONArray("skillTypes");
        List<String> excludedNightcrossHullMods = new ArrayList<>();
        for (int i = 0; i < types.length(); i++) {
            JSONArray exclusive = types.getJSONObject(i).optJSONArray("exclusiveHullMods");
            for (int j = 0; exclusive != null && j < exclusive.length(); j++) {
                String hullModId = exclusive.getString(j);
                if (NightcrossCompat.REQUIRED_HULL_MOD_IDS.contains(hullModId) || hullModId.startsWith("na_")) {
                    excludedNightcrossHullMods.add(hullModId);
                }
            }
        }

        assertEquals(NightcrossCompat.REQUIRED_HULL_MOD_IDS.stream().sorted().toList(), excludedNightcrossHullMods.stream().sorted().toList());
    }

    @Test
    void nightcrossGravityDragIsReadFromItsTimeflowModifier() {
        ShipAPI target = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat timeMult = new MutableStat(1f);
        when(target.getId()).thenReturn("target");
        when(target.getMutableStats()).thenReturn(stats);
        when(stats.getTimeMult()).thenReturn(timeMult);

        assertEquals(0f, NightcrossCompat.gravityDragPercentOn(target), 0.0001f);
        timeMult.modifyMult("target" + NightcrossCompat.GRAVITY_DRAG_MOD_SUFFIX, 0.85f);
        timeMult.modifyMult("unrelated", 0.5f);
        assertEquals(15f, NightcrossCompat.gravityDragPercentOn(target), 0.0001f);
    }

    @Test
    void exoticaProvidesTheRefitButtonTheSkillTreeButtonMakesRoomFor() {
        when(settings.getScriptClassLoader()).thenReturn(new ClassLoader(null) {
        });

        List<String> missing = ExoticaCompat.TARGET.missingFeatures().get();

        assertEquals(1, missing.size());
        assertTrue(missing.get(0).contains(ExoticaCompat.REFIT_BUTTON_CLASS));
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
