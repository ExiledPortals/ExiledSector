package exiledsector.charts.anomaly.stellarflares;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.terrain.FlareManager;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;
import exiledsector.charts.anomaly.LiveAnomalyRuns;
import exiledsector.charts.anomaly.StellarFlares;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FlareTempoTest {

    private static StarSystemAPI systemWithStar(PlanetAPI star) {
        StarSystemAPI system = mock(StarSystemAPI.class);
        when(star.isStar()).thenReturn(true);
        when(star.getContainingLocation()).thenReturn(system);
        when(system.getPlanets()).thenReturn(List.of(star, mock(PlanetAPI.class)));
        return system;
    }

    private static ChartGenContext flareFrequency(float percent) {
        return ChartGenContext.of(List.of(new AppliedEffect(ChartModifier.CHART_STELLAR_FLARES_FLARE_FREQUENCY_MULT, percent)));
    }

    @Test
    void solarMaximumAdvancesEveryCoronasFlaresFaster() {
        PlanetAPI star = mock(PlanetAPI.class);
        StarSystemAPI system = systemWithStar(star);
        StarCoronaTerrainPlugin corona = mock(StarCoronaTerrainPlugin.class);
        FlareManager flares = mock(FlareManager.class);
        when(corona.getFlareManager()).thenReturn(flares);
        when(corona.getRelatedEntity()).thenReturn(star);
        CampaignTerrainAPI terrain = mock(CampaignTerrainAPI.class);
        when(terrain.getPlugin()).thenReturn(corona);
        when(system.getTerrainCopy()).thenReturn(List.of(terrain));

        StellarFlares.INSTANCE.generate(LiveAnomalyRuns.site(system, LiveAnomalyRuns.PIRATES, flareFrequency(50f), spot -> true));

        ArgumentCaptor<EveryFrameScript> added = ArgumentCaptor.forClass(EveryFrameScript.class);
        verify(system).addScript(added.capture());
        FlareTempo tempo = (FlareTempo) added.getValue();
        assertEquals(0.5f, tempo.extraShare(), 1e-6f);

        tempo.everyFrame(null, 2f);

        verify(flares).advance(1f);
    }

    @Test
    void withoutSolarMaximumTheFlaresKeepTheirOwnPace() {
        StarSystemAPI system = systemWithStar(mock(PlanetAPI.class));

        StellarFlares.INSTANCE.generate(LiveAnomalyRuns.site(system, LiveAnomalyRuns.PIRATES, ChartGenContext.none(), spot -> true));

        verify(system, never()).addScript(any());
    }
}
