package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.HitMix;
import exiledsector.skills.skilleffect.HitSideEffectListener;
import org.lwjgl.util.vector.Vector2f;

final class BeamEmpListener extends HitSideEffectListener implements HitMix.Resolver {

    static final String EMP_PERCENT_KEY = "exiledSector_beamDamageEmpPercent";

    BeamEmpListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    protected void onHit(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        HitMix.resolve(ownerShip, this, param, target, damage, point, shieldHit);
    }

    @Override
    public void resolve(HitMix.Hit hit) {
        if (!hit.shieldHit() && hit.param() instanceof BeamAPI && CombatQueries.hostileShip(ownerShip, hit.target()) != null) {
            hit.gainEmp(magnitude(EMP_PERCENT_KEY) / 100f);
        }
    }
}
