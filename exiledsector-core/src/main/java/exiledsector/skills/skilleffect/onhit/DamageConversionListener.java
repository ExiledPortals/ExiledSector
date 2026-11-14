package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import exiledsector.skills.skilleffect.Chance;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.HitDamageRestorer;
import exiledsector.skills.skilleffect.HitSideEffectListener;
import org.lwjgl.util.vector.Vector2f;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;

final class DamageConversionListener extends HitSideEffectListener {

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

    @Override
    protected void onHit(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        WeaponAPI weapon = CombatQueries.weaponBehind(param);
        if (weapon == null) {
            return;
        }
        boolean beam = param instanceof BeamAPI;
        DamageType sourceType = beam ? weapon.getDamageType() : damage.getType();
        DamageConversion conversion = DamageConversion.first(weapon, WeaponDroneFactory.mountOf(weapon), sourceType, conversions, this::magnitude);
        if (conversion != null && conversion.toEmp()) {
            if (target instanceof ShipAPI) {
                HitDamageRestorer.moveToEmpForThisHit(damage, 1f, target);
            }
            return;
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
    }

    private void adaptDamageType(CombatEntityAPI target, DamageAPI damage, boolean shieldHit) {
        DamageType adapted = shieldHit ? DamageType.KINETIC : DamageType.HIGH_EXPLOSIVE;
        float chancePercent = magnitude(ADAPTIVE_CHANCE_PERCENT_KEY);
        if (target instanceof ShipAPI && damage.getType() != adapted && Chance.roll(random, chancePercent)) {
            HitDamageRestorer.retypeForThisHit(damage, adapted, target);
        }
    }
}
