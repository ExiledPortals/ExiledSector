package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import com.fs.starfarer.api.util.IntervalUtil;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.shipsystem.FragmentCover;
import exiledsector.skills.skilleffect.shipsystem.FragmentSwarms;

final class PhaseVoidUpkeepListener extends ShipCombatListener implements AdvanceableListener {

    static final String COVERED_PERCENT_KEY = "exiledLib_phaseUpkeepCoveredByFragmentsPercent";
    static final float FLUX_PER_FRAGMENT = 50f;
    static final float ALLIED_SWARM_RANGE = 1000f;
    static final float MAX_COVERED_PERCENT = 90f;
    private static final String MOD_ID_PREFIX = "exiledLib_phaseVoidUpkeep_";
    private static final float SWARM_CHECK_SECONDS = 0.5f;

    private final IntervalUtil swarmCheck = new IntervalUtil(SWARM_CHECK_SECONDS, SWARM_CHECK_SECONDS);
    private final FragmentCover upkeepCover = new FragmentCover(FLUX_PER_FRAGMENT);
    private RoilingSwarmEffect coveringSwarm;
    private boolean covering;
    private boolean wasPhased;

    PhaseVoidUpkeepListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    @Override
    public void advance(float amount) {
        float coveredPercent = Math.min(MAX_COVERED_PERCENT, magnitude(COVERED_PERCENT_KEY));
        if (!ownerShip.isPhased() || coveredPercent <= 0f || !ownerIsAliveNotHulk()) {
            wasPhased = false;
            coveringSwarm = null;
            setCovering(false, coveredPercent);
            return;
        }
        swarmCheck.advance(amount);
        if (!wasPhased || swarmCheck.intervalElapsed()) {
            wasPhased = true;
            coveringSwarm = nearestSwarmWithFragments();
        }
        setCovering(coveringSwarm != null, coveredPercent);
        if (!covering) {
            return;
        }
        if (!upkeepCover.charge(coveringSwarm, paidUpkeepPerSecond() * amount, coveredPercent)) {
            coveringSwarm = null;
            setCovering(false, coveredPercent);
        }
    }

    private RoilingSwarmEffect nearestSwarmWithFragments() {
        RoilingSwarmEffect ownSwarm = FragmentSwarms.swarmOf(ownerShip);
        if (FragmentSwarms.hasFragments(ownSwarm, 1)) {
            return ownSwarm;
        }
        ShipAPI ally = CombatQueries.nearestShip(ownerShip.getLocation(), ALLIED_SWARM_RANGE, this::lendsFragments);
        return ally == null ? null : FragmentSwarms.swarmOf(ally);
    }

    private boolean lendsFragments(ShipAPI candidate) {
        return candidate != ownerShip && !candidate.isFighter() && candidate.getOwner() == ownerShip.getOwner()
                && CombatQueries.isAliveNotHulk(candidate) && FragmentSwarms.hasFragments(FragmentSwarms.swarmOf(candidate), 1);
    }

    private float paidUpkeepPerSecond() {
        ShipHullSpecAPI hullSpec = ownerShip.getHullSpec();
        if (hullSpec == null || hullSpec.getShieldSpec() == null) {
            return 0f;
        }
        float baseUpkeep = hullSpec.getShieldSpec().getPhaseUpkeep() * hullSpec.getFluxCapacity();
        return ownerShip.getMutableStats().getPhaseCloakUpkeepCostBonus().computeEffective(baseUpkeep);
    }

    private void setCovering(boolean nowCovering, float coveredPercent) {
        if (nowCovering == covering) {
            return;
        }
        covering = nowCovering;
        StatBonus upkeep = ownerShip.getMutableStats().getPhaseCloakUpkeepCostBonus();
        if (nowCovering) {
            upkeep.modifyMult(modId, 1f - coveredPercent / 100f);
        } else {
            upkeep.unmodify(modId);
        }
    }
}
