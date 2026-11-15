package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;
import exiledsector.skills.skilleffect.ShipCombatListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

final class ReserveRacksListener extends ShipCombatListener implements AdvanceableListener {

    static final String RELOAD_PERCENT_KEY = "exiledSector_reserveRacksReloadPercent";

    private final IntervalUtil checkInterval = new IntervalUtil(0.2f, 0.4f);
    private final Set<WeaponAPI> reloadedLaunchers = Collections.newSetFromMap(new IdentityHashMap<>());
    private List<WeaponAPI> launchers;

    ReserveRacksListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    static boolean holdsReserve(WeaponAPI weapon) {
        return weapon.getType() == WeaponAPI.WeaponType.MISSILE && weapon.usesAmmo() && !weapon.isDecorative()
                && !weapon.getSlot().isSystemSlot() && !weapon.getSlot().isDecorative() && weapon.getMaxAmmo() > 0;
    }

    static int reloadedAmmo(int maxAmmo, float reloadPercent) {
        return Math.max(1, Math.min(maxAmmo, Math.round(maxAmmo * reloadPercent / 100f)));
    }

    @Override
    public void advance(float amount) {
        if (!ownerIsAliveNotHulk()) {
            return;
        }
        checkInterval.advance(amount);
        if (!checkInterval.intervalElapsed()) {
            return;
        }
        float reloadPercent = magnitude(RELOAD_PERCENT_KEY);
        if (reloadPercent <= 0f) {
            return;
        }
        if (launchers == null) {
            launchers = launchersOf(ownerShip);
        }
        for (int i = 0; i < launchers.size(); i++) {
            WeaponAPI launcher = launchers.get(i);
            if (launcher.getAmmo() <= 0 && reloadedLaunchers.add(launcher)) {
                launcher.setAmmo(reloadedAmmo(launcher.getMaxAmmo(), reloadPercent));
            }
        }
    }

    private static List<WeaponAPI> launchersOf(ShipAPI ship) {
        List<WeaponAPI> found = new ArrayList<>();
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (holdsReserve(weapon)) {
                found.add(weapon);
            }
        }
        return found;
    }
}
