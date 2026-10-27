package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.generation.ChartGenContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

class SupplyConvoyTest {

    @AfterEach
    void tearDown() {
        ChartContentGenerator.resetForTests();
    }

    @Test
    void theConvoyGathersAtTheSystemEdgeWithFreightersAScreenAndACloseEscort() {
        ConvoySystem convoySystem = new ConvoySystem(5);

        SupplyConvoy.INSTANCE.generate(convoySystem.site(ChartGenContext.none()));

        ConvoyScript script = convoySystem.script();
        ConvoyState convoy = script.convoys().get(0);
        CampaignFleetAPI freighters = convoySystem.fleet(convoy, ConvoyRole.FREIGHTERS);
        CampaignFleetAPI screen = convoySystem.fleet(convoy, ConvoyRole.SCREEN);
        CampaignFleetAPI escort = convoySystem.fleet(convoy, ConvoyRole.ESCORT);
        assertNotNull(freighters);
        assertNotNull(screen);
        assertNotNull(escort);
        assertEquals(List.of("pirates"), convoySystem.engine.dressedFactions);
        for (CampaignFleetAPI fleet : List.of(freighters, screen, escort)) {
            verify(fleet).setFaction("convoy", true);
        }
        assertTrue(freighters.getLocation().length() > 3000f + 1500f * 4, "the convoy starts beyond the outermost planet");
        assertEquals(FleetAssignment.ORBIT_PASSIVE, convoySystem.assignments(freighters).get(0).assignment());
        assertEquals(FleetAssignment.FOLLOW, convoySystem.assignments(escort).get(0).assignment());
        assertSame(freighters, convoySystem.assignments(escort).get(0).target());
        assertSame(convoy.screenLead(), convoySystem.assignments(screen).get(0).target());
        assertTrue(freighters.hasTag(ChartIds.runTag(ConvoySystem.RUN_ID)));
    }

    @Test
    void everyConvoyFleetIsNonHostileAndCarriesNoReputationImpact() {
        ConvoySystem convoySystem = new ConvoySystem(4);

        SupplyConvoy.INSTANCE.generate(convoySystem.site(ChartGenContext.none()));

        ConvoyState convoy = convoySystem.script().convoys().get(0);
        for (ConvoyRole role : ConvoyRole.values()) {
            CampaignFleetAPI fleet = convoySystem.fleet(convoy, role);
            assertTrue(fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE), role.key());
            assertFalse(fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_MAKE_HOSTILE), role.key());
            assertFalse(fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE), role.key());
            assertTrue(fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_NO_REP_IMPACT), role.key());
            assertEquals(role.key(), fleet.getMemoryWithoutUpdate().getString(SupplyConvoy.ROLE_MEMORY_KEY));
        }
        assertTrue(convoySystem.fleet(convoy, ConvoyRole.FREIGHTERS).getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_TRADE_FLEET));
        assertFalse(convoySystem.fleet(convoy, ConvoyRole.FREIGHTERS).getMemoryWithoutUpdate().contains(ChartIds.START_DP_MEMORY_KEY),
                "freighters carry the convoy's cargo, not ordinary fleet drops");
    }

    @Test
    void theRouteVisitsTwoToFourOtherStopsBeforeAnExistingPlanet() {
        for (int planets = 3; planets <= 7; planets++) {
            ConvoySystem convoySystem = new ConvoySystem(planets);

            SupplyConvoy.INSTANCE.generate(convoySystem.site(ChartGenContext.none()));

            ConvoyState convoy = convoySystem.script().convoys().get(0);
            assertTrue(convoySystem.entities.containsKey(convoy.destinationId()));
            assertFalse(convoy.waypointIds().contains(convoy.destinationId()));
            int stops = convoy.waypointIds().size();
            assertTrue(stops >= Math.min(ConvoyRules.MIN_WAYPOINTS, planets - 1) && stops <= ConvoyRules.MAX_WAYPOINTS, "got " + stops);
            assertEquals(stops, new HashSet<>(convoy.waypointIds()).size());
        }
    }

    @Test
    void withoutAPlanetOrFreightersThereIsNoConvoy() {
        ConvoySystem noPlanets = new ConvoySystem(0);
        assertThrows(IllegalStateException.class, () -> SupplyConvoy.INSTANCE.generate(noPlanets.site(ChartGenContext.none())));

        ConvoySystem noFreighters = new ConvoySystem(4);
        noFreighters.engine.withEmptyFleets();
        assertThrows(IllegalStateException.class, () -> SupplyConvoy.INSTANCE.generate(noFreighters.site(ChartGenContext.none())));
        assertTrue(noFreighters.scripts.isEmpty());
    }
}
