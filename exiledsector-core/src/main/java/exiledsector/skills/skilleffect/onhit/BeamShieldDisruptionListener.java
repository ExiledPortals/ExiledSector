package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.SkillEffectSupport;
import org.lwjgl.util.vector.Vector2f;

final class BeamShieldDisruptionListener implements DamageDealtModifier {

    static final String SHIELD_DAMAGE_PERCENT_KEY = "exiledSector_beamShieldDisruptionPercent";
    static final String DAMAGE_MOD_ID = "exiledSector_beamShieldDisruption";
    static final float DISRUPTION_SECONDS = 1f;

    private final ShipAPI ship;

    BeamShieldDisruptionListener(ShipAPI ship) {
        this.ship = ship;
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (shieldHit && param instanceof BeamAPI && target instanceof ShipAPI targetShip) {
            float percent = ship.getMutableStats().getDynamic().getValue(SHIELD_DAMAGE_PERCENT_KEY, 0f);
            if (percent > 0f) {
                SkillEffectSupport.ensureListener(targetShip, ShieldDisruption.class, ShieldDisruption::new).disrupt(percent);
            }
        }
        return null;
    }

    static final class ShieldDisruption implements AdvanceableListener {

        private final ShipAPI disruptedShip;
        private float disruptionPercent;
        private float remainingSeconds;

        ShieldDisruption(ShipAPI disruptedShip) {
            this.disruptedShip = disruptedShip;
        }

        void disrupt(float hitPercent) {
            remainingSeconds = DISRUPTION_SECONDS;
            if (hitPercent > disruptionPercent) {
                disruptionPercent = hitPercent;
                disruptedShip.getMutableStats().getShieldDamageTakenMult().modifyMult(DAMAGE_MOD_ID, 1f + disruptionPercent / 100f);
            }
        }

        @Override
        public void advance(float amount) {
            remainingSeconds -= amount;
            if (remainingSeconds > 0f) {
                return;
            }
            disruptedShip.getMutableStats().getShieldDamageTakenMult().unmodify(DAMAGE_MOD_ID);
            disruptedShip.removeListener(this);
        }
    }
}
