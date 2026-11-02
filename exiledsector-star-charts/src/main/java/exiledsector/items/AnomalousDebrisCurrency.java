package exiledsector.items;

import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.modifiers.ModifierEffects;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.modifiers.TierRange;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public record AnomalousDebrisCurrency(String itemId, String englishName, String modifierId, Set<ItemType> itemTypes, int minTier, float weight,
                                      Color color)
        implements ItemCurrency {

    static final Color DEFAULT_COLOR = new Color(120, 225, 210);
    static final String DESCRIPTION_KEY = "chart.debris.currency.description";
    static final String ITEMS_KEY_PREFIX = "chart.debris.currency.items.";
    static final String USED_NOTICE_KEY_PREFIX = "ui.craftingBench.notice.used.anomalousDebris.";
    static final String LOCKED_CLASH_KEY = "chart.debris.currency.lockedClash";
    static final String SLOT_KEY_PREFIX = "chart.debris.currency.slot.";
    static final String ITEMS_SEPARATOR = " or ";

    public AnomalousDebrisCurrency {
        itemTypes = Set.copyOf(itemTypes);
        weight = Math.max(0f, weight);
        color = color == null ? DEFAULT_COLOR : color;
    }

    @Override
    public String key() {
        return itemId;
    }

    @Override
    public float dropWeight() {
        return weight;
    }

    @Override
    public MaterialTier tier() {
        return MaterialTier.EXOTIC;
    }

    @Override
    public boolean synthesisable() {
        return false;
    }

    @Override
    public int partsCost() {
        return 0;
    }

    @Override
    public boolean appliesTo(ItemType type) {
        return type != null && itemTypes.contains(type);
    }

    @Override
    public boolean dropsFromShipLoot() {
        return false;
    }

    public Modifier modifier() {
        return ItemModifiers.get(modifierId);
    }

    public boolean dropsAt(int tier) {
        return tier >= minTier && weight > 0f;
    }

    @Override
    public boolean canUse(Item item, CraftingContext context) {
        ItemDefinition definition = item == null ? null : item.definition();
        if (definition == null || !appliesTo(definition.type()) || !GrantedModifiers.canGrant(definition, modifierId)) {
            return false;
        }
        Modifier granted = modifier();
        return !lockedClash(item, granted) && !keptClash(item, granted, context.inhibitedSlots());
    }

    @Override
    public boolean respectsInhibitors() {
        return true;
    }

    @Override
    public boolean wouldChange(Item item, RolledModifier rolled) {
        return true;
    }

    @Override
    public String refusalNoticeKey(Item item) {
        return item != null && lockedClash(item, modifier()) ? LOCKED_CLASH_KEY : null;
    }

    static RolledModifier lockedModifier(Item item) {
        return item.explicits().stream().filter(RolledModifier::locked).findFirst().orElse(null);
    }

    static List<RolledModifier> keptModifiers(Item item, Set<ModifierSlot> inhibited) {
        return item.explicits().stream().filter(rolled -> rolled.locked() || inhibited.contains(rolled.slot())).toList();
    }

    static boolean keptClash(Item item, Modifier granted, Set<ModifierSlot> inhibited) {
        List<RolledModifier> others = keptModifiers(item, inhibited).stream().filter(rolled -> !rolled.modifierId().equals(granted.id())).toList();
        Set<String> grantedEffects = effectsOf(granted);
        ItemDefinition definition = item.definition();
        AffixLayout.Limits limits = AffixLayout.limits(definition == null ? null : definition.type());
        long grantedSide = others.stream().filter(rolled -> rolled.slot() == granted.slot()).count();
        return grantedSide >= limits.maxPerSide() || others.size() >= limits.maxEffects()
                || others.stream().anyMatch(rolled -> rolled.lines().stream().anyMatch(line -> grantedEffects.contains(line.effectName())))
                || ModifierExclusivity.clashes(granted.id(), others.stream().map(RolledModifier::modifierId).toList());
    }

    static boolean lockedClash(Item item, Modifier granted) {
        RolledModifier locked = item == null || granted == null ? null : lockedModifier(item);
        if (locked == null || locked.modifierId().equals(granted.id())) {
            return false;
        }
        Set<String> grantedEffects = effectsOf(granted);
        boolean sharesEffect = locked.lines().stream().anyMatch(line -> grantedEffects.contains(line.effectName()));
        boolean sideFull = locked.slot() == granted.slot() && AffixLayout.limits(item.definition().type()).maxPerSide() < 2;
        return sharesEffect || sideFull || ModifierExclusivity.clashes(granted.id(), List.of(locked.modifierId()));
    }

    private static Set<String> effectsOf(Modifier modifier) {
        return modifier.lines().stream().map(ModifierLine::effectName).collect(Collectors.toSet());
    }

    @Override
    public Item use(Item item, CraftingContext context, Random random) {
        if (!canUse(item, context)) {
            return null;
        }
        item.replaceExplicits(reroll(item.definition(), modifier(), keptModifiers(item, context.inhibitedSlots()), random));
        item.refreezeName();
        return item;
    }

    static List<RolledModifier> reroll(ItemDefinition definition, Modifier granted, List<RolledModifier> kept, Random random) {
        return reroll(definition, granted, kept, AffixLayout.limits(definition.type()), random);
    }

    static List<RolledModifier> reroll(ItemDefinition definition, Modifier granted, List<RolledModifier> kept, AffixLayout.Limits limits,
                                       Random random) {
        List<RolledModifier> prefixes = new ArrayList<>();
        List<RolledModifier> suffixes = new ArrayList<>();
        RolledModifier grantedRoll = kept.stream().filter(rolled -> rolled.modifierId().equals(granted.id())).findFirst()
                .orElseGet(() -> RolledModifier.roll(granted, random));
        (granted.slot() == ModifierSlot.PREFIX ? prefixes : suffixes).add(grantedRoll);
        Set<String> takenIds = new HashSet<>(List.of(granted.id()));
        Set<String> takenEffects = new HashSet<>(effectsOf(granted));
        for (RolledModifier keptModifier : kept) {
            if (keptModifier.modifierId().equals(granted.id())) {
                continue;
            }
            (keptModifier.slot() == ModifierSlot.PREFIX ? prefixes : suffixes).add(keptModifier);
            takenIds.add(keptModifier.modifierId());
            keptModifier.lines().forEach(line -> takenEffects.add(line.effectName()));
        }
        int othersLeft = limits.maxEffects() - prefixes.size() - suffixes.size();
        othersLeft -= draw(candidates(definition, definition.prefixes(), takenIds, takenEffects),
                Math.min(limits.maxPerSide() - prefixes.size(), othersLeft), random, takenIds, prefixes);
        draw(candidates(definition, definition.suffixes(), takenIds, takenEffects), Math.min(limits.maxPerSide() - suffixes.size(), othersLeft),
                random, takenIds, suffixes);
        List<RolledModifier> rerolled = new ArrayList<>(prefixes);
        rerolled.addAll(suffixes);
        return rerolled;
    }

    private static List<PoolEntry> candidates(ItemDefinition definition, List<PoolEntry> side, Set<String> takenIds, Set<String> takenEffects) {
        return AffixLayout.rollableAt(definition.type(), side, TierRange.NO_TIER).stream()
                .filter(entry -> !takenIds.contains(entry.modifier().id()))
                .filter(entry -> !ModifierExclusivity.clashes(entry.modifier().id(), takenIds))
                .filter(entry -> entry.modifier().lines().stream().noneMatch(line -> takenEffects.contains(line.effectName())))
                .toList();
    }

    private static int draw(List<PoolEntry> pool, int count, Random random, Set<String> takenIds, List<RolledModifier> drawn) {
        List<PoolEntry> remaining = new ArrayList<>(pool);
        int drawCount = 0;
        for (int left = Math.max(0, count); left > 0 && !remaining.isEmpty(); left--) {
            PoolEntry entry = remaining.remove(WeightedPick.index(remaining, PoolEntry::weight, random.nextFloat()));
            drawn.add(RolledModifier.roll(entry.modifier(), random));
            takenIds.add(entry.modifier().id());
            remaining.removeIf(candidate -> ModifierExclusivity.clashes(candidate.modifier().id(), takenIds));
            drawCount++;
        }
        return drawCount;
    }

    @Override
    public String description() {
        return Translation.msg(DESCRIPTION_KEY).arg("items", itemsText()).arg("modifier", modifierName()).arg("slot", slotText()).text();
    }

    String itemsText() {
        return itemTypes.stream().sorted().map(type -> Translation.text(ITEMS_KEY_PREFIX + type.id())).collect(Collectors.joining(ITEMS_SEPARATOR));
    }

    String modifierName() {
        String name = ItemModifiers.name(modifierId);
        return name == null ? modifierId : name;
    }

    String slotText() {
        Modifier granted = modifier();
        return granted == null ? "" : Translation.text(SLOT_KEY_PREFIX + granted.slot().name().toLowerCase(Locale.ROOT));
    }

    @Override
    public List<StyledText> grantedLines() {
        Modifier granted = modifier();
        if (granted == null) {
            return List.of();
        }
        ModifierEffects effects = ItemEffects.forType(itemTypes.stream().sorted().findFirst().orElse(null));
        List<StyledText> lines = new ArrayList<>();
        for (ModifierLine line : granted.lines()) {
            StyledText lineText = new RolledEffect(line.effectName(), line.boundFarthestFromZero()).rangeDescription(effects, line);
            if (lineText != null) {
                lines.add(lineText);
            }
        }
        return lines;
    }

    @Override
    public String usedNoticeKey(ItemNature nature) {
        return USED_NOTICE_KEY_PREFIX + nature.name().toLowerCase(Locale.ROOT);
    }
}
