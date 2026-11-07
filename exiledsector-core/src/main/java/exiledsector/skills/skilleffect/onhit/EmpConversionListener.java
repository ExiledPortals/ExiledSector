package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

abstract class EmpConversionListener extends ShipCombatListener implements DamageDealtModifier, AdvanceableListener {

    private final String sharePercentKey;
    private final boolean convertsShieldHits;
    private final DeferredHits pendingHits;

    EmpConversionListener(ShipAPI ownerShip, String sharePercentKey, boolean convertsShieldHits, DamageType convertedDamageType) {
        super(ownerShip);
        this.sharePercentKey = sharePercentKey;
        this.convertsShieldHits = convertsShieldHits;
        this.pendingHits = convertsShieldHits ? DeferredHits.onShields(convertedDamageType) : DeferredHits.pastShields(convertedDamageType);
    }

    abstract float convertibleEmp(ShipAPI targetShip, Vector2f point, float emp, DamageAPI damage);

    // java:S3516: the engine reads a null return as "leave the damage unchanged"; the converted damage is dealt next frame instead
    @SuppressWarnings("java:S3516")
    @Override
    public final String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        ShipAPI targetShip = CombatQueries.hostileLiveShip(ownerShip, target);
        if (shieldHit != convertsShieldHits || point == null || targetShip == null) {
            return null;
        }
        float sharePercent = magnitude(sharePercentKey);
        float emp = damage.computeFluxDealt(damage.getDpsDuration());
        if (sharePercent <= 0f || emp <= 0f) {
            return null;
        }
        float convertedEmp = convertibleEmp(targetShip, point, emp, damage);
        if (convertedEmp > 0f) {
            pendingHits.add(targetShip, point, convertedEmp * sharePercent / 100f);
        }
        return null;
    }

    @Override
    public final void advance(float amount) {
        pendingHits.applyAll(ownerShip);
    }
}
