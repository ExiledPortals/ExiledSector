package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

final class GravityDragField extends BaseEveryFrameCombatPlugin {

    static final float RANGE = 1500f;
    static final float MAX_TOTAL_REDUCTION_PERCENT = 25f;
    static final float UPDATE_INTERVAL_SECONDS = 0.25f;
    static final String MOD_ID = "exiledSector_gravityDrag";
    private static final String CUSTOM_DATA_KEY = "exiledSector_gravityDragField";
    private static final float CHANGE_EPSILON_PERCENT = 0.05f;

    private final List<GravityDragListener> sources = new ArrayList<>();
    private final Map<ShipAPI, Float> appliedReductionPercent = new IdentityHashMap<>();
    private float secondsSinceUpdate = UPDATE_INTERVAL_SECONDS;

    static void register(GravityDragListener source) {
        if (Global.getSettings() == null || Global.getCurrentState() != GameState.COMBAT) {
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return;
        }
        GravityDragField field = (GravityDragField) engine.getCustomData().get(CUSTOM_DATA_KEY);
        if (field == null) {
            field = new GravityDragField();
            engine.getCustomData().put(CUSTOM_DATA_KEY, field);
            engine.addPlugin(field);
        }
        field.sources.add(source);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }
        secondsSinceUpdate += amount;
        if (secondsSinceUpdate < UPDATE_INTERVAL_SECONDS) {
            return;
        }
        secondsSinceUpdate = 0f;
        sources.removeIf(source -> !engine.isEntityInPlay(source.source()) || !CombatQueries.isAliveNotHulk(source.source()));
        applyReductions(reductionsFrom(sources, (source, radius, filter) -> CombatQueries.shipsNear(source.getLocation(), radius, filter)));
    }

    interface ShipsNear {
        List<ShipAPI> find(ShipAPI source, float radius, Predicate<ShipAPI> filter);
    }

    static Map<ShipAPI, Float> reductionsFrom(List<GravityDragListener> sources, ShipsNear shipsNear) {
        Map<ShipAPI, Float> reductions = new IdentityHashMap<>();
        for (GravityDragListener source : sources) {
            float pointBlankPercent = source.reductionPercentAtPointBlank();
            if (pointBlankPercent <= 0f) {
                continue;
            }
            ShipAPI sourceShip = source.source();
            for (ShipAPI target : shipsNear.find(sourceShip, RANGE, other -> !other.isFighter()
                    && CombatQueries.isAliveNotHulk(other) && CombatQueries.isHostile(sourceShip, other))) {
                float centreDistance = (float) Math.sqrt(CombatQueries.distanceSquared(sourceShip.getLocation(), target.getLocation()));
                float distance = Math.max(0f, centreDistance - target.getCollisionRadius());
                float closeness = Math.max(0f, 1f - distance / RANGE);
                if (closeness > 0f) {
                    reductions.merge(target, pointBlankPercent * closeness, Float::sum);
                }
            }
        }
        reductions.replaceAll((target, percent) -> Math.min(MAX_TOTAL_REDUCTION_PERCENT, percent));
        return reductions;
    }

    private void applyReductions(Map<ShipAPI, Float> reductions) {
        for (ShipAPI previouslyDragged : new ArrayList<>(appliedReductionPercent.keySet())) {
            if (!reductions.containsKey(previouslyDragged)) {
                previouslyDragged.getMutableStats().getTimeMult().unmodify(MOD_ID);
                appliedReductionPercent.remove(previouslyDragged);
            }
        }
        reductions.forEach((target, percent) -> {
            Float applied = appliedReductionPercent.get(target);
            if (applied == null || Math.abs(applied - percent) > CHANGE_EPSILON_PERCENT) {
                target.getMutableStats().getTimeMult().modifyMult(MOD_ID, 1f - percent / 100f);
                appliedReductionPercent.put(target, percent);
            }
        });
    }
}
