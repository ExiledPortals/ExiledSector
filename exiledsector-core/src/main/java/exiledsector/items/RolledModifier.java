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
    private final List<RolledLine> lines;
    private final boolean immutable;
    private final boolean warped;

    public RolledModifier(String modifierId, ModifierSlot slot, List<RolledLine> lines) {
        this(modifierId, slot, lines, false);
    }

    public RolledModifier(String modifierId, ModifierSlot slot, List<RolledLine> lines, boolean immutable) {
        this(modifierId, slot, lines, immutable, false);
    }

    public RolledModifier(String modifierId, ModifierSlot slot, List<RolledLine> lines, boolean immutable, boolean warped) {
        this.modifierId = modifierId;
        this.slot = slot;
        this.lines = new ArrayList<>(lines);
        this.immutable = immutable;
        this.warped = warped;
    }

    public static RolledModifier single(ModifierSlot slot, RolledLine line) {
        return new RolledModifier(line.effectName(), slot, List.of(line));
    }

    public static RolledModifier roll(Modifier modifier, Random random) {
        List<RolledLine> rolledLines = new ArrayList<>(modifier.lines().size());
        for (ModifierLine line : modifier.lines()) {
            rolledLines.add(new RolledLine(line.effectName(), line.roll(random)));
        }
        return new RolledModifier(modifier.id(), modifier.slot(), rolledLines);
    }

    public static List<RolledModifier> fromLegacyEffects(ItemDefinition definition, List<RolledLine> effects) {
        List<RolledModifier> modifiers = new ArrayList<>(effects.size());
        for (int index = 0; index < effects.size(); index++) {
            modifiers.add(single(legacySlot(definition, effects, index), effects.get(index)));
        }
        return definition == null ? modifiers : definition.withCurrentIds(modifiers);
    }

    private static ModifierSlot legacySlot(ItemDefinition definition, List<RolledLine> effects, int index) {
        ModifierSlot listedSlot = listedSlot(definition, effects.get(index));
        if (listedSlot != null) {
            return listedSlot;
        }
        for (RolledLine laterEffect : effects.subList(index + 1, effects.size())) {
            if (listedSlot(definition, laterEffect) == ModifierSlot.PREFIX) {
                return ModifierSlot.PREFIX;
            }
        }
        return ModifierSlot.SUFFIX;
    }

    private static ModifierSlot listedSlot(ItemDefinition definition, RolledLine effect) {
        PoolEntry entry = definition == null ? null : definition.poolEntry(effect.effectName());
        return entry == null ? null : entry.slot();
    }

    public static List<RolledLine> lines(List<RolledModifier> modifiers) {
        List<RolledLine> flattened = new ArrayList<>();
        modifiers.forEach(modifier -> flattened.addAll(modifier.lines()));
        return flattened;
    }

    private Object readResolve() {
        String currentId = RolledLine.currentName(modifierId);
        return Objects.equals(currentId, modifierId) ? this : withModifierId(currentId);
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

    public List<RolledLine> lines() {
        return lines == null ? List.of() : Collections.unmodifiableList(lines);
    }

    public boolean immutable() {
        return immutable;
    }

    public RolledModifier asImmutable() {
        return new RolledModifier(modifierId, slot, lines(), true, warped);
    }

    public boolean warped() {
        return warped;
    }

    public RolledModifier asWarped() {
        return new RolledModifier(modifierId, slot, lines(), immutable, true);
    }

    public RolledModifier withModifierId(String replacementId) {
        return new RolledModifier(replacementId, slot, lines(), immutable, warped);
    }

    public boolean isHybrid() {
        return lines().size() > 1;
    }

    public RolledModifier withLines(List<RolledLine> replacementLines) {
        return new RolledModifier(modifierId, slot, replacementLines, immutable, warped);
    }

    public RolledModifier mapLines(UnaryOperator<RolledLine> lineMapper) {
        List<RolledLine> mappedLines = new ArrayList<>(lines.size());
        for (RolledLine line : lines) {
            mappedLines.add(lineMapper.apply(line));
        }
        return withLines(mappedLines);
    }

    public RolledModifier rerolled(Function<String, ModifierLine> lineDefinitions, Random random) {
        return mapLines(line -> {
            ModifierLine lineDefinition = lineDefinitions.apply(line.effectName());
            return lineDefinition == null ? line : new RolledLine(line.effectName(), lineDefinition.roll(random));
        });
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof RolledModifier rolled && Objects.equals(modifierId, rolled.modifierId) && slot == rolled.slot
                && lines().equals(rolled.lines()) && immutable == rolled.immutable && warped == rolled.warped;
    }

    @Override
    public int hashCode() {
        return Objects.hash(modifierId, slot, lines(), immutable, warped);
    }

    @Override
    public String toString() {
        return slot + " " + modifierId + lines();
    }
}
