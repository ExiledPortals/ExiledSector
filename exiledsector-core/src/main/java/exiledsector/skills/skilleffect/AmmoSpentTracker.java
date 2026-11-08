package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.WeaponAPI;

import java.util.List;

public final class AmmoSpentTracker {

    private final List<WeaponAPI> weapons;
    private final int[] lastAmmo;
    private final float[] lastReloadProgress;

    public AmmoSpentTracker(List<WeaponAPI> weapons) {
        this.weapons = List.copyOf(weapons);
        lastAmmo = new int[this.weapons.size()];
        lastReloadProgress = new float[this.weapons.size()];
        for (int index = 0; index < lastAmmo.length; index++) {
            lastAmmo[index] = this.weapons.get(index).getAmmo();
            lastReloadProgress[index] = reloadProgress(this.weapons.get(index));
        }
    }

    public int weaponCount() {
        return weapons.size();
    }

    public int spentSinceLastCheck(int index) {
        WeaponAPI weapon = weapons.get(index);
        int ammo = weapon.getAmmo();
        float reload = reloadProgress(weapon);
        int expected = lastAmmo[index];
        if (ammo > expected && reload < lastReloadProgress[index]) {
            expected = Math.min(weapon.getMaxAmmo(), expected + Math.round(weapon.getAmmoTracker().getReloadSize()));
        }
        lastAmmo[index] = ammo;
        lastReloadProgress[index] = reload;
        return Math.max(0, expected - ammo);
    }

    public int spentByAllSinceLastCheck() {
        int spent = 0;
        for (int index = 0; index < lastAmmo.length; index++) {
            spent += spentSinceLastCheck(index);
        }
        return spent;
    }

    public void refund(int index, int shots) {
        WeaponAPI weapon = weapons.get(index);
        int ammo = Math.min(weapon.getMaxAmmo(), weapon.getAmmo() + shots);
        weapon.setAmmo(ammo);
        lastAmmo[index] = ammo;
    }

    private static float reloadProgress(WeaponAPI weapon) {
        return weapon.getAmmoTracker() == null ? 0f : weapon.getAmmoTracker().getReloadProgress();
    }
}
