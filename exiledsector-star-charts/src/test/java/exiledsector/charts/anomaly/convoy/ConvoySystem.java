package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.generation.ChartGenContext;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class ConvoySystem {

    static final String RUN_ID = "run1";

    final FakeChartContentEngine engine;
    final StarSystemAPI system = mock(StarSystemAPI.class);
    final Map<String, SectorEntityToken> entities = new HashMap<>();
    final List<EveryFrameScript> scripts = new ArrayList<>();
    final List<SectorEntityToken> tokens = new ArrayList<>();
    final List<CampaignTerrainAPI> terrain = new ArrayList<>();
    final List<PlanetAPI> planets = new ArrayList<>();

    ConvoySystem(int planetCount) {
        engine = FakeChartContentEngine.install(FakeChartContentEngine.offeringNothing());
        PlanetAPI star = planet("star", 0f, 0f);
        when(star.isStar()).thenReturn(true);
        for (int i = 0; i < planetCount; i++) {
            double angle = i * 1.7d;
            planet("planet" + i, (float) Math.cos(angle) * (3000f + 1500f * i), (float) Math.sin(angle) * (3000f + 1500f * i));
        }
        when(system.getPlanets()).thenAnswer(invocation -> new ArrayList<>(planets));
        when(system.getStar()).thenReturn(star);
        when(system.getCenter()).thenReturn(star);
        when(system.getScripts()).thenAnswer(invocation -> new ArrayList<>(scripts));
        when(system.getTerrainCopy()).thenAnswer(invocation -> new ArrayList<>(terrain));
        when(system.getEntityById(anyString())).thenAnswer(invocation -> entities.get((String) invocation.getArgument(0)));
        doAnswer(invocation -> scripts.add(invocation.getArgument(0))).when(system).addScript(any());
        doAnswer(invocation -> {
            SectorEntityToken entity = invocation.getArgument(0);
            entities.put(entity.getId(), entity);
            return null;
        }).when(system).addEntity(any());
        doAnswer(invocation -> {
            SectorEntityToken entity = invocation.getArgument(0);
            entities.remove(entity.getId());
            return null;
        }).when(system).removeEntity(any());
        when(system.createToken(anyFloat(), anyFloat())).thenAnswer(invocation -> token(invocation.getArgument(0), invocation.getArgument(1)));
    }

    PlanetAPI planet(String id, float x, float y) {
        PlanetAPI planet = mock(PlanetAPI.class);
        when(planet.getId()).thenReturn(id);
        when(planet.getName()).thenReturn("Planet " + id);
        when(planet.getLocation()).thenReturn(new Vector2f(x, y));
        when(planet.getRadius()).thenReturn(100f);
        MemoryAPI memory = FakeChartContentEngine.memory();
        when(planet.getMemoryWithoutUpdate()).thenReturn(memory);
        entities.put(id, planet);
        planets.add(planet);
        return planet;
    }

    SectorEntityToken token(float x, float y) {
        SectorEntityToken token = mock(SectorEntityToken.class);
        Vector2f location = new Vector2f(x, y);
        when(token.getLocation()).thenReturn(location);
        doAnswer(invocation -> {
            location.set(invocation.<Float>getArgument(0), invocation.<Float>getArgument(1));
            return null;
        }).when(token).setLocation(anyFloat(), anyFloat());
        tokens.add(token);
        return token;
    }

    AnomalyContentSite site(ChartGenContext context) {
        return new AnomalyContentSite(system, RUN_ID, 4, "pirates", context, List.of(), null, spot -> true,
                salt -> new Random(salt * 31L + 7L));
    }

    ConvoyScript script() {
        return (ConvoyScript) scripts.get(0);
    }

    CampaignFleetAPI fleet(ConvoyState convoy, ConvoyRole role) {
        return (CampaignFleetAPI) entities.get(convoy.fleetId(role));
    }

    List<FakeChartContentEngine.Assignment> assignments(CampaignFleetAPI fleet) {
        return engine.assignments.get(fleet);
    }
}
