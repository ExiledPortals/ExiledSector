package exiledlib.skills;

import exiledlib.skills.skilleffect.SkillEffect;
import exiledlib.skills.skilleffect.SkillEffectNames;
import exiledlib.skills.skilleffect.defense.DefenseSkillEffect;
import exiledlib.skills.skilleffect.logistics.LogisticsSkillEffect;
import exiledlib.skills.skilleffect.special.SlotItemEffect;
import exiledlib.skills.skilleffect.special.SpecialSkillEffect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocketedItemScalingTest {

    private record Slot(String category, String slotId, String targetSlotId) implements SocketedItem {
    }

    private static SkillTypeEffect effect(SkillEffect effect, float magnitude) {
        return new SkillTypeEffect(effect, magnitude);
    }

    private static SkillTypeEffect hull(float magnitude) {
        return effect(DefenseSkillEffect.HULL_PERCENT, magnitude);
    }

    private static EffectContribution item(String slotId, String targetSlotId, SkillTypeEffect... effects) {
        return EffectContribution.scaledPermanent(new Slot(SocketedItem.HULL_ITEM, slotId, targetSlotId), slotId + "_", List.of(effects));
    }

    private static EffectContribution subroutine(String socketId, SkillTypeEffect... effects) {
        return EffectContribution.scaledPermanent(new Slot(SocketedItem.SUBROUTINE, socketId, null), socketId + "_", List.of(effects));
    }

    private static EffectContribution node(SkillTypeEffect... effects) {
        return EffectContribution.scaledPermanent("node", "node_", List.of(effects));
    }

    private static float total(List<EffectContribution> contributions, SkillEffect effect) {
        return contributions.stream().flatMap(contribution -> contribution.effects().stream())
                .filter(applied -> applied.effect() == effect).map(SkillTypeEffect::magnitude).reduce(0f, Float::sum);
    }

    @Test
    void withoutScalingEffectsTheContributionsAreReturnedUnchanged() {
        List<EffectContribution> contributions = List.of(item("bridge", null, hull(5f)), node(hull(3f)));

        assertSame(contributions, SocketedItemScaling.withScaledItems(contributions));
    }

    @Test
    void aTargetingItemScalesOnlyTheItemInItsTargetSlot() {
        List<EffectContribution> scaled = SocketedItemScaling.withScaledItems(List.of(
                item("bridge", "shield", hull(1f), effect(SpecialSkillEffect.TARGETED_HULL_ITEM_EFFECT_PERCENT, 100f)),
                item("shield", null, hull(10f)),
                item("reactor", null, hull(20f))));

        assertEquals(41f, total(scaled, DefenseSkillEffect.HULL_PERCENT), 1e-4f);
    }

    @Test
    void percentsFromEverySourceAddUpAndOnlySocketedItemsAreScaled() {
        List<EffectContribution> scaled = SocketedItemScaling.withScaledItems(List.of(
                item("bridge", "shield", effect(SpecialSkillEffect.TARGETED_HULL_ITEM_EFFECT_PERCENT, 100f)),
                item("engine", "shield", effect(SpecialSkillEffect.TARGETED_HULL_ITEM_EFFECT_PERCENT, 100f)),
                item("shield", null, hull(10f)),
                node(hull(5f), effect(SpecialSkillEffect.SOCKETED_HULL_ITEM_EFFECT_PERCENT, 50f))));

        assertEquals(10f * 3.5f + 5f, total(scaled, DefenseSkillEffect.HULL_PERCENT), 1e-4f);
    }

    @Test
    void scalingEffectsAreNeverScaledThemselves() {
        List<EffectContribution> scaled = SocketedItemScaling.withScaledItems(List.of(
                item("bridge", "shield", effect(SpecialSkillEffect.TARGETED_HULL_ITEM_EFFECT_PERCENT, 100f)),
                item("shield", "bridge", effect(SpecialSkillEffect.TARGETED_HULL_ITEM_EFFECT_PERCENT, 100f)),
                node(effect(SpecialSkillEffect.SOCKETED_HULL_ITEM_EFFECT_PERCENT, 50f))));

        assertEquals(200f, total(scaled, SpecialSkillEffect.TARGETED_HULL_ITEM_EFFECT_PERCENT), 1e-4f);
    }

    @Test
    void aTargetedEffectOnANodeDoesNothing() {
        List<EffectContribution> contributions = List.of(
                node(effect(SpecialSkillEffect.TARGETED_HULL_ITEM_EFFECT_PERCENT, 100f)),
                item("shield", null, hull(10f)));

        assertEquals(10f, total(SocketedItemScaling.withScaledItems(contributions), DefenseSkillEffect.HULL_PERCENT), 1e-4f);
    }

    @Test
    void theDrawbackEffectScalesOnlyLinesThatWorkAgainstTheShip() {
        List<EffectContribution> scaled = SocketedItemScaling.withScaledItems(List.of(
                item("shield", null, hull(10f), hull(-4f), effect(LogisticsSkillEffect.SUPPLIES_PER_MONTH_MULT, 10f)),
                node(effect(SpecialSkillEffect.SOCKETED_HULL_ITEM_DRAWBACK_EFFECT_PERCENT, -50f))));

        assertEquals(8f, total(scaled, DefenseSkillEffect.HULL_PERCENT), 1e-4f);
        assertEquals(5f, total(scaled, LogisticsSkillEffect.SUPPLIES_PER_MONTH_MULT), 1e-4f);
    }

    @Test
    void scalingNeverFlipsALineAndIgnoresTemporarySources() {
        List<EffectContribution> scaled = SocketedItemScaling.withScaledItems(List.of(
                item("shield", null, hull(10f)),
                node(effect(SpecialSkillEffect.SOCKETED_HULL_ITEM_EFFECT_PERCENT, -150f)),
                new EffectContribution("buff", "buff_", List.of(effect(SpecialSkillEffect.SOCKETED_HULL_ITEM_EFFECT_PERCENT, 500f)),
                        5f, null, true, true)));

        assertEquals(0f, total(scaled, DefenseSkillEffect.HULL_PERCENT), 1e-4f);
    }

    @Test
    void aSlotEffectScalesOnlyTheItemInItsSlotAndIsFoundByName() {
        SlotItemEffect slotEffect = SlotItemEffect.register(SocketedItem.HULL_ITEM, "test_slot", () -> "Test Slot");

        List<EffectContribution> scaled = SocketedItemScaling.withScaledItems(List.of(
                item("test_slot", null, hull(10f)),
                item("other_slot", null, hull(10f)),
                node(effect(slotEffect, 50f))));

        assertEquals(25f, total(scaled, DefenseSkillEffect.HULL_PERCENT), 1e-4f);
        assertSame(slotEffect, SlotItemEffect.register(SocketedItem.HULL_ITEM, "test_slot", () -> "Renamed"));
        assertSame(slotEffect, SkillEffect.byName("TEST_SLOT_ITEM_EFFECT_PERCENT"));
        assertTrue(SkillEffectNames.all().contains("TEST_SLOT_ITEM_EFFECT_PERCENT"));
    }

    @Test
    void hullItemAndSubroutineEffectsOnlyScaleTheirOwnCategory() {
        List<EffectContribution> scaled = SocketedItemScaling.withScaledItems(List.of(
                item("shield", null, hull(10f)),
                subroutine("socket_a", hull(20f)),
                node(effect(SpecialSkillEffect.SOCKETED_HULL_ITEM_EFFECT_PERCENT, 100f), effect(SpecialSkillEffect.SOCKETED_SUBROUTINE_EFFECT_PERCENT, 50f))));

        assertEquals(10f * 2f + 20f * 1.5f, total(scaled, DefenseSkillEffect.HULL_PERCENT), 1e-4f);
    }

    @Test
    void aSubroutineCanScaleHullItemsWithoutScalingItself() {
        List<EffectContribution> scaled = SocketedItemScaling.withScaledItems(List.of(
                item("shield", null, hull(10f)),
                subroutine("socket_a", hull(4f), hull(-2f), effect(SpecialSkillEffect.SOCKETED_HULL_ITEM_EFFECT_PERCENT, 100f)),
                node(effect(SpecialSkillEffect.SOCKETED_SUBROUTINE_DRAWBACK_EFFECT_PERCENT, -50f))));

        assertEquals(20f + 4f - 1f, total(scaled, DefenseSkillEffect.HULL_PERCENT), 1e-4f);
        assertEquals(100f, total(scaled, SpecialSkillEffect.SOCKETED_HULL_ITEM_EFFECT_PERCENT), 1e-4f);
    }

    @Test
    void onlyTheTargetedScopeTargetsAnotherItem() {
        assertTrue(SpecialSkillEffect.TARGETED_HULL_ITEM_EFFECT_PERCENT.targetsSocketedItem());
        assertFalse(SpecialSkillEffect.SOCKETED_HULL_ITEM_EFFECT_PERCENT.targetsSocketedItem());
        assertTrue(SpecialSkillEffect.SOCKETED_HULL_ITEM_DRAWBACK_EFFECT_PERCENT.lowerIsBetter());
    }
}
