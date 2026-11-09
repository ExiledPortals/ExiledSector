package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.KillWatch;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

final class KillFluxVentListener extends ShipCombatListener implements DamageDealtModifier, KillWatch.KillReaction {

    static final String MAX_FLUX_PERCENT_KEY = "exiledSector_killFluxVentPercent";

    KillFluxVentListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        KillWatch.watch(ownerShip, target);
        return null;
    }

    @Override
    public void onKill(ShipAPI victim) {
        FluxTrackerAPI flux = ownerShip.getFluxTracker();
        float percent = magnitude(MAX_FLUX_PERCENT_KEY);
        if (flux != null && percent > 0f && ownerIsAliveNotHulk()) {
            flux.decreaseFlux(flux.getMaxFlux() * Math.min(percent, 100f) / 100f);
        }
    }
}
