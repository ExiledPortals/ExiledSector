package exiledsector.items;

import exiledlib.i18n.Translation;
import exiledlib.modifiers.ModifierEffect;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.modifiers.TierRange;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public record AnomalousDebrisCurrency(String itemId, String englishName, String modifierId, Set<ItemType> itemTypes, int minTier, float weight)
        implements ItemCurrency {

    static final Color COLOR = new Color(120, 225, 210);
    static final String DESCRIPTION_KEY = "chart.debris.currency.description";
    static final String ITEMS_KEY_PREFIX = "chart.debris.currency.items.";
    static final String USED_NOTICE_KEY_PREFIX = "ui.workbench.notice.used.anomalousDebris.";
    static final String LINE_SEPARATOR = ", ";
    static final String ITEMS_SEPARATOR = " or ";

    public AnomalousDebrisCurrency {
        itemTypes = Set.copyOf(itemTypes);
        weight = Math.max(0f, weight);
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
    public Color color() {
        return COLOR;
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
        Modifier granted = modifier();
        if (definition == null || granted == null || definition.unique() || definition.keepsOneModifier() || !appliesTo(definition.type())
                || !ItemModifiers.itemTypes(modifierId).contains(definition.type()) || granted.slot() == ModifierSlot.IMPLICIT) {
            return false;
        }
        if (ItemCrafting.wardBlocksTransposition(item, context.guardedSlots())) {
            return false;
        }
        return AffixLayout.rollableAt(definition.type(), List.of(new PoolEntry(granted, 1f)), TierRange.NO_TIER).size() == 1;
    }

    @Override
    public Item use(Item item, CraftingContext context, Random random) {
        if (!canUse(item, context)) {
            return null;
        }
        item.replaceExplicits(reroll(item.definition(), modifier(), random));
        item.refreezeName();
        return item;
    }

    static List<RolledModifier> reroll(ItemDefinition definition, Modifier granted, Random random) {
        AffixLayout.Limits limits = AffixLayout.limits(definition.type());
        boolean grantedPrefix = granted.slot() == ModifierSlot.PREFIX;
        List<RolledModifier> prefixes = new ArrayList<>();
        List<RolledModifier> suffixes = new ArrayList<>();
        (grantedPrefix ? prefixes : suffixes).add(RolledModifier.roll(granted, random, false));
        int othersLeft = limits.maxEffects() - 1;
        othersLeft -= draw(candidates(definition, definition.prefixes(), granted), Math.min(limits.maxPerSide() - prefixes.size(), othersLeft),
                random, prefixes);
        draw(candidates(definition, definition.suffixes(), granted), Math.min(limits.maxPerSide() - suffixes.size(), othersLeft), random, suffixes);
        List<RolledModifier> rerolled = new ArrayList<>(prefixes);
        rerolled.addAll(suffixes);
        return rerolled;
    }

    private static List<PoolEntry> candidates(ItemDefinition definition, List<PoolEntry> side, Modifier granted) {
        Set<String> grantedEffects = granted.lines().stream().map(ModifierLine::effectName).collect(Collectors.toSet());
        return AffixLayout.rollableAt(definition.type(), side, TierRange.NO_TIER).stream()
                .filter(entry -> !entry.modifier().id().equals(granted.id()))
                .filter(entry -> entry.modifier().lines().stream().noneMatch(line -> grantedEffects.contains(line.effectName())))
                .toList();
    }

    private static int draw(List<PoolEntry> pool, int count, Random random, List<RolledModifier> drawn) {
        List<PoolEntry> remaining = new ArrayList<>(pool);
        int drawCount = Math.max(0, Math.min(count, remaining.size()));
        for (int left = drawCount; left > 0; left--) {
            PoolEntry entry = remaining.remove(WeightedPick.index(remaining, PoolEntry::weight, random.nextFloat()));
            drawn.add(RolledModifier.roll(entry.modifier(), random, false));
        }
        return drawCount;
    }

    @Override
    public String description() {
        return Translation.msg(DESCRIPTION_KEY).arg("items", itemsText()).arg("modifier", modifierText()).text();
    }

    String itemsText() {
        return itemTypes.stream().sorted().map(type -> Translation.text(ITEMS_KEY_PREFIX + type.id())).collect(Collectors.joining(ITEMS_SEPARATOR));
    }

    String modifierText() {
        Modifier granted = modifier();
        if (granted == null) {
            return modifierId;
        }
        ItemType type = itemTypes.stream().sorted().findFirst().orElse(null);
        RolledModifier rolled = RolledModifier.roll(granted, new Random(0L), true);
        List<String> lines = new ArrayList<>();
        for (RolledEffect line : rolled.lines()) {
            ModifierEffect effect = ItemEffects.forType(type).byName(line.effectName());
            lines.add(effect == null ? line.effectName() : effect.description(line.magnitude()).plain());
        }
        return String.join(LINE_SEPARATOR, lines);
    }

    @Override
    public String usedNoticeKey(ItemNature nature) {
        return USED_NOTICE_KEY_PREFIX + nature.name().toLowerCase(Locale.ROOT);
    }
}
