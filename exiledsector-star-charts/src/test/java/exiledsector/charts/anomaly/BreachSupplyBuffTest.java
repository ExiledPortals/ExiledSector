package exiledsector.charts.anomaly;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BreachSupplyBuffTest {

    @Test
    void raisesTheShipsSupplyUseUntilItRunsOut() {
        MutableStat supplies = new MutableStat(10f);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getSuppliesPerMonth()).thenReturn(supplies);
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getStats()).thenReturn(stats);
        BreachSupplyBuff buff = new BreachSupplyBuff("breach_supplies", 50f, "Spatial Breach", 0.25f);

        buff.apply(member);

        assertEquals(15f, supplies.getModifiedValue(), 0.001f);
        assertFalse(buff.isExpired());
        buff.advance(0.3f);
        assertTrue(buff.isExpired());
        assertEquals("breach_supplies", buff.getId());
    }
}
