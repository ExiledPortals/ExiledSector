package exiledsector.charts.content;

import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.ShipRoles;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartAbyssalFleetsTest {

    private static final Map<String, Float> POINTS = Map.of(ChartThreatFleets.FABRICATOR_ROLE, 600f, ChartThreatFleets.HIVE_ROLE, 20f,
            ChartThreatFleets.OVERSEER_ROLE, 25f, ShipRoles.COMBAT_LARGE, 18f, ShipRoles.COMBAT_MEDIUM, 12f, ShipRoles.COMBAT_SMALL, 7f,
            ShipRoles.DWELLER_TENDRIL, 4f, ShipRoles.DWELLER_MAELSTROM, 20f, ShipRoles.DWELLER_EYE, 25f, ShipRoles.DWELLER_MAW, 40f);

    private static List<String> filled(ChartAbyssalFleets.Makeup makeup, float combatPoints, boolean boss, long seed) {
        List<String> roles = new ArrayList<>();
        ChartAbyssalFleets.fill(makeup, combatPoints, boss, new Random(seed), role -> {
            roles.add(role);
            return POINTS.get(role);
        });
        return roles;
    }

    private static float escortPoints(List<String> roles, int leaders) {
        float total = 0f;
        for (String role : roles.subList(leaders, roles.size())) {
            total += POINTS.get(role);
        }
        return total;
    }

    private static long count(List<String> roles, String role) {
        return roles.stream().filter(role::equals).count();
    }

    @Test
    void escortsFillTheChartFleetPointsAndOnlyBossesBringFabricators() {
        for (long seed = 0; seed < 50; seed++) {
            List<String> guard = filled(ChartThreatFleets.MAKEUP, 80f, false, seed);
            List<String> boss = filled(ChartThreatFleets.MAKEUP, 360f, true, seed);
            List<String> bigBoss = filled(ChartThreatFleets.MAKEUP, 2200f, true, seed);

            assertEquals(0, count(guard, ChartThreatFleets.FABRICATOR_ROLE));
            assertEquals(1, count(boss, ChartThreatFleets.FABRICATOR_ROLE));
            assertEquals(2, count(bigBoss, ChartThreatFleets.FABRICATOR_ROLE));
            assertTrue(escortPoints(guard, 0) >= 80f && escortPoints(guard, 0) < 80f + 25f, "seed " + seed);
            assertTrue(escortPoints(boss, 1) >= 360f && escortPoints(boss, 1) < 360f + 25f, "seed " + seed);
        }
    }

    @Test
    void dwellerFleetsAreMostlyTendrilsAndBossesLeadWithMaws() {
        int tendrils = 0;
        int ships = 0;
        for (long seed = 0; seed < 50; seed++) {
            List<String> guard = filled(ChartDwellerFleets.MAKEUP, 120f, false, seed);
            List<String> boss = filled(ChartDwellerFleets.MAKEUP, 400f, true, seed);

            assertEquals(ChartDwellerFleets.MAKEUP.leaderRole(), boss.get(0));
            assertEquals(ChartDwellerFleets.MAKEUP.leaderRole(), boss.get(1));
            assertTrue(escortPoints(guard, 0) >= 120f && escortPoints(guard, 0) < 120f + 40f, "seed " + seed);
            assertTrue(guard.stream().allMatch(role -> ChartDwellerFleets.MAKEUP.escortRoles().stream().anyMatch(entry -> entry.getKey().equals(role))));
            tendrils += (int) count(guard, ShipRoles.DWELLER_TENDRIL);
            ships += guard.size();
        }

        assertTrue(tendrils > ships / 2, tendrils + " of " + ships);
    }

    @Test
    void aRoleWithNoShipsIsDroppedAndTheFleetStillFills() {
        Set<String> asked = new HashSet<>();
        List<String> added = new ArrayList<>();
        ChartAbyssalFleets.fill(ChartThreatFleets.MAKEUP, 100f, false, new Random(3), role -> {
            asked.add(role);
            if (role.equals(ChartThreatFleets.HIVE_ROLE)) {
                return 0f;
            }
            added.add(role);
            return POINTS.get(role);
        });

        assertFalse(added.contains(ChartThreatFleets.HIVE_ROLE));
        assertTrue(escortPoints(added, 0) >= 100f);
        assertTrue(asked.size() > 1);
    }

    @Test
    void aFactionWithNoShipsStopsInsteadOfLooping() {
        List<String> asked = new ArrayList<>();
        ChartAbyssalFleets.fill(ChartDwellerFleets.MAKEUP, 500f, true, new Random(1), role -> {
            asked.add(role);
            return 0f;
        });

        assertTrue(asked.size() <= ChartAbyssalFleets.MOST_SHIPS + ChartAbyssalFleets.leaders(500f, true));
    }

    @Test
    void chartFleetTypesMapToTheAbyssalStrikeNames() {
        assertEquals(FleetTypes.PATROL_SMALL, ChartAbyssalFleets.fleetType(FleetTypes.PATROL_MEDIUM));
        assertEquals(FleetTypes.PATROL_MEDIUM, ChartAbyssalFleets.fleetType(FleetTypes.PATROL_LARGE));
        assertEquals(FleetTypes.PATROL_LARGE, ChartAbyssalFleets.fleetType(FleetTypes.TASK_FORCE));
    }

    @Test
    void abyssalWrecksBelongToTheFactionsLostInTheAbyssAndOthersKeepTheirOwn() {
        Random random = new Random(7);
        for (int i = 0; i < 20; i++) {
            assertTrue(ChartAbyssalFleets.WRECK_FACTIONS.contains(ChartAbyssalFleets.wreckFaction("threat", random)));
            assertTrue(ChartAbyssalFleets.WRECK_FACTIONS.contains(ChartAbyssalFleets.wreckFaction("dweller", random)));
        }
        assertEquals("hegemony", ChartAbyssalFleets.wreckFaction("hegemony", random));
        assertTrue(ChartAbyssalFleets.builds("threat"));
        assertTrue(ChartAbyssalFleets.builds("dweller"));
        assertFalse(ChartAbyssalFleets.builds("remnant"));
    }
}
