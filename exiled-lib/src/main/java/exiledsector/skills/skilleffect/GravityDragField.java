package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.compat.CombatGlow;
import exiledsector.compat.NightcrossGravityDrag;
import exiledsector.i18n.Translation;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class GravityDragField extends AuraField<GravityDragListener> {

    static final float RANGE = 1500f;
    static final float MAX_TOTAL_REDUCTION_PERCENT = 25f;
    static final String MOD_ID = "exiledSector_gravityDrag";
    static final String STATUS_ICON = "graphics/icons/na_gravitational_drag.png";
    private static final String CUSTOM_DATA_KEY = "exiledSector_gravityDragField";
    private static final String STATUS_KEY = "exiledSector_gravityDragStatus";
    private static final float CHANGE_EPSILON = 0.0005f;
    private static final float MIN_SHOWN_PERCENT = 0.5f;
    private static final float LENS_SIZE_SHARE = 2.5f;
    private static final float LENS_MAX_INTENSITY_SHARE = 0.15f;
    private static final float LENS_FADE_SECONDS = 0.5f;

    private final AppliedAuraValues appliedTimeMult = new AppliedAuraValues(AppliedAuraValues.Change.beyond(CHANGE_EPSILON));
    private final Map<ShipAPI, Float> shownPercentByTarget = new IdentityHashMap<>();
    private final Map<GravityDragListener, LensState> lenses = new IdentityHashMap<>();
    private final PlayerStatus playerStatus = new PlayerStatus(STATUS_KEY, STATUS_ICON, "combat.gravityDrag");
    private int shownRoundedPercent = Integer.MIN_VALUE;

    GravityDragField() {
        super(true);
    }

    static void register(GravityDragListener source) {
        AuraField.register(source, CUSTOM_DATA_KEY, GravityDragField.class, GravityDragField::new, (engine, field) -> loadStatusIcon());
    }

    private static void loadStatusIcon() {
        try {
            Global.getSettings().loadTexture(STATUS_ICON);
        } catch (Exception e) {
            return;
        }
    }

    @Override
    void everyFrame(CombatEngineAPI engine) {
        showPlayerStatus(engine);
        moveLenses();
    }

    @Override
    void update(float elapsedSeconds) {
        refreshLenses();
        applyReductions(reductionsFrom(sources, ENGINE_SHIPS_NEAR));
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
        Float shownPercent = shownPercentByTarget.get(engine.getPlayerShip());
        if (shownPercent == null || shownPercent < MIN_SHOWN_PERCENT) {
            return;
        }
        int roundedPercent = Math.round(shownPercent);
        if (roundedPercent != shownRoundedPercent || playerStatus.needsText()) {
            shownRoundedPercent = roundedPercent;
            playerStatus.setText(() -> Translation.msg("combat.gravityDrag.status").arg("value", roundedPercent).text());
        }
        playerStatus.show(engine, true);
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

    static float shownPercent(float ownPercent, float nightcrossPercent) {
        return Math.max(0f, combinedPercent(ownPercent, nightcrossPercent) - nightcrossPercent);
    }

    static float ownTimeMult(float ownPercent, float nightcrossPercent) {
        float nightcrossMult = Math.max(0.01f, 1f - nightcrossPercent / 100f);
        return (1f - combinedPercent(ownPercent, nightcrossPercent) / 100f) / nightcrossMult;
    }

    private void applyReductions(Map<ShipAPI, Float> ownReductions) {
        Map<ShipAPI, Float> timeMultByTarget = new IdentityHashMap<>();
        shownPercentByTarget.clear();
        ownReductions.forEach((target, ownPercent) -> {
            float nightcrossPercent = NightcrossGravityDrag.percentOn(target);
            timeMultByTarget.put(target, ownTimeMult(ownPercent, nightcrossPercent));
            shownPercentByTarget.put(target, shownPercent(ownPercent, nightcrossPercent));
        });
        appliedTimeMult.update(timeMultByTarget, target -> target.getMutableStats().getTimeMult().unmodify(MOD_ID),
                (target, timeMult) -> target.getMutableStats().getTimeMult().modifyMult(MOD_ID, timeMult));
    }
}
