package exiledsector.items;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.modifiers.TierRange;

import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record PoolEntry(Modifier modifier, float weight, TierRange tiers) {

    private static final String WEIGHT_SEPARATOR = ":";
    private static final String TIER_MARK = "t";
    private static final Pattern TIER_RANGE = Pattern.compile("t(\\d+)(?:(\\+)|-(\\d+))?");

    public PoolEntry {
        tiers = tiers == null ? TierRange.ANY : tiers;
    }

    public PoolEntry(Modifier modifier, float weight) {
        this(modifier, weight, TierRange.ANY);
    }

    public static PoolEntry of(ModifierSlot slot, String effectName, float min, float max, float weight) {
        return new PoolEntry(Modifier.single(slot, ModifierLine.range(effectName, min, max)), weight);
    }

    public static PoolEntry perHullSize(ModifierSlot slot, String effectName, float weight, List<Float> hullValues) {
        return new PoolEntry(Modifier.single(slot, ModifierLine.perHullSize(effectName, hullValues)), weight);
    }

    public static PoolEntry parse(String entryText, ModifierSlot slot) {
        return parse(entryText, slot, (ModifierScope) null);
    }

    public static PoolEntry parse(String entryText, ModifierSlot slot, ItemType itemType) {
        return parse(entryText, slot, ModifierScope.of(itemType));
    }

    public static PoolEntry parse(String entryText, ModifierSlot slot, ModifierScope scope) {
        if (ItemModifiers.isReference(entryText)) {
            return parseReference(entryText, slot, scope);
        }
        ModifierLine.Parsed parsed = ModifierLine.parse(entryText);
        float weight = parseWeight(parsed.trailingField(), entryText);
        String effectName = ItemDefinition.currentEffectName(parsed.line().effectName(), ItemEffects.forType(scope == null ? null : scope.itemType()));
        return effectName == null ? null : new PoolEntry(Modifier.single(slot, parsed.line().named(effectName)), weight);
    }

    private static PoolEntry parseReference(String entryText, ModifierSlot slot, ModifierScope scope) {
        String[] fields = entryText.substring(ItemModifiers.REFERENCE_MARK.length()).split(WEIGHT_SEPARATOR, -1);
        boolean tierGated = fields.length >= 2 && isTierRangeText(fields[fields.length - 1]);
        int weightFields = fields.length - 1 - (tierGated ? 1 : 0);
        if (weightFields > 1 || fields.length > 3) {
            throw new IllegalArgumentException("entry \"" + entryText + "\" is not @MODIFIER_ID, @MODIFIER_ID:weight, @MODIFIER_ID:tMIN-MAX "
                    + "or @MODIFIER_ID:weight:tMIN-MAX");
        }
        Modifier referencedModifier = ItemModifiers.referenced(fields[0].trim(), slot, scope);
        float weight = parseWeight(weightFields == 1 ? fields[1] : null, entryText);
        TierRange tiers = tierGated ? parseTierRange(fields[fields.length - 1], entryText) : TierRange.ANY;
        return new PoolEntry(referencedModifier, weight, tiers);
    }

    public static boolean isTierRangeText(String fieldText) {
        return fieldText != null && fieldText.trim().startsWith(TIER_MARK);
    }

    public static TierRange parseTierRange(String fieldText, String entryText) {
        Matcher tierMatch = TIER_RANGE.matcher(fieldText.trim());
        if (!tierMatch.matches()) {
            throw new IllegalArgumentException("entry \"" + entryText + "\" has the tier range \"" + fieldText.trim()
                    + "\"; write it as tN, tN-M or tN+");
        }
        int minTier = Integer.parseInt(tierMatch.group(1));
        if (tierMatch.group(2) != null) {
            return TierRange.atLeast(minTier);
        }
        int maxTier = tierMatch.group(3) == null ? minTier : Integer.parseInt(tierMatch.group(3));
        return TierRange.between(minTier, maxTier);
    }

    private static float parseWeight(String weightText, String entryText) {
        float weight = weightText == null ? 1f : ModifierLine.parseNumber(weightText, entryText);
        if (!(weight > 0f)) {
            throw new IllegalArgumentException("entry \"" + entryText + "\" needs a weight above zero");
        }
        return weight;
    }

    public PoolEntry withTiers(TierRange allowedTiers) {
        return new PoolEntry(modifier, weight, allowedTiers);
    }

    public boolean rollsAtTier(int tier) {
        return tiers.allows(tier);
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
