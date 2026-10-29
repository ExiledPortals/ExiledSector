package exiledsector.charts.layout;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.terrain.FlareManager;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartNeutronTerrainTest {

    @Test
    void thePulsarBeamLeavesEveryFleetButThePlayersAlone() {
        ChartPulsarBeamTerrainPlugin beam = spy(new ChartPulsarBeamTerrainPlugin());
        doReturn(1f).when(beam).getIntensityAtPoint(any());
        CampaignFleetAPI enemy = fleet(false);

        beam.applyEffect(enemy, 1f);

        verify(enemy, never()).getFleetData();
        verify(enemy, never()).setVelocity(anyFloat(), anyFloat());
    }

    @Test
    void thePulsarBeamStillReachesThePlayerFleet() {
        ChartPulsarBeamTerrainPlugin beam = spy(new ChartPulsarBeamTerrainPlugin());
        doReturn(0f).when(beam).getIntensityAtPoint(any());
        CampaignFleetAPI player = fleet(true);

        beam.applyEffect(player, 1f);

        verify(beam).getIntensityAtPoint(player.getLocation());
    }

    @Test
    void theNeutronCoronaLeavesEveryFleetButThePlayersAlone() throws ReflectiveOperationException {
        ChartCoronaTerrainPlugin corona = corona(1f);
        CampaignFleetAPI enemy = fleet(false);

        corona.applyEffect(enemy, 1f);

        verify(enemy, never()).getFleetData();
        verify(enemy, never()).setVelocity(anyFloat(), anyFloat());
    }

    @Test
    void theNeutronCoronaStillReachesThePlayerFleet() throws ReflectiveOperationException {
        ChartCoronaTerrainPlugin corona = corona(0f);
        CampaignFleetAPI player = fleet(true);

        corona.applyEffect(player, 1f);

        verify(corona, atLeastOnce()).getIntensityAtPoint(player.getLocation());
    }

    @Test
    void entitiesThatAreNotFleetsAreIgnored() {
        ChartPulsarBeamTerrainPlugin beam = spy(new ChartPulsarBeamTerrainPlugin());

        beam.applyEffect(mock(SectorEntityToken.class), 1f);

        verify(beam, never()).getIntensityAtPoint(any());
    }

    private static ChartCoronaTerrainPlugin corona(float intensity) throws ReflectiveOperationException {
        ChartCoronaTerrainPlugin corona = spy(new ChartCoronaTerrainPlugin());
        Field flares = StarCoronaTerrainPlugin.class.getDeclaredField("flareManager");
        flares.setAccessible(true);
        flares.set(corona, mock(FlareManager.class));
        doReturn(intensity).when(corona).getIntensityAtPoint(any());
        return corona;
    }

    private static CampaignFleetAPI fleet(boolean player) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        when(fleet.isPlayerFleet()).thenReturn(player);
        when(fleet.getLocation()).thenReturn(new Vector2f(100f, 0f));
        when(fleet.getVelocity()).thenReturn(new Vector2f());
        return fleet;
    }
}
