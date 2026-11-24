package exiledsector.charts.effect;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.HitMix;
import exiledsector.skills.skilleffect.HitSideEffectListener;
import org.lwjgl.util.vector.Vector2f;

import java.util.EnumMap;
import java.util.Map;

final class ChartEnemyDamageListener extends HitSideEffectListener implements HitMix.Resolver {

    private final Map<DamageType, Float> multsByType;

    private ChartEnemyDamageListener(Map<DamageType, Float> multsByType, ShipAPI ownerShip) {
        super(ownerShip);
        this.multsByType = multsByType;
    }

    ChartEnemyDamageListener forShip(ShipAPI ship) {
        return new ChartEnemyDamageListener(multsByType, ship);
    }

    static ChartEnemyDamageListener of(Map<ChartFleetStat, Float> enemyStatMults) {
        Map<DamageType, Float> multsByType = new EnumMap<>(DamageType.class);
        for (DamageType type : DamageType.values()) {
            ChartFleetStat stat = statFor(type);
            Float mult = stat == null ? null : enemyStatMults.get(stat);
            if (mult != null && mult != 1f) {
                multsByType.put(type, mult);
            }
        }
        return multsByType.isEmpty() ? null : new ChartEnemyDamageListener(multsByType, null);
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
    protected void onHit(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        HitMix.resolve(ownerShip, this, param, target, damage, point, shieldHit);
    }

    @Override
    public void resolve(HitMix.Hit hit) {
        for (Map.Entry<DamageType, Float> mult : multsByType.entrySet()) {
            hit.more(mult.getKey(), mult.getValue());
        }
    }
}
