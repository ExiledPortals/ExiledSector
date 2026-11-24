package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.EmpOnlyHits;
import exiledsector.skills.skilleffect.HitMix;
import exiledsector.skills.skilleffect.HitSideEffectListener;
import org.lwjgl.util.vector.Vector2f;

abstract class EmpConversionListener extends HitSideEffectListener implements HitMix.Resolver {

    private final String sharePercentKey;
    private final boolean convertsShieldHits;
    private final DamageType convertedDamageType;

    EmpConversionListener(ShipAPI ownerShip, String sharePercentKey, boolean convertsShieldHits, DamageType convertedDamageType) {
        super(ownerShip);
        this.sharePercentKey = sharePercentKey;
        this.convertsShieldHits = convertsShieldHits;
        this.convertedDamageType = convertedDamageType;
        EmpOnlyHits.register(ownerShip);
    }

    abstract float convertibleEmp(ShipAPI targetShip, Vector2f point, float emp, MutableShipStatsAPI attackerStats);

    @Override
    protected final void onHit(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (shieldHit == convertsShieldHits) {
            HitMix.resolve(ownerShip, this, param, target, damage, point, shieldHit);
        }
    }

    @Override
    public final void resolve(HitMix.Hit hit) {
        if (hit.shieldHit() == convertsShieldHits) {
            hit.convertEmp(convertedDamageType, convertibleShare(hit.target(), hit.point(), hit.originalEmp()));
        }
    }

    private float convertibleShare(CombatEntityAPI target, Vector2f point, float emp) {
        ShipAPI targetShip = CombatQueries.hostileLiveShip(ownerShip, target);
        if (point == null || targetShip == null || emp <= 0f) {
            return 0f;
        }
        float sharePercent = magnitude(sharePercentKey);
        if (sharePercent <= 0f) {
            return 0f;
        }
        return Math.min(1f, convertibleEmp(targetShip, point, emp, ownerShip.getMutableStats()) / emp * sharePercent / 100f);
    }
}
