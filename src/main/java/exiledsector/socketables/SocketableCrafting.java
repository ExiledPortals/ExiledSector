package exiledsector.socketables;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public final class SocketableCrafting {

    static final int DEFAULT_COMMON_SYNTHESIS_PARTS = 10;
    static final int MAX_RECALIBRATION_ATTEMPTS = 64;

    private SocketableCrafting() {
    }

    public static int commonSynthesisParts() {
        return SocketCraftingCosts.cost(SocketCraftingCosts.COMMON_SUBROUTINE, DEFAULT_COMMON_SYNTHESIS_PARTS);
    }

    public static int parts(SocketMaterials materials) {
        return materials.count(SocketableDisassembly.PARTS_COMMODITY_ID);
    }

    public static boolean synthesise(SocketCurrency currency, SocketMaterials materials) {
        if (!materials.take(SocketableDisassembly.PARTS_COMMODITY_ID, currency.partsCost())) {
            return false;
        }
        materials.add(currency.commodityId(), 1);
        return true;
    }

    public static Socketable synthesiseCommon(SocketMaterials materials, SocketableStore store, Random random) {
        int partsCost = commonSynthesisParts();
        if (parts(materials) < partsCost) {
            return null;
        }
        SocketableDefinition definition = SocketableDrops.pickBasic(random);
        if (definition == null) {
            return null;
        }
        long seed = random.nextLong();
        List<RolledEffect> effects = SocketableRoller.rollCommon(definition, seed);
        Socketable created = store.add(new SocketableItemData(definition.id(), seed, effects,
                SocketableNames.freeze(definition, seed, effects)));
        if (created != null) {
            materials.take(SocketableDisassembly.PARTS_COMMODITY_ID, partsCost);
        }
        return created;
    }

    public static boolean canUse(SocketCurrency currency, Socketable socketable, Predicate<SocketableDefinition> allowedUniques) {
        SocketableDefinition definition = socketable == null ? null : socketable.definition();
        if (definition == null) {
            return false;
        }
        return switch (currency) {
            case AUGMENTATION -> !AffixLayout.openSides(definition, socketable.effects()).isEmpty();
            case RECALIBRATION -> canRecalibrate(socketable, definition);
            case TRANSPOSITION -> canTranspose(definition, allowedUniques);
        };
    }

    public static Socketable use(SocketCurrency currency, Socketable socketable, SocketMaterials materials, SocketableStore store, Random random,
                                 Predicate<SocketableDefinition> allowedUniques) {
        if (materials.count(currency.commodityId()) < 1 || !canUse(currency, socketable, allowedUniques)) {
            return null;
        }
        Socketable craftedSocketable = switch (currency) {
            case AUGMENTATION -> augment(socketable, random) == null ? null : socketable;
            case RECALIBRATION -> recalibrate(socketable, random);
            case TRANSPOSITION -> transpose(store, socketable, random, allowedUniques);
        };
        if (craftedSocketable != null) {
            materials.take(currency.commodityId(), 1);
        }
        return craftedSocketable;
    }

    static RolledEffect augment(Socketable socketable, Random random) {
        SocketableDefinition definition = socketable.definition();
        List<List<PoolEntry>> openSides = definition == null ? List.of() : AffixLayout.openSides(definition, socketable.effects());
        if (definition == null || openSides.isEmpty()) {
            return null;
        }
        List<PoolEntry> chosenSide = openSides.get(random.nextInt(openSides.size()));
        PoolEntry poolEntry = WeightedPick.pick(chosenSide, PoolEntry::weight, random);
        RolledEffect addedEffect = new RolledEffect(poolEntry.effectName(), poolEntry.roll(random));
        SocketableRarity rarityBefore = socketable.rarity();
        List<RolledEffect> updatedEffects = new ArrayList<>(socketable.effects());
        updatedEffects.add(AffixLayout.insertIndex(definition, updatedEffects, addedEffect.effectName()), addedEffect);
        socketable.replaceEffects(updatedEffects);
        if (socketable.rarity() != rarityBefore) {
            socketable.refreezeName();
        }
        return addedEffect;
    }

    private static boolean canRecalibrate(Socketable socketable, SocketableDefinition definition) {
        for (RolledEffect effect : socketable.effects()) {
            PoolEntry poolEntry = definition.poolEntry(effect.effectName());
            if (poolEntry != null && poolEntry.canVary()) {
                return true;
            }
        }
        return false;
    }

    static Socketable recalibrate(Socketable socketable, Random random) {
        SocketableDefinition definition = socketable.definition();
        if (definition == null) {
            return null;
        }
        for (int attempt = 0; attempt < MAX_RECALIBRATION_ATTEMPTS; attempt++) {
            List<RolledEffect> rerolled = reroll(socketable.effects(), definition, random);
            if (!rerolled.equals(socketable.effects())) {
                socketable.replaceEffects(rerolled);
                return socketable;
            }
        }
        return null;
    }

    private static List<RolledEffect> reroll(List<RolledEffect> effects, SocketableDefinition definition, Random random) {
        List<RolledEffect> rerolled = new ArrayList<>(effects.size());
        for (RolledEffect effect : effects) {
            PoolEntry poolEntry = definition.poolEntry(effect.effectName());
            rerolled.add(poolEntry == null ? effect : new RolledEffect(effect.effectName(), poolEntry.roll(random)));
        }
        return rerolled;
    }

    private static boolean canTranspose(SocketableDefinition currentDefinition, Predicate<SocketableDefinition> allowedUniques) {
        if (!currentDefinition.unique()) {
            return false;
        }
        for (SocketableDefinition definition : SocketableDefinitions.all()) {
            if (otherUnique(definition, currentDefinition, allowedUniques)) {
                return true;
            }
        }
        return false;
    }

    static Socketable transpose(SocketableStore store, Socketable socketable, Random random, Predicate<SocketableDefinition> allowedUniques) {
        SocketableDefinition currentDefinition = socketable.definition();
        if (currentDefinition == null) {
            return null;
        }
        SocketableDefinition nextDefinition = SocketableDrops.pickUnique(random, definition -> otherUnique(definition, currentDefinition, allowedUniques));
        if (nextDefinition == null) {
            return null;
        }
        long seed = random.nextLong();
        Socketable replacement = new Socketable(socketable.id(), nextDefinition.id(), seed, SocketableRoller.roll(nextDefinition, seed));
        replacement.freezeName();
        return store.replace(socketable, replacement) ? replacement : null;
    }

    private static boolean otherUnique(SocketableDefinition definition, SocketableDefinition currentDefinition, Predicate<SocketableDefinition> allowedUniques) {
        return definition.unique() && !definition.id().equals(currentDefinition.id()) && definition.rarity() > 0f && allowedUniques.test(definition);
    }
}
