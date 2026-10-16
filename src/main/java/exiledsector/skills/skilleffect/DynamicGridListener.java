package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;

import java.util.Map;

final class DynamicGridListener extends ShipCombatListener implements AdvanceableListener {

    static final String ENERGY_BONUS_PERCENT_KEY = "exiledSector_dynamicGridEnergyBonusPercent";
    static final Map<HullSize, Float> SHIELDS_DOWN_SECONDS = Map.of(HullSize.FIGHTER, 0.75f, HullSize.FRIGATE, 0.75f,
            HullSize.DESTROYER, 1.1f, HullSize.CRUISER, 2f, HullSize.CAPITAL_SHIP, 3f);
    private static final String MOD_ID_PREFIX = "exiledSector_dynamicGrid_";

    private float shieldsDownSeconds;
    private boolean bonusApplied;

    DynamicGridListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    static float shieldsDownSecondsFor(HullSize hullSize) {
        return SHIELDS_DOWN_SECONDS.getOrDefault(hullSize, 3f);
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
        boolean bonusDue = gridOpen && shieldsDownSeconds >= shieldsDownSecondsFor(ownerShip.getHullSize());
        if (bonusDue != bonusApplied) {
            bonusApplied = bonusDue;
            apply(bonusDue ? magnitude(ENERGY_BONUS_PERCENT_KEY) : 0f);
        }
    }

    private void apply(float percent) {
        MutableShipStatsAPI stats = ownerShip.getMutableStats();
        if (percent <= 0f) {
            stats.getEnergyRoFMult().unmodify(modId);
            stats.getEnergyWeaponFluxCostMod().unmodify(modId);
            return;
        }
        stats.getEnergyRoFMult().modifyPercent(modId, percent);
        stats.getEnergyWeaponFluxCostMod().modifyPercent(modId, -percent);
    }
}
