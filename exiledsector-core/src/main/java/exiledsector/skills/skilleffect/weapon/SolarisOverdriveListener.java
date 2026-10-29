package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.PlayerStatus;
import exiledsector.skills.skilleffect.ScaledBonus;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.StatMode;

import java.awt.Color;
import java.util.EnumSet;

final class SolarisOverdriveListener extends ShipCombatListener implements AdvanceableListener {

    static final String SECONDS_KEY = "exiledLib_solarisOverdriveSeconds";
    static final String PROJECTILE_SPEED_PERCENT_KEY = "exiledLib_solarisOverdriveProjectileSpeedPercent";
    static final String RATE_OF_FIRE_PERCENT_KEY = "exiledLib_solarisOverdriveRateOfFirePercent";
    static final float COOLDOWN_SECONDS = 60f;
    private static final String MOD_ID_PREFIX = "exiledLib_solarisOverdrive_";
    private static final String STATUS_ICON = "graphics/icons/hullsys/ammo_feeder.png";
    private static final Color WEAPON_GLOW_COLOR = new Color(255, 170, 60, 200);
    private static final EnumSet<WeaponAPI.WeaponType> GLOWING_WEAPONS = EnumSet.of(WeaponAPI.WeaponType.BALLISTIC,
            WeaponAPI.WeaponType.ENERGY, WeaponAPI.WeaponType.MISSILE);
    private static final ScaledBonus OVERDRIVE_BONUS = new ScaledBonus(
            ScaledBonus.fixed(WeaponStatFamily.FLUX_COST.target(WeaponScope.ALL), StatMode.MULT, -100f),
            ScaledBonus.percent(WeaponStatFamily.PROJECTILE_SPEED.target(WeaponScope.ALL), PROJECTILE_SPEED_PERCENT_KEY),
            ScaledBonus.percent(WeaponStatFamily.FIRE_RATE.target(WeaponScope.ALL), RATE_OF_FIRE_PERCENT_KEY));

    private final PlayerStatus status;
    private boolean systemWasActive;
    private float remainingSeconds;
    private float cooldownRemaining;

    SolarisOverdriveListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
        status = new PlayerStatus(modId, STATUS_ICON, "combat.solarisOverdrive");
    }

    boolean overdriven() {
        return remainingSeconds > 0f;
    }

    @Override
    public void advance(float amount) {
        if (!ownerIsAliveNotHulk()) {
            if (overdriven()) {
                end();
            }
            return;
        }
        cooldownRemaining = Math.max(0f, cooldownRemaining - amount);
        boolean systemActive = systemActive(ownerShip.getSystem());
        if (systemActive && !systemWasActive && cooldownRemaining <= 0f) {
            start();
        }
        systemWasActive = systemActive;
        if (!overdriven()) {
            return;
        }
        remainingSeconds -= amount;
        if (!overdriven()) {
            end();
            return;
        }
        ownerShip.setWeaponGlow(1f, WEAPON_GLOW_COLOR, GLOWING_WEAPONS);
        showStatus();
    }

    static boolean systemActive(ShipSystemAPI system) {
        return system != null && system.isActive();
    }

    private void start() {
        float seconds = magnitude(SECONDS_KEY);
        if (seconds <= 0f) {
            return;
        }
        remainingSeconds = seconds;
        cooldownRemaining = COOLDOWN_SECONDS;
        OVERDRIVE_BONUS.apply(ownerShip.getMutableStats(), modId, 1f);
    }

    private void end() {
        remainingSeconds = 0f;
        OVERDRIVE_BONUS.remove(ownerShip.getMutableStats(), modId);
        ownerShip.setWeaponGlow(0f, WEAPON_GLOW_COLOR, GLOWING_WEAPONS);
    }

    private void showStatus() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine != null && engine.getPlayerShip() == ownerShip) {
            if (status.needsText()) {
                status.setText(() -> Translation.text("combat.solarisOverdrive.status"));
            }
            status.show(engine, false);
        }
    }
}
