package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.Chance;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.weapon.RiftExplosions;
import org.lwjgl.util.vector.Vector2f;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;

public final class RiftStrikeListener extends ShipCombatListener implements DamageDealtModifier {

    private final String chancePercentKey;
    private final String beamChancePercentKey;
    private final Random random;
    private final Map<WeaponAPI, Float> nextBeamRollSeconds = new IdentityHashMap<>();

    RiftStrikeListener(ShipAPI ownerShip, ShipAPI magnitudeHolder, String chancePercentKey, String beamChancePercentKey, Random random) {
        super(ownerShip, magnitudeHolder, null);
        this.chancePercentKey = chancePercentKey;
        this.beamChancePercentKey = beamChancePercentKey;
        this.random = random;
    }

    static RiftStrikeListener forShip(ShipAPI ownerShip) {
        return new RiftStrikeListener(ownerShip, ownerShip, RiftStrike.CHANCE_PERCENT_KEY, RiftStrike.BEAM_CHANCE_PERCENT_KEY, new Random());
    }

    public static RiftStrikeListener forStrikecraft(ShipAPI fighter, ShipAPI carrier) {
        return new RiftStrikeListener(fighter, carrier, RiftStrike.STRIKECRAFT_CHANCE_PERCENT_KEY, null, new Random());
    }

    // java:S3516: the engine reads a null return as "leave the damage unchanged"; rifts are spawned as a side effect instead
    @SuppressWarnings("java:S3516")
    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        WeaponAPI weapon = CombatQueries.weaponBehind(param);
        if (weapon == null || weapon.getShip() != ownerShip || CombatQueries.hostileLiveShip(ownerShip, target) == null || !ownerIsAliveNotHulk()) {
            return null;
        }
        if (RiftStrike.firesRifts(weapon)) {
            rollRift(magnitude(chancePercentKey), weapon, point);
        } else if (beamChancePercentKey != null && RiftStrike.beamFiresRifts(weapon)) {
            float beamChancePercent = magnitude(beamChancePercentKey);
            if (beamChancePercent > 0f && beamRollIsDue(weapon)) {
                rollRift(beamChancePercent, weapon, point);
            }
        }
        return null;
    }

    private void rollRift(float chancePercent, WeaponAPI weapon, Vector2f point) {
        if (Chance.roll(random, chancePercent)) {
            RiftExplosions.spawn(ownerShip, point, RiftStrike.baseRiftDamage(weapon.getSpec()), random);
        }
    }

    private boolean beamRollIsDue(WeaponAPI beamWeapon) {
        float nowSeconds = Global.getCombatEngine().getTotalElapsedTime(false);
        Float nextRollSeconds = nextBeamRollSeconds.get(beamWeapon);
        if (nextRollSeconds != null && nowSeconds < nextRollSeconds) {
            return false;
        }
        nextBeamRollSeconds.put(beamWeapon, nowSeconds + RiftStrike.BEAM_ROLL_INTERVAL_SECONDS);
        return true;
    }
}
