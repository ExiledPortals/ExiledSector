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

class ChartThreatFleetsTest {

    private static final Map<String, Float> POINTS = Map.of(ChartThreatFleets.FABRICATOR_ROLE, 600f, ChartThreatFleets.HIVE_ROLE, 20f,
            ChartThreatFleets.OVERSEER_ROLE, 25f, ShipRoles.COMBAT_LARGE, 18f, ShipRoles.COMBAT_MEDIUM, 12f, ShipRoles.COMBAT_SMALL, 7f);

    private static List<String> filled(float combatPoints, boolean boss, long seed) {
        List<String> roles = new ArrayList<>();
        ChartThreatFleets.fill(combatPoints, boss, new Random(seed), role -> {
            roles.add(role);
            return POINTS.get(role);
        });
        return roles;
    }

    private static float escortPoints(List<String> roles) {
        float total = 0f;
        for (String role : roles) {
            if (!role.equals(ChartThreatFleets.FABRICATOR_ROLE)) {
                total += POINTS.get(role);
            }
        }
        return total;
    }

    @Test
    void escortsFillTheChartFleetPointsAndOnlyBossesBringFabricators() {
        for (long seed = 0; seed < 50; seed++) {
            List<String> guard = filled(80f, false, seed);
            List<String> boss = filled(360f, true, seed);
            List<String> bigBoss = filled(2200f, true, seed);

            assertFalse(guard.contains(ChartThreatFleets.FABRICATOR_ROLE));
            assertEquals(1, boss.stream().filter(ChartThreatFleets.FABRICATOR_ROLE::equals).count());
            assertEquals(2, bigBoss.stream().filter(ChartThreatFleets.FABRICATOR_ROLE::equals).count());
            assertTrue(escortPoints(guard) >= 80f && escortPoints(guard) < 80f + 25f, "seed " + seed);
            assertTrue(escortPoints(boss) >= 360f && escortPoints(boss) < 360f + 25f, "seed " + seed);
        }
    }

    @Test
    void aRoleWithNoShipsIsDroppedAndTheFleetStillFills() {
        Set<String> asked = new HashSet<>();
        List<String> added = new ArrayList<>();
        ChartThreatFleets.fill(100f, false, new Random(3), role -> {
            asked.add(role);
            if (role.equals(ChartThreatFleets.HIVE_ROLE)) {
                return 0f;
            }
            added.add(role);
            return POINTS.get(role);
        });

        assertFalse(added.contains(ChartThreatFleets.HIVE_ROLE));
        assertTrue(escortPoints(added) >= 100f);
        assertTrue(asked.size() > 1);
    }

    @Test
    void aFactionWithNoShipsStopsInsteadOfLooping() {
        List<String> asked = new ArrayList<>();
        ChartThreatFleets.fill(500f, true, new Random(1), role -> {
            asked.add(role);
            return 0f;
        });

        assertTrue(asked.size() <= ChartThreatFleets.MOST_SHIPS + ChartThreatFleets.fabricators(500f, true));
    }

    @Test
    void chartFleetTypesMapToTheThreatStrikeNames() {
        assertEquals(FleetTypes.PATROL_SMALL, ChartThreatFleets.threatFleetType(FleetTypes.PATROL_MEDIUM));
        assertEquals(FleetTypes.PATROL_MEDIUM, ChartThreatFleets.threatFleetType(FleetTypes.PATROL_LARGE));
        assertEquals(FleetTypes.PATROL_LARGE, ChartThreatFleets.threatFleetType(FleetTypes.TASK_FORCE));
    }

    @Test
    void threatWrecksBelongToTheFactionsLostInTheAbyssAndOthersKeepTheirOwn() {
        Random random = new Random(7);
        for (int i = 0; i < 20; i++) {
            assertTrue(ChartThreatFleets.WRECK_FACTIONS.contains(ChartThreatFleets.wreckFaction("threat", random)));
        }
        assertEquals("hegemony", ChartThreatFleets.wreckFaction("hegemony", random));
        assertTrue(ChartThreatFleets.builds("threat"));
        assertFalse(ChartThreatFleets.builds("remnant"));
    }
}
