package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

final class SharedTargetDamageListener extends ShipCombatListener implements AdvanceableListener, DamageDealtModifier {

    static final String DAMAGE_PERCENT_KEY = "exiledSector_sharedTargetDamagePercent";
    static final float SCAN_INTERVAL_SECONDS = 0.5f;
    private static final String MOD_ID_PREFIX = "exiledSector_sharedTargetDamage_";

    private final Set<ShipAPI> sharedTargets = Collections.newSetFromMap(new IdentityHashMap<>());
    private float secondsSinceScan = SCAN_INTERVAL_SECONDS;

    SharedTargetDamageListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    @Override
    public void advance(float amount) {
        secondsSinceScan += amount;
        if (secondsSinceScan < SCAN_INTERVAL_SECONDS) {
            return;
        }
        secondsSinceScan = 0f;
        CombatEngineAPI engine = Global.getCombatEngine();
        scan(engine == null ? List.of() : engine.getShips());
    }

    void scan(List<ShipAPI> ships) {
        sharedTargets.clear();
        if (!ownerIsAliveNotHulk()) {
            return;
        }
        for (int i = 0; i < ships.size(); i++) {
            ShipAPI ally = ships.get(i);
            ShipAPI allyTarget = isLinkedAlly(ally) ? ally.getShipTarget() : null;
            if (allyTarget != null && CombatQueries.isHostile(ownerShip, allyTarget) && CombatQueries.isAliveNotHulk(allyTarget)) {
                sharedTargets.add(allyTarget);
            }
        }
    }

    boolean isShared(ShipAPI hitShip) {
        ShipAPI parentStation = hitShip.getParentStation();
        return sharedTargets.contains(hitShip) || (parentStation != null && sharedTargets.contains(parentStation));
    }

    private boolean isLinkedAlly(ShipAPI ship) {
        return ship != ownerShip && ship.getOwner() == ownerShip.getOwner() && !ship.isFighter() && CombatQueries.isAliveNotHulk(ship)
                && !CombatQueries.isSameStation(ownerShip, ship);
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!(target instanceof ShipAPI hitShip) || sharedTargets.isEmpty() || !isShared(hitShip)) {
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
