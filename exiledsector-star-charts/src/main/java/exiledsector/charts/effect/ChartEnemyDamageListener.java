package exiledsector.charts.effect;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lwjgl.util.vector.Vector2f;

final class ChartEnemyDamageListener implements DamageDealtModifier {

    static final ChartEnemyDamageListener INSTANCE = new ChartEnemyDamageListener();

    private ChartEnemyDamageListener() {
    }

    static boolean anyActive() {
        for (DamageType type : DamageType.values()) {
            ChartFleetStat stat = statFor(type);
            if (stat != null && ChartFleetConditions.mult(stat) != 1f) {
                return true;
            }
        }
        return false;
    }

    static ChartFleetStat statFor(DamageType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case KINETIC -> ChartFleetStat.ENEMY_KINETIC_DAMAGE_DEALT;
            case HIGH_EXPLOSIVE -> ChartFleetStat.ENEMY_HIGH_EXPLOSIVE_DAMAGE_DEALT;
            case ENERGY -> ChartFleetStat.ENEMY_ENERGY_DAMAGE_DEALT;
            case FRAGMENTATION -> ChartFleetStat.ENEMY_FRAGMENTATION_DAMAGE_DEALT;
            default -> null;
        };
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        ChartFleetStat stat = damage == null ? null : statFor(damage.getType());
        if (stat == null) {
            return null;
        }
        float mult = ChartFleetConditions.mult(stat);
        if (mult == 1f) {
            return null;
        }
        damage.getModifier().modifyMult(ChartFleetConditions.MOD_ID, mult);
        return ChartFleetConditions.MOD_ID;
    }
}
