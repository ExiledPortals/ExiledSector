package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

final class MissileSnareListener extends ShipCombatListener implements DamageDealtModifier {

    static final String TOP_SPEED_PERCENT_KEY = "exiledSector_missileSnareTopSpeedPercent";
    static final String SPEED_MOD_ID = "exiledSector_missileSnare";
    static final float SNARE_SECONDS = 2f;
    static final float MAX_SNARE_PERCENT = 90f;
    static final TimedTargetDebuff.Spec SNARE = new TimedTargetDebuff.Spec(SPEED_MOD_ID, SNARE_SECONDS, MutableShipStatsAPI::getMaxSpeed,
            percent -> 1f - percent / 100f);

    MissileSnareListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (param instanceof MissileAPI && target instanceof ShipAPI targetShip && !targetShip.isHulk() && CombatQueries.isHostile(ownerShip, targetShip)) {
            float percent = Math.min(MAX_SNARE_PERCENT, magnitude(TOP_SPEED_PERCENT_KEY));
            if (percent > 0f) {
                TimedTargetDebuff.refresh(targetShip, SNARE, percent);
            }
        }
        return null;
    }
}
