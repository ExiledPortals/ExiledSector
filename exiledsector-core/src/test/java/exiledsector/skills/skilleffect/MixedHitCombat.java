package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.CombatListenerManagerAPI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class MixedHitCombat {

    private final List<Object> registered = new ArrayList<>();
    private float elapsedTime;

    MixedHitCombat(CombatEngineAPI engine) {
        Map<String, Object> customData = new HashMap<>();
        CombatListenerManagerAPI listeners = mock(CombatListenerManagerAPI.class);
        when(engine.getCustomData()).thenReturn(customData);
        when(engine.getListenerManager()).thenReturn(listeners);
        when(engine.getTotalElapsedTime(false)).thenAnswer(call -> elapsedTime);
        doAnswer(call -> registered.add(call.getArgument(0))).when(listeners).addListener(any());
        when(listeners.getListeners(any())).thenAnswer(call -> {
            Class<?> listenerType = call.getArgument(0);
            return registered.stream().filter(listenerType::isInstance).toList();
        });
        when(listeners.hasListenerOfClass(any())).thenAnswer(call -> {
            Class<?> listenerType = call.getArgument(0);
            return registered.stream().anyMatch(listenerType::isInstance);
        });
    }

    void nextFrame() {
        elapsedTime += 0.016f;
    }

    MixedHitBlender blender() {
        return MixedHitBlender.ensureInCombat();
    }

    HitDamageRestorer restorer() {
        return SkillEffectSupport.existingEngineListener(HitDamageRestorer.class);
    }

    void report(ShipAPI target, DamageType reportedType) {
        ApplyDamageResultAPI result = mock(ApplyDamageResultAPI.class);
        when(result.getType()).thenReturn(reportedType);
        restorer().reportDamageApplied(null, target, result);
    }

    static DamageAPI damage(DamageType type) {
        DamageAPI damage = mock(DamageAPI.class, RETURNS_DEEP_STUBS);
        AtomicReference<DamageType> currentType = new AtomicReference<>(type);
        when(damage.getType()).thenAnswer(call -> currentType.get());
        doAnswer(call -> {
            currentType.set(call.getArgument(0));
            return null;
        }).when(damage).setType(any());
        return damage;
    }

    static DamageAPI trackedDamage(DamageType type, float baseDamage, float emp) {
        DamageAPI damage = damage(type);
        float[] base = {baseDamage};
        float[] flux = {emp};
        when(damage.getBaseDamage()).thenAnswer(call -> base[0]);
        doAnswer(call -> {
            base[0] = call.getArgument(0);
            return null;
        }).when(damage).setDamage(anyFloat());
        when(damage.getFluxComponent()).thenAnswer(call -> flux[0]);
        doAnswer(call -> {
            flux[0] = call.getArgument(0);
            return null;
        }).when(damage).setFluxComponent(anyFloat());
        MutableStat modifier = new MutableStat(1f);
        when(damage.getModifier()).thenReturn(modifier);
        return damage;
    }

    static ShipAPI ship() {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat neutral = mock(MutableStat.class);
        when(neutral.getModifiedValue()).thenReturn(1f);
        when(stats.getKineticDamageTakenMult()).thenReturn(neutral);
        when(stats.getHighExplosiveDamageTakenMult()).thenReturn(neutral);
        when(stats.getFragmentationDamageTakenMult()).thenReturn(neutral);
        when(stats.getEnergyDamageTakenMult()).thenReturn(neutral);
        when(stats.getKineticShieldDamageTakenMult()).thenReturn(neutral);
        when(stats.getHighExplosiveShieldDamageTakenMult()).thenReturn(neutral);
        when(stats.getFragmentationShieldDamageTakenMult()).thenReturn(neutral);
        when(stats.getEnergyShieldDamageTakenMult()).thenReturn(neutral);
        when(stats.getKineticArmorDamageTakenMult()).thenReturn(neutral);
        when(ship.getMutableStats()).thenReturn(stats);
        return ship;
    }

    static TypeShares mix(DamageType from, DamageType to, float share) {
        float[] amounts = new float[HitConversions.PORTIONS];
        amounts[from.ordinal()] = 1f - share;
        amounts[to.ordinal()] += share;
        return TypeShares.ofAmounts(amounts);
    }
}
