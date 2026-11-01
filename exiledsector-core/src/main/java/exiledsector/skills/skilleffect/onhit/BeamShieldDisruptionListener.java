package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lwjgl.util.vector.Vector2f;

final class BeamShieldDisruptionListener implements DamageDealtModifier {

    static final String SHIELD_DAMAGE_PERCENT_KEY = "exiledSector_beamShieldDisruptionPercent";
    static final String DAMAGE_MOD_ID = "exiledSector_beamShieldDisruption";
    static final float DISRUPTION_SECONDS = 1f;
    static final TimedTargetDebuff.Spec DISRUPTION = new TimedTargetDebuff.Spec(DAMAGE_MOD_ID, DISRUPTION_SECONDS,
            MutableShipStatsAPI::getShieldDamageTakenMult, percent -> 1f + percent / 100f);

    private final ShipAPI ship;

    BeamShieldDisruptionListener(ShipAPI ship) {
        this.ship = ship;
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (shieldHit && param instanceof BeamAPI && target instanceof ShipAPI targetShip) {
            float percent = ship.getMutableStats().getDynamic().getValue(SHIELD_DAMAGE_PERCENT_KEY, 0f);
            if (percent > 0f) {
                TimedTargetDebuff.refresh(targetShip, DISRUPTION, percent);
            }
        }
        return null;
    }
}
