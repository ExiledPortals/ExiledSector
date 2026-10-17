package exiledsector.hullitems;

import exiledsector.modifiers.ModifierLine;
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

    public static int parts(CraftingMaterials materials) {
        return materials.count(HullItemDisassembly.PARTS_COMMODITY_ID);
    }

    public static boolean synthesise(CraftingCurrency currency, CraftingMaterials materials) {
        if (!materials.take(HullItemDisassembly.PARTS_COMMODITY_ID, currency.partsCost())) {
            return false;
        }
        materials.add(currency.commodityId(), 1);
        return true;
    }

    public static HullItem synthesiseCommon(CraftingMaterials materials, HullItemStore store, Random random) {
        int partsCost = commonSynthesisParts();
        if (parts(materials) < partsCost) {
            return null;
        }
        HullItemDefinition definition = HullItemDrops.pickBasic(random);
        if (definition == null) {
            return null;
        }
        long seed = random.nextLong();
        List<RolledModifier> modifiers = HullItemRoller.rollCommon(definition, seed);
        HullItem created = store.add(new HullItemData(definition.id(), seed, modifiers,
                HullItemNames.freeze(definition, seed, modifiers)));
        if (created != null) {
            materials.take(HullItemDisassembly.PARTS_COMMODITY_ID, partsCost);
        }
        return created;
    }

    public static boolean canUse(CraftingCurrency currency, HullItem hullItem, Predicate<HullItemDefinition> allowedUniques) {
        HullItemDefinition definition = hullItem == null ? null : hullItem.definition();
        if (definition == null) {
            return false;
        }
        return switch (currency) {
            case AUGMENTATION -> !AffixLayout.openSides(definition, hullItem.explicits()).isEmpty();
            case RECALIBRATION -> canRecalibrate(hullItem, definition);
            case TRANSPOSITION -> canTranspose(definition, allowedUniques);
        };
    }

    public static HullItem use(CraftingCurrency currency, HullItem hullItem, CraftingMaterials materials, HullItemStore store, Random random,
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

    static RolledModifier augment(HullItem hullItem, Random random) {
        HullItemDefinition definition = hullItem.definition();
        List<List<PoolEntry>> openSides = definition == null ? List.of() : AffixLayout.openSides(definition, hullItem.explicits());
        if (definition == null || openSides.isEmpty()) {
            return null;
        }
        List<PoolEntry> chosenSide = openSides.get(random.nextInt(openSides.size()));
        PoolEntry poolEntry = WeightedPick.pick(chosenSide, PoolEntry::weight, random);
        RolledModifier addedModifier = RolledModifier.roll(poolEntry.modifier(), random, false);
        HullItemRarity rarityBefore = hullItem.rarity();
        List<RolledModifier> updatedModifiers = new ArrayList<>(hullItem.explicits());
        updatedModifiers.add(AffixLayout.insertIndex(updatedModifiers, addedModifier.slot()), addedModifier);
        hullItem.replaceExplicits(updatedModifiers);
        if (hullItem.rarity() != rarityBefore) {
            hullItem.refreezeName();
        }
        return addedModifier;
    }

    private static boolean canRecalibrate(HullItem hullItem, HullItemDefinition definition) {
        for (RolledModifier rolled : hullItem.explicits()) {
            PoolEntry poolEntry = definition.poolEntryFor(rolled.modifierId());
            if (poolEntry != null && rolled.lines().stream().map(line -> poolEntry.modifier().line(line.effectName()))
                    .anyMatch(line -> line != null && line.canVary())) {
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
            List<RolledModifier> rerolled = reroll(hullItem.explicits(), definition, random);
            if (!rerolled.equals(hullItem.explicits())) {
                hullItem.replaceExplicits(rerolled);
                return hullItem;
            }
        }
        return null;
    }

    private static List<RolledModifier> reroll(List<RolledModifier> modifiers, HullItemDefinition definition, Random random) {
        List<RolledModifier> rerolled = new ArrayList<>(modifiers.size());
        for (RolledModifier rolled : modifiers) {
            PoolEntry poolEntry = definition.poolEntryFor(rolled.modifierId());
            if (poolEntry == null) {
                rerolled.add(rolled);
                continue;
            }
            List<RolledEffect> rerolledLines = new ArrayList<>(rolled.lines().size());
            for (RolledEffect line : rolled.lines()) {
                ModifierLine lineDefinition = poolEntry.modifier().line(line.effectName());
                rerolledLines.add(lineDefinition == null ? line : new RolledEffect(line.effectName(), lineDefinition.roll(random)));
            }
            rerolled.add(rolled.withLines(rerolledLines));
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
