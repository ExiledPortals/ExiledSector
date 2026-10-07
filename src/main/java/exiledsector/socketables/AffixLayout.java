package exiledsector.socketables;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class AffixLayout {

    static final int COMMON_MAX_EFFECTS = 2;
    static final int MAX_EFFECTS = 4;
    static final int MAX_PER_SIDE = 2;
    static final float TWO_EFFECTS_CHANCE = 0.55f;
    static final float THREE_EFFECTS_CHANCE = 0.35f;

    enum Side {PREFIX, SUFFIX, EITHER}

    private AffixLayout() {
    }

    static SocketableRarity rarityFor(SocketableDefinition definition, int effectCount) {
        if (definition != null && definition.unique()) {
            return SocketableRarity.UNIQUE;
        }
        return effectCount <= COMMON_MAX_EFFECTS ? SocketableRarity.COMMON : SocketableRarity.RARE;
    }

    static int rolledEffectCount(float roll) {
        if (roll < TWO_EFFECTS_CHANCE) {
            return 2;
        }
        return roll < TWO_EFFECTS_CHANCE + THREE_EFFECTS_CHANCE ? 3 : 4;
    }

    static int prefixCount(int effectCount, boolean morePrefixes) {
        int evenShare = effectCount / 2;
        if (effectCount % 2 == 0 || !morePrefixes) {
            return evenShare;
        }
        return evenShare + 1;
    }

    static List<List<PoolEntry>> openSides(SocketableDefinition definition, List<RolledEffect> effects) {
        if (definition.unique() || effects.size() >= MAX_EFFECTS) {
            return List.of();
        }
        Set<String> presentEffects = new HashSet<>();
        int takenPrefixes = 0;
        int takenSuffixes = 0;
        for (int index = 0; index < effects.size(); index++) {
            presentEffects.add(effects.get(index).effectName());
            Side side = sideAt(definition, effects, index);
            takenPrefixes += side == Side.SUFFIX ? 0 : 1;
            takenSuffixes += side == Side.PREFIX ? 0 : 1;
        }
        List<List<PoolEntry>> sides = new ArrayList<>();
        addOpenSide(sides, definition.prefixes(), takenPrefixes, presentEffects);
        addOpenSide(sides, definition.suffixes(), takenSuffixes, presentEffects);
        return sides;
    }

    static int insertIndex(SocketableDefinition definition, List<RolledEffect> effects, String addedEffectName) {
        if (!definition.isPrefix(addedEffectName)) {
            return effects.size();
        }
        int prefixTotal = 0;
        for (int index = 0; index < effects.size(); index++) {
            prefixTotal += sideAt(definition, effects, index) == Side.PREFIX ? 1 : 0;
        }
        return prefixTotal;
    }

    static Side sideAt(SocketableDefinition definition, List<RolledEffect> effects, int index) {
        String effectName = effects.get(index).effectName();
        if (definition.isPrefix(effectName)) {
            return Side.PREFIX;
        }
        if (definition.isSuffix(effectName)) {
            return Side.SUFFIX;
        }
        for (RolledEffect laterEffect : effects.subList(index + 1, effects.size())) {
            if (definition.isPrefix(laterEffect.effectName())) {
                return Side.PREFIX;
            }
        }
        for (RolledEffect earlierEffect : effects.subList(0, index)) {
            if (definition.isSuffix(earlierEffect.effectName())) {
                return Side.SUFFIX;
            }
        }
        return Side.EITHER;
    }

    private static void addOpenSide(List<List<PoolEntry>> sides, List<PoolEntry> sidePool, int taken, Set<String> presentEffects) {
        if (taken >= MAX_PER_SIDE) {
            return;
        }
        List<PoolEntry> openEntries = sidePool.stream().filter(entry -> !presentEffects.contains(entry.effectName())).toList();
        if (!openEntries.isEmpty()) {
            sides.add(openEntries);
        }
    }
}
