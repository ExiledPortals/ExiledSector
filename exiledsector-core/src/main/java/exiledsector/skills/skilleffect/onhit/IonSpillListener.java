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

final class IonSpillListener extends ShipCombatListener implements DamageDealtModifier, AdvanceableListener {

    static final String ENERGY_SHARE_PERCENT_KEY = "exiledSector_ionSpillEnergySharePercent";

    private final DeferredHits pendingHits = new DeferredHits(DamageType.ENERGY, false);

    IonSpillListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    // java:S3516: the engine reads a null return as "leave the damage unchanged"; the energy damage is dealt next frame instead
    @SuppressWarnings("java:S3516")
    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!shieldHit || point == null || !(target instanceof ShipAPI targetShip) || !CombatQueries.isAliveNotHulk(targetShip)
                || !CombatQueries.isHostile(ownerShip, targetShip)) {
            return null;
        }
        float sharePercent = magnitude(ENERGY_SHARE_PERCENT_KEY);
        float emp = damage.computeFluxDealt(damage.getDpsDuration());
        if (sharePercent > 0f && emp > 0f) {
            pendingHits.add(targetShip, point, emp * sharePercent / 100f);
        }
        return null;
    }

    @Override
    public void advance(float amount) {
        pendingHits.applyAll(ownerShip);
    }
}
