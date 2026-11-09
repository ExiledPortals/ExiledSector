package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.AmmoSpentTracker;
import exiledsector.skills.skilleffect.ShipCombatListener;

import java.util.Random;

final class AmmoSaveListener extends ShipCombatListener implements AdvanceableListener {

    static final String CHANCE_PERCENT_KEY = "exiledSector_ammoSaveChancePercent";

    private final Random random;
    private AmmoSpentTracker ammo;

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
        if (ammo == null) {
            ammo = new AmmoSpentTracker(ownerShip.getAllWeapons().stream().filter(AmmoSaveListener::savesAmmo).toList());
        }
        float chance = magnitude(CHANCE_PERCENT_KEY) / 100f;
        for (int index = 0; index < ammo.weaponCount(); index++) {
            int spent = ammo.spentSinceLastCheck(index);
            int saved = spent > 0 && chance > 0f ? savedShots(spent, chance) : 0;
            if (saved > 0) {
                ammo.refund(index, saved);
            }
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
