package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

final class FlareBaysListener extends ShipCombatListener implements AdvanceableListener {

    static final String FLARES_PER_BURST_KEY = "exiledSector_flareBaysFlaresPerBurst";
    static final float DETECTION_RANGE = 1000f;
    static final float COOLDOWN_SECONDS = 10f;
    static final String FLARE_WEAPON_ID = "flarelauncher1";
    private static final String FLARE_SOUND_ID = "launch_flare_1";
    private static final float CHECK_INTERVAL_SECONDS = 0.25f;
    private static final float SECONDS_BETWEEN_FLARES = 0.1f;
    private static final float FLARE_SPREAD_DEGREES = 60f;
    private static final int MAX_FLARES_PER_BURST = 20;

    private final Random random;
    private float secondsUntilCheck;
    private float cooldownSecondsLeft;
    private float secondsUntilNextFlare;
    private int flaresLeft;

    FlareBaysListener(ShipAPI ownerShip) {
        this(ownerShip, new Random());
    }

    FlareBaysListener(ShipAPI ownerShip, Random random) {
        super(ownerShip);
        this.random = random;
    }

    static boolean homesOn(Object nearby, ShipAPI ship) {
        return nearby instanceof MissileAPI missile && !missile.isFlare() && !missile.isDecoyFlare() && !missile.isFizzling()
                && !missile.didDamage() && CombatQueries.isHostileOwner(ship, missile.getOwner())
                && missile.getMissileAI() instanceof GuidedMissileAI guided && guided.getTarget() == ship;
    }

    static int flaresPerBurst(float magnitude) {
        return Math.min(MAX_FLARES_PER_BURST, Math.max(0, Math.round(magnitude)));
    }

    @Override
    public void advance(float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || !ownerIsAliveNotHulk() || ownerShip.isPhased()) {
            flaresLeft = 0;
            return;
        }
        cooldownSecondsLeft = Math.max(0f, cooldownSecondsLeft - amount);
        if (flaresLeft > 0) {
            launchDueFlares(engine, amount);
            return;
        }
        secondsUntilCheck -= amount;
        if (secondsUntilCheck > 0f || cooldownSecondsLeft > 0f) {
            return;
        }
        secondsUntilCheck = CHECK_INTERVAL_SECONDS;
        int burst = flaresPerBurst(magnitude(FLARES_PER_BURST_KEY));
        if (burst > 0 && CombatQueries.anyNear(engine.getMissileGrid(), ownerShip.getLocation(), DETECTION_RANGE,
                nearby -> homesOn(nearby, ownerShip))) {
            flaresLeft = burst;
            secondsUntilNextFlare = 0f;
            cooldownSecondsLeft = COOLDOWN_SECONDS;
            launchDueFlares(engine, 0f);
        }
    }

    private void launchDueFlares(CombatEngineAPI engine, float amount) {
        secondsUntilNextFlare -= amount;
        while (flaresLeft > 0 && secondsUntilNextFlare <= 0f) {
            launchFlare(engine);
            flaresLeft--;
            secondsUntilNextFlare += SECONDS_BETWEEN_FLARES;
        }
    }

    private void launchFlare(CombatEngineAPI engine) {
        float facing = ownerShip.getFacing() + 180f + (random.nextFloat() - 0.5f) * FLARE_SPREAD_DEGREES;
        Vector2f launchPoint = new Vector2f(ownerShip.getLocation());
        engine.spawnProjectile(ownerShip, null, FLARE_WEAPON_ID, launchPoint, facing, new Vector2f(ownerShip.getVelocity()));
        Global.getSoundPlayer().playSound(FLARE_SOUND_ID, 1f, 1f, launchPoint, ownerShip.getVelocity());
    }
}
