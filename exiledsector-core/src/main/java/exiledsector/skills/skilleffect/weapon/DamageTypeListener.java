package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

import java.util.EnumMap;
import java.util.Map;

final class DamageTypeListener extends ShipCombatListener implements DamageDealtModifier {

    static final String KINETIC_PERCENT_KEY = "exiledSector_kineticDamagePercent";
    static final String HIGH_EXPLOSIVE_PERCENT_KEY = "exiledSector_highExplosiveDamagePercent";
    static final String ENERGY_PERCENT_KEY = "exiledSector_energyDamagePercent";
    static final String FRAGMENTATION_PERCENT_KEY = "exiledSector_fragmentationDamagePercent";
    private static final String MOD_ID_PREFIX = "exiledSector_damageType_";
    private static final Map<DamageType, String> PERCENT_KEYS = new EnumMap<>(Map.of(
            DamageType.KINETIC, KINETIC_PERCENT_KEY,
            DamageType.HIGH_EXPLOSIVE, HIGH_EXPLOSIVE_PERCENT_KEY,
            DamageType.ENERGY, ENERGY_PERCENT_KEY,
            DamageType.FRAGMENTATION, FRAGMENTATION_PERCENT_KEY));

    DamageTypeListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        String percentKey = damage.getType() == null ? null : PERCENT_KEYS.get(damage.getType());
        float percent = percentKey == null ? 0f : magnitude(percentKey);
        if (percent == 0f) {
            return null;
        }
        damage.getModifier().modifyPercent(modId, percent);
        return modId;
    }
}
