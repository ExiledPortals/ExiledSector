package exiledsector.socketables;

import com.fs.starfarer.api.campaign.CargoAPI;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

public final class SocketableCrafting {

    static final int DEFAULT_COMMON_SYNTHESIS_PARTS = 10;
    static final int MAX_EFFECTS = 4;
    static final int MAX_PER_SIDE = 2;

    private SocketableCrafting() {
    }

    public static int commonSynthesisParts() {
        return SocketCraftingCosts.cost(SocketCraftingCosts.COMMON_SUBROUTINE, DEFAULT_COMMON_SYNTHESIS_PARTS);
    }

    public static int count(CargoAPI cargo, String commodityId) {
        return cargo == null ? 0 : Math.round(cargo.getCommodityQuantity(commodityId));
    }

    public static int parts(CargoAPI cargo) {
        return count(cargo, SocketableDisassembly.PARTS_COMMODITY_ID);
    }

    public static boolean synthesise(SocketCurrency currency, CargoAPI cargo) {
        if (cargo == null || parts(cargo) < currency.partsCost()) {
            return false;
        }
        cargo.removeCommodity(SocketableDisassembly.PARTS_COMMODITY_ID, currency.partsCost());
        cargo.addCommodity(currency.commodityId(), 1);
        return true;
    }

    public static Socketable synthesiseCommon(CargoAPI cargo, SocketableStore store, Random random) {
        int partsCost = commonSynthesisParts();
        if (cargo == null || parts(cargo) < partsCost) {
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
            cargo.removeCommodity(SocketableDisassembly.PARTS_COMMODITY_ID, partsCost);
        }
        return created;
    }

    public static boolean canUse(SocketCurrency currency, Socketable socketable, Predicate<SocketableDefinition> allowedUniques) {
        SocketableDefinition definition = socketable == null ? null : socketable.definition();
        if (definition == null) {
            return false;
        }
        return switch (currency) {
            case AUGMENTATION -> !augmentChoices(socketable, definition).isEmpty();
            case RECALIBRATION -> canRecalibrate(socketable, definition);
            case TRANSPOSITION -> canTranspose(definition, allowedUniques);
        };
    }

    public static Socketable use(SocketCurrency currency, Socketable socketable, CargoAPI cargo, SocketableStore store, Random random,
                                 Predicate<SocketableDefinition> allowedUniques) {
        if (count(cargo, currency.commodityId()) < 1 || !canUse(currency, socketable, allowedUniques)) {
            return null;
        }
        Socketable craftedSocketable = switch (currency) {
            case AUGMENTATION -> augment(socketable, random) == null ? null : socketable;
            case RECALIBRATION -> recalibrate(socketable, random);
            case TRANSPOSITION -> transpose(store, socketable, random, allowedUniques);
        };
        if (craftedSocketable != null) {
            cargo.removeCommodity(currency.commodityId(), 1);
        }
        return craftedSocketable;
    }

    static RolledEffect augment(Socketable socketable, Random random) {
        SocketableDefinition definition = socketable.definition();
        List<List<SocketableDefinition.PoolEntry>> openSides = definition == null ? List.of() : augmentChoices(socketable, definition);
        if (definition == null || openSides.isEmpty()) {
            return null;
        }
        List<SocketableDefinition.PoolEntry> chosenSide = openSides.get(random.nextInt(openSides.size()));
        SocketableDefinition.PoolEntry poolEntry = chosenSide.get(SocketableRoller.pick(chosenSide, random.nextFloat()));
        RolledEffect addedEffect = new RolledEffect(poolEntry.effectName(), SocketableRoller.wholeNumberBetween(poolEntry.min(), poolEntry.max(), random));
        SocketableRarity rarityBefore = socketable.rarity();
        List<RolledEffect> updatedEffects = new ArrayList<>(socketable.effects());
        updatedEffects.add(definition.isPrefix(addedEffect.effectName()) ? prefixCount(socketable, definition) : updatedEffects.size(), addedEffect);
        socketable.replaceEffects(updatedEffects);
        if (socketable.rarity() != rarityBefore) {
            socketable.refreezeName();
        }
        return addedEffect;
    }

    private static int prefixCount(Socketable socketable, SocketableDefinition definition) {
        int prefixTotal = 0;
        for (RolledEffect effect : socketable.effects()) {
            prefixTotal += definition.isPrefix(effect.effectName()) ? 1 : 0;
        }
        return prefixTotal;
    }

    private static List<List<SocketableDefinition.PoolEntry>> augmentChoices(Socketable socketable, SocketableDefinition definition) {
        if (definition.unique() || socketable.effects().size() >= MAX_EFFECTS) {
            return List.of();
        }
        Set<String> presentEffects = new HashSet<>();
        int takenPrefixes = 0;
        int takenSuffixes = 0;
        for (RolledEffect effect : socketable.effects()) {
            presentEffects.add(effect.effectName());
            takenPrefixes += definition.isPrefix(effect.effectName()) ? 1 : 0;
            takenSuffixes += definition.isSuffix(effect.effectName()) ? 1 : 0;
        }
        List<List<SocketableDefinition.PoolEntry>> sides = new ArrayList<>();
        addSide(sides, definition.prefixes(), takenPrefixes, presentEffects);
        addSide(sides, definition.suffixes(), takenSuffixes, presentEffects);
        return sides;
    }

    private static void addSide(List<List<SocketableDefinition.PoolEntry>> sides, List<SocketableDefinition.PoolEntry> pool, int taken,
                                Set<String> presentEffects) {
        if (taken >= MAX_PER_SIDE) {
            return;
        }
        List<SocketableDefinition.PoolEntry> openEntries = pool.stream().filter(entry -> !presentEffects.contains(entry.effectName())).toList();
        if (!openEntries.isEmpty()) {
            sides.add(openEntries);
        }
    }

    private static boolean canRecalibrate(Socketable socketable, SocketableDefinition definition) {
        for (RolledEffect effect : socketable.effects()) {
            SocketableDefinition.PoolEntry rollRange = definition.rollRange(effect.effectName());
            if (rollRange != null && Math.ceil(rollRange.min()) < Math.floor(rollRange.max())) {
                return true;
            }
        }
        return false;
    }

    static Socketable recalibrate(Socketable socketable, Random random) {
        SocketableDefinition definition = socketable.definition();
        if (definition == null) {
            return socketable;
        }
        List<RolledEffect> rerolled = new ArrayList<>(socketable.effects().size());
        for (RolledEffect effect : socketable.effects()) {
            SocketableDefinition.PoolEntry rollRange = definition.rollRange(effect.effectName());
            rerolled.add(rollRange == null ? effect
                    : new RolledEffect(effect.effectName(), SocketableRoller.wholeNumberBetween(rollRange.min(), rollRange.max(), random)));
        }
        socketable.replaceEffects(rerolled);
        return socketable;
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
        Socketable replacement = nextDefinition.kind().create(socketable.id(), nextDefinition.id(), seed, SocketableRoller.roll(nextDefinition, seed));
        replacement.freezeName();
        return store.replace(socketable, replacement) ? replacement : null;
    }

    private static boolean otherUnique(SocketableDefinition definition, SocketableDefinition currentDefinition, Predicate<SocketableDefinition> allowedUniques) {
        return definition.unique() && !definition.id().equals(currentDefinition.id()) && definition.rarity() > 0f && allowedUniques.test(definition);
    }
}
