package exiledsector.items;

import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ForgeInfusions {

    public enum Block {
        UNIQUE,
        NONE,
        PRESENT,
        CLASH,
        FULL
    }

    private static volatile Function<ItemDefinition, String> source = definition -> null;

    private ForgeInfusions() {
    }

    public static void use(Function<ItemDefinition, String> modifierForDefinition) {
        source = modifierForDefinition == null ? definition -> null : modifierForDefinition;
    }

    static String modifierFor(Item item) {
        ItemDefinition definition = item == null ? null : item.definition();
        return definition == null ? null : source.apply(definition);
    }

    static Block block(Item item) {
        ItemDefinition definition = item.definition();
        if (definition == null || definition.unique() || definition.keepsOneModifier()) {
            return Block.UNIQUE;
        }
        String modifierId = modifierFor(item);
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
        AffixLayout.Limits limits = AffixLayout.limits(definition.type());
        long sameSide = present.stream().filter(rolled -> rolled.slot() == modifier.slot()).count();
        return present.size() >= limits.maxModifiers() || sameSide >= limits.maxPerSide() ? Block.FULL : null;
    }

    static boolean infuse(Item item, Random random) {
        if (block(item) != null) {
            return false;
        }
        item.addExplicit(RolledModifier.roll(ItemModifiers.get(modifierFor(item)), random));
        return true;
    }
}
