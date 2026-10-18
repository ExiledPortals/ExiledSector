package exiledsector.items;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;

import java.util.List;
import java.util.Random;

public record PoolEntry(Modifier modifier, float weight) {

    private static final String WEIGHT_SEPARATOR = ":";

    public static PoolEntry of(ModifierSlot slot, String effectName, float min, float max, float weight) {
        return new PoolEntry(Modifier.single(slot, ModifierLine.range(effectName, min, max)), weight);
    }

    public static PoolEntry perHullSize(ModifierSlot slot, String effectName, float weight, List<Float> hullValues) {
        return new PoolEntry(Modifier.single(slot, ModifierLine.perHullSize(effectName, hullValues)), weight);
    }

    static PoolEntry parse(String entryText, ModifierSlot slot) {
        return parse(entryText, slot, null);
    }

    static PoolEntry parse(String entryText, ModifierSlot slot, ItemType itemType) {
        if (ItemModifiers.isReference(entryText)) {
            return parseReference(entryText, slot, itemType);
        }
        ModifierLine.Parsed parsed = ModifierLine.parse(entryText);
        float weight = parseWeight(parsed.trailingField(), entryText);
        String effectName = ItemDefinition.currentEffectName(parsed.line().effectName());
        return effectName == null ? null : new PoolEntry(Modifier.single(slot, parsed.line().named(effectName)), weight);
    }

    private static PoolEntry parseReference(String entryText, ModifierSlot slot, ItemType itemType) {
        String[] fields = entryText.substring(ItemModifiers.REFERENCE_MARK.length()).split(WEIGHT_SEPARATOR, -1);
        if (fields.length > 2) {
            throw new IllegalArgumentException("entry \"" + entryText + "\" is not @MODIFIER_ID or @MODIFIER_ID:weight");
        }
        Modifier referencedModifier = ItemModifiers.referenced(fields[0].trim(), slot, itemType);
        return new PoolEntry(referencedModifier, parseWeight(fields.length == 2 ? fields[1] : null, entryText));
    }

    private static float parseWeight(String weightText, String entryText) {
        float weight = weightText == null ? 1f : ModifierLine.parseNumber(weightText, entryText);
        if (!(weight > 0f)) {
            throw new IllegalArgumentException("entry \"" + entryText + "\" needs a weight above zero");
        }
        return weight;
    }

    boolean isSingle() {
        return !modifier.isHybrid();
    }

    boolean hasLine(String effectName) {
        return modifier.line(effectName) != null;
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
