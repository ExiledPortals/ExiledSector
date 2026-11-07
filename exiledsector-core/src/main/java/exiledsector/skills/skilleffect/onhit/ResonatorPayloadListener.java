package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.weapon.RiftExplosions;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

final class ResonatorPayloadListener extends ShipCombatListener implements DamageDealtModifier, AdvanceableListener {

    static final String CHANCE_PERCENT_KEY = "exiledSector_resonatorPayloadChancePercent";
    static final int RIFTS_PER_SERIES = 4;
    static final float DAMAGE_SHARE_PERCENT = 10f;
    static final float MAX_DAMAGE = 100f;
    static final float SCATTER_RADIUS = 100f;
    static final float SECONDS_BETWEEN_RIFTS = 0.1f;

    private final Random random;
    private final List<PendingRift> pendingRifts = new ArrayList<>();

    ResonatorPayloadListener(ShipAPI ownerShip) {
        this(ownerShip, new Random());
    }

    ResonatorPayloadListener(ShipAPI ownerShip, Random random) {
        super(ownerShip);
        this.random = random;
    }

    static float riftDamage(WeaponSpecAPI missileWeaponSpec) {
        return Math.min(MAX_DAMAGE, missileWeaponSpec.getDerivedStats().getDamagePerShot() * DAMAGE_SHARE_PERCENT / 100f);
    }

    // java:S3516: the engine reads a null return as "leave the damage unchanged"; rifts are queued as a side effect instead
    @SuppressWarnings("java:S3516")
    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!(param instanceof MissileAPI missile) || !isMissileWeaponShot(missile) || !(target instanceof ShipAPI hitShip) || point == null
                || !CombatQueries.isHostile(ownerShip, hitShip) || !ownerIsAliveNotHulk()) {
            return null;
        }
        if (random.nextFloat() < magnitude(CHANCE_PERCENT_KEY) / 100f) {
            queueSeries(point, riftDamage(missile.getWeapon().getSpec()));
        }
        return null;
    }

    @Override
    public void advance(float amount) {
        if (pendingRifts.isEmpty()) {
            return;
        }
        if (!ownerIsAliveNotHulk()) {
            pendingRifts.clear();
            return;
        }
        Iterator<PendingRift> pending = pendingRifts.iterator();
        while (pending.hasNext()) {
            PendingRift rift = pending.next();
            rift.secondsLeft -= amount;
            if (rift.secondsLeft <= 0f) {
                RiftExplosions.spawn(ownerShip, rift.point, rift.damage, random);
                pending.remove();
            }
        }
    }

    private static boolean isMissileWeaponShot(MissileAPI missile) {
        WeaponAPI weapon = missile.getWeapon();
        return weapon != null && weapon.getType() == WeaponAPI.WeaponType.MISSILE && !missile.isFlare() && !missile.isMine();
    }

    private void queueSeries(Vector2f impactPoint, float riftDamage) {
        for (int riftIndex = 0; riftIndex < RIFTS_PER_SERIES; riftIndex++) {
            pendingRifts.add(new PendingRift(scatteredAround(impactPoint), riftDamage, riftIndex * SECONDS_BETWEEN_RIFTS));
        }
    }

    private Vector2f scatteredAround(Vector2f impactPoint) {
        double angleRadians = random.nextFloat() * 2.0 * Math.PI;
        float distance = SCATTER_RADIUS * (float) Math.sqrt(random.nextFloat());
        return new Vector2f(impactPoint.x + distance * (float) Math.cos(angleRadians), impactPoint.y + distance * (float) Math.sin(angleRadians));
    }

    private static final class PendingRift {

        private final Vector2f point;
        private final float damage;
        private float secondsLeft;

        private PendingRift(Vector2f point, float damage, float secondsLeft) {
            this.point = point;
            this.damage = damage;
            this.secondsLeft = secondsLeft;
        }
    }
}
