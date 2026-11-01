package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.generation.ChartGenContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void theConvoyDeliversToPlanetsNoOtherAnomalyHoldsAndClaimsThem() {
        ConvoySystem convoySystem = new ConvoySystem(3);
        AnomalyContentSite site = convoySystem.site(ChartGenContext.none());
        site.claim(convoySystem.entities.get("planet0"));
        site.claim(convoySystem.entities.get("planet1"));

        SupplyConvoy.INSTANCE.generate(site);

        assertEquals("planet2", convoySystem.script().convoys().get(0).destinationId());
        assertTrue(site.isClaimed(convoySystem.entities.get("planet2")));
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

    private static ChartGenContext with(ChartEffect effect, float value) {
        return ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(effect, value)));
    }

    @Test
    void theExtraConvoyLineAddsConvoysWithTheirOwnDestinations() {
        ConvoySystem convoySystem = new ConvoySystem(6);

        SupplyConvoy.INSTANCE.generate(convoySystem.site(with(ChartEffect.CHART_EXTRA_CONVOYS, 2f)));

        List<ConvoyState> convoys = convoySystem.script().convoys();
        assertEquals(3, convoys.size());
        assertEquals(3, new HashSet<>(convoys.stream().map(ConvoyState::destinationId).toList()).size());
        assertEquals(ConvoyRules.MAX_CONVOYS, ConvoyRules.convoyCount(10f));
    }

    @Test
    void theEscortLineScalesTheEscortsAndCanRemoveThem() {
        ConvoySystem plain = new ConvoySystem(4);
        SupplyConvoy.INSTANCE.generate(plain.site(ChartGenContext.none()));
        ConvoySystem doubled = new ConvoySystem(4);
        SupplyConvoy.INSTANCE.generate(doubled.site(with(ChartEffect.CHART_CONVOY_ESCORT_PERCENT, 100f)));
        ConvoySystem stripped = new ConvoySystem(4);
        SupplyConvoy.INSTANCE.generate(stripped.site(with(ChartEffect.CHART_CONVOY_ESCORT_PERCENT, -100f)));

        assertEquals(2f * plain.engine.fleetParams.get(2).combatPts, doubled.engine.fleetParams.get(2).combatPts, 0.01f);
        assertEquals(plain.engine.fleetParams.get(0).freighterPts, doubled.engine.fleetParams.get(0).freighterPts, 0.01f);
        ConvoyState strippedConvoy = stripped.script().convoys().get(0);
        assertNotNull(strippedConvoy.fleetId(ConvoyRole.FREIGHTERS));
        assertNull(strippedConvoy.fleetId(ConvoyRole.SCREEN));
        assertNull(strippedConvoy.fleetId(ConvoyRole.ESCORT));
    }

    @Test
    void theCargoLineScalesWhatTheFreightersCarry() {
        ConvoySystem plain = new ConvoySystem(4);
        plain.engine.fleetPoints = 100;
        SupplyConvoy.INSTANCE.generate(plain.site(ChartGenContext.none()));
        ConvoySystem richer = new ConvoySystem(4);
        richer.engine.fleetPoints = 100;
        SupplyConvoy.INSTANCE.generate(richer.site(with(ChartEffect.CHART_CONVOY_CARGO_PERCENT, 50f)));

        ConvoyState plainConvoy = plain.script().convoys().get(0);
        ConvoyState richerConvoy = richer.script().convoys().get(0);
        assertEquals(100f * ConvoyRules.CARGO_STEPS_PER_FREIGHTER_POINT, plainConvoy.cargoSteps(), 0.001f);
        assertEquals(1.5f * plainConvoy.cargoSteps(), richerConvoy.cargoSteps(), 0.001f);
        assertEquals(1.5f * plainConvoy.itemChanceMult(), richerConvoy.itemChanceMult(), 0.001f);
    }

    @Test
    void aReducedEscortLineReadsAsReduced() {
        assertTrue(ChartEffect.CHART_CONVOY_ESCORT_PERCENT.description(-25f).plain().contains("25"));
        assertFalse(ChartEffect.CHART_CONVOY_ESCORT_PERCENT.description(-25f).plain().contains("-25"));
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
