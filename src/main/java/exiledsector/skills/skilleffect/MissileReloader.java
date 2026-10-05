package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class MissileReloader {

    private static final String RELOADERS_KEY = "exiledSector_missileReloaders";

    private final WeaponAPI[] launchers;
    private final float[] baseAmmo;
    private final float[] progress;

    private MissileReloader(List<WeaponAPI> launchers) {
        this.launchers = launchers.toArray(new WeaponAPI[0]);
        this.baseAmmo = new float[this.launchers.length];
        this.progress = new float[this.launchers.length];
        for (int i = 0; i < this.launchers.length; i++) {
            baseAmmo[i] = this.launchers[i].getSpec().getMaxAmmo();
        }
    }

    static void advance(ShipAPI ship, String modId, float percentPerMinute, float amount) {
        if (amount <= 0f || percentPerMinute <= 0f || ship.isHulk() || !ship.isAlive()) {
            return;
        }
        reloaderFor(ship, modId).reload(amount * percentPerMinute / 100f / 60f);
    }

    static boolean reloads(WeaponAPI weapon) {
        return weapon.getType() == WeaponAPI.WeaponType.MISSILE && weapon.usesAmmo() && !weapon.isDecorative()
                && !weapon.getSlot().isSystemSlot() && !weapon.getSlot().isDecorative()
                && weapon.getSpec().getAmmoPerSecond() <= 0f && weapon.getSpec().getMaxAmmo() > 0;
    }

    private static MissileReloader reloaderFor(ShipAPI ship, String modId) {
        Reloaders reloaders;
        Map<String, Object> customData = ship.getCustomData();
        if (customData != null && customData.get(RELOADERS_KEY) instanceof Reloaders existing) {
            reloaders = existing;
        } else {
            reloaders = new Reloaders();
            ship.setCustomData(RELOADERS_KEY, reloaders);
        }
        return reloaders.byModId.computeIfAbsent(modId, id -> new MissileReloader(launchersOf(ship)));
    }

    private static List<WeaponAPI> launchersOf(ShipAPI ship) {
        List<WeaponAPI> launchers = new ArrayList<>();
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (reloads(weapon)) {
                launchers.add(weapon);
            }
        }
        return launchers;
    }

    private void reload(float fractionOfBaseAmmo) {
        for (int i = 0; i < launchers.length; i++) {
            WeaponAPI launcher = launchers[i];
            int maxAmmo = launcher.getMaxAmmo();
            int ammo = launcher.getAmmo();
            if (ammo >= maxAmmo) {
                progress[i] = 0f;
                continue;
            }
            progress[i] += fractionOfBaseAmmo * baseAmmo[i];
            if (progress[i] >= 1f) {
                int rounds = (int) progress[i];
                progress[i] -= rounds;
                launcher.setAmmo(Math.min(maxAmmo, ammo + rounds));
            }
        }
    }

    private static final class Reloaders {
        private final Map<String, MissileReloader> byModId = new HashMap<>();
    }
}
