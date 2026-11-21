package exiledsector.charts.anomaly.debris;

import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SleeperShipsTest {

    private static final SleeperShips.Shares ALL_THREE = new SleeperShips.Shares(30f, 30f, 30f);

    @Test
    void theThreeSharesSplitOneRollSoTheyNeverClaimTheSameShip() {
        assertEquals(Factions.REMNANTS, ALL_THREE.factionFor(0f));
        assertEquals(Factions.REMNANTS, ALL_THREE.factionFor(0.29f));
        assertEquals(Factions.THREAT, ALL_THREE.factionFor(0.31f));
        assertEquals(Factions.THREAT, ALL_THREE.factionFor(0.59f));
        assertEquals(Factions.DWELLER, ALL_THREE.factionFor(0.61f));
        assertEquals(Factions.DWELLER, ALL_THREE.factionFor(0.89f));
        assertNull(ALL_THREE.factionFor(0.91f));
    }

    @Test
    void withAllThreeNinetyPercentOfShipsAreReplacedInAMix() {
        Random random = new Random(11L);
        Map<String, Integer> picks = new HashMap<>();
        int draws = 10000;
        for (int draw = 0; draw < draws; draw++) {
            picks.merge(String.valueOf(ALL_THREE.factionFor(random.nextFloat())), 1, Integer::sum);
        }
        for (String factionId : List.of(Factions.REMNANTS, Factions.THREAT, Factions.DWELLER)) {
            assertTrue(Math.abs(picks.get(factionId) - 3000) < 250, factionId + " " + picks.get(factionId));
        }
        assertTrue(Math.abs(picks.get("null") - 1000) < 200, String.valueOf(picks.get("null")));
    }

    @Test
    void oneShareAloneReplacesThirtyPercentAndSharesNeverExceedTheWholeFleet() {
        SleeperShips.Shares threatOnly = new SleeperShips.Shares(0f, 30f, 0f);
        assertEquals(Factions.THREAT, threatOnly.factionFor(0.1f));
        assertNull(threatOnly.factionFor(0.5f));
        assertFalse(new SleeperShips.Shares(0f, 0f, 0f).any());
        assertTrue(threatOnly.any());

        SleeperShips.Shares overfull = new SleeperShips.Shares(80f, 80f, 80f);
        assertEquals(Factions.REMNANTS, overfull.factionFor(0.5f));
        assertEquals(Factions.THREAT, overfull.factionFor(0.9f));
        assertEquals(Factions.THREAT, overfull.factionFor(0.9999f));
    }

    @Test
    void anEquivalentHullKeepsTheHullSizeAndIsClosestInDeploymentPoints() {
        List<SleeperShips.Candidate> candidates = List.of(new SleeperShips.Candidate("frigate_a", HullSize.FRIGATE, 5f),
                new SleeperShips.Candidate("cruiser_a", HullSize.CRUISER, 14f), new SleeperShips.Candidate("cruiser_b", HullSize.CRUISER, 22f),
                new SleeperShips.Candidate("capital_a", HullSize.CAPITAL_SHIP, 40f));

        assertEquals("cruiser_b", SleeperShips.equivalent(candidates, HullSize.CRUISER, 20f, new Random(1L)).variantId());
        assertEquals("cruiser_a", SleeperShips.equivalent(candidates, HullSize.CRUISER, 15f, new Random(1L)).variantId());
        assertEquals("frigate_a", SleeperShips.equivalent(candidates, HullSize.FRIGATE, 12f, new Random(1L)).variantId());
    }

    @Test
    void withoutThatHullSizeTheNearestSizeIsTaken() {
        List<SleeperShips.Candidate> candidates = List.of(new SleeperShips.Candidate("frigate_a", HullSize.FRIGATE, 5f),
                new SleeperShips.Candidate("capital_a", HullSize.CAPITAL_SHIP, 40f));

        assertEquals("frigate_a", SleeperShips.equivalent(candidates, HullSize.DESTROYER, 10f, new Random(1L)).variantId());
        assertEquals("capital_a", SleeperShips.equivalent(candidates, HullSize.CRUISER, 10f, new Random(1L)).variantId());
        assertNull(SleeperShips.equivalent(List.of(), HullSize.CRUISER, 10f, new Random(1L)));
    }

    @Test
    void tiedHullsAreChosenAtRandom() {
        List<SleeperShips.Candidate> candidates = List.of(new SleeperShips.Candidate("first", HullSize.DESTROYER, 10f),
                new SleeperShips.Candidate("second", HullSize.DESTROYER, 10f));
        Random random = new Random(3L);
        List<String> picked = new ArrayList<>();
        for (int draw = 0; draw < 40; draw++) {
            picked.add(SleeperShips.equivalent(candidates, HullSize.DESTROYER, 10f, random).variantId());
        }
        assertTrue(picked.contains("first") && picked.contains("second"));
    }

    private static ShipHullSpecAPI hull(HullSize hullSize, float deploymentPoints) {
        ShipHullSpecAPI hull = mock(ShipHullSpecAPI.class);
        when(hull.getHullSize()).thenReturn(hullSize);
        when(hull.getSuppliesToRecover()).thenReturn(deploymentPoints);
        return hull;
    }

    private static FleetMemberAPI member(String id, ShipHullSpecAPI hull) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn(id);
        when(member.getHullSpec()).thenReturn(hull);
        return member;
    }

    private static CampaignFleetAPI fleetOf(List<FleetMemberAPI> members) {
        List<FleetMemberAPI> held = new ArrayList<>(members);
        FleetDataAPI data = mock(FleetDataAPI.class);
        when(data.getMembersListCopy()).thenAnswer(invocation -> new ArrayList<>(held));
        when(data.addFleetMember(anyString())).thenAnswer(invocation -> {
            FleetMemberAPI added = member("new_" + invocation.getArgument(0) + "_" + held.size(), hull(HullSize.CRUISER, 20f));
            held.add(added);
            return added;
        });
        doAnswer(invocation -> held.remove((FleetMemberAPI) invocation.getArgument(0))).when(data).removeFleetMember(any());
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        when(fleet.getFleetData()).thenReturn(data);
        return fleet;
    }

    @Test
    void replacedShipsAreSwappedForTheFactionsHullAndRememberedWithIt() {
        List<FleetMemberAPI> members = new ArrayList<>();
        for (int index = 0; index < 50; index++) {
            members.add(member("old_" + index, hull(HullSize.CRUISER, 18f)));
        }
        CampaignFleetAPI fleet = fleetOf(members);
        Map<String, List<SleeperShips.Candidate>> hulls = Map.of(
                Factions.REMNANTS, List.of(new SleeperShips.Candidate("remnant_cruiser", HullSize.CRUISER, 20f)),
                Factions.THREAT, List.of(new SleeperShips.Candidate("threat_cruiser", HullSize.CRUISER, 20f)),
                Factions.DWELLER, List.of(new SleeperShips.Candidate("dweller_cruiser", HullSize.CRUISER, 20f)));

        Map<FleetMemberAPI, String> replaced = SleeperShips.replace(fleet, ALL_THREE, new Random(7L), hulls::get);

        assertEquals(50, fleet.getFleetData().getMembersListCopy().size());
        assertTrue(replaced.size() >= 35 && replaced.size() <= 50, String.valueOf(replaced.size()));
        for (Map.Entry<FleetMemberAPI, String> entry : replaced.entrySet()) {
            String expectedVariant = Map.of(Factions.REMNANTS, "remnant_cruiser", Factions.THREAT, "threat_cruiser", Factions.DWELLER, "dweller_cruiser")
                    .get(entry.getValue());
            assertTrue(entry.getKey().getId().startsWith("new_" + expectedVariant), entry.getKey().getId());
            assertTrue(fleet.getFleetData().getMembersListCopy().contains(entry.getKey()));
        }
        long kept = fleet.getFleetData().getMembersListCopy().stream().filter(member -> member.getId().startsWith("old_")).count();
        assertEquals(50 - replaced.size(), kept);
    }

    @Test
    void aFactionWithoutHullsAndNoSharesLeaveTheFleetAlone() {
        CampaignFleetAPI fleet = fleetOf(List.of(member("old", hull(HullSize.FRIGATE, 5f))));

        assertTrue(SleeperShips.replace(fleet, new SleeperShips.Shares(100f, 0f, 0f), new Random(1L), factionId -> List.of()).isEmpty());
        assertTrue(SleeperShips.replace(fleet, new SleeperShips.Shares(0f, 0f, 0f), new Random(1L), factionId -> List.of()).isEmpty());
        assertEquals("old", fleet.getFleetData().getMembersListCopy().get(0).getId());
    }

    @Test
    void candidatesComeFromTheFactionsCombatRolesWithTheirDeploymentPoints() {
        FactionAPI faction = mock(FactionAPI.class);
        when(faction.getVariantsForRole(anyString())).thenReturn(Set.of());
        when(faction.getVariantsForRole(DormantFleets.ROLES.get(0).getKey())).thenReturn(Set.of("remnant_frigate", "missing_variant"));
        when(faction.getVariantsForRole(DormantFleets.ROLES.get(DormantFleets.ROLES.size() - 1).getKey())).thenReturn(Set.of("remnant_capital", "remnant_station"));
        SettingsAPI settings = mock(SettingsAPI.class);
        variant(settings, "remnant_frigate", hull(HullSize.FRIGATE, 4f));
        variant(settings, "remnant_capital", hull(HullSize.CAPITAL_SHIP, 45f));
        variant(settings, "remnant_station", hull(HullSize.FIGHTER, 0f));

        List<SleeperShips.Candidate> candidates = SleeperShips.candidatesOf(faction, settings);

        assertEquals(2, candidates.size());
        assertTrue(candidates.contains(new SleeperShips.Candidate("remnant_frigate", HullSize.FRIGATE, 4f)));
        assertTrue(candidates.contains(new SleeperShips.Candidate("remnant_capital", HullSize.CAPITAL_SHIP, 45f)));
    }

    private static void variant(SettingsAPI settings, String variantId, ShipHullSpecAPI hull) {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getHullSpec()).thenReturn(hull);
        when(settings.doesVariantExist(variantId)).thenReturn(true);
        when(settings.getVariant(variantId)).thenReturn(variant);
    }
}
