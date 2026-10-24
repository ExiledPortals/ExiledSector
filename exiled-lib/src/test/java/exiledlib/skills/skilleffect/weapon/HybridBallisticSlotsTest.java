package exiledlib.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.loading.VariantSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HybridBallisticSlotsTest {

    private static CloneableTestHullSpec spec(Set<String> tags, RetypableTestSlot... slots) {
        CloneableTestHullSpec spec = mock(CloneableTestHullSpec.class);
        when(spec.getAllWeaponSlotsCopy()).thenAnswer(call -> new ArrayList<>(List.of(slots)));
        for (RetypableTestSlot slot : slots) {
            when(spec.getWeaponSlotAPI(slot.getId())).thenReturn(slot);
        }
        when(spec.getTags()).thenReturn(tags);
        when(spec.hasTag(anyString())).thenAnswer(call -> tags.contains(call.<String>getArgument(0)));
        doAnswer(call -> tags.add(call.getArgument(0))).when(spec).addTag(anyString());
        return spec;
    }

    private static ShipVariantAPI variant(ShipHullSpecAPI hullSpec, VariantSource source) {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getHullSpec()).thenReturn(hullSpec);
        when(variant.getSource()).thenReturn(source);
        return variant;
    }

    @Test
    void convertRetypesOnlyTheCopysNonBuiltInBallisticSlotsAndTagsThem() {
        RetypableTestSlot ballistic = new RetypableTestSlot("WS0001", WeaponType.BALLISTIC, false);
        RetypableTestSlot energy = new RetypableTestSlot("WS0002", WeaponType.ENERGY, false);
        RetypableTestSlot builtIn = new RetypableTestSlot("WS0003", WeaponType.BALLISTIC, true);
        CloneableTestHullSpec shared = spec(new LinkedHashSet<>(), ballistic, energy, builtIn);
        RetypableTestSlot ballisticCopy = ballistic.copy();
        RetypableTestSlot energyCopy = energy.copy();
        RetypableTestSlot builtInCopy = builtIn.copy();
        ShipHullSpecAPI copy = spec(new LinkedHashSet<>(), ballisticCopy, energyCopy, builtInCopy);
        when(shared.clone()).thenReturn(copy);
        ShipVariantAPI variant = variant(shared, VariantSource.REFIT);

        HybridBallisticSlots.convert(variant);

        verify(variant).setHullSpecAPI(copy);
        assertEquals(WeaponType.HYBRID, ballisticCopy.getWeaponType());
        assertEquals(WeaponType.ENERGY, energyCopy.getWeaponType());
        assertEquals(WeaponType.BALLISTIC, builtInCopy.getWeaponType());
        assertEquals(WeaponType.BALLISTIC, ballistic.getWeaponType());
        assertEquals(Set.of(HybridBallisticSlots.SLOT_TAG_PREFIX + "WS0001", HybridBallisticSlots.HULL_TAG), copy.getTags());
        assertTrue(shared.getTags().isEmpty());
        assertTrue(HybridBallisticSlots.converted(copy));
    }

    @Test
    void stockVariantsConvertedSpecsAndHullsWithoutBallisticSlotsAreLeftAlone() {
        CloneableTestHullSpec stockHull = spec(new LinkedHashSet<>(), new RetypableTestSlot("WS0001", WeaponType.BALLISTIC, false));
        ShipVariantAPI stock = variant(stockHull, VariantSource.STOCK);
        CloneableTestHullSpec convertedHull = spec(new LinkedHashSet<>(Set.of(HybridBallisticSlots.HULL_TAG)),
                new RetypableTestSlot("WS0001", WeaponType.HYBRID, false));
        ShipVariantAPI converted = variant(convertedHull, VariantSource.REFIT);
        CloneableTestHullSpec energyHull = spec(new LinkedHashSet<>(), new RetypableTestSlot("WS0001", WeaponType.ENERGY, false));
        ShipVariantAPI energyOnly = variant(energyHull, VariantSource.REFIT);

        HybridBallisticSlots.convert(stock);
        HybridBallisticSlots.convert(converted);
        HybridBallisticSlots.convert(energyOnly);
        HybridBallisticSlots.convert(null);

        verify(stockHull, never()).clone();
        verify(convertedHull, never()).clone();
        verify(energyHull, never()).clone();
        verify(stock, never()).setHullSpecAPI(any());
        verify(converted, never()).setHullSpecAPI(any());
        verify(energyOnly, never()).setHullSpecAPI(any());
    }

    @Test
    void restoreRetypesTheRecordedSlotsBackOnAFreshCopyAndDropsTheTags() {
        RetypableTestSlot hybrid = new RetypableTestSlot("WS0001", WeaponType.HYBRID, false);
        RetypableTestSlot nativeHybrid = new RetypableTestSlot("WS0002", WeaponType.HYBRID, false);
        Set<String> convertedTags = new LinkedHashSet<>(List.of("other_tag", HybridBallisticSlots.SLOT_TAG_PREFIX + "WS0001", HybridBallisticSlots.HULL_TAG));
        CloneableTestHullSpec converted = spec(convertedTags, hybrid, nativeHybrid);
        RetypableTestSlot hybridCopy = hybrid.copy();
        RetypableTestSlot nativeHybridCopy = nativeHybrid.copy();
        ShipHullSpecAPI copy = spec(new LinkedHashSet<>(convertedTags), hybridCopy, nativeHybridCopy);
        when(converted.clone()).thenReturn(copy);
        ShipVariantAPI variant = variant(converted, VariantSource.REFIT);

        HybridBallisticSlots.restore(variant);

        verify(variant).setHullSpecAPI(copy);
        assertEquals(WeaponType.BALLISTIC, hybridCopy.getWeaponType());
        assertEquals(WeaponType.HYBRID, nativeHybridCopy.getWeaponType());
        assertEquals(WeaponType.HYBRID, hybrid.getWeaponType());
        assertEquals(Set.of("other_tag"), copy.getTags());
        assertFalse(HybridBallisticSlots.converted(copy));
    }

    @Test
    void restoreLeavesAnUnconvertedHullAlone() {
        CloneableTestHullSpec hull = spec(new LinkedHashSet<>(), new RetypableTestSlot("WS0001", WeaponType.BALLISTIC, false));
        ShipVariantAPI variant = variant(hull, VariantSource.REFIT);

        HybridBallisticSlots.restore(variant);
        HybridBallisticSlots.restore(null);

        verify(hull, never()).clone();
        verify(variant, never()).setHullSpecAPI(any());
    }

    @Test
    void theSlotEffectConvertsTheVariantItIsAppliedTo() {
        RetypableTestSlot ballistic = new RetypableTestSlot("WS0001", WeaponType.BALLISTIC, false);
        CloneableTestHullSpec shared = spec(new LinkedHashSet<>(), ballistic);
        RetypableTestSlot ballisticCopy = ballistic.copy();
        ShipHullSpecAPI copy = spec(new LinkedHashSet<>(), ballisticCopy);
        when(shared.clone()).thenReturn(copy);
        ShipVariantAPI variant = variant(shared, VariantSource.REFIT);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getVariant()).thenReturn(variant);

        WeaponSkillEffect.BALLISTIC_SLOTS_HYBRID.apply(stats, "fafnir", 0f);

        verify(variant).setHullSpecAPI(copy);
        assertEquals(WeaponType.HYBRID, ballisticCopy.getWeaponType());
    }
}
