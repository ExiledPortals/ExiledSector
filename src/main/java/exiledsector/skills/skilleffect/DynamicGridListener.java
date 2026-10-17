package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.HullSizeTable;

import static exiledsector.skills.skilleffect.ScaledBonus.percent;
import static exiledsector.skills.skilleffect.StatTarget.bonus;
import static exiledsector.skills.skilleffect.StatTarget.scaled;
import static exiledsector.skills.skilleffect.StatTarget.stat;

final class DynamicGridListener extends ShipCombatListener implements AdvanceableListener {

    static final String ENERGY_BONUS_PERCENT_KEY = "exiledSector_dynamicGridEnergyBonusPercent";
    static final HullSizeTable SHIELDS_DOWN_SECONDS = new HullSizeTable(0.75f, 0.75f, 1.1f, 2f, 3f, 3f);
    private static final String MOD_ID_PREFIX = "exiledSector_dynamicGrid_";
    private static final ScaledBonus ENERGY_BONUS = new ScaledBonus(percent(stat(MutableShipStatsAPI::getEnergyRoFMult)),
            percent(scaled(bonus(MutableShipStatsAPI::getEnergyWeaponFluxCostMod), -1f)));

    private float shieldsDownSeconds;
    private boolean bonusApplied;

    DynamicGridListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    boolean bonusActive() {
        return bonusApplied;
    }

    @Override
    public void advance(float amount) {
        ShieldAPI shield = ownerShip.getShield();
        FluxTrackerAPI fluxTracker = ownerShip.getFluxTracker();
        boolean gridOpen = shield != null && !shield.isOn() && !fluxTracker.isVenting() && !fluxTracker.isOverloaded()
                && ownerIsAliveNotHulk();
        shieldsDownSeconds = gridOpen ? shieldsDownSeconds + amount : 0f;
        boolean bonusDue = gridOpen && shieldsDownSeconds >= SHIELDS_DOWN_SECONDS.of(ownerShip.getHullSize());
        if (bonusDue != bonusApplied) {
            bonusApplied = bonusDue;
            ENERGY_BONUS.apply(ownerShip.getMutableStats(), modId, bonusDue ? magnitude(ENERGY_BONUS_PERCENT_KEY) : 0f);
        }
    }
}
