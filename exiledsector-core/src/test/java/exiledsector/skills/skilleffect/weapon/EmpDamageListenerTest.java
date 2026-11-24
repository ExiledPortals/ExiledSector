package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.CombatListenerManagerAPI;
import exiledsector.skills.skilleffect.CombatEngineExtension;
import exiledsector.skills.skilleffect.DamageTypeMultsExtension;
import exiledsector.skills.skilleffect.HitMix;
import exiledsector.skills.skilleffect.MixedHitBlender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmpDamageListenerTest {

    @RegisterExtension
    final CombatEngineExtension combat = new CombatEngineExtension();

    @RegisterExtension
    final DamageTypeMultsExtension typeMults = new DamageTypeMultsExtension();

    private final List<Object> engineListeners = new ArrayList<>();

    @BeforeEach
    void setUp() {
        CombatEngineAPI engine = combat.engine();
        CombatListenerManagerAPI listeners = mock(CombatListenerManagerAPI.class);
        when(engine.getListenerManager()).thenReturn(listeners);
        when(engine.getCustomData()).thenReturn(new HashMap<>());
        doAnswer(call -> engineListeners.add(call.getArgument(0))).when(listeners).addListener(any());
        when(listeners.getListeners(any())).thenAnswer(call -> {
            Class<?> listenerType = call.getArgument(0);
            return engineListeners.stream().filter(listenerType::isInstance).toList();
        });
    }

    private static ShipAPI owner(float increasedPercent, float morePercent) {
        ShipAPI owner = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(owner.getMutableStats().getDynamic().getValue(EmpDamageListener.EMP_INCREASED_PERCENT_KEY, 0f)).thenReturn(increasedPercent);
        when(owner.getMutableStats().getDynamic().getValue(EmpDamageListener.EMP_MORE_PERCENT_KEY, 0f)).thenReturn(morePercent);
        return owner;
    }

    private static HitMix.EmpOnlyResult empOnlyHit(ShipAPI owner, HitMix.Resolver... resolvers) {
        when(owner.hasListenerOfClass(HitMix.Resolver.class)).thenReturn(true);
        when(owner.getListeners(HitMix.Resolver.class)).thenReturn(List.of(resolvers));
        return HitMix.resolveEmpOnly(owner, null, mock(ShipAPI.class), new Vector2f(), false, 200f);
    }

    private static float empMultOf(float increasedPercent, float morePercent) {
        ShipAPI owner = owner(increasedPercent, morePercent);
        return empOnlyHit(owner, new EmpDamageListener(owner)).empMult();
    }

    private static DamageAPI damage(float baseDamage, float emp, float damageIncreasedPercent) {
        DamageAPI damage = mock(DamageAPI.class);
        float[] empHolder = {emp};
        MutableStat modifier = new MutableStat(1f);
        modifier.modifyPercent("test", damageIncreasedPercent);
        when(damage.getType()).thenReturn(DamageType.KINETIC);
        when(damage.getBaseDamage()).thenReturn(baseDamage);
        when(damage.getModifier()).thenReturn(modifier);
        when(damage.getFluxComponent()).thenAnswer(call -> empHolder[0]);
        doAnswer(call -> empHolder[0] = call.getArgument(0)).when(damage).setFluxComponent(anyFloat());
        return damage;
    }

    private MixedHitBlender blender() {
        return engineListeners.stream().filter(MixedHitBlender.class::isInstance).map(MixedHitBlender.class::cast).findFirst().orElseThrow();
    }

    @Test
    void empOnAShipHitIsScaledByItsMoreMultiplierAndNothingIsMoved() {
        ShipAPI owner = owner(0f, 15f);

        HitMix.EmpOnlyResult hit = empOnlyHit(owner, new EmpDamageListener(owner));

        assertEquals(1.15f, hit.empMult(), 0.0001f);
        assertEquals(230f, hit.emp(), 0.01f);
        assertTrue(hit.movedDamage().isEmpty());
    }

    @Test
    void increasedEmpIsAppliedBeforeTheMoreMultiplier() {
        assertEquals(1.32f, empMultOf(10f, 20f), 0.0001f);
    }

    @Test
    void otherIncreasesOnTheHitAddUpWithItsOwnBeforeTheMoreMultiplier() {
        ShipAPI owner = owner(10f, 20f);

        HitMix.EmpOnlyResult hit = empOnlyHit(owner, new EmpDamageListener(owner), resolved -> resolved.increaseEmp(20f));

        assertEquals(1.3f * 1.2f, hit.empMult(), 0.0001f);
    }

    @Test
    void theEmpScaleNeverGoesBelowZero() {
        assertEquals(0f, empMultOf(0f, -150f), 0.0001f);
        assertEquals(0f, empMultOf(-150f, 10f), 0.0001f);
    }

    @Test
    void theIncreasedEmpOfADamagingShipHitJoinsTheHitsDamageBonuses() {
        EmpDamageListener listener = new EmpDamageListener(owner(30f, 0f));
        ShipAPI target = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        DamageAPI damage = damage(100f, 200f, 50f);

        listener.modifyDamageDealt(null, target, damage, new Vector2f(), false);
        blender().modifyDamageTaken(null, target, damage, new Vector2f(), false);

        assertEquals(200f * (1.5f + 0.3f) / 1.5f, damage.getFluxComponent(), 0.01f);
    }
}
