package exiledsector.charts.layout;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NebulaCentreTest {

    private static SectorEntityToken orbiting(SectorEntityToken focus) {
        SectorEntityToken entity = mock(SectorEntityToken.class);
        when(entity.getOrbitFocus()).thenReturn(focus);
        return entity;
    }

    private static PlanetAPI body(boolean nebulaCentre) {
        PlanetSpecAPI spec = mock(PlanetSpecAPI.class);
        when(spec.isNebulaCenter()).thenReturn(nebulaCentre);
        PlanetAPI body = mock(PlanetAPI.class);
        when(body.getSpec()).thenReturn(spec);
        return body;
    }

    private static StarSystemAPI system(StarSystemType type, SectorEntityToken centre, List<SectorEntityToken> entities) {
        StarSystemAPI system = mock(StarSystemAPI.class);
        when(system.getType()).thenReturn(type);
        when(system.getCenter()).thenReturn(centre);
        when(system.getAllEntities()).thenReturn(entities);
        return system;
    }

    @Test
    void everythingCirclingANebulasCentreStopsWhileMoonsKeepTheirPlanets() {
        SectorEntityToken centre = mock(SectorEntityToken.class);
        PlanetAPI hiddenStar = body(true);
        PlanetAPI planet = body(false);
        SectorEntityToken relay = orbiting(centre);
        SectorEntityToken leftover = orbiting(hiddenStar);
        SectorEntityToken moon = orbiting(planet);

        NebulaCentre.holdStill(system(StarSystemType.NEBULA, centre, List.of(relay, leftover, moon)));

        verify(relay).setOrbit(null);
        verify(leftover).setOrbit(null);
        verify(moon, never()).setOrbit(null);
    }

    @Test
    void aSystemWithAStarKeepsItsOrbits() {
        SectorEntityToken centre = mock(SectorEntityToken.class);
        SectorEntityToken relay = orbiting(centre);

        NebulaCentre.holdStill(system(StarSystemType.SINGLE, centre, List.of(relay)));

        verify(relay, never()).setOrbit(null);
    }
}
