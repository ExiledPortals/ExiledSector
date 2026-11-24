package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.CombatListenerManagerAPI;
import exiledsector.skills.skilleffect.CombatEngineExtension;
import exiledsector.skills.skilleffect.DamageTypeMults;
import exiledsector.skills.skilleffect.DamageTypeMults.Mults;
import exiledsector.skills.skilleffect.DamageTypeMultsExtension;
import exiledsector.skills.skilleffect.HitMix;
import exiledsector.skills.skilleffect.MixedHitBlender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DamageTypeListenerTest {

    private static final float TOLERANCE = 1e-4f;

    @RegisterExtension
    final CombatEngineExtension combat = new CombatEngineExtension();

    @RegisterExtension
    final DamageTypeMultsExtension typeMults = new DamageTypeMultsExtension();

    private final List<Object> registered = new ArrayList<>();

    @BeforeEach
    void setUp() {
        CombatListenerManagerAPI listeners = mock(CombatListenerManagerAPI.class);
        when(combat.engine().getListenerManager()).thenReturn(listeners);
        when(combat.engine().getCustomData()).thenReturn(new HashMap<>());
        doAnswer(call -> registered.add(call.getArgument(0))).when(listeners).addListener(any());
        when(listeners.getListeners(any())).thenAnswer(call -> {
            Class<?> listenerType = call.getArgument(0);
            return registered.stream().filter(listenerType::isInstance).toList();
        });
    }

    private static ShipAPI ship() {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn("ship");
        when(ship.getMutableStats().getDynamic().getValue(DamageTypeListener.KINETIC_PERCENT_KEY, 0f)).thenReturn(15f);
        when(ship.getMutableStats().getDynamic().getValue(DamageTypeListener.FRAGMENTATION_PERCENT_KEY, 0f)).thenReturn(30f);
        when(ship.getMutableStats().getDynamic().getValue(DamageTypeListener.FRAGMENTATION_MORE_PERCENT_KEY, 0f)).thenReturn(-40f);
        return ship;
    }

    private static ShipAPI shipConverting(DamageType from, DamageType to, float share) {
        ShipAPI ship = ship();
        HitMix.Resolver converter = hit -> hit.convert(from, to, share);
        when(ship.hasListenerOfClass(HitMix.Resolver.class)).thenReturn(true);
        when(ship.getListeners(HitMix.Resolver.class)).thenReturn(List.of(converter));
        return ship;
    }

    private static ShipAPI target() {
        ShipAPI target = mock(ShipAPI.class);
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
        when(target.getMutableStats()).thenReturn(stats);
        return target;
    }

    private static DamageAPI damage(DamageType type, float baseDamage, float emp) {
        DamageAPI damage = mock(DamageAPI.class);
        AtomicReference<DamageType> currentType = new AtomicReference<>(type);
        float[] amounts = {baseDamage, emp};
        MutableStat modifier = new MutableStat(1f);
        when(damage.getModifier()).thenReturn(modifier);
        when(damage.getType()).thenAnswer(call -> currentType.get());
        doAnswer(call -> {
            currentType.set(call.getArgument(0));
            return null;
        }).when(damage).setType(any());
        when(damage.getBaseDamage()).thenAnswer(call -> amounts[0]);
        doAnswer(call -> amounts[0] = call.getArgument(0)).when(damage).setDamage(anyFloat());
        when(damage.getFluxComponent()).thenAnswer(call -> amounts[1]);
        doAnswer(call -> amounts[1] = call.getArgument(0)).when(damage).setFluxComponent(anyFloat());
        return damage;
    }

    private String land(DamageTypeListener listener, CombatEntityAPI target, DamageAPI damage) {
        assertNull(listener.modifyDamageDealt(null, target, damage, null, false));
        MixedHitBlender blender = registered.stream().filter(MixedHitBlender.class::isInstance).map(MixedHitBlender.class::cast).findFirst()
                .orElseThrow();
        return blender.modifyDamageTaken(null, target, damage, null, false);
    }

    @Test
    void eachDamageTypeGetsItsOwnIncrease() {
        DamageTypeListener listener = new DamageTypeListener(ship());
        DamageAPI kinetic = damage(DamageType.KINETIC, 100f, 0f);
        DamageAPI energy = damage(DamageType.ENERGY, 100f, 0f);

        assertNull(land(listener, target(), kinetic));
        assertNull(land(listener, target(), energy));

        assertEquals(115f, kinetic.getBaseDamage(), TOLERANCE);
        assertEquals(100f, energy.getBaseDamage(), TOLERANCE);
    }

    @Test
    void aTypeIncreaseAddsToTheHitsOtherPercentBonusesAndLeavesTheHitsModifierAlone() {
        DamageTypeListener listener = new DamageTypeListener(ship());
        DamageAPI kinetic = damage(DamageType.KINETIC, 100f, 0f);
        kinetic.getModifier().modifyPercent("otherBonus", 50f);

        land(listener, target(), kinetic);

        assertEquals(110f, kinetic.getBaseDamage(), TOLERANCE);
        assertEquals(1, kinetic.getModifier().getPercentMods().size());
        assertTrue(kinetic.getModifier().getMultMods().isEmpty());
    }

    @Test
    void lessFragmentationDamageMultipliesTheIncreasedFragmentationDamage() {
        DamageTypeListener listener = new DamageTypeListener(ship());
        DamageAPI fragmentation = damage(DamageType.FRAGMENTATION, 100f, 0f);

        land(listener, target(), fragmentation);

        assertEquals(78f, fragmentation.getBaseDamage(), TOLERANCE);
    }

    @Test
    void aTypeIncreaseAppliesOnlyToItsOwnShareOfAPartlyConvertedHit() {
        DamageTypeListener listener = new DamageTypeListener(shipConverting(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 0.4f));
        DamageAPI kinetic = damage(DamageType.KINETIC, 100f, 0f);

        land(listener, target(), kinetic);

        Mults carrier = DamageTypeMults.current(DamageType.OTHER);
        assertEquals(DamageType.OTHER, kinetic.getType());
        assertEquals(100f, kinetic.getBaseDamage(), TOLERANCE);
        assertEquals(0.6f * 1.15f * 2f + 0.4f * 0.5f, carrier.shield(), TOLERANCE);
        assertEquals(0.6f * 1.15f * 0.5f + 0.4f * 2f, carrier.armor(), TOLERANCE);
        assertEquals(0.6f * 1.15f + 0.4f, carrier.hull(), TOLERANCE);
    }

    @Test
    void aPartlyConvertedHitOnSomethingOtherThanAShipScalesItsModifierByEachTypesShare() {
        DamageTypeListener listener = new DamageTypeListener(shipConverting(DamageType.FRAGMENTATION, DamageType.ENERGY, 0.5f));
        DamageAPI fragmentation = damage(DamageType.FRAGMENTATION, 100f, 0f);

        String modId = land(listener, mock(CombatEntityAPI.class), fragmentation);

        assertNotNull(modId);
        assertEquals(0.5f * 1.3f * 0.6f + 0.5f, fragmentation.getModifier().getMultStatMod(modId).getValue(), TOLERANCE);
        assertEquals(100f, fragmentation.getBaseDamage(), TOLERANCE);
    }

    @Test
    void aFullyConvertedHitLosesTheIncreaseOfTheTypeItLeft() {
        DamageTypeListener listener = new DamageTypeListener(shipConverting(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 1f));
        DamageAPI kinetic = damage(DamageType.KINETIC, 100f, 0f);

        land(listener, target(), kinetic);

        assertEquals(DamageType.HIGH_EXPLOSIVE, kinetic.getType());
        assertEquals(100f, kinetic.getBaseDamage(), TOLERANCE);
    }

    @Test
    void typeBonusesDoNotScaleEmp() {
        DamageTypeListener listener = new DamageTypeListener(ship());
        DamageAPI kinetic = damage(DamageType.KINETIC, 100f, 40f);

        land(listener, target(), kinetic);

        assertEquals(115f, kinetic.getBaseDamage(), TOLERANCE);
        assertEquals(40f, kinetic.getFluxComponent(), TOLERANCE);
    }
}
