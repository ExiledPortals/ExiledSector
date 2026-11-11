package exiledsector.items;

import exiledsector.charts.loot.BiomeModifiers;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.skills.tags.TagWeights;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public enum BiomeNanoswarm implements CraftingMaterialFamily {

    ADAPTIVE("exiledSector_adaptive_nanoswarm", "adaptive", new Color(120, 210, 140)),
    GRAFTING("exiledSector_grafting_nanoswarm", "grafting", new Color(215, 170, 95));

    static final float DROP_WEIGHT = 0.2f;
    static final int MIN_OTHER_MODIFIERS = 1;
    static final int MAX_OTHER_MODIFIERS = 3;

    private final String itemId;
    private final String key;
    private final Color color;

    BiomeNanoswarm(String itemId, String key, Color color) {
        this.itemId = itemId;
        this.key = key;
        this.color = color;
    }

    public static void registerAll() {
        for (BiomeNanoswarm craftingItem : values()) {
            if (!CraftingMaterialFamilies.isRegistered(craftingItem)) {
                CraftingMaterialFamilies.register(craftingItem);
            }
        }
    }

    public static BiomeNanoswarm byItemId(String itemId) {
        for (BiomeNanoswarm craftingItem : values()) {
            if (craftingItem.itemId.equals(itemId)) {
                return craftingItem;
            }
        }
        return null;
    }

    @Override
    public String itemId() {
        return itemId;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public float dropWeight() {
        return DROP_WEIGHT;
    }

    @Override
    public MaterialTier tier() {
        return MaterialTier.MYTHIC;
    }

    @Override
    public Color color() {
        return color;
    }

    @Override
    public boolean growable() {
        return false;
    }

    @Override
    public int feedstockCost() {
        return 0;
    }

    @Override
    public boolean appliesTo(ItemType type) {
        return type == ItemType.HULL_ITEM;
    }

    @Override
    public boolean dropsFromShipLoot() {
        return false;
    }

    @Override
    public boolean respectsInhibitors() {
        return true;
    }

    @Override
    public boolean wouldChange(Item item, RolledModifier rolled) {
        return switch (this) {
            case ADAPTIVE -> true;
            case GRAFTING -> !graftsAfterRemoving(item, rolled).isEmpty();
        };
    }

    @Override
    public boolean canUse(Item item, CraftingContext context) {
        ItemDefinition definition = item == null ? null : item.definition();
        if (definition == null || !appliesTo(definition.type())) {
            return false;
        }
        return switch (this) {
            case ADAPTIVE -> !adaptiveGrants(item, context.inhibitedSlots()).isEmpty();
            case GRAFTING -> !graftable(item, context.inhibitedSlots()).isEmpty();
        };
    }

    @Override
    public Item use(Item item, CraftingContext context, Random random) {
        if (!canUse(item, context)) {
            return null;
        }
        List<RolledModifier> replacement = switch (this) {
            case ADAPTIVE -> adapt(item, context.inhibitedSlots(), random);
            case GRAFTING -> graft(item, context.inhibitedSlots(), random);
        };
        item.replaceExplicits(replacement);
        item.refreezeName();
        return item;
    }

    static List<String> adaptiveGrants(Item item, Set<ModifierSlot> inhibited) {
        ItemDefinition definition = item.definition();
        List<RolledModifier> kept = Engram.keptModifiers(item, inhibited);
        return BiomeModifiers.modifiersFor(definition.kind()).stream()
                .filter(modifierId -> GrantedModifiers.canGrant(definition, modifierId))
                .filter(modifierId -> BiomeModifiers.hasRoomFor(kept, modifierId))
                .filter(modifierId -> !Engram.keptClash(item, ItemModifiers.get(modifierId), inhibited))
                .toList();
    }

    static List<RolledModifier> adapt(Item item, Set<ModifierSlot> inhibited, Random random) {
        ItemDefinition definition = item.definition();
        List<String> grants = adaptiveGrants(item, inhibited);
        Modifier granted = ItemModifiers.get(grants.get(random.nextInt(grants.size())));
        List<RolledModifier> modifiers = new ArrayList<>(Engram.keptModifiers(item, inhibited));
        if (modifiers.stream().noneMatch(rolled -> rolled.modifierId().equals(granted.id()))) {
            AffixLayout.insert(modifiers, RolledModifier.roll(granted, random));
        }
        int otherCount = MIN_OTHER_MODIFIERS + random.nextInt(MAX_OTHER_MODIFIERS - MIN_OTHER_MODIFIERS + 1);
        int targetCount = Math.min(AffixLayout.limits(definition.type()).maxModifiers(), 1 + otherCount);
        while (modifiers.size() < targetCount) {
            List<List<PoolEntry>> openSides = AffixLayout.openSides(definition, modifiers);
            if (openSides.isEmpty()) {
                break;
            }
            List<PoolEntry> side = openSides.get(random.nextInt(openSides.size()));
            PoolEntry entry = side.get(WeightedPick.index(side, candidate -> candidate.biasedWeight(TagWeights.NONE), random.nextFloat()));
            AffixLayout.insert(modifiers, RolledModifier.roll(entry.modifier(), random));
        }
        return modifiers;
    }

    static List<RolledModifier> graftable(Item item, Set<ModifierSlot> inhibited) {
        return item.explicits().stream()
                .filter(rolled -> !rolled.immutable() && !inhibited.contains(rolled.slot()))
                .filter(rolled -> !graftsAfterRemoving(item, rolled).isEmpty())
                .toList();
    }

    static List<String> graftsAfterRemoving(Item item, RolledModifier removed) {
        ItemDefinition definition = item == null ? null : item.definition();
        if (definition == null || removed == null || removed.immutable()) {
            return List.of();
        }
        List<RolledModifier> remaining = remainingAfter(item, removed);
        AffixLayout.Limits limits = AffixLayout.limits(definition.type());
        Set<String> remainingIds = remaining.stream().map(RolledModifier::modifierId).collect(Collectors.toSet());
        Set<String> remainingEffects = remaining.stream().flatMap(rolled -> rolled.lines().stream()).map(RolledLine::effectName)
                .collect(Collectors.toSet());
        return BiomeModifiers.modifiersFor(definition.kind()).stream()
                .filter(modifierId -> !modifierId.equals(removed.modifierId()) && !remainingIds.contains(modifierId))
                .filter(modifierId -> GrantedModifiers.canGrant(definition, modifierId))
                .filter(modifierId -> BiomeModifiers.hasRoomFor(remaining, modifierId))
                .filter(modifierId -> {
                    Modifier granted = ItemModifiers.get(modifierId);
                    long sameSide = remaining.stream().filter(rolled -> rolled.slot() == granted.slot()).count();
                    return sameSide < limits.maxPerSide() && remaining.size() < limits.maxModifiers()
                            && granted.lines().stream().map(ModifierLine::effectName).noneMatch(remainingEffects::contains)
                            && !ModifierExclusivity.clashes(modifierId, remainingIds);
                })
                .toList();
    }

    static List<RolledModifier> graft(Item item, Set<ModifierSlot> inhibited, Random random) {
        List<RolledModifier> removable = graftable(item, inhibited);
        RolledModifier removed = removable.get(random.nextInt(removable.size()));
        List<String> grafts = graftsAfterRemoving(item, removed);
        Modifier granted = ItemModifiers.get(grafts.get(random.nextInt(grafts.size())));
        List<RolledModifier> modifiers = remainingAfter(item, removed);
        AffixLayout.insert(modifiers, RolledModifier.roll(granted, random));
        return modifiers;
    }

    private static List<RolledModifier> remainingAfter(Item item, RolledModifier removed) {
        List<RolledModifier> remaining = new ArrayList<>(item.explicits());
        remaining.remove(removed);
        return remaining;
    }
}
