package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import exiledsector.compat.CombatGlow;
import exiledsector.compat.NightcrossCompat;
import exiledsector.i18n.Translation;

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
    static final String STATUS_ICON = "graphics/icons/na_gravitational_drag.png";
    private static final String CUSTOM_DATA_KEY = "exiledSector_gravityDragField";
    private static final String STATUS_KEY = "exiledSector_gravityDragStatus";
    private static final float CHANGE_EPSILON = 0.0005f;
    private static final float LENS_SIZE_SHARE = 2.5f;
    private static final float LENS_MAX_INTENSITY_SHARE = 0.15f;
    private static final float LENS_FADE_SECONDS = 0.5f;

    private final List<GravityDragListener> sources = new ArrayList<>();
    private final Map<ShipAPI, Float> appliedTimeMult = new IdentityHashMap<>();
    private final Map<ShipAPI, Float> combinedPercentByTarget = new IdentityHashMap<>();
    private final Map<GravityDragListener, LensState> lenses = new IdentityHashMap<>();
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
            loadStatusIcon();
            engine.getCustomData().put(CUSTOM_DATA_KEY, field);
            engine.addPlugin(field);
        }
        field.sources.add(source);
    }

    private static void loadStatusIcon() {
        try {
            Global.getSettings().loadTexture(STATUS_ICON);
        } catch (Exception e) {
            return;
        }
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }
        showPlayerStatus(engine);
        moveLenses();
        secondsSinceUpdate += amount;
        if (secondsSinceUpdate < UPDATE_INTERVAL_SECONDS) {
            return;
        }
        secondsSinceUpdate = 0f;
        sources.removeIf(source -> !engine.isEntityInPlay(source.source()) || !CombatQueries.isAliveNotHulk(source.source()));
        refreshLenses();
        applyReductions(reductionsFrom(sources, (source, radius, filter) -> CombatQueries.shipsNear(source.getLocation(), radius, filter)));
    }

    private static final class LensState {
        private final CombatGlow.Lens lens;
        private float size;
        private float intensity;

        private LensState(CombatGlow.Lens lens) {
            this.lens = lens;
        }
    }

    private void refreshLenses() {
        lenses.entrySet().removeIf(entry -> {
            if (sources.contains(entry.getKey())) {
                return false;
            }
            entry.getValue().lens.fadeOut(LENS_FADE_SECONDS);
            return true;
        });
        for (GravityDragListener source : sources) {
            ShipAPI ship = source.source();
            float radius = ship.getCollisionRadius();
            float strength = Math.min(1f, Math.max(0f, source.reductionPercentAtPointBlank() / MAX_TOTAL_REDUCTION_PERCENT));
            LensState state = lenses.get(source);
            if (state == null) {
                state = new LensState(CombatGlow.lens(ship.getLocation(), radius * LENS_SIZE_SHARE, radius * LENS_MAX_INTENSITY_SHARE * strength));
                lenses.put(source, state);
            }
            state.size = radius * LENS_SIZE_SHARE;
            state.intensity = radius * LENS_MAX_INTENSITY_SHARE * strength;
        }
    }

    private void moveLenses() {
        lenses.forEach((source, state) -> state.lens.update(source.source().getLocation(), state.size, state.intensity));
    }

    private void showPlayerStatus(CombatEngineAPI engine) {
        Float combinedPercent = combinedPercentByTarget.get(engine.getPlayerShip());
        if (combinedPercent != null && combinedPercent >= 0.5f) {
            engine.maintainStatusForPlayerShip(STATUS_KEY, STATUS_ICON, Translation.gameText("combat.gravityDrag"),
                    Translation.msg("combat.gravityDrag.status").arg("value", Math.round(combinedPercent)).text(), true);
        }
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

    static float combinedPercent(float ownPercent, float nightcrossPercent) {
        return Math.min(MAX_TOTAL_REDUCTION_PERCENT, ownPercent + nightcrossPercent);
    }

    static float ownTimeMult(float ownPercent, float nightcrossPercent) {
        float nightcrossMult = Math.max(0.01f, 1f - nightcrossPercent / 100f);
        return (1f - combinedPercent(ownPercent, nightcrossPercent) / 100f) / nightcrossMult;
    }

    private void applyReductions(Map<ShipAPI, Float> ownReductions) {
        for (ShipAPI previouslyDragged : new ArrayList<>(appliedTimeMult.keySet())) {
            if (!ownReductions.containsKey(previouslyDragged)) {
                previouslyDragged.getMutableStats().getTimeMult().unmodify(MOD_ID);
                appliedTimeMult.remove(previouslyDragged);
                combinedPercentByTarget.remove(previouslyDragged);
            }
        }
        ownReductions.forEach((target, ownPercent) -> {
            float nightcrossPercent = NightcrossCompat.gravityDragPercentOn(target);
            float timeMult = ownTimeMult(ownPercent, nightcrossPercent);
            Float applied = appliedTimeMult.get(target);
            if (applied == null || Math.abs(applied - timeMult) > CHANGE_EPSILON) {
                target.getMutableStats().getTimeMult().modifyMult(MOD_ID, timeMult);
                appliedTimeMult.put(target, timeMult);
            }
            combinedPercentByTarget.put(target, combinedPercent(ownPercent, nightcrossPercent));
        });
    }
}
