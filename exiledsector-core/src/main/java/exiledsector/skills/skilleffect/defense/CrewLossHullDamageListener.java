package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.fleet.CrewCompositionAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.skills.skilleffect.ParameterBonus;
import exiledsector.skills.skilleffect.ShipCombatListener;

final class CrewLossHullDamageListener extends ShipCombatListener implements DamageListener {

    static final String PERCENT_PER_STEP_KEY = "exiledSector_crewLossHullDamageTakenPercent";
    static final String MAX_REDUCTION_FLAT_KEY = "exiledSector_crewLossHullDamageTakenMaxFlat";
    static final float CREW_PER_STEP = 10f;
    static final float MAX_REDUCTION_PERCENT = 20f;
    static final float AVERAGE_LOSS_ROLL = 0.75f;
    private static final String MOD_ID_PREFIX = "exiledSector_crewLossHullDamage_";

    private boolean crewCounted;
    private float crewAboard;
    private float hullDamageTaken;
    private float appliedReductionPercent;

    CrewLossHullDamageListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    static float crewAboard(ShipAPI ship) {
        ShipAPI parentStation = ship.getParentStation();
        FleetMemberAPI member = (parentStation != null ? parentStation : ship).getFleetMember();
        CrewCompositionAPI crew = member == null ? null : member.getCrewComposition();
        return crew == null ? 0f : crew.getCrew();
    }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        if (target != ownerShip || result == null || result.getDamageToHull() <= 0f) {
            return;
        }
        if (!crewCounted) {
            crewCounted = true;
            crewAboard = crewAboard(ownerShip);
        }
        if (crewAboard <= 0f) {
            return;
        }
        hullDamageTaken += result.getDamageToHull();
        MutableShipStatsAPI stats = ownerShip.getMutableStats();
        float maxHull = ownerShip.getMaxHitpoints();
        float hullDamageFraction = maxHull <= 0f ? 0f : hullDamageTaken / maxHull;
        float crewLost = crewLost(crewAboard, hullDamageFraction, stats.getCrewLossMult().getModifiedValue());
        float reductionPercent = reductionPercent(magnitude(PERCENT_PER_STEP_KEY), crewLost, maxReductionPercent(magnitude(MAX_REDUCTION_FLAT_KEY)));
        if (Float.compare(reductionPercent, appliedReductionPercent) == 0) {
            return;
        }
        appliedReductionPercent = reductionPercent;
        if (reductionPercent <= 0f) {
            stats.getHullDamageTakenMult().unmodifyPercent(modId);
        } else {
            stats.getHullDamageTakenMult().modifyPercent(modId, -reductionPercent);
        }
    }

    static float crewLost(float crewAboard, float hullDamageFraction, float crewLossMult) {
        float lossFraction = hullDamageFraction * hullDamageFraction * AVERAGE_LOSS_ROLL * crewLossMult;
        return crewAboard * Math.min(1f, Math.max(0f, lossFraction));
    }

    static float maxReductionPercent(float maxReductionBonus) {
        return ParameterBonus.plusFlat(MAX_REDUCTION_PERCENT, maxReductionBonus);
    }

    static float reductionPercent(float percentPerStep, float crewLost, float maxReductionPercent) {
        if (percentPerStep <= 0f) {
            return 0f;
        }
        return Math.min(maxReductionPercent, percentPerStep * (float) Math.floor(crewLost / CREW_PER_STEP));
    }
}
