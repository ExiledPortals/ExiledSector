package exiledsector.items.crafting;

import exiledsector.items.Item;
import exiledsector.items.ItemDefinition;
import exiledsector.items.modifier.AffixLayout;
import exiledsector.items.modifier.ItemModifiers;
import exiledsector.items.modifier.ModifierExclusivity;
import exiledsector.items.modifier.RolledModifier;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public final class ForgeInfusions {

    public enum Block {
        UNIQUE,
        NONE,
        PRESENT,
        CLASH,
        LIMIT,
        FULL
    }

    @FunctionalInterface
    public interface Source {

        Source NONE = definition -> null;

        String modifierFor(ItemDefinition definition);

        default boolean hasRoomFor(List<RolledModifier> present, String modifierId) {
            return true;
        }
    }

    private ForgeInfusions() {
    }

    static String modifierFor(Item item, Source source) {
        ItemDefinition definition = item == null ? null : item.definition();
        return definition == null || source == null ? null : source.modifierFor(definition);
    }

    static Block block(Item item, Source source) {
        ItemDefinition definition = item.definition();
        if (definition == null || definition.unique() || definition.keepsOneModifier()) {
            return Block.UNIQUE;
        }
        String modifierId = modifierFor(item, source);
        Modifier modifier = modifierId == null ? null : ItemModifiers.get(modifierId);
        if (modifier == null || modifier.slot() == ModifierSlot.IMPLICIT || !ItemModifiers.itemTypes(modifierId).contains(definition.type())) {
            return Block.NONE;
        }
        List<RolledModifier> present = item.explicits();
        if (present.stream().anyMatch(rolled -> rolled.modifierId().equals(modifierId))) {
            return Block.PRESENT;
        }
        Set<String> effects = modifier.lines().stream().map(ModifierLine::effectName).collect(Collectors.toSet());
        boolean sharesEffect = present.stream().anyMatch(rolled -> rolled.lines().stream().anyMatch(line -> effects.contains(line.effectName())));
        if (sharesEffect || ModifierExclusivity.clashesWithRolled(modifierId, present)) {
            return Block.CLASH;
        }
        if (!source.hasRoomFor(present, modifierId)) {
            return Block.LIMIT;
        }
        AffixLayout.Limits limits = AffixLayout.limits(definition.type());
        long sameSide = present.stream().filter(rolled -> rolled.slot() == modifier.slot()).count();
        return present.size() >= limits.maxModifiers() || sameSide >= limits.maxPerSide() ? Block.FULL : null;
    }

    static List<ForgeChoice> choices(Item item, Source source) {
        return block(item, source) == null ? List.of(ForgeChoice.WHOLE_ITEM) : List.of();
    }

    static String refusalReason(ForgeOperation operation, Item item, Source source) {
        Block block = block(item, source);
        return block == null ? null : operation.id() + "." + block.name().toLowerCase(Locale.ROOT);
    }

    static boolean infuse(Item item, Source source, Random random) {
        if (block(item, source) != null) {
            return false;
        }
        item.addExplicit(RolledModifier.roll(ItemModifiers.get(modifierFor(item, source)), random));
        return true;
    }
}
