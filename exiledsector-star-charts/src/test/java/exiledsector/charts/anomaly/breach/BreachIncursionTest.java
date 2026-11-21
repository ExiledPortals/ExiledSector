package exiledsector.charts.anomaly.breach;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import exiledsector.charts.generation.ChartTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BreachIncursionTest {

    private final List<CampaignFleetAPI> fleets = new ArrayList<>();
    private StarSystemAPI system;
    private SectorEntityToken station;

    @BeforeEach
    void setUp() {
        system = mock(StarSystemAPI.class);
        when(system.getFleets()).thenReturn(fleets);
        station = mock(SectorEntityToken.class);
        when(system.getEntitiesWithTag(BreachIncursion.STATION_TAG)).thenReturn(List.of(station));
    }

    private CampaignFleetAPI guardAt(float x) {
        CampaignFleetAPI guard = mock(CampaignFleetAPI.class);
        when(guard.hasTag(BreachIncursion.GUARD_TAG)).thenReturn(true);
        when(guard.isAlive()).thenReturn(true);
        when(guard.getLocation()).thenReturn(new Vector2f(x, 0f));
        fleets.add(guard);
        return guard;
    }

    @Test
    void theStationUnlocksOnceItsGuardIsDestroyed() {
        CampaignFleetAPI guard = guardAt(800f);

        assertEquals(BreachIncursion.GUARDED, BreachIncursion.track(BreachIncursion.GUARDED, system, new Vector2f(), 2000f, false));
        verify(station, never()).removeTag(Tags.NON_CLICKABLE);

        when(guard.isEmpty()).thenReturn(true);

        assertEquals(BreachIncursion.CLEARED, BreachIncursion.track(BreachIncursion.GUARDED, system, new Vector2f(), 2000f, false));
        verify(station).removeTag(Tags.NON_CLICKABLE);
    }

    @Test
    void theStationAndItsGuardVanishOnceTheCollapsingEdgePassesTheGuard() {
        CampaignFleetAPI guard = guardAt(1500f);

        assertEquals(BreachIncursion.GUARDED, BreachIncursion.track(BreachIncursion.GUARDED, system, new Vector2f(), 2000f, true));
        assertEquals(BreachIncursion.GUARDED, BreachIncursion.track(BreachIncursion.GUARDED, system, new Vector2f(), 1000f, false));

        assertEquals(BreachIncursion.GONE, BreachIncursion.track(BreachIncursion.GUARDED, system, new Vector2f(), 1000f, true));
        verify(guard).despawn();
        verify(system).removeEntity(station);
    }

    @Test
    void aClearedOrFinishedIncursionIsLeftAloneByTheEdgeAndTheCollapse() {
        CampaignFleetAPI guard = guardAt(1500f);

        assertEquals(BreachIncursion.CLEARED, BreachIncursion.track(BreachIncursion.CLEARED, system, new Vector2f(), 1000f, true));
        assertEquals(BreachIncursion.CLEARED, BreachIncursion.collapse(BreachIncursion.CLEARED, system));
        assertEquals(BreachIncursion.NONE, BreachIncursion.collapse(BreachIncursion.NONE, system));

        verify(guard, never()).despawn();
        verify(system, never()).removeEntity(station);
    }

    @Test
    void anIncursionThatNeverSpawnedEndsWithTheCollapseAndLeavesNothingToRemove() {
        assertEquals(BreachIncursion.GONE, BreachIncursion.collapse(BreachIncursion.WAITING, system));

        verify(system, never()).removeEntity(station);
    }

    @Test
    void aGuardedIncursionVanishesWhenTheBreachCollapses() {
        CampaignFleetAPI guard = guardAt(800f);

        assertEquals(BreachIncursion.GONE, BreachIncursion.collapse(BreachIncursion.GUARDED, system));

        verify(guard).despawn();
        verify(system).removeEntity(station);
    }

    @Test
    void theGuardIsTenPercentLargerThanTheBossFleetOrTheRegularFleetsWhereThereIsNoBoss() {
        ChartTier withBoss = new ChartTier(20, 80f, 1, 2, 0, 0, 1f, 1, 2, 0f, 0f, 0f, 0f, true, 200f);
        ChartTier withoutBoss = new ChartTier(2, 80f, 1, 2, 0, 0, 1f, 1, 2, 0f, 0f, 0f, 0f, false, 0f);

        assertEquals(220f, BreachIncursion.guardSize(withBoss), 0.01f);
        assertEquals(88f, BreachIncursion.guardSize(withoutBoss), 0.01f);
        CampaignFleetAPI guard = guardAt(0f);
        assertTrue(BreachIncursion.isGuard(guard));
        assertFalse(BreachIncursion.isGuard(mock(CampaignFleetAPI.class)));
        assertFalse(BreachIncursion.isGuard(null));
    }
}
