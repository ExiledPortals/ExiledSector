package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.i18n.Translation;

import java.awt.Color;
import java.util.EnumSet;

final class SolarisOverdriveListener extends ShipCombatListener implements AdvanceableListener {

    static final String SECONDS_KEY = "exiledSector_solarisOverdriveSeconds";
    static final float COOLDOWN_SECONDS = 20f;
    private static final String MOD_ID_PREFIX = "exiledSector_solarisOverdrive_";
    private static final String STATUS_ICON = "graphics/icons/hullsys/ammo_feeder.png";
    private static final Color WEAPON_GLOW_COLOR = new Color(255, 170, 60, 200);
    private static final EnumSet<WeaponAPI.WeaponType> GLOWING_WEAPONS = EnumSet.of(WeaponAPI.WeaponType.BALLISTIC,
            WeaponAPI.WeaponType.ENERGY, WeaponAPI.WeaponType.MISSILE);
    private static final ScaledBonus FREE_WEAPON_FLUX = new ScaledBonus(
            ScaledBonus.fixed(WeaponStatFamily.FLUX_COST.target(WeaponScope.ALL), StatMode.MULT, -100f));

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
        FREE_WEAPON_FLUX.apply(ownerShip.getMutableStats(), modId, 1f);
    }

    private void end() {
        remainingSeconds = 0f;
        FREE_WEAPON_FLUX.remove(ownerShip.getMutableStats(), modId);
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
