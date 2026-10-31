package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;
import exiledsector.compat.LostSectorCompat;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.PlayerStatus;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.StatMode;
import exiledsector.skills.skilleffect.StatTarget;
import exiledsector.skills.skilleffect.weapon.WeaponScope;
import exiledsector.skills.skilleffect.weapon.WeaponStatFamily;

import java.awt.Color;
import java.util.Locale;

final class FluxScaledVolatilityListener extends ShipCombatListener implements AdvanceableListener {

    static final String TOP_SPEED_KEY = "exiledLib_fluxScaledTopSpeed";
    static final String RATE_OF_FIRE_KEY = "exiledLib_fluxScaledRateOfFire";
    static final String AUGMENTED_PENALTY_REDUCTION_KEY = "exiledLib_fluxScaledAugmentedPenaltyReduction";
    private static final String MOD_ID_PREFIX = "exiledLib_fluxScaled_";
    private static final String STATUS_KEY = "exiledLib_fluxScaledStatus";
    private static final String STATUS_ICON = "graphics/icons/hullsys/high_energy_focus.png";
    private static final float RATIO_STEP = 0.02f;
    private static final StatTarget RATE_OF_FIRE = WeaponStatFamily.FIRE_RATE.target(WeaponScope.ALL);
    static final float AFTERIMAGE_INTERVAL = 0.05f;
    private static final float AFTERIMAGE_DRIFT = -0.8f;
    private static final float AFTERIMAGE_DURATION = 0.35f;
    private static final float ENGINE_TINT_STRENGTH = 0.5f;
    private static final Color AFTERIMAGE_COLOR = new Color(100, 42, 201, 160);
    private static final Color ENGINE_COLOR = new Color(100, 42, 201, 255);

    private final IntervalUtil afterimageTimer = new IntervalUtil(AFTERIMAGE_INTERVAL, AFTERIMAGE_INTERVAL);
    private float appliedRatio = Float.NaN;
    private Float penaltyScale;
    private Color afterimageColor = AFTERIMAGE_COLOR;
    private final PlayerStatus status = new PlayerStatus(STATUS_KEY, STATUS_ICON, "combat.fluxScaled.title");

    FluxScaledVolatilityListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    static float ratio(FluxTrackerAPI flux) {
        float level = flux.isOverloaded() || flux.isVenting() ? 1f : flux.getFluxLevel();
        float ratio = Math.max(-1f, Math.min(1f, 1f - 2f * level));
        return Math.round(ratio / RATIO_STEP) * RATIO_STEP;
    }

    @Override
    public void advance(float amount) {
        if (!ownerIsAliveNotHulk()) {
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }
        float ratio = ratio(ownerShip.getFluxTracker());
        float scale = ratio < 0f ? penaltyScale() : 1f;
        float topSpeed = magnitude(TOP_SPEED_KEY) * scale;
        float rateOfFire = magnitude(RATE_OF_FIRE_KEY) * scale;
        if (ratio != appliedRatio) {
            applyRatio(ratio, topSpeed, rateOfFire);
        }
        if (engine.getPlayerShip() == ownerShip) {
            showStatus(ratio, topSpeed, rateOfFire);
        }
        if (ratio > 0f) {
            showLowFluxEffects(engine, ratio);
        }
    }

    private void applyRatio(float ratio, float topSpeed, float rateOfFire) {
        appliedRatio = ratio;
        status.invalidate();
        afterimageColor = new Color(AFTERIMAGE_COLOR.getRed(), AFTERIMAGE_COLOR.getGreen(), AFTERIMAGE_COLOR.getBlue(),
                Math.round(AFTERIMAGE_COLOR.getAlpha() * Math.max(ratio, 0f)));
        MutableShipStatsAPI stats = ownerShip.getMutableStats();
        float agility = Math.max(topSpeed * ratio, 0f);
        stats.getMaxSpeed().modifyFlat(modId, topSpeed * ratio);
        stats.getAcceleration().modifyPercent(modId, agility);
        stats.getTurnAcceleration().modifyPercent(modId, agility);
        RATE_OF_FIRE.apply(stats, modId, StatMode.PERCENT, rateOfFire * ratio);
    }

    private void showStatus(float ratio, float topSpeed, float rateOfFire) {
        status.showIfPlayer(ownerShip, ratio < 0f, () -> Translation.msg("combat.fluxScaled.status")
                .arg("speed", signed(topSpeed * ratio)).arg("rateOfFire", signed(rateOfFire * ratio)).text());
    }

    private void showLowFluxEffects(CombatEngineAPI engine, float ratio) {
        afterimageTimer.advance(engine.getElapsedInLastFrame());
        if (afterimageTimer.intervalElapsed()) {
            ownerShip.addAfterimage(afterimageColor, 0f, 0f, ownerShip.getVelocity().x * AFTERIMAGE_DRIFT,
                    ownerShip.getVelocity().y * AFTERIMAGE_DRIFT, 0f, 0f, 0f, AFTERIMAGE_DURATION, true, true, false);
        }
        ownerShip.getEngineController().fadeToOtherColor(this, ENGINE_COLOR, null, 1f, ENGINE_TINT_STRENGTH * ratio);
    }

    private float penaltyScale() {
        if (penaltyScale == null) {
            float reduction = LostSectorCompat.hasAugmentedSystems(ownerShip.getVariant()) ? magnitude(AUGMENTED_PENALTY_REDUCTION_KEY) : 0f;
            penaltyScale = Math.max(0f, 1f - reduction / 100f);
        }
        return penaltyScale;
    }

    private static String signed(float value) {
        return String.format(Locale.ROOT, "%+d", Math.round(value));
    }
}
