package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import java.util.Map;

public final class BattleRepairRecord {

    private final Map<String, Object> engineCustomData;
    private final String recordKey;

    private BattleRepairRecord(Map<String, Object> engineCustomData, String recordKey) {
        this.engineCustomData = engineCustomData;
        this.recordKey = recordKey;
    }

    public static BattleRepairRecord of(CombatEngineAPI engine, ShipAPI ship, String recordKeyPrefix) {
        return new BattleRepairRecord(engine.getCustomData(), recordKey(ship, recordKeyPrefix));
    }

    static String recordKey(ShipAPI ship, String recordKeyPrefix) {
        String memberId = ship.getFleetMemberId();
        return recordKeyPrefix + (memberId != null ? memberId : ship.getId());
    }

    public float repaired() {
        return engineCustomData.get(recordKey) instanceof Float earlier ? earlier : 0f;
    }

    public float remainingBelow(float battleCap) {
        return battleCap - repaired();
    }

    public void record(float amount) {
        engineCustomData.put(recordKey, repaired() + amount);
    }
}
