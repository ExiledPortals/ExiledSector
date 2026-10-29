package exiledsector.skills.skilleffect.special;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.ScaledBonus;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.logistics.FleetCrewLedger;

import java.util.List;
import java.util.Random;

import static exiledsector.skills.skilleffect.ScaledBonus.percent;
import static exiledsector.skills.skilleffect.StatTarget.liveBonus;
import static exiledsector.skills.skilleffect.StatTarget.liveStat;

final class LiveMunitionsListener extends ShipCombatListener implements AdvanceableListener {

    static final String CREW_DEATH_CHANCE_PERCENT_PER_STACK_KEY = "exiledLib_heartlessCrewDeathChancePerStack";
    static final String MISSILE_DAMAGE_PERCENT_PER_STACK_KEY = "exiledLib_heartlessMissileDamagePercentPerStack";
    static final String MISSILE_SPEED_PERCENT_PER_STACK_KEY = "exiledLib_heartlessMissileSpeedPercentPerStack";
    private static final String MOD_ID_PREFIX = "exiledLib_liveMunitions_";
    private static final ScaledBonus MISSILE_BONUS = new ScaledBonus(
            percent(liveStat(MutableShipStatsAPI::getMissileWeaponDamageMult), MISSILE_DAMAGE_PERCENT_PER_STACK_KEY),
            percent(liveBonus(MutableShipStatsAPI::getMissileMaxSpeedBonus), MISSILE_SPEED_PERCENT_PER_STACK_KEY));

    private final Random random;
    private List<WeaponAPI> missileWeapons;
    private int[] lastAmmo;
    private float[] lastReloadProgress;
    private HeartlessStacks heartlessStacks;
    private final FleetCrewLedger.Lazy crewLedger = new FleetCrewLedger.Lazy();
    private final ScaledBonus.Tracker missileBonus = MISSILE_BONUS.tracker();

    LiveMunitionsListener(ShipAPI ownerShip) {
        this(ownerShip, new Random());
    }

    LiveMunitionsListener(ShipAPI ownerShip, Random random) {
        super(ownerShip, MOD_ID_PREFIX);
        this.random = random;
    }

    @Override
    public void advance(float amount) {
        if (!ownerIsAliveNotHulk()) {
            return;
        }
        if (heartlessStacks == null) {
            heartlessStacks = HeartlessStacks.of(ownerShip);
            missileWeapons = ownerShip.getAllWeapons().stream()
                    .filter(w -> w.getType() == WeaponAPI.WeaponType.MISSILE && w.usesAmmo()).toList();
            lastAmmo = new int[missileWeapons.size()];
            lastReloadProgress = new float[missileWeapons.size()];
            for (int i = 0; i < lastAmmo.length; i++) {
                lastAmmo[i] = missileWeapons.get(i).getAmmo();
                lastReloadProgress[i] = reloadProgress(missileWeapons.get(i));
            }
        }
        int fired = missilesFiredSinceLastFrame();
        int stackCount = heartlessStacks.stacks();
        if (fired > 0 && stackCount > 0 && crewLeft()) {
            sacrificeCrew(fired, stackCount);
        }
        missileBonus.apply(ownerShip.getMutableStats(), modId, crewLeft() ? stackCount : 0);
    }

    private int missilesFiredSinceLastFrame() {
        int fired = 0;
        for (int i = 0; i < lastAmmo.length; i++) {
            WeaponAPI weapon = missileWeapons.get(i);
            int ammo = weapon.getAmmo();
            float reload = reloadProgress(weapon);
            int expected = lastAmmo[i];
            if (ammo > lastAmmo[i] && reload < lastReloadProgress[i]) {
                expected = Math.min(weapon.getMaxAmmo(), lastAmmo[i] + Math.round(weapon.getAmmoTracker().getReloadSize()));
            }
            if (ammo < expected) {
                fired += expected - ammo;
            }
            lastAmmo[i] = ammo;
            lastReloadProgress[i] = reload;
        }
        return fired;
    }

    private static float reloadProgress(WeaponAPI weapon) {
        return weapon.getAmmoTracker() == null ? 0f : weapon.getAmmoTracker().getReloadProgress();
    }

    private boolean crewLeft() {
        return !FleetCrewLedger.usesFleetCrew(ownerShip) || crewLedger.get().hasCrew();
    }

    private void sacrificeCrew(int fired, int stackCount) {
        float chance = magnitude(CREW_DEATH_CHANCE_PERCENT_PER_STACK_KEY) * stackCount / 100f;
        if (chance <= 0f || !FleetCrewLedger.usesFleetCrew(ownerShip)) {
            return;
        }
        FleetCrewLedger ledger = crewLedger.get();
        for (int i = 0; i < fired && ledger.hasCrew(); i++) {
            if (random.nextFloat() < chance) {
                ledger.sacrifice();
            }
        }
    }
}
