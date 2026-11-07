package exiledsector.skills.skilleffect.shipsystem;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.ScaledBonus;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.StatTarget;
import exiledsector.skills.skilleffect.TaperedBonus;
import exiledsector.skills.skilleffect.weapon.RiftSeries;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Random;

final class TemporalExhaustListener extends ShipCombatListener implements AdvanceableListener {

    static final String TIMEFLOW_PERCENT_KEY = "exiledSector_temporalExhaustTimeflowPercent";
    static final float TAPER_SECONDS = 4f;
    static final float COOLDOWN_SECONDS = 30f;
    static final int RIFT_COUNT = 8;
    static final float RIFT_DAMAGE = 100f;
    static final float MIN_RIFT_DISTANCE_BEHIND = 40f;
    static final float MAX_RIFT_DISTANCE_BEHIND = 160f;
    static final float RIFT_SIDEWAYS_SCATTER = 50f;
    private static final float SECONDS_BETWEEN_RIFTS = TAPER_SECONDS / RIFT_COUNT;
    private static final String MOD_ID_PREFIX = "exiledSector_temporalExhaust_";
    private static final ScaledBonus TIMEFLOW_BONUS = new ScaledBonus(
            ScaledBonus.percent(StatTarget.liveStat(MutableShipStatsAPI::getTimeMult), TIMEFLOW_PERCENT_KEY));

    private final TaperedBonus timeflowBonus;
    private final Random random;
    private boolean systemWasActive;
    private float cooldownSecondsLeft;
    private RiftSeries riftTrail;

    TemporalExhaustListener(ShipAPI ownerShip) {
        this(ownerShip, new Random());
    }

    TemporalExhaustListener(ShipAPI ownerShip, Random random) {
        super(ownerShip, MOD_ID_PREFIX);
        this.random = random;
        this.timeflowBonus = new TaperedBonus(TIMEFLOW_BONUS, TAPER_SECONDS, ownerShip.getMutableStats(), modId);
    }

    @Override
    public void advance(float amount) {
        if (!ownerIsAliveNotHulk()) {
            riftTrail = null;
            timeflowBonus.stop();
            return;
        }
        cooldownSecondsLeft = Math.max(0f, cooldownSecondsLeft - amount);
        boolean systemActive = systemActive(ownerShip.getSystem());
        if (systemActive && !systemWasActive && cooldownSecondsLeft <= 0f && magnitude(TIMEFLOW_PERCENT_KEY) > 0f) {
            start();
        }
        systemWasActive = systemActive;
        timeflowBonus.advance(amount);
        if (riftTrail != null) {
            riftTrail.advance(amount);
        }
    }

    private static boolean systemActive(ShipSystemAPI system) {
        return system != null && system.isActive();
    }

    private void start() {
        cooldownSecondsLeft = COOLDOWN_SECONDS;
        timeflowBonus.start();
        riftTrail = new RiftSeries(ownerShip, RIFT_COUNT, SECONDS_BETWEEN_RIFTS, RIFT_DAMAGE, this::pointBehindEngines, random);
    }

    private Vector2f pointBehindEngines() {
        double rearRadians = Math.toRadians(ownerShip.getFacing() + 180f);
        float rearX = (float) Math.cos(rearRadians);
        float rearY = (float) Math.sin(rearRadians);
        ShipEngineAPI engine = randomEngine();
        Vector2f origin = engine == null ? ownerShip.getLocation() : engine.getLocation();
        float distanceBehind = MIN_RIFT_DISTANCE_BEHIND + random.nextFloat() * (MAX_RIFT_DISTANCE_BEHIND - MIN_RIFT_DISTANCE_BEHIND);
        if (engine == null) {
            distanceBehind += ownerShip.getCollisionRadius();
        }
        float sideways = (random.nextFloat() * 2f - 1f) * RIFT_SIDEWAYS_SCATTER;
        return new Vector2f(origin.x + rearX * distanceBehind - rearY * sideways, origin.y + rearY * distanceBehind + rearX * sideways);
    }

    private ShipEngineAPI randomEngine() {
        ShipEngineControllerAPI engineController = ownerShip.getEngineController();
        List<ShipEngineAPI> mainEngines = engineController == null ? List.of()
                : engineController.getShipEngines().stream().filter(engine -> !engine.isSystemActivated()).toList();
        return mainEngines.isEmpty() ? null : mainEngines.get(random.nextInt(mainEngines.size()));
    }
}
