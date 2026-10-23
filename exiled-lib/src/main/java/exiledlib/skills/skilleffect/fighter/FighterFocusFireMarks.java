package exiledlib.skills.skilleffect.fighter;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledlib.skills.skilleffect.CombatQueries;
import exiledlib.skills.skilleffect.ShipCombatListener;
import exiledlib.skills.skilleffect.SkillEffectSupport;
import org.lwjgl.util.vector.Vector2f;

import java.util.IdentityHashMap;
import java.util.Map;

final class FighterFocusFireMarks extends ShipCombatListener {

    static final String PERCENT_PER_HIT_KEY = "exiledSector_fighterFocusFirePercentPerHit";
    static final String MAX_PERCENT_KEY = "exiledSector_fighterFocusFireMaxPercent";
    static final float RESET_AFTER_SECONDS = 2f;
    private static final int PRUNE_ABOVE_MARKS = 16;
    private static final String MOD_ID_PREFIX = "exiledSector_fighterFocusFire_";

    private final Map<ShipAPI, Mark> marksByTarget = new IdentityHashMap<>();

    FighterFocusFireMarks(ShipAPI carrier) {
        super(carrier, MOD_ID_PREFIX);
    }

    static FighterFocusFireMarks of(ShipAPI carrier) {
        return SkillEffectSupport.ensureListener(carrier, FighterFocusFireMarks.class, FighterFocusFireMarks::new);
    }

    static void markFighter(ShipAPI fighter, ShipAPI carrier) {
        SkillEffectSupport.ensureListener(fighter, FighterHits.class, ignored -> new FighterHits(of(carrier)));
    }

    float markPercent(ShipAPI target) {
        Mark mark = marksByTarget.get(target);
        return mark == null || nowSeconds() - mark.lastHitSeconds > RESET_AFTER_SECONDS ? 0f : mark.percent;
    }

    float hit(ShipAPI target, float hitsWorth) {
        float now = nowSeconds();
        Mark mark = marksByTarget.computeIfAbsent(target, ignored -> new Mark());
        if (now - mark.lastHitSeconds > RESET_AFTER_SECONDS) {
            mark.percent = 0f;
        }
        float appliedPercent = mark.percent;
        mark.percent = Math.min(magnitude(MAX_PERCENT_KEY), mark.percent + magnitude(PERCENT_PER_HIT_KEY) * hitsWorth);
        mark.lastHitSeconds = now;
        if (marksByTarget.size() > PRUNE_ABOVE_MARKS) {
            marksByTarget.values().removeIf(other -> now - other.lastHitSeconds > RESET_AFTER_SECONDS);
        }
        return appliedPercent;
    }

    private static float nowSeconds() {
        CombatEngineAPI engine = Global.getCombatEngine();
        return engine == null ? 0f : engine.getTotalElapsedTime(false);
    }

    private static final class Mark {
        private float percent;
        private float lastHitSeconds = Float.NEGATIVE_INFINITY;
    }

    static final class FighterHits implements DamageDealtModifier {

        private final FighterFocusFireMarks marks;

        FighterHits(FighterFocusFireMarks marks) {
            this.marks = marks;
        }

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (!(target instanceof ShipAPI targetShip) || !CombatQueries.isHostile(marks.ownerShip, targetShip)) {
                return null;
            }
            float appliedPercent = marks.hit(targetShip, damage.isDps() ? damage.getDpsDuration() : 1f);
            if (appliedPercent <= 0f) {
                return null;
            }
            damage.getModifier().modifyPercent(marks.modId, appliedPercent);
            return marks.modId;
        }
    }
}
