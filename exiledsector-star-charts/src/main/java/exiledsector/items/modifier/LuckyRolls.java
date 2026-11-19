package exiledsector.items.modifier;

import exiledsector.Seeds;
import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.crafting.ItemForging;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ScaleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public final class LuckyRolls {

    private static final long LUCK_SEED_SALT = 0x6c75636b79L;
    private static final long LUCKY_SET_SEED_SALT = 0x6c75636b79736574L;

    private LuckyRolls() {
    }

    public static ItemData withLuckyModifier(ItemData item) {
        if (item == null || !item.isHullItem()) {
            return item;
        }
        Item preview = item.preview();
        if (preview == null) {
            return item;
        }
        List<RolledModifier> explicits = preview.explicits();
        List<Integer> varying = new ArrayList<>();
        for (int index = 0; index < explicits.size(); index++) {
            if (canVary(preview, explicits.get(index))) {
                varying.add(index);
            }
        }
        if (varying.isEmpty()) {
            return item;
        }
        Random random = Seeds.derived(item.seed(), LUCK_SEED_SALT);
        int luckyIndex = varying.get(random.nextInt(varying.size()));
        RolledModifier rolled = explicits.get(luckyIndex);
        RolledModifier lucky = rolledAgain(preview, rolled, random);
        if (lucky.equals(rolled)) {
            return item;
        }
        List<RolledModifier> modifiers = new ArrayList<>(explicits);
        modifiers.set(luckyIndex, lucky);
        return item.withExplicits(modifiers, preview.frozenName());
    }

    public static ItemData withLuckyModifiers(ItemData item, Predicate<String> luckyModifierIds) {
        if (item == null || !item.isHullItem()) {
            return item;
        }
        Item preview = item.preview();
        if (preview == null) {
            return item;
        }
        Random random = Seeds.derived(item.seed(), LUCKY_SET_SEED_SALT);
        List<RolledModifier> modifiers = new ArrayList<>(preview.explicits());
        boolean changed = false;
        for (int index = 0; index < modifiers.size(); index++) {
            RolledModifier rolled = modifiers.get(index);
            if (luckyModifierIds.test(rolled.modifierId()) && canVary(preview, rolled)) {
                RolledModifier lucky = rolledAgain(preview, rolled, random);
                changed |= !lucky.equals(rolled);
                modifiers.set(index, lucky);
            }
        }
        return changed ? item.withExplicits(modifiers, preview.frozenName()) : item;
    }

    private static boolean canVary(Item item, RolledModifier rolled) {
        Modifier modifier = item.explicitModifier(rolled.modifierId());
        return !rolled.immutable() && !rolled.overcharged() && modifier != null && rolled.lines().stream().anyMatch(line -> {
            ModifierLine lineDefinition = modifier.line(line.effectName());
            return lineDefinition != null && lineDefinition.canVary();
        });
    }

    private static RolledModifier rolledAgain(Item item, RolledModifier rolled, Random random) {
        Modifier modifier = item.explicitModifier(rolled.modifierId());
        ScaleContext context = item.scaleContext(null);
        return rolled.mapLines(line -> {
            ModifierLine lineDefinition = modifier.line(line.effectName());
            if (lineDefinition == null || !lineDefinition.canVary()) {
                return line;
            }
            float secondRoll = lineDefinition.roll(random);
            boolean better = lineDefinition.isBetter(secondRoll, line.magnitude(), context, ItemForging.lowerIsBetter(item, line.effectName()));
            return better ? new RolledLine(line.effectName(), secondRoll) : line;
        });
    }
}
