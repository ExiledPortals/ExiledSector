package exiledlib.skills.skilleffect.shield;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledlib.skills.HullSizeTable;
import exiledlib.skills.skilleffect.ScaledBonus;
import exiledlib.skills.skilleffect.ShipCombatListener;
import exiledlib.skills.skilleffect.weapon.BallisticAsEnergy;

import static exiledlib.skills.skilleffect.ScaledBonus.percent;
import static exiledlib.skills.skilleffect.StatTarget.bonus;
import static exiledlib.skills.skilleffect.StatTarget.scaled;
import static exiledlib.skills.skilleffect.StatTarget.stat;

final class DynamicGridListener extends ShipCombatListener implements AdvanceableListener {

    static final String ENERGY_BONUS_PERCENT_KEY = "exiledSector_dynamicGridEnergyBonusPercent";
    static final HullSizeTable SHIELDS_DOWN_SECONDS = new HullSizeTable(0.75f, 0.75f, 1.1f, 2f, 3f, 3f);
    private static final String MOD_ID_PREFIX = "exiledSector_dynamicGrid_";
    private static final ScaledBonus ENERGY_BONUS = new ScaledBonus(percent(stat(MutableShipStatsAPI::getEnergyRoFMult)),
            percent(scaled(bonus(MutableShipStatsAPI::getEnergyWeaponFluxCostMod), -1f)));
    private static final ScaledBonus BALLISTIC_BONUS = new ScaledBonus(percent(stat(MutableShipStatsAPI::getBallisticRoFMult)),
            percent(scaled(bonus(MutableShipStatsAPI::getBallisticWeaponFluxCostMod), -1f)));

    private final ScaledBonus.Tracker energyBonus = ENERGY_BONUS.tracker();
    private final ScaledBonus.Tracker ballisticBonus = BALLISTIC_BONUS.tracker();
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
        bonusApplied = bonusDue;
        MutableShipStatsAPI stats = ownerShip.getMutableStats();
        float bonusPercent = bonusDue ? magnitude(ENERGY_BONUS_PERCENT_KEY) : 0f;
        energyBonus.apply(stats, modId, bonusPercent);
        ballisticBonus.apply(stats, modId, BallisticAsEnergy.active(stats) ? bonusPercent : 0f);
    }
}
