package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import data.kaysaar.aotd.vok.scripts.research.AoTDFactionResearchManager;
import data.kaysaar.aotd.vok.scripts.research.AoTDMainResearchManager;
import data.kaysaar.aotd.vok.scripts.research.models.ResearchOption;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VaultsOfKnowledgeCompatTest {

    private static MockedStatic<Global> vaultsInstalled(ClassLoader scriptLoader) {
        SettingsAPI settings = mock(SettingsAPI.class);
        ModManagerAPI modManager = mock(ModManagerAPI.class);
        when(settings.getModManager()).thenReturn(modManager);
        when(settings.getScriptClassLoader()).thenReturn(scriptLoader);
        when(modManager.isModEnabled(VaultsOfKnowledgeCompat.MOD_ID)).thenReturn(true);
        MockedStatic<Global> global = Mockito.mockStatic(Global.class);
        global.when(Global::getSettings).thenReturn(settings);
        VaultsOfKnowledgeCompat.clearCachedLookups();
        return global;
    }

    @Test
    void thePlayerResearchLookupResolvesAgainstVaultsOfKnowledge() {
        try (MockedStatic<Global> ignored = vaultsInstalled(VaultsOfKnowledgeCompatTest.class.getClassLoader())) {
            assertEquals(List.of(), VaultsOfKnowledgeCompat.missingFeatures());
        }
    }

    @Test
    void aMissingPlayerResearchLookupIsListed() throws Exception {
        try (URLClassLoader empty = new URLClassLoader(new URL[0], null);
             MockedStatic<Global> ignored = vaultsInstalled(empty)) {
            assertTrue(VaultsOfKnowledgeCompat.missingFeatures().stream().anyMatch(feature -> feature.contains("isResearchedForPlayer")));
        }
    }

    @Test
    void thePlayerResearchIsReadFromTheResearchManager() {
        AoTDMainResearchManager.instance = new AoTDMainResearchManager();
        try (MockedStatic<Global> ignored = vaultsInstalled(VaultsOfKnowledgeCompatTest.class.getClassLoader())) {
            assertFalse(VaultsOfKnowledgeCompat.isResearchedForPlayer(VaultsOfKnowledgeCompat.GATEWAY_ADVANCED_RESEARCH_ID));
            AoTDMainResearchManager.RESEARCHED.add(VaultsOfKnowledgeCompat.GATEWAY_ADVANCED_RESEARCH_ID);
            assertTrue(VaultsOfKnowledgeCompat.isResearchedForPlayer(VaultsOfKnowledgeCompat.GATEWAY_ADVANCED_RESEARCH_ID));
        } finally {
            AoTDMainResearchManager.instance = null;
            AoTDMainResearchManager.RESEARCHED.clear();
            VaultsOfKnowledgeCompat.clearCachedLookups();
        }
    }

    @Test
    void aFailedPlayerResearchLookupCountsAsResearched() throws Exception {
        try (URLClassLoader empty = new URLClassLoader(new URL[0], null);
             MockedStatic<Global> ignored = vaultsInstalled(empty)) {
            assertTrue(VaultsOfKnowledgeCompat.isResearchedForPlayer(VaultsOfKnowledgeCompat.GATEWAY_PERFECTED_RESEARCH_ID));
        } finally {
            VaultsOfKnowledgeCompat.clearCachedLookups();
        }
    }

    @Test
    void theDefenceRollStaysInsideAoTDsRangeAndReachesBothEnds() {
        Random random = new Random(3L);
        Set<Integer> rolled = new HashSet<>();
        for (int roll = 0; roll < 2000; roll++) {
            int points = VaultsOfKnowledgeCompat.defenceFleetPoints(VaultsOfKnowledgeCompat.DEFAULT_DEFENCE_MIN,
                    VaultsOfKnowledgeCompat.DEFAULT_DEFENCE_MAX, random);
            assertTrue(points >= VaultsOfKnowledgeCompat.DEFAULT_DEFENCE_MIN && points <= VaultsOfKnowledgeCompat.DEFAULT_DEFENCE_MAX,
                    String.valueOf(points));
            rolled.add(points);
        }
        assertTrue(rolled.contains(VaultsOfKnowledgeCompat.DEFAULT_DEFENCE_MIN));
        assertTrue(rolled.contains(VaultsOfKnowledgeCompat.DEFAULT_DEFENCE_MAX));
    }

    private static AoTDFactionResearchManager factionWithGatewayResearch() {
        AoTDFactionResearchManager faction = new AoTDFactionResearchManager();
        faction.getResearchRepoOfFaction().add(new ResearchOption("aotd_tech_other"));
        for (String id : VaultsOfKnowledgeCompat.GATEWAY_RESEARCH_IDS) {
            ResearchOption option = new ResearchOption(id);
            faction.getResearchRepoOfFaction().add(option);
            faction.getQueueManager().queuedResearchOptions.add(option);
        }
        faction.setCurrentFocus(VaultsOfKnowledgeCompat.GATEWAY_RESEARCH_ID);
        return faction;
    }

    private static List<String> repoIds(AoTDFactionResearchManager faction) {
        return faction.getResearchRepoOfFaction().stream().map(option -> option.Id).toList();
    }

    @Test
    void hidingTheGatewayResearchRemovesItFromEveryFactionTreeQueueAndFocus() {
        AoTDMainResearchManager.instance = new AoTDMainResearchManager();
        AoTDFactionResearchManager player = factionWithGatewayResearch();
        AoTDFactionResearchManager other = factionWithGatewayResearch();
        other.setCurrentFocus("aotd_tech_other");
        AoTDMainResearchManager.instance.factionResearchManagers.addAll(List.of(player, other));
        try (MockedStatic<Global> ignored = vaultsInstalled(VaultsOfKnowledgeCompatTest.class.getClassLoader())) {
            VaultsOfKnowledgeCompat.showGatewayResearch(false);

            for (AoTDFactionResearchManager faction : List.of(player, other)) {
                assertEquals(List.of("aotd_tech_other"), repoIds(faction));
                assertTrue(faction.getQueueManager().queuedResearchOptions.isEmpty());
            }
            assertNull(player.currentFocusId);
            assertEquals("aotd_tech_other", other.currentFocusId);
        } finally {
            AoTDMainResearchManager.instance = null;
            VaultsOfKnowledgeCompat.clearCachedLookups();
        }
    }

    @Test
    void showingTheGatewayResearchRebuildsItFromTheSpecsOnlyWhenItIsMissing() {
        AoTDMainResearchManager.instance = new AoTDMainResearchManager();
        AoTDMainResearchManager.SPEC_IDS.addAll(VaultsOfKnowledgeCompat.GATEWAY_RESEARCH_IDS);
        AoTDFactionResearchManager player = factionWithGatewayResearch();
        AoTDMainResearchManager.instance.factionResearchManagers.add(player);
        try (MockedStatic<Global> ignored = vaultsInstalled(VaultsOfKnowledgeCompatTest.class.getClassLoader())) {
            VaultsOfKnowledgeCompat.showGatewayResearch(true);
            assertEquals(0, AoTDMainResearchManager.instance.specUpdates);

            VaultsOfKnowledgeCompat.showGatewayResearch(false);
            VaultsOfKnowledgeCompat.showGatewayResearch(true);
            assertEquals(1, AoTDMainResearchManager.instance.specUpdates);
            assertTrue(repoIds(player).containsAll(VaultsOfKnowledgeCompat.GATEWAY_RESEARCH_IDS));
        } finally {
            AoTDMainResearchManager.instance = null;
            AoTDMainResearchManager.SPEC_IDS.clear();
            VaultsOfKnowledgeCompat.clearCachedLookups();
        }
    }

    @Test
    void aMissingResearchTreeEditIsListed() throws Exception {
        try (URLClassLoader empty = new URLClassLoader(new URL[0], null);
             MockedStatic<Global> ignored = vaultsInstalled(empty)) {
            assertTrue(VaultsOfKnowledgeCompat.missingFeatures().stream().anyMatch(feature -> feature.contains("ResearchQueueManager")));
        }
    }

    @Test
    void aSwappedOrNegativeRangeStillRolls() {
        int swapped = VaultsOfKnowledgeCompat.defenceFleetPoints(200, 100, new Random(1L));
        assertTrue(swapped >= 100 && swapped <= 200, String.valueOf(swapped));
        assertEquals(0, VaultsOfKnowledgeCompat.defenceFleetPoints(-5, -10, new Random(1L)));
    }
}
