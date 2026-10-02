package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;
import exiledsector.compat.LostSectorCompat;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;

import java.awt.Color;
import java.util.Locale;

final class FluxScaledVolatilityListener implements AdvanceableListener {

    static final String TOP_SPEED_KEY = "exiledSector_fluxScaledTopSpeed";
    static final String RATE_OF_FIRE_KEY = "exiledSector_fluxScaledRateOfFire";
    static final String AUGMENTED_PENALTY_REDUCTION_KEY = "exiledSector_fluxScaledAugmentedPenaltyReduction";
    private static final String MOD_ID_PREFIX = "exiledSector_fluxScaled_";
    private static final String STATUS_KEY = "exiledSector_fluxScaledStatus";
    private static final String STATUS_ICON = "graphics/icons/hullsys/high_energy_focus.png";
    private static final float RATIO_STEP = 0.02f;
    private static final float AFTERIMAGE_INTERVAL = 0.01f;
    private static final float AFTERIMAGE_DRIFT = -0.8f;
    private static final float AFTERIMAGE_DURATION = 0.3f;
    private static final float ENGINE_TINT_STRENGTH = 0.5f;
    private static final Color AFTERIMAGE_COLOR = new Color(100, 42, 201, 80);
    private static final Color ENGINE_COLOR = new Color(100, 42, 201, 255);

    private final ShipAPI ship;
    private final String modId;
    private final IntervalUtil afterimageTimer = new IntervalUtil(AFTERIMAGE_INTERVAL, AFTERIMAGE_INTERVAL);
    private float appliedRatio = Float.NaN;
    private Float penaltyScale;
    private Color afterimageColor = AFTERIMAGE_COLOR;
    private String statusTitle;
    private String statusText;

    FluxScaledVolatilityListener(ShipAPI ship) {
        this.ship = ship;
        this.modId = MOD_ID_PREFIX + ship.getId();
    }

    static float ratio(FluxTrackerAPI flux) {
        float level = flux.isOverloaded() || flux.isVenting() ? 1f : flux.getFluxLevel();
        float ratio = Math.max(-1f, Math.min(1f, 1f - 2f * level));
        return Math.round(ratio / RATIO_STEP) * RATIO_STEP;
    }

    @Override
    public void advance(float amount) {
        if (!ship.isAlive() || ship.isHulk()) {
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }
        float ratio = ratio(ship.getFluxTracker());
        float scale = ratio < 0f ? penaltyScale() : 1f;
        float topSpeed = magnitude(TOP_SPEED_KEY) * scale;
        float rateOfFire = magnitude(RATE_OF_FIRE_KEY) * scale;
        if (ratio != appliedRatio) {
            applyRatio(ratio, topSpeed, rateOfFire);
        }
        if (ship == engine.getPlayerShip()) {
            showStatus(engine, ratio, topSpeed, rateOfFire);
        }
        if (ratio > 0f) {
            showLowFluxEffects(engine, ratio);
        }
    }

    private void applyRatio(float ratio, float topSpeed, float rateOfFire) {
        appliedRatio = ratio;
        statusText = null;
        afterimageColor = new Color(AFTERIMAGE_COLOR.getRed(), AFTERIMAGE_COLOR.getGreen(), AFTERIMAGE_COLOR.getBlue(),
                Math.round(AFTERIMAGE_COLOR.getAlpha() * Math.max(ratio, 0f)));
        MutableShipStatsAPI stats = ship.getMutableStats();
        float agility = Math.max(topSpeed * ratio, 0f);
        stats.getMaxSpeed().modifyFlat(modId, topSpeed * ratio);
        stats.getAcceleration().modifyPercent(modId, agility);
        stats.getTurnAcceleration().modifyPercent(modId, agility);
        stats.getBallisticRoFMult().modifyPercent(modId, rateOfFire * ratio);
        stats.getEnergyRoFMult().modifyPercent(modId, rateOfFire * ratio);
        stats.getMissileRoFMult().modifyPercent(modId, rateOfFire * ratio);
    }

    private void showStatus(CombatEngineAPI engine, float ratio, float topSpeed, float rateOfFire) {
        if (statusText == null) {
            statusTitle = I18n.forGameText(() -> Translation.text("combat.fluxScaled.title"));
            statusText = I18n.forGameText(() -> Translation.msg("combat.fluxScaled.status")
                    .arg("speed", signed(topSpeed * ratio)).arg("rateOfFire", signed(rateOfFire * ratio)).text());
        }
        engine.maintainStatusForPlayerShip(STATUS_KEY, STATUS_ICON, statusTitle, statusText, ratio < 0f);
    }

    private void showLowFluxEffects(CombatEngineAPI engine, float ratio) {
        afterimageTimer.advance(engine.getElapsedInLastFrame());
        if (afterimageTimer.intervalElapsed()) {
            ship.addAfterimage(afterimageColor, 0f, 0f, ship.getVelocity().x * AFTERIMAGE_DRIFT,
                    ship.getVelocity().y * AFTERIMAGE_DRIFT, 0f, 0f, 0f, AFTERIMAGE_DURATION, true, true, false);
        }
        ship.getEngineController().fadeToOtherColor(this, ENGINE_COLOR, null, 1f, ENGINE_TINT_STRENGTH * ratio);
    }

    private float penaltyScale() {
        if (penaltyScale == null) {
            float reduction = LostSectorCompat.hasAugmentedSystems(ship.getVariant()) ? magnitude(AUGMENTED_PENALTY_REDUCTION_KEY) : 0f;
            penaltyScale = Math.max(0f, 1f - reduction / 100f);
        }
        return penaltyScale;
    }

    private static String signed(float value) {
        return String.format(Locale.ROOT, "%+d", Math.round(value));
    }

    private float magnitude(String key) {
        return ship.getMutableStats().getDynamic().getValue(key, 0f);
    }
}
