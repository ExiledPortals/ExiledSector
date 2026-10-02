package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;

import java.util.List;
import java.util.Random;

final class LiveMunitionsListener implements AdvanceableListener {

    static final String CREW_DEATH_CHANCE_PERCENT_PER_STACK_KEY = "exiledSector_heartlessCrewDeathChancePerStack";
    static final String MISSILE_DAMAGE_PERCENT_PER_STACK_KEY = "exiledSector_heartlessMissileDamagePercentPerStack";
    static final String MISSILE_SPEED_PERCENT_PER_STACK_KEY = "exiledSector_heartlessMissileSpeedPercentPerStack";
    private static final String MOD_ID_PREFIX = "exiledSector_liveMunitions_";

    private final ShipAPI ship;
    private final String modId;
    private final Random random;
    private List<WeaponAPI> missileWeapons;
    private int[] lastAmmo;
    private float[] lastReload;
    private HeartlessStacks stacks;
    private LiveMunitionsCrew crew;
    private int appliedStacks;

    LiveMunitionsListener(ShipAPI ship) {
        this(ship, new Random());
    }

    LiveMunitionsListener(ShipAPI ship, Random random) {
        this.ship = ship;
        this.modId = MOD_ID_PREFIX + ship.getId();
        this.random = random;
    }

    @Override
    public void advance(float amount) {
        if (!ship.isAlive() || ship.isHulk()) {
            return;
        }
        if (stacks == null) {
            stacks = HeartlessStacks.of(ship);
            missileWeapons = ship.getAllWeapons().stream()
                    .filter(w -> w.getType() == WeaponAPI.WeaponType.MISSILE && w.usesAmmo()).toList();
            lastAmmo = new int[missileWeapons.size()];
            lastReload = new float[missileWeapons.size()];
            for (int i = 0; i < lastAmmo.length; i++) {
                lastAmmo[i] = missileWeapons.get(i).getAmmo();
                lastReload[i] = reloadProgress(missileWeapons.get(i));
            }
        }
        int fired = missilesFiredSinceLastFrame();
        int stackCount = stacks.stacks();
        if (fired > 0 && stackCount > 0 && crewLeft()) {
            sacrificeCrew(fired, stackCount);
        }
        applyBonuses(crewLeft() ? stackCount : 0);
    }

    private int missilesFiredSinceLastFrame() {
        int fired = 0;
        for (int i = 0; i < lastAmmo.length; i++) {
            WeaponAPI weapon = missileWeapons.get(i);
            int ammo = weapon.getAmmo();
            float reload = reloadProgress(weapon);
            int expected = lastAmmo[i];
            if (ammo > lastAmmo[i] && reload < lastReload[i]) {
                expected = Math.min(weapon.getMaxAmmo(), lastAmmo[i] + Math.round(weapon.getAmmoTracker().getReloadSize()));
            }
            if (ammo < expected) {
                fired += expected - ammo;
            }
            lastAmmo[i] = ammo;
            lastReload[i] = reload;
        }
        return fired;
    }

    private static float reloadProgress(WeaponAPI weapon) {
        return weapon.getAmmoTracker() == null ? 0f : weapon.getAmmoTracker().getReloadProgress();
    }

    private boolean crewLeft() {
        if (!paysWithFleetCrew()) {
            return true;
        }
        if (crew == null) {
            crew = LiveMunitionsCrew.forCurrentCombat();
        }
        return crew.hasCrew();
    }

    private boolean paysWithFleetCrew() {
        return ship.getOwner() == 0 && !ship.isAlly();
    }

    private void sacrificeCrew(int fired, int stackCount) {
        float chance = magnitude(CREW_DEATH_CHANCE_PERCENT_PER_STACK_KEY) * stackCount / 100f;
        if (chance <= 0f || !paysWithFleetCrew()) {
            return;
        }
        for (int i = 0; i < fired && crew.hasCrew(); i++) {
            if (random.nextFloat() < chance) {
                crew.sacrifice();
            }
        }
    }

    private void applyBonuses(int effectiveStacks) {
        if (effectiveStacks == appliedStacks) {
            return;
        }
        appliedStacks = effectiveStacks;
        MutableShipStatsAPI stats = ship.getMutableStats();
        if (effectiveStacks <= 0) {
            stats.getMissileWeaponDamageMult().unmodify(modId);
            stats.getMissileMaxSpeedBonus().unmodify(modId);
            return;
        }
        stats.getMissileWeaponDamageMult().modifyPercent(modId, magnitude(MISSILE_DAMAGE_PERCENT_PER_STACK_KEY) * effectiveStacks);
        stats.getMissileMaxSpeedBonus().modifyPercent(modId, magnitude(MISSILE_SPEED_PERCENT_PER_STACK_KEY) * effectiveStacks);
    }

    private float magnitude(String key) {
        return ship.getMutableStats().getDynamic().getValue(key, 0f);
    }
}
