package exiledlib.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledlib.skills.skilleffect.HitDamageRestorer;
import exiledlib.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

final class DamageConversionListener extends ShipCombatListener implements DamageDealtModifier {

    private final List<DamageConversion> conversions = OnHitSkillEffect.damageConversions();
    private final Set<DamageAPI> convertedBeamDamage = Collections.newSetFromMap(new WeakHashMap<>());

    DamageConversionListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        WeaponAPI weapon = weaponOf(param);
        if (weapon == null) {
            return null;
        }
        boolean beam = param instanceof BeamAPI;
        DamageType sourceType = beam ? weapon.getDamageType() : damage.getType();
        DamageConversion conversion = DamageConversion.first(weapon, WeaponDroneFactory.mountOf(weapon), sourceType, conversions, this::magnitude);
        if (conversion == null) {
            if (beam && convertedBeamDamage.remove(damage)) {
                damage.setType(sourceType);
            }
        } else if (conversion.toEmp()) {
            if (target instanceof ShipAPI) {
                HitDamageRestorer.moveToEmpForThisHit(damage, 1f, target);
            }
        } else {
            damage.setType(conversion.toType());
            if (beam) {
                convertedBeamDamage.add(damage);
            }
        }
        return null;
    }

    private static WeaponAPI weaponOf(Object param) {
        if (param instanceof DamagingProjectileAPI projectile) {
            return projectile.getWeapon();
        }
        return param instanceof BeamAPI beam ? beam.getWeapon() : null;
    }
}
