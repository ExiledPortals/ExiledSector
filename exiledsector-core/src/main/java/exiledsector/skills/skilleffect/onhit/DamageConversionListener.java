package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.HitDamageRestorer;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;

final class DamageConversionListener extends ShipCombatListener implements DamageDealtModifier {

    static final String ADAPTIVE_CHANCE_PERCENT_KEY = "exiledSector_adaptiveDamageTypeChancePercent";

    private final List<DamageConversion> conversions = OnHitSkillEffect.damageConversions();
    private final Set<DamageAPI> convertedBeamDamage = Collections.newSetFromMap(new WeakHashMap<>());
    private final Random random;

    DamageConversionListener(ShipAPI ownerShip) {
        this(ownerShip, new Random());
    }

    DamageConversionListener(ShipAPI ownerShip, Random random) {
        super(ownerShip);
        this.random = random;
    }

    // java:S3516: the engine reads a null return as "leave the damage unchanged"; the damage type is rewritten in place instead
    @SuppressWarnings("java:S3516")
    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        WeaponAPI weapon = CombatQueries.weaponBehind(param);
        if (weapon == null) {
            return null;
        }
        boolean beam = param instanceof BeamAPI;
        DamageType sourceType = beam ? weapon.getDamageType() : damage.getType();
        DamageConversion conversion = DamageConversion.first(weapon, WeaponDroneFactory.mountOf(weapon), sourceType, conversions, this::magnitude);
        if (conversion != null && conversion.toEmp()) {
            if (target instanceof ShipAPI) {
                HitDamageRestorer.moveToEmpForThisHit(damage, 1f, target);
            }
            return null;
        }
        if (conversion == null) {
            if (beam && convertedBeamDamage.remove(damage)) {
                damage.setType(sourceType);
            }
        } else {
            damage.setType(conversion.toType());
            if (beam) {
                convertedBeamDamage.add(damage);
            }
        }
        adaptDamageType(target, damage, shieldHit);
        return null;
    }

    private void adaptDamageType(CombatEntityAPI target, DamageAPI damage, boolean shieldHit) {
        DamageType adapted = shieldHit ? DamageType.KINETIC : DamageType.HIGH_EXPLOSIVE;
        float chancePercent = magnitude(ADAPTIVE_CHANCE_PERCENT_KEY);
        if (chancePercent > 0f && target instanceof ShipAPI && damage.getType() != adapted && random.nextFloat() * 100f < chancePercent) {
            HitDamageRestorer.retypeForThisHit(damage, adapted, target);
        }
    }
}
