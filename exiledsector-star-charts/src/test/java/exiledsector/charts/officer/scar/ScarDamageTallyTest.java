package exiledsector.charts.officer.scar;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.CombatListenerManagerAPI;
import exiledsector.charts.officer.scar.ScarEvidence.FiredAs;
import exiledsector.skills.skilleffect.CombatEngineExtension;
import exiledsector.skills.skilleffect.HitMix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScarDamageTallyTest {

    private static final float TOLERANCE = 1e-4f;

    @RegisterExtension
    final CombatEngineExtension combat = new CombatEngineExtension();

    private final ScarEvidence evidence = new ScarEvidence();
    private ShipAPI flagship;
    private ScarDamageTally tally;

    @BeforeEach
    void setUp() {
        CombatListenerManagerAPI listeners = mock(CombatListenerManagerAPI.class);
        when(listeners.getListeners(any())).thenReturn(List.of());
        when(combat.engine().getListenerManager()).thenReturn(listeners);
        when(combat.engine().getCustomData()).thenReturn(new HashMap<>());
        flagship = ship(0);
        ScarFlagshipTracker tracker = mock(ScarFlagshipTracker.class);
        when(tracker.flagship()).thenReturn(flagship);
        when(tracker.evidence()).thenReturn(evidence);
        tally = new ScarDamageTally(tracker);
    }

    private static ShipAPI ship(int owner) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getLocation()).thenReturn(new Vector2f());
        return ship;
    }

    private static DamageAPI damage(DamageType type, float amount) {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getType()).thenReturn(type);
        when(damage.getDamage()).thenReturn(amount);
        when(damage.getBaseDamage()).thenReturn(amount);
        return damage;
    }

    private static DamagingProjectileAPI shotFrom(ShipAPI attacker) {
        DamagingProjectileAPI projectile = mock(DamagingProjectileAPI.class);
        when(projectile.getSource()).thenReturn(attacker);
        return projectile;
    }

    @Test
    void aMixedHitCountsTowardsEachDamageTypeByItsShare() {
        ShipAPI attacker = ship(1);
        HitMix.Resolver converter = hit -> hit.convert(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 0.4f);
        when(attacker.hasListenerOfClass(HitMix.Resolver.class)).thenReturn(true);
        when(attacker.getListeners(HitMix.Resolver.class)).thenReturn(List.of(converter));
        DamagingProjectileAPI projectile = shotFrom(attacker);
        DamageAPI damage = damage(DamageType.KINETIC, 100f);
        HitMix.resolve(attacker, projectile, flagship, damage, null, false);

        tally.modifyDamageTaken(projectile, flagship, damage, null, false);

        assertEquals(0.6f, evidence.share(FiredAs.KINETIC), TOLERANCE);
        assertEquals(0.4f, evidence.share(FiredAs.HIGH_EXPLOSIVE), TOLERANCE);
        assertEquals(1f, evidence.share(FiredAs.PROJECTILE), TOLERANCE);
    }

    @Test
    void anUnconvertedHitCountsWhollyTowardsItsOwnType() {
        DamagingProjectileAPI projectile = shotFrom(ship(1));

        tally.modifyDamageTaken(projectile, flagship, damage(DamageType.ENERGY, 100f), null, false);

        assertEquals(1f, evidence.share(FiredAs.ENERGY), TOLERANCE);
        assertEquals(0f, evidence.share(FiredAs.KINETIC), TOLERANCE);
    }
}
