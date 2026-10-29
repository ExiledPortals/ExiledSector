package exiledsector.charts.run.cleanup;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.GenericPluginManagerAPI;
import com.fs.starfarer.api.campaign.GenericPluginManagerAPI.GenericPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed.SDMParams;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed.SalvageDefenderModificationPlugin;
import exiledsector.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.FakeChartContentEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartDefenderTalliesTest {

    private static final String TALLY_KEY = SalvageGenFromSeed.DEFEATED_DERELICT_STR;
    private static final String MODDED_FLAG = "$someMod_defeatedSomething";
    private static final float DEFEATED_FLEET_POINTS = 30f;

    private MockSector mockSector;
    private MemoryAPI global;
    private GenericPluginManagerAPI plugins;
    private final List<GenericPlugin> registered = new ArrayList<>();
    private final ChartDefenderTallies tallies = new ChartDefenderTallies();

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        global = FakeChartContentEngine.memory();
        when(mockSector.sector.getMemoryWithoutUpdate()).thenReturn(global);
        plugins = mock(GenericPluginManagerAPI.class);
        when(mockSector.sector.getGenericPlugins()).thenReturn(plugins);
        when(plugins.getPluginsOfClass(SalvageDefenderModificationPlugin.class)).thenAnswer(invocation -> new ArrayList<>(registered));
        registered.add(tallies);
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private static SectorEntityToken siteIn(boolean chartSystem) {
        StarSystemAPI system = mock(StarSystemAPI.class);
        when(system.hasTag(anyString())).thenReturn(false);
        when(system.hasTag(ChartIds.SYSTEM_TAG)).thenReturn(chartSystem);
        MemoryAPI siteMemory = FakeChartContentEngine.memory();
        SectorEntityToken site = mock(SectorEntityToken.class);
        when(site.getContainingLocation()).thenReturn(system);
        when(site.getMemoryWithoutUpdate()).thenReturn(siteMemory);
        return site;
    }

    private static SDMParams derelictParams(SectorEntityToken site) {
        SDMParams params = new SDMParams();
        params.entity = site;
        params.factionId = Factions.DERELICT;
        return params;
    }

    private void winAt(SectorEntityToken site) {
        SDMParams params = derelictParams(site);
        SalvageDefenderModificationPlugin picked = pick(params);
        if (picked != null) {
            picked.reportDefeated(params, site, mock(CampaignFleetAPI.class));
        }
    }

    private SalvageDefenderModificationPlugin pick(SDMParams params) {
        SalvageDefenderModificationPlugin picked = null;
        int pickedPriority = -1;
        for (GenericPlugin plugin : registered) {
            int priority = plugin.getHandlingPriority(params);
            if (priority > pickedPriority && priority >= 0) {
                picked = (SalvageDefenderModificationPlugin) plugin;
                pickedPriority = priority;
            }
        }
        return picked;
    }

    private SalvageDefenderModificationPlugin vanillaLikeDerelictTally() {
        SalvageDefenderModificationPlugin vanilla = mock(SalvageDefenderModificationPlugin.class);
        when(vanilla.getHandlingPriority(any())).thenAnswer(invocation -> invocation.getArgument(0) instanceof SDMParams p
                && Factions.DERELICT.equals(p.factionId) ? 1 : -1);
        doAnswer(invocation -> {
            global.set(TALLY_KEY, global.getFloat(TALLY_KEY) + DEFEATED_FLEET_POINTS);
            return null;
        }).when(vanilla).reportDefeated(any(), any(), any());
        registered.add(vanilla);
        return vanilla;
    }

    @Test
    void aWinAtAChartSiteLeavesTheDerelictTallyWhereItWas() {
        vanillaLikeDerelictTally();
        global.set(TALLY_KEY, 50f);

        winAt(siteIn(true));

        assertEquals(50f, global.get(TALLY_KEY));
    }

    @Test
    void anUnsetTallyStaysUnsetAfterAChartWin() {
        vanillaLikeDerelictTally();

        winAt(siteIn(true));

        assertFalse(global.contains(TALLY_KEY));
    }

    @Test
    void aMainSectorWinStillRaisesTheTally() {
        vanillaLikeDerelictTally();
        global.set(TALLY_KEY, 50f);

        winAt(siteIn(false));

        assertEquals(50f + DEFEATED_FLEET_POINTS, global.get(TALLY_KEY));
    }

    @Test
    void aMainSectorWinDuringARunIsKeptAfterALaterChartWin() {
        vanillaLikeDerelictTally();
        global.set(TALLY_KEY, 50f);

        winAt(siteIn(false));
        winAt(siteIn(true));

        assertEquals(50f + DEFEATED_FLEET_POINTS, global.get(TALLY_KEY));
    }

    @Test
    void anotherModsGlobalFlagFromAChartWinIsTakenBackToo() {
        SalvageDefenderModificationPlugin modded = vanillaLikeDerelictTally();
        doAnswer(invocation -> {
            global.set(MODDED_FLAG, true);
            global.set("$unrelated", "changed");
            return null;
        }).when(modded).reportDefeated(any(), any(), any());
        global.set("$unrelated", "kept", 5f);

        winAt(siteIn(true));

        assertFalse(global.contains(MODDED_FLAG));
        assertEquals("kept", global.get("$unrelated"));
    }

    @Test
    void chartSitesTakeOverOnlyWhenAnotherPluginWouldHandleThem() {
        SectorEntityToken chartSite = siteIn(true);
        assertEquals(ChartDefenderTallies.NOT_HANDLED, tallies.getHandlingPriority(derelictParams(chartSite)));

        vanillaLikeDerelictTally();

        assertEquals(GenericPluginManagerAPI.HIGHEST, tallies.getHandlingPriority(derelictParams(chartSite)));
        assertEquals(ChartDefenderTallies.NOT_HANDLED, tallies.getHandlingPriority(derelictParams(siteIn(false))));
    }

    @Test
    void defenderStrengthAndFleetsStillComeFromThePluginItStandsIn() {
        SalvageDefenderModificationPlugin vanilla = vanillaLikeDerelictTally();
        when(vanilla.getStrength(any(), anyFloat(), any(), anyBoolean())).thenReturn(77f);
        SDMParams params = derelictParams(siteIn(true));
        CampaignFleetAPI defenders = mock(CampaignFleetAPI.class);
        Random random = new Random(1L);

        assertSame(tallies, pick(params));
        assertEquals(77f, tallies.getStrength(params, 10f, random, false));
        tallies.modifyFleet(params, defenders, random, false);

        verify(vanilla).modifyFleet(params, defenders, random, false);
    }

    @Test
    void registeringTwiceAddsOnePlugin() {
        when(plugins.hasPlugin(ChartDefenderTallies.class)).thenReturn(false, true);

        ChartDefenderTallies.register(mockSector.sector);
        ChartDefenderTallies.register(mockSector.sector);

        verify(plugins, times(1)).addPlugin(any(ChartDefenderTallies.class), eq(true));
    }
}
