package exiledsector.items;

import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierCategory;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class RolledModifier {

    private final String modifierId;
    private final ModifierSlot slot;
    private final List<RolledEffect> lines;
    private final boolean locked;

    public RolledModifier(String modifierId, ModifierSlot slot, List<RolledEffect> lines) {
        this(modifierId, slot, lines, false);
    }

    public RolledModifier(String modifierId, ModifierSlot slot, List<RolledEffect> lines, boolean locked) {
        this.modifierId = modifierId;
        this.slot = slot;
        this.lines = new ArrayList<>(lines);
        this.locked = locked;
    }

    public static RolledModifier single(ModifierSlot slot, RolledEffect line) {
        return new RolledModifier(line.effectName(), slot, List.of(line));
    }

    public static RolledModifier roll(Modifier modifier, Random random, boolean averageRolls) {
        List<RolledEffect> rolledLines = new ArrayList<>(modifier.lines().size());
        for (ModifierLine line : modifier.lines()) {
            float rolledMagnitude = line.roll(random);
            rolledLines.add(new RolledEffect(line.effectName(), averageRolls ? line.average() : rolledMagnitude));
        }
        return new RolledModifier(modifier.id(), modifier.slot(), rolledLines);
    }

    public static List<RolledModifier> fromLegacyEffects(ItemDefinition definition, List<RolledEffect> effects) {
        List<RolledModifier> modifiers = new ArrayList<>(effects.size());
        for (int index = 0; index < effects.size(); index++) {
            modifiers.add(single(legacySlot(definition, effects, index), effects.get(index)));
        }
        return definition == null ? modifiers : definition.withCurrentIds(modifiers);
    }

    private static ModifierSlot legacySlot(ItemDefinition definition, List<RolledEffect> effects, int index) {
        ModifierSlot listedSlot = listedSlot(definition, effects.get(index));
        if (listedSlot != null) {
            return listedSlot;
        }
        for (RolledEffect laterEffect : effects.subList(index + 1, effects.size())) {
            if (listedSlot(definition, laterEffect) == ModifierSlot.PREFIX) {
                return ModifierSlot.PREFIX;
            }
        }
        return ModifierSlot.SUFFIX;
    }

    private static ModifierSlot listedSlot(ItemDefinition definition, RolledEffect effect) {
        PoolEntry entry = definition == null ? null : definition.poolEntry(effect.effectName());
        return entry == null ? null : entry.slot();
    }

    public static List<RolledEffect> lines(List<RolledModifier> modifiers) {
        List<RolledEffect> flattened = new ArrayList<>();
        modifiers.forEach(modifier -> flattened.addAll(modifier.lines()));
        return flattened;
    }

    private Object readResolve() {
        String currentId = RolledEffect.currentName(modifierId);
        return Objects.equals(currentId, modifierId) ? this : new RolledModifier(currentId, slot, lines(), locked);
    }

    public String modifierId() {
        return modifierId;
    }

    public ModifierSlot slot() {
        return slot;
    }

    public ModifierCategory category() {
        return slot.category();
    }

    public List<RolledEffect> lines() {
        return lines == null ? List.of() : Collections.unmodifiableList(lines);
    }

    public boolean locked() {
        return locked;
    }

    public RolledModifier asLocked() {
        return new RolledModifier(modifierId, slot, lines(), true);
    }

    public boolean isHybrid() {
        return lines().size() > 1;
    }

    public RolledModifier withLines(List<RolledEffect> replacementLines) {
        return new RolledModifier(modifierId, slot, replacementLines, locked);
    }

    public RolledModifier mapLines(UnaryOperator<RolledEffect> lineMapper) {
        List<RolledEffect> mappedLines = new ArrayList<>(lines.size());
        for (RolledEffect line : lines) {
            mappedLines.add(lineMapper.apply(line));
        }
        return withLines(mappedLines);
    }

    public RolledModifier rerolled(Function<String, ModifierLine> lineDefinitions, Random random) {
        return mapLines(line -> {
            ModifierLine lineDefinition = lineDefinitions.apply(line.effectName());
            return lineDefinition == null ? line : new RolledEffect(line.effectName(), lineDefinition.roll(random));
        });
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof RolledModifier rolled && Objects.equals(modifierId, rolled.modifierId) && slot == rolled.slot
                && lines().equals(rolled.lines()) && locked == rolled.locked;
    }

    @Override
    public int hashCode() {
        return Objects.hash(modifierId, slot, lines(), locked);
    }

    @Override
    public String toString() {
        return slot + " " + modifierId + lines();
    }
}
