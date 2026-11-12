package exiledsector.skills.skilleffect.scar;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.Chance;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.HitDamageRestorer;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

final class HighExplosiveConversionListener extends ShipCombatListener implements DamageDealtModifier {

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

    // java:S3516: the engine reads a null return as "leave the damage unchanged"; the damage type is rewritten in place instead
    @SuppressWarnings("java:S3516")
    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!(target instanceof ShipAPI targetShip) || !CombatQueries.isHostile(ownerShip, targetShip) || CombatQueries.weaponBehind(param) == null
                || !converts(damage.getType())) {
            return null;
        }
        if (Chance.roll(random, magnitude(CHANCE_PERCENT_KEY))) {
            HitDamageRestorer.retypeForThisHit(damage, DamageType.HIGH_EXPLOSIVE, target);
        }
        return null;
    }
}
