package exiledsector.skills.skilleffect.scar;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.Chance;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.HitDamageRestorer;
import exiledsector.skills.skilleffect.HitSideEffectListener;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

final class HighExplosiveConversionListener extends HitSideEffectListener {

    static final String CHANCE_PERCENT_KEY = "exiledSector_scarHighExplosiveConversionChancePercent";

    private final Random random;

    HighExplosiveConversionListener(ShipAPI ownerShip) {
        this(ownerShip, new Random());
    }

    HighExplosiveConversionListener(ShipAPI ownerShip, Random random) {
        super(ownerShip);
        this.random = random;
    }

    static boolean converts(DamageType type) {
        return type == DamageType.KINETIC || type == DamageType.ENERGY;
    }

    @Override
    protected void onHit(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (CombatQueries.hostileShip(ownerShip, target) == null || CombatQueries.weaponBehind(param) == null || !converts(damage.getType())) {
            return;
        }
        if (Chance.roll(random, magnitude(CHANCE_PERCENT_KEY))) {
            HitDamageRestorer.retypeForThisHit(damage, DamageType.HIGH_EXPLOSIVE, target);
        }
    }
}
