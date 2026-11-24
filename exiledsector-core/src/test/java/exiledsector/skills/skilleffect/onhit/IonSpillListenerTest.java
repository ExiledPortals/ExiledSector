package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatEngineExtension;
import exiledsector.skills.skilleffect.HitMix;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IonSpillListenerTest {

    private static final float TOLERANCE = 1e-4f;

    @RegisterExtension
    final CombatEngineExtension combat = CombatEngineExtension.withListenerManager();

    private static ShipAPI ship(int owner) {
        ShipAPI ship = mock(ShipAPI.class, Mockito.RETURNS_DEEP_STUBS);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getParentStation()).thenReturn(null);
        when(ship.getShield().isWithinArc(any())).thenReturn(true);
        return ship;
    }

    private static ShipAPI owner(float sharePercent) {
        ShipAPI owner = ship(0);
        when(owner.getMutableStats().getDynamic().getValue(IonSpillListener.ENERGY_SHARE_PERCENT_KEY, 0f)).thenReturn(sharePercent);
        return owner;
    }

    private static IonSpillListener resolvingFor(ShipAPI owner) {
        IonSpillListener listener = new IonSpillListener(owner);
        when(owner.hasListenerOfClass(HitMix.Resolver.class)).thenReturn(true);
        when(owner.getListeners(HitMix.Resolver.class)).thenReturn(List.of(listener));
        return listener;
    }

    static DamageAPI hit(DamageType type, float baseDamage, float emp) {
        DamageAPI damage = mock(DamageAPI.class);
        float[] baseAndEmp = {baseDamage, emp};
        when(damage.getType()).thenReturn(type);
        when(damage.getBaseDamage()).thenAnswer(call -> baseAndEmp[0]);
        when(damage.getFluxComponent()).thenAnswer(call -> baseAndEmp[1]);
        when(damage.computeFluxDealt(anyFloat())).thenAnswer(call -> baseAndEmp[1]);
        doAnswer(call -> baseAndEmp[0] = call.getArgument(0)).when(damage).setDamage(anyFloat());
        doAnswer(call -> baseAndEmp[1] = call.getArgument(0)).when(damage).setFluxComponent(anyFloat());
        return damage;
    }

    @Test
    void theShareOfEmpOnAShieldIsConvertedToEnergyDamageInTheSameHit() {
        ShipAPI owner = owner(25f);
        IonSpillListener listener = new IonSpillListener(owner);
        DamageAPI damage = hit(DamageType.KINETIC, 100f, 200f);

        HitMix.Hit hit = HitMix.resolve(owner, listener, null, ship(1), damage, new Vector2f(30f, 40f), true);

        assertEquals(150f, damage.getBaseDamage(), TOLERANCE);
        assertEquals(150f, damage.getFluxComponent(), TOLERANCE);
        assertEquals(50f / 150f, hit.shares().share(DamageType.ENERGY), TOLERANCE);
        assertEquals(100f / 150f, hit.shares().share(DamageType.KINETIC), TOLERANCE);
    }

    @Test
    void anEnergyHitOnlyGainsDamageAndStaysEnergy() {
        ShipAPI owner = owner(25f);
        IonSpillListener listener = new IonSpillListener(owner);
        DamageAPI damage = hit(DamageType.ENERGY, 100f, 200f);

        HitMix.Hit hit = HitMix.resolve(owner, listener, null, ship(1), damage, new Vector2f(), true);

        assertEquals(150f, damage.getBaseDamage(), TOLERANCE);
        assertEquals(150f, damage.getFluxComponent(), TOLERANCE);
        assertEquals(1f, hit.shares().share(DamageType.ENERGY), TOLERANCE);
    }

    @Test
    void aShareAboveAHundredPercentMovesAllTheEmpAndNoMore() {
        ShipAPI owner = owner(150f);
        IonSpillListener listener = new IonSpillListener(owner);
        DamageAPI damage = hit(DamageType.KINETIC, 100f, 200f);

        HitMix.resolve(owner, listener, null, ship(1), damage, new Vector2f(), true);

        assertEquals(300f, damage.getBaseDamage(), TOLERANCE);
        assertEquals(0f, damage.getFluxComponent(), TOLERANCE);
    }

    @Test
    void hullHitsAlliesDestroyedTargetsAndHitsWithoutEmpSpillNothing() {
        ShipAPI owner = owner(25f);
        IonSpillListener listener = new IonSpillListener(owner);
        DamageAPI hullHit = hit(DamageType.KINETIC, 100f, 200f);
        DamageAPI allyHit = hit(DamageType.KINETIC, 100f, 200f);
        DamageAPI destroyedHit = hit(DamageType.KINETIC, 100f, 200f);
        DamageAPI noEmp = hit(DamageType.KINETIC, 100f, 0f);
        ShipAPI destroyed = ship(1);
        when(destroyed.isAlive()).thenReturn(false);

        listener.modifyDamageDealt(null, ship(1), hullHit, new Vector2f(), false);
        listener.modifyDamageDealt(null, ship(0), allyHit, new Vector2f(), true);
        listener.modifyDamageDealt(null, destroyed, destroyedHit, new Vector2f(), true);
        listener.modifyDamageDealt(null, ship(1), noEmp, new Vector2f(), true);

        assertEquals(100f, hullHit.getBaseDamage(), TOLERANCE);
        assertEquals(200f, hullHit.getFluxComponent(), TOLERANCE);
        assertEquals(100f, allyHit.getBaseDamage(), TOLERANCE);
        assertEquals(100f, destroyedHit.getBaseDamage(), TOLERANCE);
        assertEquals(100f, noEmp.getBaseDamage(), TOLERANCE);
    }

    @Test
    void asOneOfTheDealersResolversItConvertsOnlyShieldHits() {
        ShipAPI owner = owner(25f);
        resolvingFor(owner);
        DamageAPI hullHit = hit(DamageType.KINETIC, 100f, 200f);
        DamageAPI shieldHit = hit(DamageType.KINETIC, 100f, 200f);

        HitMix.resolve(owner, null, ship(1), hullHit, new Vector2f(), false);
        HitMix.resolve(owner, null, ship(1), shieldHit, new Vector2f(), true);

        assertEquals(100f, hullHit.getBaseDamage(), TOLERANCE);
        assertEquals(200f, hullHit.getFluxComponent(), TOLERANCE);
        assertEquals(150f, shieldHit.getBaseDamage(), TOLERANCE);
        assertEquals(150f, shieldHit.getFluxComponent(), TOLERANCE);
    }

    @Test
    void anEmpOnlyShieldHitHasItsShareMovedToEnergyDamage() {
        ShipAPI owner = owner(25f);
        resolvingFor(owner);

        HitMix.EmpOnlyResult hit = HitMix.resolveEmpOnly(owner, null, ship(1), new Vector2f(), true, 200f);

        assertEquals(50f, hit.movedDamage().get(DamageType.ENERGY), TOLERANCE);
        assertEquals(1, hit.movedDamage().size());
        assertEquals(150f, hit.emp(), TOLERANCE);
        assertEquals(1f, hit.empMult(), TOLERANCE);
    }

    @Test
    void anEmpOnlyHullHitMovesNothing() {
        ShipAPI owner = owner(25f);
        resolvingFor(owner);

        HitMix.EmpOnlyResult hit = HitMix.resolveEmpOnly(owner, null, ship(1), new Vector2f(), false, 200f);

        assertTrue(hit.movedDamage().isEmpty());
        assertEquals(200f, hit.emp(), TOLERANCE);
    }
}
