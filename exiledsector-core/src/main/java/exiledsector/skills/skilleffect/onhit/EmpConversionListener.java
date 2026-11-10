package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.EmpOnlyHits;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

abstract class EmpConversionListener extends ShipCombatListener implements DamageDealtModifier, AdvanceableListener, EmpOnlyHits.Handler {

    private final String sharePercentKey;
    private final boolean convertsShieldHits;
    private final DeferredHits pendingHits;

    EmpConversionListener(ShipAPI ownerShip, String sharePercentKey, boolean convertsShieldHits, DamageType convertedDamageType) {
        super(ownerShip);
        this.sharePercentKey = sharePercentKey;
        this.convertsShieldHits = convertsShieldHits;
        this.pendingHits = convertsShieldHits ? DeferredHits.onShields(convertedDamageType) : DeferredHits.pastShields(convertedDamageType);
        EmpOnlyHits.register(ownerShip, this);
    }

    abstract float convertibleEmp(ShipAPI targetShip, Vector2f point, float emp, MutableShipStatsAPI attackerStats);

    // java:S3516: the engine reads a null return as "leave the damage unchanged"; the converted damage is dealt next frame instead
    @SuppressWarnings("java:S3516")
    @Override
    public final String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (shieldHit == convertsShieldHits) {
            convertLater(target, point, damage.computeFluxDealt(damage.getDpsDuration()), damage.getStats());
        }
        return null;
    }

    @Override
    public final void onEmpOnlyHit(EmpOnlyHits.EmpOnlyHit hit) {
        if (hit.shieldHit() == convertsShieldHits) {
            convertLater(hit.target(), hit.point(), hit.emp(), ownerShip.getMutableStats());
        }
    }

    private void convertLater(CombatEntityAPI target, Vector2f point, float emp, MutableShipStatsAPI attackerStats) {
        ShipAPI targetShip = CombatQueries.hostileLiveShip(ownerShip, target);
        if (point == null || targetShip == null || emp <= 0f) {
            return;
        }
        float sharePercent = magnitude(sharePercentKey);
        if (sharePercent <= 0f) {
            return;
        }
        float convertedEmp = convertibleEmp(targetShip, point, emp, attackerStats);
        if (convertedEmp > 0f) {
            pendingHits.add(targetShip, point, convertedEmp * sharePercent / 100f);
        }
    }

    @Override
    public final void advance(float amount) {
        pendingHits.applyAll(ownerShip);
    }
}
