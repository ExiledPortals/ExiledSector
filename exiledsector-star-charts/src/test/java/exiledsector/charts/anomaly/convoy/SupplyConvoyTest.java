package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.loot.ChartLoot;
import org.json.JSONArray;
import org.json.JSONObject;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

class SupplyConvoyTest {

    @AfterEach
    void tearDown() {
        ChartContentGenerator.resetForTests();
        ChartLoot.clear();
    }

    private static JSONObject lootRow(String id, String scope, String mode, String amount) throws Exception {
        return new JSONObject().put("id", id).put("drop_type", "weapon").put("scope", scope).put("group", "weapons1").put("mode", mode)
                .put("amount", amount).put("min_tier", scope.isEmpty() ? "" : "1");
    }

    @Test
    void theConvoyCarriesTheDropsOfAnOrdinaryChartFleet() throws Exception {
        ChartLoot.register(new JSONArray().put(lootRow("weapons1", "", "", "")).put(lootRow("fleet_weapons", "fleet", "random", "1")), group -> true);
        ConvoySystem convoySystem = new ConvoySystem(4);
        convoySystem.engine.fleetPoints = 100;

        SupplyConvoy.INSTANCE.generate(convoySystem.site(ChartGenContext.none()));

        verify(convoySystem.fleet(convoySystem.script().convoys().get(0))).addDropRandom(any());
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
    void theConvoyGathersAtTheSystemEdgeAsOneFleetOfFreightersAndWarships() {
        ConvoySystem convoySystem = new ConvoySystem(5);

        SupplyConvoy.INSTANCE.generate(convoySystem.site(ChartGenContext.none()));

        ConvoyScript script = convoySystem.script();
        ConvoyState convoy = script.convoys().get(0);
        CampaignFleetAPI fleet = convoySystem.fleet(convoy);
        assertNotNull(fleet);
        assertEquals(1, convoySystem.engine.fleetParams.size());
        assertTrue(convoySystem.engine.fleetParams.get(0).combatPts > ConvoyRules.FREIGHTER_GUARD_POINTS);
        assertTrue(convoySystem.engine.fleetParams.get(0).freighterPts > 0f);
        assertEquals(List.of("pirates"), convoySystem.engine.dressedFactions);
        verify(fleet).setFaction("convoy", true);
        assertTrue(fleet.getLocation().length() > 3000f + 1500f * 4, "the convoy starts beyond the outermost planet");
        assertEquals(FleetAssignment.ORBIT_PASSIVE, convoySystem.assignments(fleet).get(0).assignment());
        assertSame(convoy.entryToken(), convoySystem.assignments(fleet).get(0).target());
        assertTrue(fleet.hasTag(ChartIds.runTag(ConvoySystem.RUN_ID)));
    }

    @Test
    void theConvoyIsNonHostileAndCarriesNoReputationImpact() {
        ConvoySystem convoySystem = new ConvoySystem(4);

        SupplyConvoy.INSTANCE.generate(convoySystem.site(ChartGenContext.none()));

        MemoryAPI memory = convoySystem.fleet(convoySystem.script().convoys().get(0)).getMemoryWithoutUpdate();
        assertTrue(memory.getBoolean(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE));
        assertFalse(memory.getBoolean(MemFlags.MEMORY_KEY_MAKE_HOSTILE));
        assertFalse(memory.getBoolean(MemFlags.MEMORY_KEY_IGNORE_PLAYER_COMMS));
        assertFalse(memory.getBoolean(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE));
        assertTrue(memory.getBoolean(MemFlags.MEMORY_KEY_NO_REP_IMPACT));
        assertFalse(memory.getBoolean(MemFlags.MEMORY_KEY_TRADE_FLEET));
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

    private static ChartGenContext with(ChartModifier effect, float value) {
        return ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(effect, value)));
    }

    @Test
    void theExtraConvoyLineAddsConvoysWithTheirOwnDestinations() {
        ConvoySystem convoySystem = new ConvoySystem(6);

        SupplyConvoy.INSTANCE.generate(convoySystem.site(with(ChartModifier.CHART_EXTRA_CONVOYS, 2f)));

        List<ConvoyState> convoys = convoySystem.script().convoys();
        assertEquals(3, convoys.size());
        assertEquals(3, new HashSet<>(convoys.stream().map(ConvoyState::destinationId).toList()).size());
        assertEquals(ConvoyRules.MAX_CONVOYS, ConvoyRules.convoyCount(10f));
    }

    @Test
    void theEscortLineScalesTheWarshipsAndCanStripThemToTheGuard() {
        ConvoySystem plain = new ConvoySystem(4);
        SupplyConvoy.INSTANCE.generate(plain.site(ChartGenContext.none()));
        ConvoySystem halved = new ConvoySystem(4);
        SupplyConvoy.INSTANCE.generate(halved.site(with(ChartModifier.CHART_CONVOY_ESCORT_PERCENT, -50f)));
        ConvoySystem stripped = new ConvoySystem(4);
        SupplyConvoy.INSTANCE.generate(stripped.site(with(ChartModifier.CHART_CONVOY_ESCORT_PERCENT, -100f)));

        assertEquals(0.5f * plain.engine.fleetParams.get(0).combatPts, halved.engine.fleetParams.get(0).combatPts, 0.01f);
        assertEquals(plain.engine.fleetParams.get(0).freighterPts, halved.engine.fleetParams.get(0).freighterPts, 0.01f);
        assertEquals(ConvoyRules.FREIGHTER_GUARD_POINTS, stripped.engine.fleetParams.get(0).combatPts, 0.01f);
        assertNotNull(stripped.script().convoys().get(0).fleetId());
    }

    @Test
    void theCargoLineScalesWhatTheFreightersCarry() {
        ConvoySystem plain = new ConvoySystem(4);
        plain.engine.fleetPoints = 100;
        SupplyConvoy.INSTANCE.generate(plain.site(ChartGenContext.none()));
        ConvoySystem richer = new ConvoySystem(4);
        richer.engine.fleetPoints = 100;
        SupplyConvoy.INSTANCE.generate(richer.site(with(ChartModifier.CHART_CONVOY_CARGO_PERCENT, 50f)));

        ConvoyState plainConvoy = plain.script().convoys().get(0);
        ConvoyState richerConvoy = richer.script().convoys().get(0);
        assertEquals(100f * ConvoyRules.CARGO_STEPS_PER_FREIGHTER_POINT, plainConvoy.cargoSteps(), 0.001f);
        assertEquals(1.5f * plainConvoy.cargoSteps(), richerConvoy.cargoSteps(), 0.001f);
        assertEquals(1.5f * plainConvoy.itemChanceMult(), richerConvoy.itemChanceMult(), 0.001f);
    }

    @Test
    void aReducedEscortLineReadsAsReduced() {
        assertTrue(ChartModifier.CHART_CONVOY_ESCORT_PERCENT.description(-25f).plain().contains("25"));
        assertFalse(ChartModifier.CHART_CONVOY_ESCORT_PERCENT.description(-25f).plain().contains("-25"));
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
