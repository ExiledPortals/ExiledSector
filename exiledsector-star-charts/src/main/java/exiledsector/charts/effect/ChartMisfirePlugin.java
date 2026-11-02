package exiledsector.charts.effect;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.input.InputEventAPI;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class ChartMisfirePlugin extends BaseEveryFrameCombatPlugin {

    static final int GRACE_SECONDS = 60;
    static final float REFRESH_SECONDS = 1f;
    static final float MAX_MISFIRE_SECONDS = 4f;
    static final float FIRE_SECONDS = 0.5f;
    static final float SWING_TURN_RATE_MULT = 3f;
    private static final Logger LOG = Logger.getLogger(ChartMisfirePlugin.class);
    private static boolean failureReported;

    private final Random random;
    private final List<Misfire> misfires = new ArrayList<>();
    private CombatEngineAPI engine;
    private float misfiresPerWeaponSecond;
    private List<Armed> armedWeapons = List.of();
    private boolean refreshed;
    private float secondsSinceRefresh;
    private float secondsToMisfire;

    record Armed(ShipAPI ship, WeaponAPI weapon) {
    }

    private static final class Misfire {

        private final Armed armed;
        private final Float swingOffset;
        private float secondsLeft = MAX_MISFIRE_SECONDS;
        private boolean aimed;
        private boolean ready;
        private float firingSeconds = -1f;

        private Misfire(Armed armed, Float swingOffset) {
            this.armed = armed;
            this.swingOffset = swingOffset;
            aimed = swingOffset == null;
        }
    }

    public ChartMisfirePlugin() {
        this(new Random());
    }

    ChartMisfirePlugin(Random random) {
        this.random = random;
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        misfires.clear();
        armedWeapons = List.of();
        refreshed = false;
        secondsSinceRefresh = 0f;
        boolean campaignBattle = engine != null && engine.isInCampaign() && !engine.isSimulation() && !engine.isInCampaignSim();
        misfiresPerWeaponSecond = campaignBattle ? ChartFleetConditions.weaponMisfireRate() : 0f;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (misfiresPerWeaponSecond <= 0f || engine.isPaused() || amount <= 0f) {
            return;
        }
        if (engine.isCombatOver()) {
            endAll();
            return;
        }
        if (engine.getTotalElapsedTime(false) < GRACE_SECONDS) {
            return;
        }
        try {
            secondsSinceRefresh += amount;
            if (!refreshed || secondsSinceRefresh >= REFRESH_SECONDS) {
                refresh();
            }
            secondsToMisfire -= amount;
            while (secondsToMisfire <= 0f) {
                start(armedWeapons.get(random.nextInt(armedWeapons.size())));
                secondsToMisfire += secondsToNextMisfire();
            }
            advanceMisfires(amount);
        } catch (RuntimeException | LinkageError e) {
            misfiresPerWeaponSecond = 0f;
            endAll();
            if (!failureReported) {
                failureReported = true;
                LOG.error("[ExiledSector] The Star Chart's weapon misfires failed; they are off for this battle", e);
            }
        }
    }

    private void refresh() {
        refreshed = true;
        secondsSinceRefresh = 0f;
        armedWeapons = armedWeapons(engine.getShips());
        secondsToMisfire = secondsToNextMisfire();
    }

    private float secondsToNextMisfire() {
        if (armedWeapons.isEmpty()) {
            return Float.MAX_VALUE;
        }
        return (float) (-Math.log(1d - random.nextDouble()) / (misfiresPerWeaponSecond * armedWeapons.size()));
    }

    static List<Armed> armedWeapons(List<ShipAPI> ships) {
        List<Armed> armed = new ArrayList<>();
        for (ShipAPI ship : ships) {
            if (canMisfire(ship)) {
                for (WeaponAPI weapon : ship.getAllWeapons()) {
                    if (canMisfire(weapon)) {
                        armed.add(new Armed(ship, weapon));
                    }
                }
            }
        }
        return armed;
    }

    static boolean canMisfire(ShipAPI ship) {
        return ship != null && ship.getOwner() == ChartBattlePlugin.PLAYER_OWNER && ship.isAlive() && !ship.isHulk() && !ship.isFighter()
                && !ship.isShuttlePod();
    }

    static boolean canMisfire(WeaponAPI weapon) {
        return weapon != null && (weapon.getType() == WeaponType.BALLISTIC || weapon.getType() == WeaponType.ENERGY) && !weapon.isDecorative()
                && !weapon.isPermanentlyDisabled() && weapon.getSlot() != null && !weapon.getSlot().isSystemSlot();
    }

    private void start(Armed armed) {
        for (Misfire misfire : misfires) {
            if (misfire.armed.weapon() == armed.weapon()) {
                return;
            }
        }
        WeaponAPI weapon = armed.weapon();
        if (weapon.getSlot().isHardpoint()) {
            misfires.add(new Misfire(armed, null));
            return;
        }
        float arc = weapon.getArc();
        float offset = arc >= 360f ? random.nextFloat() * 360f - 180f : (random.nextFloat() - 0.5f) * arc;
        weapon.setSuspendAutomaticTurning(true);
        misfires.add(new Misfire(armed, offset));
    }

    private void advanceMisfires(float amount) {
        Iterator<Misfire> iterator = misfires.iterator();
        while (iterator.hasNext()) {
            Misfire misfire = iterator.next();
            if (!advance(misfire, amount)) {
                end(misfire);
                iterator.remove();
            }
        }
    }

    private static boolean advance(Misfire misfire, float amount) {
        ShipAPI ship = misfire.armed.ship();
        WeaponAPI weapon = misfire.armed.weapon();
        misfire.secondsLeft -= amount;
        if (misfire.secondsLeft <= 0f || !canMisfire(ship) || weapon.isDisabled()) {
            return false;
        }
        if (misfire.swingOffset != null) {
            float midArc = weapon.getSlot().computeMidArcAngle(ship);
            float maxTurn = weapon.getSpec().getTurnRate() * SWING_TURN_RATE_MULT * amount;
            misfire.aimed = misfire.aimed || swing(weapon, midArc, misfire.swingOffset, maxTurn);
            if (misfire.aimed) {
                weapon.setCurrAngle(midArc + misfire.swingOffset);
            }
        }
        if (!misfire.aimed) {
            weapon.setForceNoFireOneFrame(true);
            return true;
        }
        if (ship.isPhased()) {
            return true;
        }
        boolean continuousBeam = weapon.isBeam() && !weapon.isBurstBeam();
        if (misfire.firingSeconds < 0f) {
            if (misfire.ready && (continuousBeam ? weapon.getChargeLevel() >= 1f : weapon.isFiring())) {
                misfire.firingSeconds = 0f;
            }
            misfire.ready = misfire.ready || (weapon.getCooldownRemaining() <= 0f && !weapon.isFiring());
        } else {
            misfire.firingSeconds += amount;
            if (misfire.firingSeconds >= FIRE_SECONDS
                    && (continuousBeam || !weapon.isFiring() || weapon.getCooldownRemaining() > 0f)) {
                return false;
            }
        }
        if (misfire.ready) {
            weapon.setForceFireOneFrame(true);
        } else {
            weapon.setForceNoFireOneFrame(true);
        }
        return true;
    }

    static boolean swing(WeaponAPI weapon, float midArc, float offset, float maxTurn) {
        float current = weapon.getCurrAngle();
        float turn = weapon.getArc() >= 360f ? shortestTurn(current, midArc + offset) : offset - shortestTurn(midArc, current);
        if (Math.abs(turn) <= maxTurn) {
            return true;
        }
        weapon.setCurrAngle(current + Math.signum(turn) * maxTurn);
        return false;
    }

    static float shortestTurn(float from, float to) {
        float turn = (to - from) % 360f;
        if (turn > 180f) {
            return turn - 360f;
        }
        return turn < -180f ? turn + 360f : turn;
    }

    private static void end(Misfire misfire) {
        if (misfire.swingOffset != null) {
            misfire.armed.weapon().setSuspendAutomaticTurning(false);
        }
    }

    private void endAll() {
        misfires.forEach(ChartMisfirePlugin::end);
        misfires.clear();
    }
}
