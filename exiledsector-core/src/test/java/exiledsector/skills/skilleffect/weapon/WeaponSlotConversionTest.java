package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WeaponSlotConversionTest {

    private static final WeaponSlotConversion.Rule BALLISTIC_TO_HYBRID = new WeaponSlotConversion.Rule(WeaponType.BALLISTIC, null, WeaponType.HYBRID);
    private static final WeaponSlotConversion.Rule BALLISTIC_TO_ENERGY = new WeaponSlotConversion.Rule(WeaponType.BALLISTIC, null, WeaponType.ENERGY);
    private static final WeaponSlotConversion.Rule ENERGY_TO_BALLISTIC = new WeaponSlotConversion.Rule(WeaponType.ENERGY, null, WeaponType.BALLISTIC);
    private static final WeaponSlotConversion.Rule SMALL_TO_UNIVERSAL = new WeaponSlotConversion.Rule(null, WeaponSize.SMALL, WeaponType.UNIVERSAL);

    private static CloneableTestHullSpec spec(Set<String> tags, RetypableTestSlot... slots) {
        CloneableTestHullSpec spec = mock(CloneableTestHullSpec.class);
        when(spec.getAllWeaponSlotsCopy()).thenAnswer(call -> new ArrayList<>(List.of(slots)));
        for (RetypableTestSlot slot : slots) {
            when(spec.getWeaponSlotAPI(slot.getId())).thenReturn(slot);
        }
        when(spec.getTags()).thenReturn(tags);
        when(spec.hasTag(anyString())).thenAnswer(call -> tags.contains(call.<String>getArgument(0)));
        doAnswer(call -> tags.add(call.getArgument(0))).when(spec).addTag(anyString());
        when(spec.clone()).thenAnswer(call -> {
            RetypableTestSlot[] copies = new RetypableTestSlot[slots.length];
            for (int i = 0; i < slots.length; i++) {
                copies[i] = slots[i].copy();
            }
            return spec(new LinkedHashSet<>(tags), copies);
        });
        return spec;
    }

    private static ShipVariantAPI variant(ShipHullSpecAPI hullSpec, VariantSource source) {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        ShipHullSpecAPI[] currentHull = {hullSpec};
        when(variant.getHullSpec()).thenAnswer(call -> currentHull[0]);
        doAnswer(call -> currentHull[0] = call.getArgument(0)).when(variant).setHullSpecAPI(any());
        when(variant.getSource()).thenReturn(source);
        return variant;
    }

    private static WeaponType slotType(ShipVariantAPI variant, String slotId) {
        return variant.getHullSpec().getWeaponSlotAPI(slotId).getWeaponType();
    }

    private static String slotTag(String slotId, WeaponType originalType) {
        return WeaponSlotConversion.SLOT_TAG_PREFIX + slotId + WeaponSlotConversion.ORIGINAL_TYPE_SEPARATOR + originalType.name();
    }

    @Test
    void syncRetypesOnlyTheCopysNonBuiltInMatchingSlotsAndRecordsTheirOriginalTypes() {
        RetypableTestSlot ballistic = new RetypableTestSlot("WS0001", WeaponType.BALLISTIC, false);
        CloneableTestHullSpec shared = spec(new LinkedHashSet<>(), ballistic, new RetypableTestSlot("WS0002", WeaponType.ENERGY, false),
                new RetypableTestSlot("WS0003", WeaponType.BALLISTIC, true));
        ShipVariantAPI variant = variant(shared, VariantSource.REFIT);

        WeaponSlotConversion.sync(variant, List.of(BALLISTIC_TO_HYBRID));

        assertEquals(WeaponType.HYBRID, slotType(variant, "WS0001"));
        assertEquals(WeaponType.ENERGY, slotType(variant, "WS0002"));
        assertEquals(WeaponType.BALLISTIC, slotType(variant, "WS0003"));
        assertEquals(WeaponType.BALLISTIC, ballistic.getWeaponType());
        assertEquals(Set.of(slotTag("WS0001", WeaponType.BALLISTIC), WeaponSlotConversion.HULL_TAG), variant.getHullSpec().getTags());
        assertTrue(shared.getTags().isEmpty());
        assertTrue(WeaponSlotConversion.converted(variant.getHullSpec()));
    }

    @Test
    void theFirstMatchingRuleWinsAndRulesReadTheOriginalTypeSoConversionsNeverChain() {
        CloneableTestHullSpec shared = spec(new LinkedHashSet<>(), new RetypableTestSlot("WS0001", WeaponType.BALLISTIC, false),
                new RetypableTestSlot("WS0002", WeaponType.ENERGY, false),
                new RetypableTestSlot("WS0003", WeaponType.MISSILE, false, WeaponSize.SMALL),
                new RetypableTestSlot("WS0004", WeaponType.SYSTEM, false, WeaponSize.SMALL),
                new RetypableTestSlot("WS0005", WeaponType.STATION_MODULE, false, WeaponSize.SMALL));
        ShipVariantAPI variant = variant(shared, VariantSource.REFIT);
        List<WeaponSlotConversion.Rule> rules = List.of(BALLISTIC_TO_ENERGY, ENERGY_TO_BALLISTIC, BALLISTIC_TO_HYBRID, SMALL_TO_UNIVERSAL);

        WeaponSlotConversion.sync(variant, rules);
        ShipHullSpecAPI converted = variant.getHullSpec();
        WeaponSlotConversion.sync(variant, rules);

        assertSame(converted, variant.getHullSpec());
        assertEquals(WeaponType.ENERGY, slotType(variant, "WS0001"));
        assertEquals(WeaponType.BALLISTIC, slotType(variant, "WS0002"));
        assertEquals(WeaponType.UNIVERSAL, slotType(variant, "WS0003"));
        assertEquals(WeaponType.SYSTEM, slotType(variant, "WS0004"));
        assertEquals(WeaponType.STATION_MODULE, slotType(variant, "WS0005"));
        assertEquals(Set.of(WeaponType.BALLISTIC), WeaponSlotConversion.conflicts(converted, rules));
        assertEquals(Set.of(), WeaponSlotConversion.conflicts(converted, List.of(BALLISTIC_TO_ENERGY, ENERGY_TO_BALLISTIC)));
    }

    @Test
    void changingTheRulesRetypesFromTheRecordedOriginalType() {
        CloneableTestHullSpec converted = spec(new LinkedHashSet<>(List.of(slotTag("WS0001", WeaponType.BALLISTIC), WeaponSlotConversion.HULL_TAG)),
                new RetypableTestSlot("WS0001", WeaponType.HYBRID, false));
        ShipVariantAPI variant = variant(converted, VariantSource.REFIT);

        WeaponSlotConversion.sync(variant, List.of(BALLISTIC_TO_ENERGY));

        assertEquals(WeaponType.ENERGY, slotType(variant, "WS0001"));
        assertEquals(Set.of(slotTag("WS0001", WeaponType.BALLISTIC), WeaponSlotConversion.HULL_TAG), variant.getHullSpec().getTags());
    }

    @Test
    void stockVariantsAndHullsWithNothingToConvertAreLeftAlone() {
        CloneableTestHullSpec stockHull = spec(new LinkedHashSet<>(), new RetypableTestSlot("WS0001", WeaponType.BALLISTIC, false));
        ShipVariantAPI stock = variant(stockHull, VariantSource.STOCK);
        CloneableTestHullSpec energyHull = spec(new LinkedHashSet<>(), new RetypableTestSlot("WS0001", WeaponType.ENERGY, false));
        ShipVariantAPI energyOnly = variant(energyHull, VariantSource.REFIT);

        WeaponSlotConversion.sync(stock, List.of(BALLISTIC_TO_HYBRID));
        WeaponSlotConversion.add(stock, BALLISTIC_TO_HYBRID);
        WeaponSlotConversion.sync(energyOnly, List.of(BALLISTIC_TO_HYBRID));
        WeaponSlotConversion.sync(null, List.of(BALLISTIC_TO_HYBRID));

        verify(stockHull, never()).clone();
        verify(energyHull, never()).clone();
        verify(stock, never()).setHullSpecAPI(any());
        verify(energyOnly, never()).setHullSpecAPI(any());
    }

    @Test
    void restoreRetypesTheRecordedSlotsBackOnAFreshCopyAndDropsTheTags() {
        RetypableTestSlot hybrid = new RetypableTestSlot("WS0001", WeaponType.HYBRID, false);
        CloneableTestHullSpec converted = spec(new LinkedHashSet<>(List.of("other_tag", slotTag("WS0001", WeaponType.BALLISTIC),
                WeaponSlotConversion.HULL_TAG)), hybrid, new RetypableTestSlot("WS0002", WeaponType.HYBRID, false));
        ShipVariantAPI variant = variant(converted, VariantSource.REFIT);

        WeaponSlotConversion.restore(variant);

        assertEquals(WeaponType.BALLISTIC, slotType(variant, "WS0001"));
        assertEquals(WeaponType.HYBRID, slotType(variant, "WS0002"));
        assertEquals(WeaponType.HYBRID, hybrid.getWeaponType());
        assertEquals(Set.of("other_tag"), variant.getHullSpec().getTags());
        assertFalse(WeaponSlotConversion.converted(variant.getHullSpec()));
    }

    @Test
    void restoreLeavesAnUnconvertedHullAlone() {
        CloneableTestHullSpec hull = spec(new LinkedHashSet<>(), new RetypableTestSlot("WS0001", WeaponType.BALLISTIC, false));
        ShipVariantAPI variant = variant(hull, VariantSource.REFIT);

        WeaponSlotConversion.restore(variant);
        WeaponSlotConversion.restore(null);

        verify(hull, never()).clone();
        verify(variant, never()).setHullSpecAPI(any());
    }

    @Test
    void addKeepsEarlierConversionsAndOnlyClaimsSlotsNoRuleHasChanged() {
        CloneableTestHullSpec shared = spec(new LinkedHashSet<>(), new RetypableTestSlot("WS0001", WeaponType.BALLISTIC, false),
                new RetypableTestSlot("WS0002", WeaponType.ENERGY, false));
        ShipVariantAPI variant = variant(shared, VariantSource.REFIT);

        WeaponSlotConversion.add(variant, BALLISTIC_TO_HYBRID);
        ShipHullSpecAPI hybridHull = variant.getHullSpec();
        WeaponSlotConversion.add(variant, BALLISTIC_TO_ENERGY);
        assertSame(hybridHull, variant.getHullSpec());
        WeaponSlotConversion.add(variant, ENERGY_TO_BALLISTIC);

        assertEquals(WeaponType.HYBRID, slotType(variant, "WS0001"));
        assertEquals(WeaponType.BALLISTIC, slotType(variant, "WS0002"));
        assertEquals(Set.of(slotTag("WS0001", WeaponType.BALLISTIC), slotTag("WS0002", WeaponType.ENERGY), WeaponSlotConversion.HULL_TAG),
                variant.getHullSpec().getTags());
    }

    @Test
    void theSlotEffectsCarryTheirRulesAndConvertTheVariantOutsideTheOpCostPass() {
        CloneableTestHullSpec shared = spec(new LinkedHashSet<>(), new RetypableTestSlot("WS0001", WeaponType.ENERGY, false));
        ShipVariantAPI variant = variant(shared, VariantSource.REFIT);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getVariant()).thenReturn(variant);

        WeaponSkillEffect.ENERGY_SLOTS_BALLISTIC.apply(stats, "breech", 0f);
        assertSame(shared, variant.getHullSpec());
        when(stats.getFleetMember()).thenReturn(mock(FleetMemberAPI.class));
        WeaponSkillEffect.ENERGY_SLOTS_BALLISTIC.apply(stats, "breech", 0f);

        assertEquals(WeaponType.BALLISTIC, slotType(variant, "WS0001"));
        assertEquals(BALLISTIC_TO_HYBRID, SlotConversionBacking.ruleOf(WeaponSkillEffect.BALLISTIC_SLOTS_HYBRID));
        assertEquals(SMALL_TO_UNIVERSAL, SlotConversionBacking.ruleOf(WeaponSkillEffect.SMALL_SLOTS_UNIVERSAL));
        assertNull(SlotConversionBacking.ruleOf(WeaponSkillEffect.WEAPON_DAMAGE_MULT_PER_DMOD));
    }
}
