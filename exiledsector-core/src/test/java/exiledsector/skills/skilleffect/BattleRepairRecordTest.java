package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BattleRepairRecordTest {

    private static ShipAPI ship(String shipId, String memberId) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getId()).thenReturn(shipId);
        when(ship.getFleetMemberId()).thenReturn(memberId);
        return ship;
    }

    @Test
    void repairsAddUpPerFleetMemberAcrossDeployments() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        Map<String, Object> customData = new HashMap<>();
        when(engine.getCustomData()).thenReturn(customData);

        BattleRepairRecord.of(engine, ship("first", "member"), "prefix_").record(30f);
        BattleRepairRecord redeployed = BattleRepairRecord.of(engine, ship("second", "member"), "prefix_");
        redeployed.record(15f);

        assertEquals(45f, redeployed.repaired(), 0.001f);
        assertEquals(55f, redeployed.remainingBelow(100f), 0.001f);
        assertEquals(45f, customData.get("prefix_member"));
        assertEquals(0f, BattleRepairRecord.of(engine, ship("other", "member"), "otherPrefix_").repaired());
    }

    @Test
    void aShipWithoutAFleetMemberIsTrackedByItsShipId() {
        assertEquals("prefix_drone", BattleRepairRecord.recordKey(ship("drone", null), "prefix_"));
        assertEquals("prefix_member", BattleRepairRecord.recordKey(ship("drone", "member"), "prefix_"));
    }
}
