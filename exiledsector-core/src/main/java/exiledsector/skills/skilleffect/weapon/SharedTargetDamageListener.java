package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

final class SharedTargetDamageListener extends ShipCombatListener implements AdvanceableListener, DamageDealtModifier {

    static final String DAMAGE_PERCENT_KEY = "exiledSector_sharedTargetDamagePercent";
    private static final String MOD_ID_PREFIX = "exiledSector_sharedTargetDamage_";

    private AlliedShipTargets alliedTargets;

    SharedTargetDamageListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    @Override
    public void advance(float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        alliedTargets = engine == null || !ownerIsAliveNotHulk() ? null : AlliedShipTargets.forSide(engine, ownerShip.getOwner());
    }

    boolean isShared(ShipAPI hitShip) {
        return alliedTargets != null && alliedTargets.isTargetedByAllyOf(ownerShip, hitShip);
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!(target instanceof ShipAPI hitShip) || !isShared(hitShip)) {
            return null;
        }
        float percent = magnitude(DAMAGE_PERCENT_KEY);
        if (percent == 0f) {
            return null;
        }
        damage.getModifier().modifyPercent(modId, percent);
        return modId;
    }
}
