package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.Flagships;
import exiledsector.skills.skilleffect.ScaledBonus;
import exiledsector.skills.skilleffect.ShipCombatListener;

import static exiledsector.skills.skilleffect.StatTarget.liveStat;

final class FlagshipGuardListener extends ShipCombatListener implements AdvanceableListener {

    static final String DAMAGE_TAKEN_PERCENT_KEY = "exiledSector_flagshipGuardDamageTakenPercent";
    static final float RANGE = 1500f;
    static final float SCAN_INTERVAL_SECONDS = 0.25f;
    private static final String MOD_ID_PREFIX = "exiledSector_flagshipGuard_";
    private static final ScaledBonus GUARD_DAMAGE_TAKEN = new ScaledBonus(
            ScaledBonus.percent(liveStat(MutableShipStatsAPI::getHullDamageTakenMult), DAMAGE_TAKEN_PERCENT_KEY),
            ScaledBonus.percent(liveStat(MutableShipStatsAPI::getArmorDamageTakenMult), DAMAGE_TAKEN_PERCENT_KEY),
            ScaledBonus.percent(liveStat(MutableShipStatsAPI::getShieldDamageTakenMult), DAMAGE_TAKEN_PERCENT_KEY));

    private final ScaledBonus.Tracker guardDamageTaken = GUARD_DAMAGE_TAKEN.tracker();
    private float secondsSinceScan = SCAN_INTERVAL_SECONDS;

    FlagshipGuardListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    @Override
    public void advance(float amount) {
        secondsSinceScan += amount;
        if (secondsSinceScan < SCAN_INTERVAL_SECONDS) {
            return;
        }
        secondsSinceScan = 0f;
        guardDamageTaken.apply(ownerShip.getMutableStats(), modId, isGuardingFlagship() ? 1f : 0f);
    }

    private boolean isGuardingFlagship() {
        if (!ownerIsAliveNotHulk()) {
            return false;
        }
        ShipAPI flagship = Flagships.deployedFlagshipOf(ownerShip);
        return flagship != null && CombatQueries.withinRadius(ownerShip.getLocation(), flagship.getLocation(), RANGE);
    }
}
