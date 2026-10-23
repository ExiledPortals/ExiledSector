package exiledlib.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import exiledlib.skills.skilleffect.SkillEffectSupport;
import org.lwjgl.util.vector.Vector2f;

final class BeamHardFluxListener implements DamageDealtModifier {

    static final String HARD_FLUX_PERCENT_KEY = "exiledSector_beamDamageHardFluxPercent";

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (shieldHit && param instanceof BeamAPI && target instanceof ShipAPI targetShip) {
            SkillEffectSupport.ensureListener(targetShip, BeamHardFluxConverter.class, BeamHardFluxConverter::new);
        }
        return null;
    }

    static void convertToHardFlux(FluxTrackerAPI flux, float shieldFlux, float percent) {
        float converted = shieldFlux * Math.min(percent, 100f) / 100f;
        if (converted <= 0f) return;
        flux.setHardFlux(Math.min(flux.getCurrFlux(), flux.getHardFlux() + converted));
    }

    private static final class BeamHardFluxConverter implements DamageListener {

        private final ShipAPI shieldedShip;

        private BeamHardFluxConverter(ShipAPI shieldedShip) {
            this.shieldedShip = shieldedShip;
        }

        @Override
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
            float shieldFlux = result.getDamageToShields();
            if (shieldFlux <= 0f || BeamSplitListener.isApplyingSimulatedHit()
                    || !(shieldedShip.getParamAboutToApplyDamage() instanceof BeamAPI beam) || beam.getSource() == null
                    || beam.getDamage().isForceHardFlux()) return;

            convertToHardFlux(shieldedShip.getFluxTracker(), shieldFlux,
                    beam.getSource().getMutableStats().getDynamic().getValue(HARD_FLUX_PERCENT_KEY, 0f));
        }
    }
}
