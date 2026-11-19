package exiledsector.items.modifier;

import exiledsector.Seeds;
import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.ItemTraits;
import exiledsector.items.crafting.ItemForging;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ScaleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;

public final class LuckyRolls {

    private static final long LUCK_SEED_SALT = 0x6c75636b79L;
    private static final long EXPLICIT_ROLL_SALT = 0x6578706c6963L;
    private static final long IMPLICIT_ROLL_SALT = 0x696d706c6963L;

    private LuckyRolls() {
    }

    public static ItemData withLuckyModifier(ItemData item) {
        return withLuck(item, Luck.ONE_MODIFIER);
    }

    public static ItemData withLuck(ItemData item, Luck luck) {
        Item preview = hullItemPreview(item);
        if (preview == null || luck == null || luck.isNone()) {
            return item;
        }
        List<Integer> luckyIndices = luck.everyModifier() ? varyingIndices(preview, rolled -> true)
                : pickedIndices(varyingIndices(preview, rolled -> true), luck.luckyModifiers(), Seeds.derived(item.seed(), LUCK_SEED_SALT));
        ItemData lucky = withLuckyExplicits(item, preview, luckyIndices, luck.rolls());
        return luck.luckyImplicits() ? withLuckyImplicits(lucky, luck.rolls()) : lucky;
    }

    public static ItemData withLuckyModifiers(ItemData item, Predicate<RolledModifier> lucky, int rolls) {
        Item preview = hullItemPreview(item);
        return preview == null || lucky == null ? item : withLuckyExplicits(item, preview, varyingIndices(preview, lucky), rolls);
    }

    private static Item hullItemPreview(ItemData item) {
        return item == null || !item.isHullItem() ? null : item.preview();
    }

    private static List<Integer> varyingIndices(Item item, Predicate<RolledModifier> lucky) {
        List<RolledModifier> explicits = item.explicits();
        List<Integer> varying = new ArrayList<>();
        for (int index = 0; index < explicits.size(); index++) {
            RolledModifier rolled = explicits.get(index);
            if (canVary(item, rolled) && lucky.test(rolled)) {
                varying.add(index);
            }
        }
        return varying;
    }

    private static List<Integer> pickedIndices(List<Integer> varying, int count, Random random) {
        List<Integer> remaining = new ArrayList<>(varying);
        List<Integer> picked = new ArrayList<>();
        while (picked.size() < count && !remaining.isEmpty()) {
            picked.add(remaining.remove(random.nextInt(remaining.size())));
        }
        return picked;
    }

    private static ItemData withLuckyExplicits(ItemData item, Item preview, List<Integer> luckyIndices, int rolls) {
        List<RolledModifier> explicits = preview.explicits();
        List<RolledModifier> modifiers = new ArrayList<>(explicits);
        boolean changed = false;
        for (int index : luckyIndices) {
            RolledModifier rolled = explicits.get(index);
            Modifier modifier = preview.explicitModifier(rolled.modifierId());
            Random random = Seeds.derived(item.seed(), EXPLICIT_ROLL_SALT + rolled.modifierId().hashCode());
            RolledModifier lucky = luckier(preview, rolled, modifier::line, rolls, random);
            if (!lucky.equals(rolled)) {
                modifiers.set(index, lucky);
                changed = true;
            }
        }
        return changed ? item.withExplicits(modifiers, preview.frozenName()) : item;
    }

    private static ItemData withLuckyImplicits(ItemData item, int rolls) {
        Item preview = item.preview();
        if (preview == null) {
            return item;
        }
        List<RolledModifier> implicits = new ArrayList<>(preview.implicits());
        boolean changed = false;
        for (int index = 0; index < implicits.size(); index++) {
            RolledModifier implicit = implicits.get(index);
            if (implicit.immutable() || !preview.implicitCanVary(implicit)) {
                continue;
            }
            Random random = Seeds.derived(item.seed(), IMPLICIT_ROLL_SALT + implicit.modifierId().hashCode());
            RolledModifier lucky = luckier(preview, implicit, preview::implicitLine, rolls, random);
            if (!lucky.equals(implicit)) {
                implicits.set(index, lucky);
                changed = true;
            }
        }
        if (!changed) {
            return item;
        }
        ItemTraits traits = item.traits() != null ? item.traits() : preview.traits();
        return item.withTraits(new ItemTraits(traits.tier(), traits.quantity(), traits.corruption(), implicits, traits.mirrored()));
    }

    private static boolean canVary(Item item, RolledModifier rolled) {
        Modifier modifier = item.explicitModifier(rolled.modifierId());
        return !rolled.immutable() && !rolled.overcharged() && modifier != null && rolled.lines().stream().anyMatch(line -> {
            ModifierLine lineDefinition = modifier.line(line.effectName());
            return lineDefinition != null && lineDefinition.canVary();
        });
    }

    private static RolledModifier luckier(Item item, RolledModifier rolled, Function<String, ModifierLine> lineDefinitions, int rolls,
                                          Random random) {
        ScaleContext context = item.scaleContext(null);
        return rolled.mapLines(line -> {
            ModifierLine lineDefinition = lineDefinitions.apply(line.effectName());
            if (lineDefinition == null || !lineDefinition.canVary()) {
                return line;
            }
            boolean lowerIsBetter = ItemForging.lowerIsBetter(item, line.effectName());
            float best = line.magnitude();
            boolean improved = false;
            for (int roll = 1; roll < rolls; roll++) {
                float another = lineDefinition.roll(random);
                if (lineDefinition.isBetter(another, best, context, lowerIsBetter)) {
                    best = another;
                    improved = true;
                }
            }
            return improved ? new RolledLine(line.effectName(), best) : line;
        });
    }
}
