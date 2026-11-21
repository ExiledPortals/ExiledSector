package exiledsector.charts.anomaly.debris;

import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.MockSector;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.breach.SpatialBreach;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BreachedGraveyardTest {

    private static final ChartGenContext GRAVEYARD = ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartModifier.CHART_ANOMALOUS_DEBRIS_BREACH_FIELD, 1f)));

    private MockSector mockSector;
    private StarSystemAPI system;
    private final List<CampaignTerrainAPI> terrains = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        system = mock(StarSystemAPI.class);
        when(system.getTerrainCopy()).thenAnswer(invocation -> new ArrayList<>(terrains));
        when(system.getFleets()).thenReturn(new ArrayList<>());
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private AnomalyContentSite site(ChartGenContext context) {
        return new AnomalyContentSite(system, "run1", 4, "pirates", context, List.of(), new Vector2f(), spot -> true, salt -> new Random(salt));
    }

    private void debrisFieldAt(float x, float y) {
        SectorEntityToken entity = mock(SectorEntityToken.class);
        when(entity.getLocation()).thenReturn(new Vector2f(x, y));
        AnomalousDebrisTerrainPlugin field = mock(AnomalousDebrisTerrainPlugin.class);
        when(field.getEntity()).thenReturn(entity);
        CampaignTerrainAPI terrain = mock(CampaignTerrainAPI.class);
        when(terrain.getPlugin()).thenReturn(field);
        terrains.add(terrain);
    }

    @Test
    void aBreachOpensInTheDebrisFieldFarthestFromTheEntry() {
        debrisFieldAt(3000f, 0f);
        debrisFieldAt(0f, -7000f);
        debrisFieldAt(5000f, 1000f);
        Vector2f picked = new Vector2f(100f, 100f);

        assertEquals(new Vector2f(0f, -7000f), BreachedGraveyard.breachCentreOr(site(GRAVEYARD), picked));
    }

    @Test
    void withoutTheNodeOrAnyDebrisTheBreachKeepsItsOwnSpot() {
        Vector2f picked = new Vector2f(100f, 100f);
        assertSame(picked, BreachedGraveyard.breachCentreOr(site(GRAVEYARD), picked));

        debrisFieldAt(3000f, 0f);
        assertSame(picked, BreachedGraveyard.breachCentreOr(site(ChartGenContext.none()), picked));
    }

    @Test
    void debrisPlacedAfterTheBreachPutsItsFirstFieldOnTheBreachCore() {
        SectorEntityToken core = mock(SectorEntityToken.class);
        when(core.getLocation()).thenReturn(new Vector2f(4200f, -800f));
        when(system.getEntitiesWithTag(SpatialBreach.CORE_TAG)).thenReturn(List.of(core));

        AnomalousDebris.Field field = BreachedGraveyard.breachFieldFor(site(GRAVEYARD), DebrisTuning.of(GRAVEYARD), new Random(1L));
        assertNotNull(field);
        assertEquals(new Vector2f(4200f, -800f), field.centre());
        assertEquals(field, AnomalousDebris.pickFields(site(GRAVEYARD), 1, field, new Random(1L)).get(0));

        assertNull(BreachedGraveyard.breachFieldFor(site(ChartGenContext.none()), DebrisTuning.of(ChartGenContext.none()), new Random(1L)));
    }

    @Test
    void debrisPlacedWithoutABreachPicksItsOwnFields() {
        when(system.getEntitiesWithTag(SpatialBreach.CORE_TAG)).thenReturn(List.of());

        assertNull(BreachedGraveyard.breachFieldFor(site(GRAVEYARD), DebrisTuning.of(GRAVEYARD), new Random(1L)));
    }
}
