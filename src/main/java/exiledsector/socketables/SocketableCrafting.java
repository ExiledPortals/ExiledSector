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
        int cost = commonSynthesisParts();
        if (cargo == null || parts(cargo) < cost) {
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
            cargo.removeCommodity(SocketableDisassembly.PARTS_COMMODITY_ID, cost);
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
        Socketable result = switch (currency) {
            case AUGMENTATION -> augment(socketable, random) == null ? null : socketable;
            case RECALIBRATION -> recalibrate(socketable, random);
            case TRANSPOSITION -> transpose(store, socketable, random, allowedUniques);
        };
        if (result != null) {
            cargo.removeCommodity(currency.commodityId(), 1);
        }
        return result;
    }

    static RolledEffect augment(Socketable socketable, Random random) {
        SocketableDefinition definition = socketable.definition();
        List<List<SocketableDefinition.PoolEntry>> sides = definition == null ? List.of() : augmentChoices(socketable, definition);
        if (definition == null || sides.isEmpty()) {
            return null;
        }
        List<SocketableDefinition.PoolEntry> side = sides.get(random.nextInt(sides.size()));
        SocketableDefinition.PoolEntry entry = side.get(SocketableRoller.pick(side, random.nextFloat()));
        RolledEffect added = new RolledEffect(entry.effectName(), SocketableRoller.wholeNumberBetween(entry.min(), entry.max(), random));
        SocketableRarity before = socketable.rarity();
        List<RolledEffect> updated = new ArrayList<>(socketable.effects());
        updated.add(definition.isPrefix(added.effectName()) ? prefixCount(socketable, definition) : updated.size(), added);
        socketable.replaceEffects(updated);
        if (socketable.rarity() != before) {
            socketable.refreezeName();
        }
        return added;
    }

    private static int prefixCount(Socketable socketable, SocketableDefinition definition) {
        int count = 0;
        for (RolledEffect effect : socketable.effects()) {
            count += definition.isPrefix(effect.effectName()) ? 1 : 0;
        }
        return count;
    }

    private static List<List<SocketableDefinition.PoolEntry>> augmentChoices(Socketable socketable, SocketableDefinition definition) {
        if (definition.unique() || socketable.effects().size() >= MAX_EFFECTS) {
            return List.of();
        }
        Set<String> present = new HashSet<>();
        int prefixes = 0;
        int suffixes = 0;
        for (RolledEffect effect : socketable.effects()) {
            present.add(effect.effectName());
            prefixes += definition.isPrefix(effect.effectName()) ? 1 : 0;
            suffixes += definition.isSuffix(effect.effectName()) ? 1 : 0;
        }
        List<List<SocketableDefinition.PoolEntry>> sides = new ArrayList<>();
        addSide(sides, definition.prefixes(), prefixes, present);
        addSide(sides, definition.suffixes(), suffixes, present);
        return sides;
    }

    private static void addSide(List<List<SocketableDefinition.PoolEntry>> sides, List<SocketableDefinition.PoolEntry> pool, int taken,
                                Set<String> present) {
        if (taken >= MAX_PER_SIDE) {
            return;
        }
        List<SocketableDefinition.PoolEntry> open = pool.stream().filter(entry -> !present.contains(entry.effectName())).toList();
        if (!open.isEmpty()) {
            sides.add(open);
        }
    }

    private static boolean canRecalibrate(Socketable socketable, SocketableDefinition definition) {
        for (RolledEffect effect : socketable.effects()) {
            SocketableDefinition.PoolEntry range = definition.rollRange(effect.effectName());
            if (range != null && Math.ceil(range.min()) < Math.floor(range.max())) {
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
            SocketableDefinition.PoolEntry range = definition.rollRange(effect.effectName());
            rerolled.add(range == null ? effect
                    : new RolledEffect(effect.effectName(), SocketableRoller.wholeNumberBetween(range.min(), range.max(), random)));
        }
        socketable.replaceEffects(rerolled);
        return socketable;
    }

    private static boolean canTranspose(SocketableDefinition current, Predicate<SocketableDefinition> allowedUniques) {
        if (!current.unique()) {
            return false;
        }
        for (SocketableDefinition definition : SocketableDefinitions.all()) {
            if (otherUnique(definition, current, allowedUniques)) {
                return true;
            }
        }
        return false;
    }

    static Socketable transpose(SocketableStore store, Socketable socketable, Random random, Predicate<SocketableDefinition> allowedUniques) {
        SocketableDefinition current = socketable.definition();
        if (current == null) {
            return null;
        }
        SocketableDefinition next = SocketableDrops.pickUnique(random, definition -> otherUnique(definition, current, allowedUniques));
        if (next == null) {
            return null;
        }
        long seed = random.nextLong();
        Socketable replacement = next.kind().create(socketable.id(), next.id(), seed, SocketableRoller.roll(next, seed));
        replacement.freezeName();
        return store.replace(socketable, replacement) ? replacement : null;
    }

    private static boolean otherUnique(SocketableDefinition definition, SocketableDefinition current, Predicate<SocketableDefinition> allowed) {
        return definition.unique() && !definition.id().equals(current.id()) && definition.rarity() > 0f && allowed.test(definition);
    }
}
