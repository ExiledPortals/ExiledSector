package exiledsector.charts.officer.scar;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class ScarEvidence {

    public enum FiredAs {
        MISSILE, STRIKE, MINE, PROJECTILE, BEAM, FIGHTER, BOMBER,
        KINETIC, HIGH_EXPLOSIVE, ENERGY, FRAGMENTATION, EMP,
        PHASE_ATTACKER, BEYOND_RANGE, COLLISION, SYSTEM_ACTIVE, REAR, FASTER_ATTACKER
    }

    static final Set<FiredAs> DELIVERIES = EnumSet.of(FiredAs.MISSILE, FiredAs.PROJECTILE, FiredAs.BEAM, FiredAs.FIGHTER);
    static final Set<FiredAs> DAMAGE_TYPES = EnumSet.of(FiredAs.KINETIC, FiredAs.HIGH_EXPLOSIVE, FiredAs.ENERGY, FiredAs.FRAGMENTATION);
    static final float QUICK_DEATH_SECONDS = 30f;
    static final float KITED_DAMAGE_DEALT_SHARE = 0.5f;

    private final Map<FiredAs, Float> fired = new EnumMap<>(FiredAs.class);
    private final ScarFleetSnapshot fleet = new ScarFleetSnapshot();
    private float totalFired;
    private float damageDealt;
    private int overloads;
    private float highFluxSeconds;
    private int mostAttackersInWindow;
    private boolean killedByPlayerFlagship;
    private float firstHitSeconds = -1f;
    private float deathSeconds = -1f;
    private boolean peakCrRanOut;
    private int malfunctions;
    private float escortLossShareAtFirstHit;
    private float outOfRangeSeconds;
    private float trackedSeconds;

    public ScarFleetSnapshot fleet() {
        return fleet;
    }

    void addFired(float damage, Set<FiredAs> categories) {
        if (damage <= 0f) {
            return;
        }
        totalFired += damage;
        for (FiredAs category : categories) {
            fired.merge(category, damage, Float::sum);
        }
    }

    void addFired(FiredAs category, float amount) {
        if (amount > 0f) {
            fired.merge(category, amount, Float::sum);
        }
    }

    void addDamageDealt(float damage) {
        damageDealt += Math.max(0f, damage);
    }

    void countOverload() {
        overloads++;
    }

    void addHighFluxSeconds(float seconds) {
        highFluxSeconds += seconds;
    }

    void noteAttackersInWindow(int attackers) {
        mostAttackersInWindow = Math.max(mostAttackersInWindow, attackers);
    }

    void firstHit(float elapsedSeconds, float escortLossShare) {
        if (firstHitSeconds < 0f) {
            firstHitSeconds = elapsedSeconds;
            escortLossShareAtFirstHit = escortLossShare;
        }
    }

    void died(float elapsedSeconds, boolean byPlayerFlagship) {
        if (deathSeconds < 0f) {
            deathSeconds = elapsedSeconds;
            killedByPlayerFlagship = byPlayerFlagship;
        }
    }

    void peakCrRanOut() {
        peakCrRanOut = true;
    }

    void countMalfunction() {
        malfunctions++;
    }

    void addTrackedSeconds(float seconds, boolean outOfRange) {
        trackedSeconds += seconds;
        if (outOfRange) {
            outOfRangeSeconds += seconds;
        }
    }

    public boolean hasFirstHit() {
        return firstHitSeconds >= 0f;
    }

    public boolean died() {
        return deathSeconds >= 0f;
    }

    float share(FiredAs category) {
        return totalFired <= 0f ? 0f : fired.getOrDefault(category, 0f) / totalFired;
    }

    float largestShare(FiredAs category, Set<FiredAs> group) {
        float own = fired.getOrDefault(category, 0f);
        for (FiredAs other : group) {
            if (other != category && fired.getOrDefault(other, 0f) > own) {
                return 0f;
            }
        }
        return share(category);
    }

    float kitedShare() {
        return damageDealt < totalFired * KITED_DAMAGE_DEALT_SHARE ? share(FiredAs.FASTER_ATTACKER) : 0f;
    }

    float empShare() {
        return totalFired <= 0f ? 0f : fired.getOrDefault(FiredAs.EMP, 0f) / totalFired;
    }

    int overloads() {
        return overloads;
    }

    float highFluxSeconds() {
        return highFluxSeconds;
    }

    int mostAttackersInWindow() {
        return mostAttackersInWindow;
    }

    boolean killedByPlayerFlagship() {
        return killedByPlayerFlagship;
    }

    float quickDeath() {
        if (!died() || !hasFirstHit()) {
            return 0f;
        }
        return QUICK_DEATH_SECONDS / Math.max(1f, deathSeconds - firstHitSeconds);
    }

    boolean peakCrRanOutBeforeDeath() {
        return peakCrRanOut;
    }

    int malfunctions() {
        return malfunctions;
    }

    float escortLossShareAtFirstHit() {
        return escortLossShareAtFirstHit;
    }

    float outOfRangeShare() {
        return trackedSeconds <= 0f ? 0f : outOfRangeSeconds / trackedSeconds;
    }
}
