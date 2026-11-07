package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;
import java.util.function.Supplier;

public final class RiftSeries {

    private final ShipAPI sourceShip;
    private final int riftCount;
    private final float secondsBetweenRifts;
    private final float riftDamage;
    private final Supplier<Vector2f> nextRiftPoint;
    private final Random random;
    private int riftsSpawned;
    private float elapsedSeconds;

    public RiftSeries(ShipAPI sourceShip, int riftCount, float secondsBetweenRifts, float riftDamage, Supplier<Vector2f> nextRiftPoint, Random random) {
        this.sourceShip = sourceShip;
        this.riftCount = riftCount;
        this.secondsBetweenRifts = secondsBetweenRifts;
        this.riftDamage = riftDamage;
        this.nextRiftPoint = nextRiftPoint;
        this.random = random;
    }

    public void advance(float amount) {
        elapsedSeconds += amount;
        while (riftsSpawned < riftCount && riftsSpawned * secondsBetweenRifts <= elapsedSeconds) {
            RiftExplosions.spawn(sourceShip, nextRiftPoint.get(), riftDamage, random);
            riftsSpawned++;
        }
    }

    public boolean isFinished() {
        return riftsSpawned >= riftCount;
    }
}
