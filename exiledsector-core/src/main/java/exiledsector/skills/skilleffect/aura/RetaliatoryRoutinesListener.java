package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.NearbyDestructionListener;
import exiledsector.skills.skilleffect.ScaledBonus;
import exiledsector.skills.skilleffect.ShipDestructionWatch.DestroyedShip;
import exiledsector.skills.skilleffect.weapon.WeaponScope;
import exiledsector.skills.skilleffect.weapon.WeaponStatFamily;

import java.util.List;

import static exiledsector.skills.skilleffect.ScaledBonus.percent;

final class RetaliatoryRoutinesListener extends NearbyDestructionListener {

    static final String PERCENT_PER_STACK_KEY = "exiledSector_terribleBalancePercentPerStack";
    static final float RANGE = 1500f;
    static final int MAX_STACKS = 5;
    private static final String MOD_ID = "exiledSector_terribleBalance";
    private static final ScaledBonus BALANCE_BONUS = new ScaledBonus(
            percent(WeaponStatFamily.DAMAGE.target(WeaponScope.ALL), PERCENT_PER_STACK_KEY),
            percent(WeaponStatFamily.RATE_OF_FIRE.target(WeaponScope.ALL), PERCENT_PER_STACK_KEY));

    private final ScaledBonus.Tracker balanceBonus = BALANCE_BONUS.tracker();
    private int stacks;

    RetaliatoryRoutinesListener(ShipAPI ownerShip) {
        super(ownerShip, destroyed -> isAlliedShip(ownerShip, destroyed));
    }

    static boolean isAlliedShip(ShipAPI ownerShip, DestroyedShip destroyed) {
        ShipAPI wreck = destroyed.wreck();
        return !wreck.isFighter() && wreck.getParentStation() == null && destroyed.ownerWhileAlive() == ownerShip.getOwner();
    }

    int stacks() {
        return stacks;
    }

    @Override
    protected float activeRange() {
        return magnitude(PERCENT_PER_STACK_KEY) > 0f ? RANGE : 0f;
    }

    @Override
    protected void onDestroyed(List<ShipAPI> wrecks) {
        stacks = Math.min(MAX_STACKS, stacks + wrecks.size());
        balanceBonus.apply(ownerShip.getMutableStats(), MOD_ID, stacks);
    }
}
