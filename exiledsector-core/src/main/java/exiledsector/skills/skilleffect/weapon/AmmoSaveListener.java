package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.ShipCombatListener;

import java.util.List;
import java.util.Random;

final class AmmoSaveListener extends ShipCombatListener implements AdvanceableListener {

    static final String CHANCE_PERCENT_KEY = "exiledSector_ammoSaveChancePercent";

    private final Random random;
    private List<WeaponAPI> ammoWeapons;
    private int[] lastAmmo;

    AmmoSaveListener(ShipAPI ownerShip) {
        this(ownerShip, new Random());
    }

    AmmoSaveListener(ShipAPI ownerShip, Random random) {
        super(ownerShip);
        this.random = random;
    }

    static boolean savesAmmo(WeaponAPI weapon) {
        return weapon.usesAmmo() && !weapon.isDecorative()
                && (weapon.getType() == WeaponType.BALLISTIC || weapon.getType() == WeaponType.ENERGY);
    }

    @Override
    public void advance(float amount) {
        if (!ownerIsAliveNotHulk()) {
            return;
        }
        if (ammoWeapons == null) {
            ammoWeapons = ownerShip.getAllWeapons().stream().filter(AmmoSaveListener::savesAmmo).toList();
            lastAmmo = new int[ammoWeapons.size()];
            for (int i = 0; i < lastAmmo.length; i++) {
                lastAmmo[i] = ammoWeapons.get(i).getAmmo();
            }
        }
        float chance = magnitude(CHANCE_PERCENT_KEY) / 100f;
        for (int i = 0; i < lastAmmo.length; i++) {
            WeaponAPI weapon = ammoWeapons.get(i);
            int ammo = weapon.getAmmo();
            int spent = lastAmmo[i] - ammo;
            if (spent > 0 && chance > 0f) {
                int saved = savedShots(spent, chance);
                if (saved > 0) {
                    ammo = Math.min(weapon.getMaxAmmo(), ammo + saved);
                    weapon.setAmmo(ammo);
                }
            }
            lastAmmo[i] = ammo;
        }
    }

    private int savedShots(int spent, float chance) {
        int saved = 0;
        for (int shot = 0; shot < spent; shot++) {
            if (random.nextFloat() < chance) {
                saved++;
            }
        }
        return saved;
    }
}
