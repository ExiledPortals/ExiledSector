package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.loading.WeaponSpecAPI;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

final class MissilePrefabrication {

    private static final String STOCKED_WEAPONS_KEY = "exiledSector_prefabricatedMissileWeapons";

    private MissilePrefabrication() {
    }

    static void stock(ShipAPI ship, float reloadSeconds) {
        if (reloadSeconds <= 0f) {
            return;
        }
        Set<WeaponAPI> stocked = stockedWeapons(ship);
        float regenMult = ship.getMutableStats().getMissileAmmoRegenMult().getModifiedValue();
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (!qualifies(weapon) || !stocked.add(weapon)) {
                continue;
            }
            int extraAmmo = extraAmmo(weapon.getSpec(), reloadSeconds, regenMult);
            weapon.setMaxAmmo(weapon.getMaxAmmo() + extraAmmo);
            weapon.getAmmoTracker().setAmmo(weapon.getAmmo() + extraAmmo);
            weapon.getAmmoTracker().setAmmoPerSecond(0f);
        }
    }

    static boolean qualifies(WeaponAPI weapon) {
        return weapon.getType() == WeaponType.MISSILE && weapon.getSlot() != null && !weapon.getSlot().isBuiltIn() && weapon.usesAmmo()
                && weapon.getSpec().getAmmoPerSecond() > 0f;
    }

    static int extraAmmo(WeaponSpecAPI spec, float reloadSeconds, float regenMult) {
        float reloadSize = Math.max(1f, spec.getReloadSize());
        float ammo = spec.getAmmoPerSecond() * reloadSeconds * regenMult;
        return Math.round((float) Math.ceil(ammo / reloadSize) * reloadSize);
    }

    private static Set<WeaponAPI> stockedWeapons(ShipAPI ship) {
        if (ship.getCustomData().get(STOCKED_WEAPONS_KEY) instanceof StockedWeapons existing) {
            return existing.weapons;
        }
        StockedWeapons created = new StockedWeapons();
        ship.setCustomData(STOCKED_WEAPONS_KEY, created);
        return created.weapons;
    }

    private static final class StockedWeapons {
        private final Set<WeaponAPI> weapons = Collections.newSetFromMap(new IdentityHashMap<>());
    }
}
