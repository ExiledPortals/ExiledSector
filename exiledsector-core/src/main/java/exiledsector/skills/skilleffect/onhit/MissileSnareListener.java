package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.SkillEffectSupport;
import org.lwjgl.util.vector.Vector2f;

final class MissileSnareListener extends ShipCombatListener implements DamageDealtModifier {

    static final String TOP_SPEED_PERCENT_KEY = "exiledSector_missileSnareTopSpeedPercent";
    static final String SPEED_MOD_ID = "exiledSector_missileSnare";
    static final float SNARE_SECONDS = 2f;
    static final float MAX_SNARE_PERCENT = 90f;

    MissileSnareListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (param instanceof MissileAPI && target instanceof ShipAPI targetShip && !targetShip.isHulk() && CombatQueries.isHostile(ownerShip, targetShip)) {
            float percent = Math.min(MAX_SNARE_PERCENT, magnitude(TOP_SPEED_PERCENT_KEY));
            if (percent > 0f) {
                SkillEffectSupport.ensureListener(targetShip, Snare.class, Snare::new).snare(percent);
            }
        }
        return null;
    }

    static final class Snare implements AdvanceableListener {

        private final ShipAPI snaredShip;
        private float snarePercent;
        private float remainingSeconds;

        Snare(ShipAPI snaredShip) {
            this.snaredShip = snaredShip;
        }

        void snare(float hitPercent) {
            remainingSeconds = SNARE_SECONDS;
            if (hitPercent > snarePercent) {
                snarePercent = hitPercent;
                snaredShip.getMutableStats().getMaxSpeed().modifyMult(SPEED_MOD_ID, 1f - snarePercent / 100f);
            }
        }

        @Override
        public void advance(float amount) {
            remainingSeconds -= amount;
            if (remainingSeconds > 0f) {
                return;
            }
            snaredShip.getMutableStats().getMaxSpeed().unmodify(SPEED_MOD_ID);
            snaredShip.removeListener(this);
        }
    }
}
