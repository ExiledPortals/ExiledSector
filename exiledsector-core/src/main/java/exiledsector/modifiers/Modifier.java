package exiledsector.modifiers;

import java.util.List;

public record Modifier(String id, ModifierSlot slot, List<ModifierLine> lines) {

    public Modifier {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("a modifier needs an id");
        }
        if (slot == null) {
            throw new IllegalArgumentException("modifier " + id + " needs a slot");
        }
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("modifier " + id + " needs at least one line");
        }
        lines = List.copyOf(lines);
    }

    public static Modifier single(ModifierSlot slot, ModifierLine line) {
        return new Modifier(line.effectName(), slot, List.of(line));
    }

    public static Modifier byId(List<Modifier> modifiers, String modifierId) {
        for (Modifier modifier : modifiers) {
            if (modifier.id().equals(modifierId)) {
                return modifier;
            }
        }
        return null;
    }

    public static ModifierLine lineIn(List<Modifier> modifiers, String effectName) {
        for (Modifier modifier : modifiers) {
            ModifierLine line = modifier.line(effectName);
            if (line != null) {
                return line;
            }
        }
        return null;
    }

    public ModifierLine line() {
        return lines.get(0);
    }

    public boolean isHybrid() {
        return lines.size() > 1;
    }

    public ModifierLine line(String effectName) {
        for (ModifierLine candidate : lines) {
            if (candidate.effectName().equals(effectName)) {
                return candidate;
            }
        }
        return null;
    }
}
