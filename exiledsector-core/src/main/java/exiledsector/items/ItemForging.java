package exiledsector.items;

import exiledlib.modifiers.ModifierEffect;
import exiledlib.skills.skilleffect.SkillEffect;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierEffects;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ScaleContext;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

public final class ItemForging {

    static final String PART_ROOT = SkillEffect.class.getPackageName() + ".";

    private ItemForging() {
    }

    public static String partOf(ModifierEffect effect) {
        if (!(effect instanceof SkillEffect)) {
            return null;
        }
        Class<?> effectClass = effect instanceof Enum<?> constant ? constant.getDeclaringClass() : effect.getClass();
        String packageName = effectClass.getPackageName();
        if (!packageName.startsWith(PART_ROOT)) {
            return null;
        }
        String part = packageName.substring(PART_ROOT.length());
        int nested = part.indexOf('.');
        return nested < 0 ? part : part.substring(0, nested);
    }

    static Set<String> partsOf(Modifier modifier, ModifierEffects effects) {
        Set<String> parts = new LinkedHashSet<>();
        for (ModifierLine line : modifier.lines()) {
            String part = partOf(effects.byName(line.effectName()));
            if (part != null) {
                parts.add(part);
            }
        }
        return parts;
    }

    static boolean lowerIsBetter(Item item, String effectName) {
        ModifierEffect effect = item.modifierEffects().byName(effectName);
        return effect != null && effect.invertsDescriptionColours();
    }

    private static float bestRoll(Item item, ModifierLine lineDefinition, RolledEffect line, ScaleContext context) {
        return lineDefinition.bestRoll(context, lowerIsBetter(item, line.effectName()));
    }

    private static boolean improves(Item item, ModifierLine lineDefinition, RolledEffect line, ScaleContext context) {
        return lineDefinition != null && lineDefinition.canVary()
                && lineDefinition.isBetter(bestRoll(item, lineDefinition, line, context), line.magnitude(), context,
                        lowerIsBetter(item, line.effectName()));
    }

    public static List<String> graftParts(Item item) {
        ItemDefinition definition = item.definition();
        if (definition == null) {
            return List.of();
        }
        Set<String> parts = new TreeSet<>();
        for (List<PoolEntry> side : AffixLayout.openSides(definition, item.explicits(), item.tier())) {
            side.forEach(entry -> parts.addAll(partsOf(entry.modifier(), item.modifierEffects())));
        }
        return List.copyOf(parts);
    }

    public static RolledModifier graft(Item item, String part, Random random) {
        ItemDefinition definition = item.definition();
        if (definition == null || part == null) {
            return null;
        }
        List<List<PoolEntry>> sides = new ArrayList<>();
        for (List<PoolEntry> side : AffixLayout.openSides(definition, item.explicits(), item.tier())) {
            List<PoolEntry> partEntries = side.stream().filter(entry -> partsOf(entry.modifier(), item.modifierEffects()).contains(part)).toList();
            if (!partEntries.isEmpty()) {
                sides.add(partEntries);
            }
        }
        if (sides.isEmpty()) {
            return null;
        }
        PoolEntry poolEntry = WeightedPick.pick(sides.get(random.nextInt(sides.size())), PoolEntry::weight, random);
        RolledModifier added = RolledModifier.roll(poolEntry.modifier(), random, false);
        ItemRarity rarityBefore = item.rarity();
        List<RolledModifier> modifiers = new ArrayList<>(item.explicits());
        modifiers.add(AffixLayout.insertIndex(modifiers, added.slot()), added);
        item.replaceExplicits(modifiers);
        if (item.rarity() != rarityBefore) {
            item.refreezeName();
        }
        return added;
    }

    public static List<RolledModifier> excisable(Item item) {
        ItemDefinition definition = item.definition();
        if (definition == null || definition.unique()) {
            return List.of();
        }
        return item.explicits().stream().filter(rolled -> !rolled.locked()).toList();
    }

    public static boolean excise(Item item, RolledModifier removed) {
        if (!excisable(item).contains(removed)) {
            return false;
        }
        List<RolledModifier> remaining = new ArrayList<>(item.explicits());
        remaining.remove(removed);
        item.replaceExplicits(remaining);
        item.refreezeName();
        return true;
    }

    public static List<RolledModifier> perfectible(Item item) {
        return item.explicits().stream().filter(rolled -> !perfected(item, rolled).equals(rolled)).toList();
    }

    public static boolean perfect(Item item, RolledModifier chosen) {
        int index = item.explicits().indexOf(chosen);
        if (index < 0) {
            return false;
        }
        RolledModifier perfected = perfected(item, chosen);
        if (perfected.equals(chosen)) {
            return false;
        }
        List<RolledModifier> modifiers = new ArrayList<>(item.explicits());
        modifiers.set(index, perfected);
        item.replaceExplicits(modifiers);
        return true;
    }

    private static RolledModifier perfected(Item item, RolledModifier rolled) {
        Modifier modifier = item.explicitModifier(rolled.modifierId());
        ScaleContext context = item.scaleContext(null);
        if (modifier == null) {
            return rolled;
        }
        List<RolledEffect> lines = new ArrayList<>(rolled.lines().size());
        for (RolledEffect line : rolled.lines()) {
            ModifierLine lineDefinition = modifier.line(line.effectName());
            boolean improved = improves(item, lineDefinition, line, context);
            lines.add(improved ? new RolledEffect(line.effectName(), bestRoll(item, lineDefinition, line, context)) : line);
        }
        return rolled.withLines(lines);
    }

    public static List<RolledModifier> anchors(Item item) {
        ItemDefinition definition = item.definition();
        if (definition == null || definition.unique() || item.explicits().stream().filter(rolled -> !rolled.locked()).count() < 2) {
            return List.of();
        }
        return item.explicits().stream().filter(rolled -> !rolled.locked()).toList();
    }

    public static boolean anchorReroll(Item item, RolledModifier anchor, Random random) {
        ItemDefinition definition = item.definition();
        if (!anchors(item).contains(anchor)) {
            return false;
        }
        int modifierCount = item.explicits().size();
        List<RolledModifier> modifiers = new ArrayList<>(item.explicits().stream().filter(rolled -> rolled.locked() || rolled.equals(anchor)).toList());
        while (modifiers.size() < modifierCount) {
            List<List<PoolEntry>> openSides = AffixLayout.openSides(definition, modifiers, item.tier());
            if (openSides.isEmpty()) {
                break;
            }
            PoolEntry poolEntry = WeightedPick.pick(openSides.get(random.nextInt(openSides.size())), PoolEntry::weight, random);
            RolledModifier added = RolledModifier.roll(poolEntry.modifier(), random, false);
            modifiers.add(AffixLayout.insertIndex(modifiers, added.slot()), added);
        }
        item.replaceExplicits(modifiers);
        item.refreezeName();
        return true;
    }

    public static boolean canRetemper(Item item) {
        ScaleContext context = item.scaleContext(null);
        for (RolledModifier implicit : item.implicits()) {
            for (RolledEffect line : implicit.lines()) {
                ModifierLine lineDefinition = item.implicitLine(line.effectName());
                if (improves(item, lineDefinition, line, context)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean retemper(Item item, Random random) {
        if (!canRetemper(item)) {
            return false;
        }
        ScaleContext context = item.scaleContext(null);
        List<RolledModifier> retempered = new ArrayList<>(item.implicits().size());
        for (RolledModifier implicit : item.implicits()) {
            RolledModifier rerolled = item.rerolledImplicit(implicit, random);
            List<RolledEffect> kept = new ArrayList<>(implicit.lines().size());
            for (int index = 0; index < implicit.lines().size(); index++) {
                RolledEffect current = implicit.lines().get(index);
                RolledEffect candidate = rerolled.lines().get(index);
                ModifierLine lineDefinition = item.implicitLine(current.effectName());
                boolean better = lineDefinition != null
                        && lineDefinition.isBetter(candidate.magnitude(), current.magnitude(), context, lowerIsBetter(item, current.effectName()));
                kept.add(better ? candidate : current);
            }
            retempered.add(implicit.withLines(kept));
        }
        item.replaceImplicits(retempered);
        return true;
    }
}
