package exiledsector.items.drop;

import exiledsector.items.ItemDefinition;
import exiledsector.loot.DropType;

import java.util.Random;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public final class UniqueDropRoll {

    private UniqueDropRoll() {
    }

    public static ItemDefinition roll(Random random, float baseChance, ItemChances chances, Function<Random, ItemDefinition> picker) {
        return roll(random, type -> baseChance * chances.quantity(type), picker);
    }

    public static ItemDefinition rollScaledByRarity(Random random, float baseChance, ItemChances chances,
                                                    Function<Random, ItemDefinition> picker) {
        return roll(random, type -> baseChance * chances.rarity(type) * chances.quantity(type), picker);
    }

    public static ItemDefinition rollConversion(Random random, float baseShare, ItemChances chances, Function<Random, ItemDefinition> picker) {
        return roll(random, type -> conversionChance(baseShare, chances, type), picker);
    }

    public static float conversionChance(float baseShare, ItemChances chances, DropType type) {
        return Math.min(1f, baseShare * chances.rarity(type));
    }

    public static ItemDefinition roll(Random random, ToDoubleFunction<DropType> chanceOf, Function<Random, ItemDefinition> picker) {
        float uniqueRoll = random.nextFloat();
        if (uniqueRoll >= highestChance(chanceOf)) {
            return null;
        }
        ItemDefinition unique = picker.apply(random);
        if (unique == null) {
            return null;
        }
        return uniqueRoll < (float) chanceOf.applyAsDouble(DropType.forDefinition(unique)) ? unique : null;
    }

    private static float highestChance(ToDoubleFunction<DropType> chanceOf) {
        float highest = Math.max((float) chanceOf.applyAsDouble(DropType.ITEM), (float) chanceOf.applyAsDouble(DropType.HULL_ITEM));
        for (DropType type : DropType.values()) {
            if (type.itemKind() != null) {
                highest = Math.max(highest, (float) chanceOf.applyAsDouble(type));
            }
        }
        return highest;
    }
}
