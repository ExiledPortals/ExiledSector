package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import exiledsector.skills.skilleffect.Chance;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.weapon.RiftSeries;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
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
    private final List<RiftSeries> activeSeries = new ArrayList<>();

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
        if (!(param instanceof MissileAPI missile) || !isOwnMissileWeaponShot(missile) || point == null
                || CombatQueries.hostileLiveShip(ownerShip, target) == null || !ownerIsAliveNotHulk()) {
            return null;
        }
        if (Chance.roll(random, magnitude(CHANCE_PERCENT_KEY))) {
            Vector2f impactPoint = new Vector2f(point);
            activeSeries.add(new RiftSeries(ownerShip, RIFTS_PER_SERIES, SECONDS_BETWEEN_RIFTS, riftDamage(missile.getWeapon().getSpec()),
                    () -> scatteredAround(impactPoint), random));
        }
        return null;
    }

    @Override
    public void advance(float amount) {
        if (activeSeries.isEmpty()) {
            return;
        }
        if (!ownerIsAliveNotHulk()) {
            activeSeries.clear();
            return;
        }
        for (int index = activeSeries.size() - 1; index >= 0; index--) {
            RiftSeries series = activeSeries.get(index);
            series.advance(amount);
            if (series.isFinished()) {
                activeSeries.remove(index);
            }
        }
    }

    private boolean isOwnMissileWeaponShot(MissileAPI missile) {
        WeaponAPI weapon = missile.getWeapon();
        return weapon != null && weapon.getShip() == ownerShip && weapon.getType() == WeaponAPI.WeaponType.MISSILE
                && !missile.isFlare() && !missile.isMine();
    }

    private Vector2f scatteredAround(Vector2f impactPoint) {
        double angleRadians = random.nextFloat() * 2.0 * Math.PI;
        float distance = SCATTER_RADIUS * (float) Math.sqrt(random.nextFloat());
        return new Vector2f(impactPoint.x + distance * (float) Math.cos(angleRadians), impactPoint.y + distance * (float) Math.sin(angleRadians));
    }
}
