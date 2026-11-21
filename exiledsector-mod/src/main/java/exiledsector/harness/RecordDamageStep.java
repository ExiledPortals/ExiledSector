package exiledsector.harness;

import com.fs.starfarer.api.combat.CombatEngineAPI;

import java.util.Locale;

final class RecordDamageStep implements HarnessStep {

    static final float DEFAULT_COMBAT_SECONDS = 20f;

    private final String dataName;
    private final float combatSeconds;
    private final boolean untilNoEnemies;
    private final float minEnemyDamage;
    private DamageTally tally;
    private float startElapsed;

    RecordDamageStep(String dataName, float combatSeconds, boolean untilNoEnemies, float minEnemyDamage) {
        this.dataName = dataName;
        this.combatSeconds = combatSeconds;
        this.untilNoEnemies = untilNoEnemies;
        this.minEnemyDamage = minEnemyDamage;
    }

    @Override
    public String name() {
        return "recordDamage " + dataName + " for " + combatSeconds + " combat s" + (untilNoEnemies ? " or until no enemies are left" : "");
    }

    @Override
    public StepOutcome advance(StepContext context) throws Throwable {
        CombatEngineAPI engine = SimulatorAccess.combat();
        if (engine == null) {
            if (tally != null) {
                tally.stop();
            }
            return StepOutcome.failed("recordDamage only works in combat; run a simulate step first");
        }
        if (tally == null) {
            tally = new DamageTally();
            startElapsed = engine.getTotalElapsedTime(false);
        }
        tally.watch(engine);
        float elapsed = engine.getTotalElapsedTime(false) - startElapsed;
        boolean enemiesGone = untilNoEnemies && !context.firstFrame() && !CombatShips.anyEnemyFighting(engine);
        if (elapsed < combatSeconds && !enemiesGone) {
            return StepOutcome.RUNNING;
        }
        tally.stop();
        context.results().addData(dataName, tally.toJson(elapsed));
        String detail = String.format(Locale.ROOT, "%.0f damage to the enemy side and %.0f to the player side over %.1f combat s%s",
                tally.enemySideTotal(), tally.playerSideTotal(), elapsed, enemiesGone ? ", no enemies left" : "");
        if (tally.enemySideTotal() < minEnemyDamage) {
            return StepOutcome.failed(detail + ", below the expected " + minEnemyDamage);
        }
        return StepOutcome.passed(detail);
    }

    @Override
    public float timeoutSeconds() {
        return Math.max(DEFAULT_TIMEOUT_SECONDS, combatSeconds * 2f + DEFAULT_TIMEOUT_SECONDS);
    }
}
