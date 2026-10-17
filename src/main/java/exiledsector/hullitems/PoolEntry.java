package exiledsector.hullitems;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;

import java.util.List;
import java.util.Random;

public record PoolEntry(Modifier modifier, float weight) {

    public static PoolEntry of(ModifierSlot slot, String effectName, float min, float max, float weight) {
        return new PoolEntry(Modifier.single(slot, ModifierLine.range(effectName, min, max)), weight);
    }

    public static PoolEntry perHullSize(ModifierSlot slot, String effectName, float weight, List<Float> hullValues) {
        return new PoolEntry(Modifier.single(slot, ModifierLine.perHullSize(effectName, hullValues)), weight);
    }

    static PoolEntry parse(String entryText, ModifierSlot slot) {
        ModifierLine.Parsed parsed = ModifierLine.parse(entryText);
        float weight = parsed.trailingField() == null ? 1f : ModifierLine.parseNumber(parsed.trailingField(), entryText);
        if (!(weight > 0f)) {
            throw new IllegalArgumentException("entry \"" + entryText + "\" needs a weight above zero");
        }
        return new PoolEntry(Modifier.single(slot, parsed.line()), weight);
    }

    PoolEntry named(String currentEffectName) {
        return new PoolEntry(Modifier.single(modifier.slot(), line().named(currentEffectName)), weight);
    }

    public ModifierLine line() {
        return modifier.line();
    }

    public ModifierSlot slot() {
        return modifier.slot();
    }

    public String effectName() {
        return line().effectName();
    }

    public float min() {
        return line().min();
    }

    public float max() {
        return line().max();
    }

    public List<Float> hullValues() {
        return line().hullValues();
    }

    public float roll(Random random) {
        return line().roll(random);
    }

    public float average() {
        return line().average();
    }

    public int stepCount() {
        return line().stepCount();
    }

    public boolean canVary() {
        return line().canVary();
    }

    public boolean scalesWithHullSize() {
        return line().scalesWithHullSize();
    }

    public boolean hasHullValueFor(HullSize hullSize) {
        return line().hasHullValueFor(hullSize);
    }

    public boolean listsEveryHullValueFor(HullSize hullSize) {
        return line().listsEveryHullValueFor(hullSize);
    }

    public float valueFor(float rolledMagnitude, HullSize hullSize) {
        return line().valueFor(rolledMagnitude, hullSize);
    }

    public float boundNearestZero() {
        return line().boundNearestZero();
    }

    public float boundFarthestFromZero() {
        return line().boundFarthestFromZero();
    }
}
