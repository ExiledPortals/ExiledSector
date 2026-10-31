package exiledsector.charts.anomaly;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.FleetAdvanceScript;
import exiledsector.charts.content.FakeChartContentEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HeldGuardsTest {

    private static final String KEY = "$exiledSector_testGuards";

    private final List<EveryFrameScript> planetScripts = new ArrayList<>();
    private StarSystemAPI system;
    private PlanetAPI planet;
    private MemoryAPI planetMemory;

    @BeforeEach
    void setUp() {
        system = mock(StarSystemAPI.class);
        planet = mock(PlanetAPI.class);
        planetMemory = FakeChartContentEngine.memory();
        when(planet.getMemoryWithoutUpdate()).thenReturn(planetMemory);
        when(planet.getContainingLocation()).thenReturn(system);
        when(planet.getLocation()).thenReturn(new Vector2f(100f, 200f));
        doAnswer(invocation -> planetScripts.add(invocation.getArgument(0))).when(planet).addScript(any());
        when(planet.hasScriptOfClass(FleetAdvanceScript.class)).thenAnswer(invocation -> !planetScripts.isEmpty());
        doAnswer(invocation -> {
            planetScripts.clear();
            return null;
        }).when(planet).removeScriptsOfClass(FleetAdvanceScript.class);
    }

    private static CampaignFleetAPI guards() {
        CampaignFleetAPI guards = mock(CampaignFleetAPI.class);
        when(guards.isEmpty()).thenReturn(false);
        return guards;
    }

    @Test
    void heldGuardsSitAtTheSiteAndRepairBetweenFightsLikeVanillaDefenders() {
        CampaignFleetAPI guards = guards();

        assertSame(guards, HeldGuards.held(planet, KEY, () -> guards));
        assertSame(guards, HeldGuards.held(planet, KEY, HeldGuardsTest::guards));

        assertSame(guards, planetMemory.get(KEY));
        verify(guards, times(2)).setContainingLocation(system);
        verify(guards, times(2)).setDoNotAdvanceAI(true);
        verify(guards, times(2)).setLocation(100f, 200f);
        assertEquals(1, planetScripts.size());
        assertInstanceOf(FleetAdvanceScript.class, planetScripts.get(0));
    }

    @Test
    void forgottenGuardsStopAdvancingAndNoneAreHeldWhenNoneCanBeBuilt() {
        HeldGuards.held(planet, KEY, HeldGuardsTest::guards);

        HeldGuards.forget(planet, KEY);

        assertNull(planetMemory.get(KEY));
        assertTrue(planetScripts.isEmpty());
        assertNull(HeldGuards.held(planet, KEY, () -> null));
        assertTrue(planetScripts.isEmpty());
    }
}
