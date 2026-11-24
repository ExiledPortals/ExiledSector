package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.HitMix;
import exiledsector.skills.skilleffect.HitSideEffectListener;
import exiledsector.skills.skilleffect.SkillEffectSupport;
import org.lwjgl.util.vector.Vector2f;

import java.util.EnumMap;
import java.util.Map;

final class DamageTypeListener extends HitSideEffectListener implements HitMix.Resolver {

    static final String KINETIC_PERCENT_KEY = "exiledSector_kineticDamagePercent";
    static final String HIGH_EXPLOSIVE_PERCENT_KEY = "exiledSector_highExplosiveDamagePercent";
    static final String ENERGY_PERCENT_KEY = "exiledSector_energyDamagePercent";
    static final String FRAGMENTATION_PERCENT_KEY = "exiledSector_fragmentationDamagePercent";
    static final String FRAGMENTATION_MORE_PERCENT_KEY = "exiledSector_fragmentationDamageMorePercent";
    private static final Map<DamageType, String> PERCENT_KEYS = new EnumMap<>(Map.of(
            DamageType.KINETIC, KINETIC_PERCENT_KEY,
            DamageType.HIGH_EXPLOSIVE, HIGH_EXPLOSIVE_PERCENT_KEY,
            DamageType.ENERGY, ENERGY_PERCENT_KEY,
            DamageType.FRAGMENTATION, FRAGMENTATION_PERCENT_KEY));

    DamageTypeListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    protected void onHit(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        HitMix.resolve(ownerShip, this, param, target, damage, point, shieldHit);
    }

    @Override
    public void resolve(HitMix.Hit hit) {
        for (Map.Entry<DamageType, String> percentKey : PERCENT_KEYS.entrySet()) {
            hit.increase(percentKey.getKey(), magnitude(percentKey.getValue()));
        }
        hit.more(DamageType.FRAGMENTATION, SkillEffectSupport.multFrom(magnitude(FRAGMENTATION_MORE_PERCENT_KEY)));
    }
}
