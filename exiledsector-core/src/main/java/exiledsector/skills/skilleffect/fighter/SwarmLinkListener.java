package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ParameterBonus;
import exiledsector.skills.skilleffect.ScaledBonus;
import exiledsector.skills.skilleffect.weapon.WeaponScope;
import exiledsector.skills.skilleffect.weapon.WeaponStatFamily;

final class SwarmLinkListener {

    static final String DAMAGE_PERCENT_PER_STRIKECRAFT_KEY = "exiledSector_swarmLinkDamagePercentPerStrikecraft";
    static final String MAX_DAMAGE_FLAT_KEY = "exiledSector_swarmLinkMaxDamageFlat";
    static final String DAMAGE_MOD_ID = "exiledSector_swarmLinkDamage";
    static final float RANGE = 500f;
    static final float MAX_DAMAGE_PERCENT = 20f;
    private static final ScaledBonus SWARM_DAMAGE = new ScaledBonus(ScaledBonus.percent(WeaponStatFamily.DAMAGE.target(WeaponScope.ALL)));

    private final ShipAPI fighter;
    private final ShipAPI carrier;
    private final ScaledBonus.Tracker swarmDamage = SWARM_DAMAGE.tracker();

    SwarmLinkListener(ShipAPI fighter, ShipAPI carrier) {
        this.fighter = fighter;
        this.carrier = carrier;
        SwarmLinkCensus.register(this);
    }

    ShipAPI fighter() {
        return fighter;
    }

    static float maxDamagePercent(float maxDamageBonus) {
        return ParameterBonus.plusFlat(MAX_DAMAGE_PERCENT, maxDamageBonus);
    }

    static float damagePercent(float percentPerStrikecraft, int nearbyStrikecraft, float maxDamagePercent) {
        return Math.min(maxDamagePercent, Math.max(0f, percentPerStrikecraft) * nearbyStrikecraft);
    }

    static int strikecraftNeededToReachCap(float percentPerStrikecraft, float maxDamagePercent) {
        if (percentPerStrikecraft <= 0f || maxDamagePercent <= 0f) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE - 1d, Math.ceil(maxDamagePercent / percentPerStrikecraft)) + 1;
    }

    void refresh(SwarmLinkCensus census) {
        float percentPerStrikecraft = FighterListenerEffect.carrierValue(carrier, DAMAGE_PERCENT_PER_STRIKECRAFT_KEY);
        float maxDamagePercent = maxDamagePercent(FighterListenerEffect.carrierValue(carrier, MAX_DAMAGE_FLAT_KEY));
        int nearbyStrikecraft = CombatQueries.isAliveNotHulk(fighter)
                ? census.strikecraftNear(fighter, RANGE, strikecraftNeededToReachCap(percentPerStrikecraft, maxDamagePercent)) : 0;
        swarmDamage.apply(fighter.getMutableStats(), DAMAGE_MOD_ID, damagePercent(percentPerStrikecraft, nearbyStrikecraft, maxDamagePercent));
    }
}
