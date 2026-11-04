package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ScaledBonus;
import exiledsector.skills.skilleffect.weapon.WeaponScope;
import exiledsector.skills.skilleffect.weapon.WeaponStatFamily;

final class SwarmLinkListener implements AdvanceableListener {

    static final String DAMAGE_PERCENT_PER_STRIKECRAFT_KEY = "exiledSector_swarmLinkDamagePercentPerStrikecraft";
    static final String DAMAGE_MOD_ID = "exiledSector_swarmLinkDamage";
    static final float RANGE = 500f;
    static final float MAX_DAMAGE_PERCENT = 20f;
    static final float SCAN_INTERVAL_SECONDS = 0.5f;
    private static final ScaledBonus SWARM_DAMAGE = new ScaledBonus(ScaledBonus.percent(WeaponStatFamily.DAMAGE.target(WeaponScope.ALL)));

    private final ShipAPI fighter;
    private final ShipAPI carrier;
    private final ScaledBonus.Tracker swarmDamage = SWARM_DAMAGE.tracker();
    private float secondsSinceScan = SCAN_INTERVAL_SECONDS;

    SwarmLinkListener(ShipAPI fighter, ShipAPI carrier) {
        this.fighter = fighter;
        this.carrier = carrier;
    }

    static float damagePercent(float percentPerStrikecraft, int nearbyStrikecraft) {
        return Math.min(MAX_DAMAGE_PERCENT, Math.max(0f, percentPerStrikecraft) * nearbyStrikecraft);
    }

    @Override
    public void advance(float amount) {
        secondsSinceScan += amount;
        if (secondsSinceScan < SCAN_INTERVAL_SECONDS) {
            return;
        }
        secondsSinceScan = 0f;
        int nearbyStrikecraft = CombatQueries.isAliveNotHulk(fighter)
                ? CombatQueries.shipsWithin(fighter.getLocation(), RANGE, this::isSwarmMate).size() : 0;
        float percentPerStrikecraft = FighterListenerEffect.carrierValue(carrier, DAMAGE_PERCENT_PER_STRIKECRAFT_KEY);
        swarmDamage.apply(fighter.getMutableStats(), DAMAGE_MOD_ID, damagePercent(percentPerStrikecraft, nearbyStrikecraft));
    }

    private boolean isSwarmMate(ShipAPI ship) {
        return ship != fighter && ship.isFighter() && !ship.isDrone() && ship.getOwner() == fighter.getOwner() && CombatQueries.isAliveNotHulk(ship);
    }
}
