package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatQueries;

import java.util.IdentityHashMap;
import java.util.Map;

final class IsolatedShipDamageListener extends TargetDamageBonusListener {

    static final String DAMAGE_PERCENT_KEY = "exiledSector_damageToIsolatedShipsPercent";
    static final float ALLY_RANGE = 1000f;
    static final float RECHECK_SECONDS = 0.25f;
    private static final String MOD_ID_PREFIX = "exiledSector_isolatedShipDamage_";

    private final Map<ShipAPI, IsolationCheck> checksByTarget = new IdentityHashMap<>();

    IsolatedShipDamageListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX, DAMAGE_PERCENT_KEY);
    }

    static boolean hasAllyNearby(ShipAPI target) {
        return CombatQueries.anyNear(Global.getCombatEngine().getShipGrid(), target.getLocation(), ALLY_RANGE,
                object -> object instanceof ShipAPI other && isAllyOf(target, other)
                        && CombatQueries.withinRadius(other.getLocation(), target.getLocation(), ALLY_RANGE));
    }

    private static boolean isAllyOf(ShipAPI target, ShipAPI other) {
        return other != target && other.getOwner() == target.getOwner() && !other.isFighter()
                && CombatQueries.isAliveNotHulk(other) && !CombatQueries.isSameStation(target, other);
    }

    @Override
    boolean qualifies(ShipAPI target) {
        return !target.isFighter() && isIsolated(target);
    }

    private boolean isIsolated(ShipAPI target) {
        CombatEngineAPI engine = Global.getCombatEngine();
        float now = engine.getTotalElapsedTime(false);
        IsolationCheck check = checksByTarget.get(target);
        if (check == null || now - check.checkedAtSeconds() >= RECHECK_SECONDS) {
            check = new IsolationCheck(now, !hasAllyNearby(target));
            checksByTarget.put(target, check);
        }
        return check.isolated();
    }

    private record IsolationCheck(float checkedAtSeconds, boolean isolated) {
    }
}
