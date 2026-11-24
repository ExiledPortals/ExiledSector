package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import exiledsector.skills.skilleffect.ArcWeapons;
import exiledsector.skills.skilleffect.Chance;
import exiledsector.skills.skilleffect.HitMix;
import exiledsector.skills.skilleffect.HitSideEffectListener;
import exiledsector.skills.skilleffect.TypeShares;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Random;

final class DamageConversionListener extends HitSideEffectListener implements HitMix.Resolver {

    static final String ADAPTIVE_CHANCE_PERCENT_KEY = "exiledSector_adaptiveDamageTypeChancePercent";

    private final List<DamageConversion> conversions = OnHitSkillEffect.damageConversions();
    private final Random random;
    private final ArcWeapons arcWeapons;

    DamageConversionListener(ShipAPI ownerShip) {
        this(ownerShip, new Random());
    }

    DamageConversionListener(ShipAPI ownerShip, Random random) {
        super(ownerShip);
        this.random = random;
        this.arcWeapons = new ArcWeapons(ownerShip);
    }

    @Override
    protected void onHit(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        HitMix.resolve(ownerShip, this, param, target, damage, point, shieldHit);
    }

    @Override
    public void resolve(HitMix.Hit hit) {
        WeaponAPI weapon = arcWeapons.weaponBehind(hit.param(), hit.target());
        if (weapon == null) {
            return;
        }
        DamageType sourceType = hit.param() instanceof BeamAPI ? weapon.getDamageType() : hit.originalType();
        DamageConversion conversion = DamageConversion.first(weapon, WeaponDroneFactory.mountOf(weapon), sourceType, conversions, this::magnitude);
        if (conversion != null && conversion.toEmp()) {
            if (hit.target() instanceof ShipAPI) {
                hit.convertToEmp(conversion.fromType(), conversion.share());
            }
        } else if (conversion != null) {
            hit.convert(conversion.fromType(), conversion.toType(), conversion.share());
        }
        adaptDamageType(hit);
    }

    private void adaptDamageType(HitMix.Hit hit) {
        DamageType adapted = hit.shieldHit() ? DamageType.KINETIC : DamageType.HIGH_EXPLOSIVE;
        float chancePercent = magnitude(ADAPTIVE_CHANCE_PERCENT_KEY);
        if (hit.target() instanceof ShipAPI && !hit.originalShares().equals(TypeShares.single(adapted)) && Chance.roll(random, chancePercent)) {
            hit.convert(null, adapted, 1f);
        }
    }
}
