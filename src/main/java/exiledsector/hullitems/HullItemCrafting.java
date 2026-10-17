package exiledsector.hullitems;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public final class HullItemCrafting {

    static final int DEFAULT_COMMON_SYNTHESIS_PARTS = 10;
    static final int MAX_RECALIBRATION_ATTEMPTS = 64;

    private HullItemCrafting() {
    }

    public static int commonSynthesisParts() {
        return SocketCraftingCosts.cost(SocketCraftingCosts.COMMON_SUBROUTINE, DEFAULT_COMMON_SYNTHESIS_PARTS);
    }

    public static int parts(SocketMaterials materials) {
        return materials.count(HullItemDisassembly.PARTS_COMMODITY_ID);
    }

    public static boolean synthesise(SocketCurrency currency, SocketMaterials materials) {
        if (!materials.take(HullItemDisassembly.PARTS_COMMODITY_ID, currency.partsCost())) {
            return false;
        }
        materials.add(currency.commodityId(), 1);
        return true;
    }

    public static HullItem synthesiseCommon(SocketMaterials materials, HullItemStore store, Random random) {
        int partsCost = commonSynthesisParts();
        if (parts(materials) < partsCost) {
            return null;
        }
        HullItemDefinition definition = HullItemDrops.pickBasic(random);
        if (definition == null) {
            return null;
        }
        long seed = random.nextLong();
        List<RolledEffect> effects = HullItemRoller.rollCommon(definition, seed);
        HullItem created = store.add(new HullItemData(definition.id(), seed, effects,
                HullItemNames.freeze(definition, seed, effects)));
        if (created != null) {
            materials.take(HullItemDisassembly.PARTS_COMMODITY_ID, partsCost);
        }
        return created;
    }

    public static boolean canUse(SocketCurrency currency, HullItem hullItem, Predicate<HullItemDefinition> allowedUniques) {
        HullItemDefinition definition = hullItem == null ? null : hullItem.definition();
        if (definition == null) {
            return false;
        }
        return switch (currency) {
            case AUGMENTATION -> !AffixLayout.openSides(definition, hullItem.effects()).isEmpty();
            case RECALIBRATION -> canRecalibrate(hullItem, definition);
            case TRANSPOSITION -> canTranspose(definition, allowedUniques);
        };
    }

    public static HullItem use(SocketCurrency currency, HullItem hullItem, SocketMaterials materials, HullItemStore store, Random random,
                                 Predicate<HullItemDefinition> allowedUniques) {
        if (materials.count(currency.commodityId()) < 1 || !canUse(currency, hullItem, allowedUniques)) {
            return null;
        }
        HullItem craftedHullItem = switch (currency) {
            case AUGMENTATION -> augment(hullItem, random) == null ? null : hullItem;
            case RECALIBRATION -> recalibrate(hullItem, random);
            case TRANSPOSITION -> transpose(store, hullItem, random, allowedUniques);
        };
        if (craftedHullItem != null) {
            materials.take(currency.commodityId(), 1);
        }
        return craftedHullItem;
    }

    static RolledEffect augment(HullItem hullItem, Random random) {
        HullItemDefinition definition = hullItem.definition();
        List<List<PoolEntry>> openSides = definition == null ? List.of() : AffixLayout.openSides(definition, hullItem.effects());
        if (definition == null || openSides.isEmpty()) {
            return null;
        }
        List<PoolEntry> chosenSide = openSides.get(random.nextInt(openSides.size()));
        PoolEntry poolEntry = WeightedPick.pick(chosenSide, PoolEntry::weight, random);
        RolledEffect addedEffect = new RolledEffect(poolEntry.effectName(), poolEntry.roll(random));
        HullItemRarity rarityBefore = hullItem.rarity();
        List<RolledEffect> updatedEffects = new ArrayList<>(hullItem.effects());
        updatedEffects.add(AffixLayout.insertIndex(definition, updatedEffects, addedEffect.effectName()), addedEffect);
        hullItem.replaceEffects(updatedEffects);
        if (hullItem.rarity() != rarityBefore) {
            hullItem.refreezeName();
        }
        return addedEffect;
    }

    private static boolean canRecalibrate(HullItem hullItem, HullItemDefinition definition) {
        for (RolledEffect effect : hullItem.effects()) {
            PoolEntry poolEntry = definition.poolEntry(effect.effectName());
            if (poolEntry != null && poolEntry.canVary()) {
                return true;
            }
        }
        return false;
    }

    static HullItem recalibrate(HullItem hullItem, Random random) {
        HullItemDefinition definition = hullItem.definition();
        if (definition == null) {
            return null;
        }
        for (int attempt = 0; attempt < MAX_RECALIBRATION_ATTEMPTS; attempt++) {
            List<RolledEffect> rerolled = reroll(hullItem.effects(), definition, random);
            if (!rerolled.equals(hullItem.effects())) {
                hullItem.replaceEffects(rerolled);
                return hullItem;
            }
        }
        return null;
    }

    private static List<RolledEffect> reroll(List<RolledEffect> effects, HullItemDefinition definition, Random random) {
        List<RolledEffect> rerolled = new ArrayList<>(effects.size());
        for (RolledEffect effect : effects) {
            PoolEntry poolEntry = definition.poolEntry(effect.effectName());
            rerolled.add(poolEntry == null ? effect : new RolledEffect(effect.effectName(), poolEntry.roll(random)));
        }
        return rerolled;
    }

    private static boolean canTranspose(HullItemDefinition currentDefinition, Predicate<HullItemDefinition> allowedUniques) {
        if (!currentDefinition.unique()) {
            return false;
        }
        for (HullItemDefinition definition : HullItemDefinitions.all()) {
            if (otherUnique(definition, currentDefinition, allowedUniques)) {
                return true;
            }
        }
        return false;
    }

    static HullItem transpose(HullItemStore store, HullItem hullItem, Random random, Predicate<HullItemDefinition> allowedUniques) {
        HullItemDefinition currentDefinition = hullItem.definition();
        if (currentDefinition == null) {
            return null;
        }
        HullItemDefinition nextDefinition = HullItemDrops.pickUnique(random, definition -> otherUnique(definition, currentDefinition, allowedUniques));
        if (nextDefinition == null) {
            return null;
        }
        long seed = random.nextLong();
        HullItem replacement = new HullItem(hullItem.id(), nextDefinition.id(), seed, HullItemRoller.roll(nextDefinition, seed));
        replacement.freezeName();
        return store.replace(hullItem, replacement) ? replacement : null;
    }

    private static boolean otherUnique(HullItemDefinition definition, HullItemDefinition currentDefinition, Predicate<HullItemDefinition> allowedUniques) {
        return definition.unique() && !definition.id().equals(currentDefinition.id()) && definition.rarity() > 0f && allowedUniques.test(definition);
    }
}
