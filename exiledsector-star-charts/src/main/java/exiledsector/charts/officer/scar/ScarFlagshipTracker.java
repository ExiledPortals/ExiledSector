package exiledsector.charts.officer.scar;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import exiledsector.skills.skilleffect.CombatQueries;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

final class ScarFlagshipTracker {

    static final float HIGH_FLUX_LEVEL = 0.9f;
    static final float SWARM_WINDOW_SECONDS = 5f;
    static final float MALFUNCTION_CR = 0.4f;
    static final float KILLING_BLOW_SECONDS = 3f;

    private final CombatEngineAPI engine;
    private final String flagshipMemberId;
    private final ScarEvidence evidence = new ScarEvidence();
    private final Map<ShipAPI, Float> lastHitSecondsByAttacker = new IdentityHashMap<>();
    private final Set<WeaponAPI> disabledWeapons = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<ShipAPI> escorts = Collections.newSetFromMap(new IdentityHashMap<>());
    private ShipAPI flagship;
    private ShipAPI lastAttacker;
    private float lastAttackerSeconds;
    private boolean wasOverloaded;
    private boolean finished;

    ScarFlagshipTracker(CombatEngineAPI engine, String flagshipMemberId) {
        this.engine = engine;
        this.flagshipMemberId = flagshipMemberId;
    }

    String flagshipMemberId() {
        return flagshipMemberId;
    }

    ScarEvidence evidence() {
        return evidence;
    }

    ShipAPI flagship() {
        return flagship;
    }

    boolean found() {
        return flagship != null;
    }

    boolean finished() {
        return finished;
    }

    float elapsedSeconds() {
        return engine.getTotalElapsedTime(false);
    }

    void attach(ShipAPI ship) {
        flagship = ship;
        PersonAPI captain = ship.getCaptain();
        int officerLevel = captain == null || captain.isDefault() ? 0 : captain.getStats().getLevel();
        evidence.fleet().flagship(ScarFleetSnapshot.longestRange(ship), ship.getMaxSpeedWithoutBoost(), officerLevel);
        if (!ship.hasListenerOfClass(ScarDamageTally.class)) {
            ship.addListener(new ScarDamageTally(this));
        }
        ScarBattleResults.publish(flagshipMemberId, evidence);
    }

    void noteEscort(ShipAPI ship) {
        escorts.add(ship);
    }

    void recordHit(ShipAPI attacker) {
        float elapsed = elapsedSeconds();
        if (!evidence.hasFirstHit()) {
            evidence.firstHit(elapsed, escortLossShare());
        }
        if (attacker != null) {
            lastAttacker = attacker;
            lastAttackerSeconds = elapsed;
            lastHitSecondsByAttacker.put(attacker, elapsed);
        }
    }

    float escortLossShare() {
        if (escorts.isEmpty()) {
            return 0f;
        }
        int lost = 0;
        for (ShipAPI escort : escorts) {
            if (!CombatQueries.isAliveNotHulk(escort)) {
                lost++;
            }
        }
        return lost / (float) escorts.size();
    }

    void advanceFrame(float amount) {
        if (flagship == null || finished) {
            return;
        }
        if (!CombatQueries.isAliveNotHulk(flagship)) {
            evidence.died(elapsedSeconds(), killedByPlayerFlagship());
            finished = true;
            return;
        }
        if (!engine.isEntityInPlay(flagship)) {
            finished = true;
            return;
        }
        boolean overloaded = flagship.getFluxTracker().isOverloaded();
        if (overloaded && !wasOverloaded) {
            evidence.countOverload();
        }
        wasOverloaded = overloaded;
        if (flagship.getFluxTracker().getFluxLevel() >= HIGH_FLUX_LEVEL) {
            evidence.addHighFluxSeconds(amount);
        }
    }

    private boolean killedByPlayerFlagship() {
        if (lastAttacker == null || elapsedSeconds() - lastAttackerSeconds > KILLING_BLOW_SECONDS) {
            return false;
        }
        PersonAPI captain = lastAttacker.getCaptain();
        return captain != null && captain.isPlayer();
    }

    void sample(float seconds) {
        if (flagship == null || finished) {
            return;
        }
        samplePeakReadiness();
        sampleMalfunctions();
        sampleSwarm();
        float range = evidence.fleet().flagshipLongestRange() + flagship.getCollisionRadius();
        boolean outOfRange = CombatQueries.hostilesNear(flagship, flagship.getLocation(), range, ship -> !ship.isFighter()).isEmpty();
        evidence.addTrackedSeconds(seconds, outOfRange);
    }

    private void samplePeakReadiness() {
        if (flagship.getHullSpec() != null && flagship.getHullSpec().getNoCRLossTime() < HullFacts.PEAKLESS_SECONDS
                && flagship.getFullTimeDeployed() > 0f && flagship.getPeakTimeRemaining() <= 0f) {
            evidence.peakCrRanOut();
        }
    }

    private void sampleMalfunctions() {
        boolean lowReadiness = flagship.getCurrentCR() < MALFUNCTION_CR;
        for (WeaponAPI weapon : flagship.getAllWeapons()) {
            if (!weapon.isDisabled()) {
                disabledWeapons.remove(weapon);
            } else if (disabledWeapons.add(weapon) && lowReadiness) {
                evidence.countMalfunction();
            }
        }
    }

    private void sampleSwarm() {
        float windowStart = elapsedSeconds() - SWARM_WINDOW_SECONDS;
        Iterator<Float> hitSeconds = lastHitSecondsByAttacker.values().iterator();
        while (hitSeconds.hasNext()) {
            if (hitSeconds.next() < windowStart) {
                hitSeconds.remove();
            }
        }
        evidence.noteAttackersInWindow(lastHitSecondsByAttacker.size());
    }
}
