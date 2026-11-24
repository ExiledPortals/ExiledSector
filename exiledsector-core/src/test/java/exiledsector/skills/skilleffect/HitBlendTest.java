package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import exiledsector.skills.skilleffect.DamageTypeMults.Mults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

import static exiledsector.skills.skilleffect.MixedHitCombat.mix;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HitBlendTest {

    private static final float TOLERANCE = 1e-4f;
    private static final float HIT_DAMAGE = 200f;
    private static final ToDoubleFunction<DamageType> UNSCALED = type -> 1f;

    @RegisterExtension
    final DamageTypeMultsExtension typeMults = new DamageTypeMultsExtension();

    private final Map<DamageType, Float> damageTaken = new EnumMap<>(DamageType.class);
    private final Map<DamageType, Float> shieldDamageTaken = new EnumMap<>(DamageType.class);
    private float kineticArmorTaken = 1f;

    private MutableShipStatsAPI stats() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat kinetic = stat(damageTaken.getOrDefault(DamageType.KINETIC, 1f));
        MutableStat highExplosive = stat(damageTaken.getOrDefault(DamageType.HIGH_EXPLOSIVE, 1f));
        MutableStat fragmentation = stat(damageTaken.getOrDefault(DamageType.FRAGMENTATION, 1f));
        MutableStat energy = stat(damageTaken.getOrDefault(DamageType.ENERGY, 1f));
        MutableStat kineticShield = stat(shieldDamageTaken.getOrDefault(DamageType.KINETIC, 1f));
        MutableStat highExplosiveShield = stat(shieldDamageTaken.getOrDefault(DamageType.HIGH_EXPLOSIVE, 1f));
        MutableStat fragmentationShield = stat(shieldDamageTaken.getOrDefault(DamageType.FRAGMENTATION, 1f));
        MutableStat energyShield = stat(shieldDamageTaken.getOrDefault(DamageType.ENERGY, 1f));
        MutableStat kineticArmor = stat(kineticArmorTaken);
        when(stats.getKineticDamageTakenMult()).thenReturn(kinetic);
        when(stats.getHighExplosiveDamageTakenMult()).thenReturn(highExplosive);
        when(stats.getFragmentationDamageTakenMult()).thenReturn(fragmentation);
        when(stats.getEnergyDamageTakenMult()).thenReturn(energy);
        when(stats.getKineticShieldDamageTakenMult()).thenReturn(kineticShield);
        when(stats.getHighExplosiveShieldDamageTakenMult()).thenReturn(highExplosiveShield);
        when(stats.getFragmentationShieldDamageTakenMult()).thenReturn(fragmentationShield);
        when(stats.getEnergyShieldDamageTakenMult()).thenReturn(energyShield);
        when(stats.getKineticArmorDamageTakenMult()).thenReturn(kineticArmor);
        return stats;
    }

    private static MutableStat stat(float value) {
        MutableStat stat = mock(MutableStat.class);
        when(stat.getModifiedValue()).thenReturn(value);
        return stat;
    }

    private float engineShieldDamage(DamageType type) {
        return HIT_DAMAGE * shieldDamageTaken.getOrDefault(type, 1f) * type.getShieldMult();
    }

    private float engineArmorDamage(DamageType type) {
        float armorFactor = type.getArmorMult() * (type == DamageType.KINETIC ? kineticArmorTaken : 1f);
        return HIT_DAMAGE * damageTaken.getOrDefault(type, 1f) * armorFactor;
    }

    private float engineHullDamage(DamageType type) {
        return HIT_DAMAGE * damageTaken.getOrDefault(type, 1f) * type.getHullMult();
    }

    private void assertMatchesSplitHits(TypeShares shares) {
        Mults blend = HitBlend.of(shares, stats(), UNSCALED);
        assertEquals(splitDamage(shares, this::engineShieldDamage), HIT_DAMAGE * blend.shield(), TOLERANCE);
        assertEquals(splitDamage(shares, this::engineArmorDamage), HIT_DAMAGE * blend.armor(), TOLERANCE);
        assertEquals(splitDamage(shares, this::engineHullDamage), HIT_DAMAGE * blend.hull(), TOLERANCE);
    }

    private static float splitDamage(TypeShares shares, Function<DamageType, Float> wholeHitDamage) {
        float total = 0f;
        for (DamageType type : shares.types()) {
            total += shares.share(type) * wholeHitDamage.apply(type);
        }
        return total;
    }

    @Test
    void aSingleTypeBlendsToThatTypesOwnMultipliers() {
        for (DamageType type : DamageType.values()) {
            Mults blend = HitBlend.of(TypeShares.single(type), stats(), UNSCALED);
            assertEquals(DamageTypeMults.defaults(type), blend, type.name());
        }
    }

    @Test
    void sixtyPercentKineticAndFortyPercentHighExplosiveBlendsEachLayer() {
        Mults blend = HitBlend.of(mix(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 0.4f), stats(), UNSCALED);

        assertEquals(1.4f, blend.shield(), TOLERANCE);
        assertEquals(1.1f, blend.armor(), TOLERANCE);
        assertEquals(1f, blend.hull(), TOLERANCE);
    }

    @Test
    void aFragmentationMixMatchesTheSplitHits() {
        assertMatchesSplitHits(mix(DamageType.FRAGMENTATION, DamageType.ENERGY, 0.3f));
    }

    @Test
    void kineticArmorDamageTakenScalesOnlyTheKineticArmorShare() {
        kineticArmorTaken = 0.5f;

        assertMatchesSplitHits(mix(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 0.4f));
        assertEquals(0.6f * 0.5f * 0.5f + 0.4f * 2f, HitBlend.of(mix(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 0.4f), stats(), UNSCALED).armor(),
                TOLERANCE);
    }

    @Test
    void perTypeResistancesScaleArmorAndHullWhileShieldsUseOnlyTheirOwn() {
        damageTaken.put(DamageType.KINETIC, 0.8f);
        damageTaken.put(DamageType.HIGH_EXPLOSIVE, 1.5f);
        shieldDamageTaken.put(DamageType.KINETIC, 0.7f);
        shieldDamageTaken.put(DamageType.HIGH_EXPLOSIVE, 1.2f);

        assertMatchesSplitHits(mix(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 0.4f));
    }

    @Test
    void aTypeTheTargetIgnoresAddsNothing() {
        damageTaken.put(DamageType.ENERGY, 0f);
        shieldDamageTaken.put(DamageType.ENERGY, 0f);

        Mults blend = HitBlend.of(mix(DamageType.KINETIC, DamageType.ENERGY, 0.5f), stats(), UNSCALED);

        assertMatchesSplitHits(mix(DamageType.KINETIC, DamageType.ENERGY, 0.5f));
        assertEquals(1f, blend.shield(), TOLERANCE);
        assertEquals(0.25f, blend.armor(), TOLERANCE);
        assertEquals(0.5f, blend.hull(), TOLERANCE);
    }

    @Test
    void aScaledSingleTypeScalesEveryLayerOfItsOwnMultipliers() {
        Mults defaults = DamageTypeMults.defaults(DamageType.ENERGY);

        Mults blend = HitBlend.of(TypeShares.single(DamageType.ENERGY), stats(), type -> 1.5f);

        assertEquals(defaults.shield() * 1.5f, blend.shield(), TOLERANCE);
        assertEquals(defaults.armor() * 1.5f, blend.armor(), TOLERANCE);
        assertEquals(defaults.hull() * 1.5f, blend.hull(), TOLERANCE);
    }

    @Test
    void eachTypesScaleWeighsOnlyItsOwnShareOfTheBlend() {
        Map<DamageType, Float> scaleByType = Map.of(DamageType.KINETIC, 1.5f, DamageType.HIGH_EXPLOSIVE, 0.5f);

        Mults blend = HitBlend.of(mix(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 0.4f), stats(), type -> scaleByType.getOrDefault(type, 1f));

        assertEquals(0.6f * 1.5f * 2f + 0.4f * 0.5f * 0.5f, blend.shield(), TOLERANCE);
        assertEquals(0.6f * 1.5f * 0.5f + 0.4f * 0.5f * 2f, blend.armor(), TOLERANCE);
        assertEquals(0.6f * 1.5f + 0.4f * 0.5f, blend.hull(), TOLERANCE);
    }

    @Test
    void theBlendReadsTheDefaultsRecordedAtLoadNotTheLiveValues() {
        DamageType.KINETIC.setShieldMult(9f);

        assertEquals(2f, HitBlend.of(TypeShares.single(DamageType.KINETIC), stats(), UNSCALED).shield(), TOLERANCE);
    }
}
