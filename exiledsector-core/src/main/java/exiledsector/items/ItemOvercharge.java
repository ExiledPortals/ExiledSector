package exiledsector.items;

import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ItemOvercharge {

    public static final float PAST_TOP_FRACTION = 0.25f;

    private ItemOvercharge() {
    }

    public static ItemData overchargeHullItem(ItemData item, Random random) {
        ItemDefinition definition = item == null ? null : item.definition();
        return definition == null || definition.type() != ItemType.HULL_ITEM ? item : overcharge(item, random);
    }

    public static ItemData overcharge(ItemData item, Random random) {
        Item preview = item == null ? null : item.preview();
        if (preview == null || preview.explicits().stream().anyMatch(RolledModifier::overcharged)) {
            return item;
        }
        List<RolledModifier> candidates = preview.explicits().stream().filter(rolled -> canOvercharge(preview, rolled)).toList();
        if (candidates.isEmpty()) {
            return item;
        }
        RolledModifier chosen = candidates.get(random.nextInt(candidates.size()));
        List<RolledModifier> explicits = new ArrayList<>(preview.explicits());
        explicits.set(explicits.indexOf(chosen), overcharged(preview, chosen));
        return new ItemData(item.definitionId(), item.seed(), explicits, item.frozenName(), item.traits());
    }

    static boolean canOvercharge(Item item, RolledModifier rolled) {
        Modifier modifier = item.explicitModifier(rolled.modifierId());
        if (modifier == null || rolled.lines().isEmpty()) {
            return false;
        }
        for (RolledLine line : rolled.lines()) {
            ModifierLine lineDefinition = modifier.line(line.effectName());
            if (lineDefinition == null || !lineDefinition.canOvercharge(ItemForging.lowerIsBetter(item, line.effectName()), PAST_TOP_FRACTION)) {
                return false;
            }
        }
        return true;
    }

    static RolledModifier overcharged(Item item, RolledModifier rolled) {
        Modifier modifier = item.explicitModifier(rolled.modifierId());
        List<RolledLine> lines = new ArrayList<>(rolled.lines().size());
        for (RolledLine line : rolled.lines()) {
            boolean lowerIsBetter = ItemForging.lowerIsBetter(item, line.effectName());
            lines.add(new RolledLine(line.effectName(), modifier.line(line.effectName()).overchargedRoll(lowerIsBetter, PAST_TOP_FRACTION)));
        }
        return rolled.overchargedWith(lines);
    }
}
