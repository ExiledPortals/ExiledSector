package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.RealityDisruptorChargeGlow.RDRepairRateDebuff;
import exiledsector.skills.skilleffect.Chance;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.HitSideEffectListener;
import exiledsector.skills.skilleffect.SkillEffectSupport;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

final class HullHitRepairDisruptionListener extends HitSideEffectListener {

    static final String CHANCE_PERCENT_KEY = "exiledSector_hullHitRepairDisruptionChancePercent";
    static final float DISRUPTION_SECONDS = 5f;

    private final Random random;

    HullHitRepairDisruptionListener(ShipAPI ownerShip) {
        this(ownerShip, new Random());
    }

    HullHitRepairDisruptionListener(ShipAPI ownerShip, Random random) {
        super(ownerShip);
        this.random = random;
    }

    @Override
    protected void onHit(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        WeaponAPI weapon = CombatQueries.weaponBehind(param);
        ShipAPI hitShip = CombatQueries.hostileLiveShip(ownerShip, target);
        if (shieldHit || weapon == null || weapon.getShip() != ownerShip || hitShip == null || hitShip.isFighter() || !ownerIsAliveNotHulk()) {
            return;
        }
        if (Chance.roll(random, chancePercentFor(damage, magnitude(CHANCE_PERCENT_KEY)))) {
            disrupt(hitShip);
        }
    }

    static float chancePercentFor(DamageAPI damage, float chancePercent) {
        return damage.isDps() ? chancePercent * damage.getDpsDuration() : chancePercent;
    }

    static void disrupt(ShipAPI hitShip) {
        SkillEffectSupport.ensureListener(hitShip, RDRepairRateDebuff.class, ship -> new RDRepairRateDebuff(ship, DISRUPTION_SECONDS))
                .resetDur(DISRUPTION_SECONDS);
    }
}
