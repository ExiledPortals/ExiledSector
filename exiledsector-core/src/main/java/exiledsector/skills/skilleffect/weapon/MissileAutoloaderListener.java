package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.hullmods.MissileAutoloader;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.TimeoutTracker;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.PlayerStatus;
import exiledsector.skills.skilleffect.ShipCombatListener;

final class MissileAutoloaderListener extends ShipCombatListener implements AdvanceableListener {

    static final String CAPACITY_PERCENT_KEY = "exiledSector_missileAutoloaderCapacityPercent";
    static final String COOLDOWN_SECONDS_KEY = "exiledSector_missileAutoloaderCooldownSeconds";
    private static final String STATUS_KEY = "exiledSector_missileAutoloader";
    private static final String STATUS_ICON = "graphics/icons/tactical/cr_tactical.png";
    private static final float EXHAUSTED_STATUS_SECONDS = 5f;
    private static final float EMPTY_CAPACITY = 0.05f;

    private final IntervalUtil reloadInterval = new IntervalUtil(0.2f, 0.4f);
    private final TimeoutTracker<WeaponAPI> cooldowns = new TimeoutTracker<>();
    private final PlayerStatus status = new PlayerStatus(STATUS_KEY, STATUS_ICON, "combat.missileAutoloader");
    private float capacityLeft = -1f;
    private float exhaustedStatusSeconds = EXHAUSTED_STATUS_SECONDS;

    MissileAutoloaderListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public void advance(float amount) {
        if (!ownerIsAliveNotHulk()) {
            return;
        }
        if (capacityLeft < 0f) {
            capacityLeft = startingCapacity();
        }
        if (capacityLeft <= EMPTY_CAPACITY) {
            if (capacityLeft > 0f) {
                status.invalidate();
            }
            capacityLeft = 0f;
            exhaustedStatusSeconds -= amount;
            if (exhaustedStatusSeconds <= 0f) {
                return;
            }
        }
        cooldowns.advance(amount * ownerShip.getMutableStats().getMissileRoFMult().getModifiedValue());
        for (WeaponAPI weapon : cooldowns.getItems()) {
            weapon.setRemainingCooldownTo(weapon.getCooldown());
        }
        reloadInterval.advance(amount);
        if (reloadInterval.intervalElapsed()) {
            reloadEmptyWeapons();
        }
        showStatus();
    }

    private float startingCapacity() {
        MissileAutoloader.ReloadCapacityData capacityData = MissileAutoloader.getCapacityData(ownerShip);
        if (capacityData == null) {
            exhaustedStatusSeconds = 0f;
            return 0f;
        }
        return capacityData.capacity * magnitude(CAPACITY_PERCENT_KEY) / 100f;
    }

    private void reloadEmptyWeapons() {
        float cooldownSeconds = magnitude(COOLDOWN_SECONDS_KEY);
        for (WeaponAPI weapon : ownerShip.getAllWeapons()) {
            if (capacityLeft <= 0f) {
                return;
            }
            if (!MissileAutoloader.isAffected(weapon) || cooldowns.contains(weapon) || weapon.getAmmo() > 0) {
                continue;
            }
            float reloadCost = MissileAutoloader.getReloadCost(weapon, ownerShip);
            weapon.setAmmo(reloadSize(weapon, reloadCost));
            if (cooldownSeconds > 0f) {
                cooldowns.set(weapon, cooldownSeconds);
            }
            capacityLeft = Math.max(0f, capacityLeft - reloadCost);
            status.invalidate();
        }
    }

    private int reloadSize(WeaponAPI weapon, float reloadCost) {
        float reloadSize = weapon.getSpec().getMaxAmmo();
        if (reloadCost <= capacityLeft) {
            return Math.round(reloadSize);
        }
        float salvoSize = Math.max(1, weapon.getSpec().getBurstSize());
        float partialSalvos = (float) Math.ceil(reloadSize * capacityLeft / reloadCost / salvoSize);
        return Math.round(partialSalvos * salvoSize);
    }

    private void showStatus() {
        boolean exhausted = capacityLeft <= 0f;
        status.showIfPlayer(ownerShip, exhausted, () -> exhausted
                ? Translation.text("combat.missileAutoloader.exhausted")
                : Translation.msg("combat.missileAutoloader.remaining").arg("value", Misc.getRoundedValueOneAfterDecimalIfNotWhole(capacityLeft)).text());
    }
}
