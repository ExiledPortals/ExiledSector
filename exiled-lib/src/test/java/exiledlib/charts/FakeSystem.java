package exiledlib.charts;

import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class FakeSystem {

    public final StarSystemAPI system = mock(StarSystemAPI.class);
    public final List<SectorEntityToken> entities = new ArrayList<>();
    public final Map<SectorEntityToken, String> names = new LinkedHashMap<>();
    private PlanetAPI primaryStar;

    public FakeSystem() {
        when(system.getAllEntities()).thenAnswer(invocation -> new ArrayList<>(entities));
        when(system.getPlanets()).thenAnswer(invocation -> entities.stream().filter(PlanetAPI.class::isInstance).map(PlanetAPI.class::cast).toList());
        when(system.getJumpPoints()).thenAnswer(invocation -> entities.stream().filter(JumpPointAPI.class::isInstance).toList());
        when(system.getStar()).thenAnswer(invocation -> primaryStar);
        when(system.getLocation()).thenReturn(new Vector2f());
        doAnswer(invocation -> entities.remove(invocation.<SectorEntityToken>getArgument(0))).when(system).removeEntity(any());
        doAnswer(invocation -> primaryStar = invocation.getArgument(0)).when(system).setStar(any());
    }

    public PlanetAPI addStar(String id, float radius) {
        PlanetAPI star = body(id, radius, null, 0f);
        when(star.isStar()).thenReturn(true);
        if (primaryStar == null) {
            primaryStar = star;
        }
        return star;
    }

    public PlanetAPI addPlanet(String id, float radius, SectorEntityToken focus, float orbitRadius) {
        PlanetAPI planet = body(id, radius, focus, orbitRadius);
        MarketAPI market = mock(MarketAPI.class);
        when(market.getId()).thenReturn("market_" + id);
        when(planet.getMarket()).thenReturn(market);
        return planet;
    }

    public JumpPointAPI addJumpPoint() {
        JumpPointAPI jumpPoint = mock(JumpPointAPI.class);
        entities.add(jumpPoint);
        return jumpPoint;
    }

    private PlanetAPI body(String id, float radius, SectorEntityToken focus, float orbitRadius) {
        PlanetAPI body = mock(PlanetAPI.class);
        when(body.getId()).thenReturn(id);
        when(body.getRadius()).thenReturn(radius);
        when(body.getOrbitFocus()).thenReturn(focus);
        when(body.getCircularOrbitRadius()).thenReturn(orbitRadius);
        Vector2f focusLocation = focus == null ? new Vector2f() : focus.getLocation();
        when(body.getLocation()).thenReturn(new Vector2f(focusLocation.x + orbitRadius, focusLocation.y));
        doAnswer(invocation -> names.put(body, invocation.getArgument(0))).when(body).setName(anyString());
        entities.add(body);
        return body;
    }

    public List<String> namesInOrder() {
        List<String> ordered = new ArrayList<>();
        for (SectorEntityToken entity : entities) {
            ordered.add(entity.getId() + "=" + names.get(entity));
        }
        return ordered;
    }
}
