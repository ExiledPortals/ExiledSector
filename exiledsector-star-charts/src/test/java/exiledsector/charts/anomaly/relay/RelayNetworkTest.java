package exiledsector.charts.anomaly.relay;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.content.ChartSpot;
import exiledsector.charts.generation.ChartGenContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RelayNetworkTest {

    private StarSystemAPI system;
    private PlanetAPI star;
    private final List<SectorEntityToken> added = new ArrayList<>();
    private final List<String> addedTypes = new ArrayList<>();
    private final List<String> addedFactions = new ArrayList<>();

    @BeforeEach
    void setUp() {
        system = mock(StarSystemAPI.class);
        star = mock(PlanetAPI.class);
        when(star.getLocation()).thenReturn(new Vector2f());
        when(system.getCenter()).thenReturn(star);
        when(system.addCustomEntity(anyString(), any(), anyString(), anyString())).thenAnswer(invocation -> {
            SectorEntityToken entity = RelayFixtures.entity(new Vector2f());
            added.add(entity);
            addedTypes.add(invocation.getArgument(2));
            addedFactions.add(invocation.getArgument(3));
            return entity;
        });
    }

    private AnomalyContentSite site(List<SectorEntityToken> guarded, Predicate<ChartSpot> clear, Random random) {
        return new AnomalyContentSite(system, "run1", 4, "pirates", ChartGenContext.none(), guarded, guarded, null, clear, salt -> random);
    }

    @Test
    void theNetworkPlacesOneDormantRelayOfEachKindOnClearOrbits() {
        AnomalyContentSite site = site(List.of(), spot -> true, new Random(3L));

        RelayNetwork.INSTANCE.generate(site);

        assertEquals(3, added.size());
        assertEquals(List.of("comm_relay", "nav_buoy", "sensor_array"), addedTypes.stream().sorted().toList());
        assertTrue(addedFactions.stream().allMatch(Factions.NEUTRAL::equals));
        assertEquals(added, site.placed());
        for (SectorEntityToken relay : added) {
            assertTrue(RelayNetwork.isRelay(relay));
            assertTrue(relay.hasTag(ChartIds.runTag("run1")));
            assertTrue(relay.getMemoryWithoutUpdate().getBoolean(MemFlags.OBJECTIVE_NON_FUNCTIONAL));
            verify(relay).setCircularOrbitPointingDown(eq(star), anyFloat(), anyFloat(), anyFloat());
        }
        assertEquals(List.of(RelayKind.values()).stream().sorted().toList(), added.stream().map(RelayKind::of).sorted().toList());
    }

    @Test
    void aRelayCanShareAGuardedPointOfInterest() {
        SectorEntityToken guardedPoi = RelayFixtures.entity(new Vector2f(4000f, 0f));
        when(guardedPoi.getRadius()).thenReturn(50f);
        Random alwaysGuarded = new Random(1L) {
            @Override
            public float nextFloat() {
                return 0f;
            }
        };

        RelayNetwork.INSTANCE.generate(site(List.of(guardedPoi), spot -> true, alwaysGuarded));

        List<SectorEntityToken> foci = new ArrayList<>();
        List<Float> radii = new ArrayList<>();
        for (SectorEntityToken relay : added) {
            ArgumentCaptor<SectorEntityToken> focus = ArgumentCaptor.forClass(SectorEntityToken.class);
            ArgumentCaptor<Float> radius = ArgumentCaptor.forClass(Float.class);
            verify(relay).setCircularOrbitPointingDown(focus.capture(), anyFloat(), radius.capture(), anyFloat());
            foci.add(focus.getValue());
            radii.add(radius.getValue());
        }
        int nearPoi = foci.indexOf(guardedPoi);
        assertEquals(1, foci.stream().filter(guardedPoi::equals).count());
        assertEquals(50f + RelayRules.GUARD_GAP_MIN, radii.get(nearPoi), 0.001f);
    }

    @Test
    void withNowhereClearTheNetworkFails() {
        assertThrows(IllegalStateException.class, () -> RelayNetwork.INSTANCE.generate(site(List.of(), spot -> false, new Random(5L))));
    }

    @Test
    void relayKindsAreReadBackFromTheirEntities() {
        SectorEntityToken relay = RelayFixtures.relay(RelayKind.SENSOR_ARRAY, new Vector2f());

        assertSame(RelayKind.SENSOR_ARRAY, RelayKind.of(relay));
        assertEquals(null, RelayKind.of(RelayFixtures.entity(new Vector2f())));
        assertEquals(RelayKind.COMM_RELAY, RelayKind.parse("comm_relay"));
    }
}
