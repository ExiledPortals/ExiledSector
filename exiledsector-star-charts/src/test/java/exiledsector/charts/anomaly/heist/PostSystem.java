package exiledsector.charts.anomaly.heist;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
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

final class PostSystem {

    static final String RUN_ID = "run1";

    final FakeChartContentEngine engine;
    final StarSystemAPI system = mock(StarSystemAPI.class);
    final Map<String, SectorEntityToken> entities = new HashMap<>();
    final List<EveryFrameScript> scripts = new ArrayList<>();
    final PlanetAPI star;

    PostSystem() {
        engine = FakeChartContentEngine.install(FakeChartContentEngine.offeringNothing());
        star = mock(PlanetAPI.class);
        when(star.getId()).thenReturn("star");
        when(star.isStar()).thenReturn(true);
        when(star.getLocation()).thenReturn(new Vector2f());
        when(system.getId()).thenReturn("charted space");
        when(system.getOptionalUniqueId()).thenReturn(ChartIds.systemId(RUN_ID));
        when(system.getStar()).thenReturn(star);
        when(system.getCenter()).thenReturn(star);
        when(system.getScripts()).thenAnswer(invocation -> new ArrayList<>(scripts));
        when(system.getEntityById(anyString())).thenAnswer(invocation -> entities.get((String) invocation.getArgument(0)));
        doAnswer(invocation -> scripts.add(invocation.getArgument(0))).when(system).addScript(any());
        doAnswer(invocation -> {
            SectorEntityToken entity = invocation.getArgument(0);
            entities.put(entity.getId(), entity);
            when(entity.getContainingLocation()).thenReturn(system);
            return null;
        }).when(system).addEntity(any());
        when(system.createToken(anyFloat(), anyFloat())).thenAnswer(invocation -> token(invocation.getArgument(0), invocation.getArgument(1)));
        when(system.addCustomEntity(anyString(), anyString(), anyString(), anyString())).thenAnswer(invocation -> customEntity(invocation.getArgument(0)));
    }

    private CustomCampaignEntityAPI customEntity(String id) {
        CustomCampaignEntityAPI entity = engine.tagged(mock(CustomCampaignEntityAPI.class));
        when(entity.getId()).thenReturn(id);
        when(entity.getContainingLocation()).thenReturn(system);
        when(entity.getName()).thenReturn("Listening Post");
        Vector2f location = new Vector2f();
        when(entity.getLocation()).thenReturn(location);
        doAnswer(invocation -> {
            SectorEntityToken focus = invocation.getArgument(0);
            double angle = Math.toRadians(invocation.<Float>getArgument(1));
            float radius = invocation.getArgument(2);
            location.set(focus.getLocation().x + (float) Math.cos(angle) * radius, focus.getLocation().y + (float) Math.sin(angle) * radius);
            return null;
        }).when(entity).setCircularOrbitPointingDown(any(), anyFloat(), anyFloat(), anyFloat());
        entities.put(id, entity);
        return entity;
    }

    SectorEntityToken token(float x, float y) {
        SectorEntityToken token = mock(SectorEntityToken.class);
        Vector2f location = new Vector2f(x, y);
        when(token.getLocation()).thenReturn(location);
        doAnswer(invocation -> {
            location.set(invocation.<Float>getArgument(0), invocation.<Float>getArgument(1));
            return null;
        }).when(token).setLocation(anyFloat(), anyFloat());
        return token;
    }

    static PostScript placeIn(AnomalyContentSite site, ChartRun run) {
        PostScript script = ListeningPost.place(site);
        site.commitStates(run);
        return script;
    }

    AnomalyContentSite site(ChartGenContext context, Vector2f entry, long seed) {
        return new AnomalyContentSite(system, RUN_ID, 4, "pirates", context, List.of(), entry, spot -> true, salt -> new Random(seed ^ salt));
    }

    PostScript script() {
        return (PostScript) scripts.get(0);
    }

    SectorEntityToken post() {
        return entities.get(ListeningPost.postId(RUN_ID));
    }

    List<CampaignFleetAPI> pickets() {
        return script().livingPickets();
    }

    List<FakeChartContentEngine.Assignment> assignments(CampaignFleetAPI fleet) {
        return engine.assignments.get(fleet);
    }
}
