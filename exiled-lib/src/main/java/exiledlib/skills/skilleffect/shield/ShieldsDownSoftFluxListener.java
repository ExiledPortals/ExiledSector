package exiledlib.skills.skilleffect.shield;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import exiledlib.skills.skilleffect.CombatQueries;
import exiledlib.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

final class ShieldsDownSoftFluxListener extends ShipCombatListener implements DamageTakenModifier {

    static final String CONVERTED_PERCENT_KEY = "exiledLib_shieldsDownSoftFluxPercent";
    static final float MAX_CONVERTED_PERCENT = 90f;
    private static final String CONVERT_MOD_ID = "exiledLib_shieldsDownSoftFlux";

    ShieldsDownSoftFluxListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        ShieldAPI shield = ownerShip.getShield();
        if (shieldHit || target != ownerShip || shield == null || !shield.isOff()) return null;

        float share = Math.min(magnitude(CONVERTED_PERCENT_KEY), MAX_CONVERTED_PERCENT) / 100f;
        float hitDamage = CombatQueries.hitDamage(damage);
        if (share <= 0f || hitDamage <= 0f) return null;

        FluxTrackerAPI flux = ownerShip.getFluxTracker();
        float converted = Math.min(hitDamage * share, flux.getMaxFlux() - flux.getCurrFlux());
        if (converted <= 0f) return null;

        damage.getModifier().modifyMult(CONVERT_MOD_ID, 1f - converted / hitDamage);
        flux.increaseFlux(converted, false);
        return CONVERT_MOD_ID;
    }
}
