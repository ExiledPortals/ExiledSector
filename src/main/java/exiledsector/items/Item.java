package exiledsector.items;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierCategory;
import exiledsector.modifiers.ModifierEffect;
import exiledsector.modifiers.ModifierEffects;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.modifiers.ScaleContext;
import exiledsector.modifiers.TierRange;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.SkillEffect;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public abstract class Item {

    private final String id;
    private String definitionId;
    private final long seed;
    private List<RolledEffect> effects;
    private List<RolledEffect> implicits;
    private List<RolledModifier> explicitModifiers;
    private List<RolledModifier> implicitModifiers;
    private FrozenName frozenName;
    private boolean mirrored;
    private CorruptionOutcome corruption;
    private transient boolean explicitIdsCurrent;
    private transient boolean implicitIdsCurrent;

    Item(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers) {
        this.id = id;
        this.definitionId = ItemIdAliases.currentId(definitionId);
        this.seed = seed;
        this.explicitModifiers = new ArrayList<>(explicitModifiers);
    }

    Item(Item source) {
        this.id = source.id;
        this.definitionId = source.definitionId;
        this.seed = source.seed;
        this.effects = source.effects;
        this.implicits = source.implicits;
        this.explicitModifiers = source.explicitModifiers;
        this.implicitModifiers = source.implicitModifiers;
        this.frozenName = source.frozenName;
        this.mirrored = source.mirrored;
        this.corruption = source.corruption;
    }

    static Item create(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers) {
        ItemDefinition definition = ItemDefinitions.get(definitionId);
        ItemType type = definition == null ? null : definition.type();
        if (type == null) {
            return new UnresolvedItem(id, definitionId, seed, explicitModifiers);
        }
        return switch (type) {
            case SUBROUTINE -> new Subroutine(id, definitionId, seed, explicitModifiers);
            case HULL_ITEM -> new HullItem(id, definitionId, seed, explicitModifiers);
            case STAR_CHART -> new StarChart(id, definitionId, seed, explicitModifiers);
        };
    }

    static Item create(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers, ItemTraits traits) {
        Item item = create(id, definitionId, seed, explicitModifiers);
        if (traits != null) {
            item.applyTraits(traits);
        }
        return item;
    }

    void applyTraits(ItemTraits traits) {
        if (traits.corruption() != null) {
            corruption = traits.corruption();
        }
        if (traits.mirrored()) {
            mirrored = true;
        }
    }

    void copyTraitsFrom(Item source) {
    }

    public ItemTraits traits() {
        return null;
    }

    static Item withEffects(String id, String definitionId, long seed, List<RolledEffect> effects) {
        return create(id, definitionId, seed, RolledModifier.fromLegacyEffects(ItemDefinitions.get(definitionId), effects));
    }

    public abstract ItemType type();

    public ModifierEffects modifierEffects() {
        return ItemEffects.forType(type());
    }

    public int tier() {
        return TierRange.NO_TIER;
    }

    public ScaleContext scaleContext(HullSize hullSize) {
        return ScaleContext.of(hullSize, tier());
    }

    protected Object readResolve() {
        definitionId = ItemIdAliases.currentId(definitionId);
        ItemDefinition definition = definition();
        ItemType definedType = definition == null ? null : definition.type();
        if (definedType == type()) {
            return this;
        }
        if (definedType == null) {
            return new UnresolvedItem(this);
        }
        return switch (definedType) {
            case SUBROUTINE -> new Subroutine(this);
            case HULL_ITEM -> new HullItem(this);
            case STAR_CHART -> new StarChart(this);
        };
    }

    public ItemKind kind() {
        ItemDefinition definition = definition();
        return definition == null ? null : definition.kind();
    }

    public String id() {
        return id;
    }

    public String definitionId() {
        return definitionId;
    }

    public long seed() {
        return seed;
    }

    public List<RolledModifier> explicits() {
        ItemDefinition definition = definition();
        if (explicitModifiers != null) {
            if (!explicitIdsCurrent && definition != null) {
                explicitModifiers = new ArrayList<>(definition.withCurrentIds(explicitModifiers));
                explicitIdsCurrent = true;
            }
            return Collections.unmodifiableList(explicitModifiers);
        }
        List<RolledModifier> converted = RolledModifier.fromLegacyEffects(definition, effects == null ? List.of() : effects);
        if (definition != null || effects == null) {
            explicitModifiers = new ArrayList<>(converted);
            explicitIdsCurrent = definition != null;
            effects = null;
        }
        return Collections.unmodifiableList(converted);
    }

    public List<RolledEffect> effects() {
        return RolledModifier.lines(explicits());
    }

    public List<RolledModifier> implicits() {
        if (implicitModifiers != null) {
            if (!implicitIdsCurrent && corruption != CorruptionOutcome.IMPLICIT_REPLACED && ItemImplicits.isLoaded() && kind() != null && !kind().implicits().isEmpty()) {
                implicitModifiers = new ArrayList<>(ItemImplicits.withCurrentIds(kind(), implicitModifiers, seed));
                implicitIdsCurrent = true;
            }
            return Collections.unmodifiableList(implicitModifiers);
        }
        boolean resolvable = ItemImplicits.isLoaded() && kind() != null;
        List<RolledModifier> rolled = implicits == null ? ItemImplicits.roll(kind(), seed)
                : resolvable ? ItemImplicits.withCurrentIds(kind(), implicitsFromLegacy(implicits), seed) : implicitsFromLegacy(implicits);
        if (implicits != null || resolvable) {
            implicitModifiers = new ArrayList<>(rolled);
            implicitIdsCurrent = resolvable && !kind().implicits().isEmpty();
            implicits = null;
        }
        return rolled;
    }

    List<RolledModifier> storedImplicits() {
        return implicitModifiers;
    }

    private static List<RolledModifier> implicitsFromLegacy(List<RolledEffect> legacyImplicits) {
        return legacyImplicits.stream().map(implicit -> RolledModifier.single(ModifierSlot.IMPLICIT, implicit)).toList();
    }

    public ItemDefinition definition() {
        return ItemDefinitions.get(definitionId);
    }

    public ItemName displayName() {
        ItemDefinition definition = definition();
        return ItemNames.render(definition, kind(), explicits(), frozenName(definition));
    }

    FrozenName frozenName() {
        return frozenName(definition());
    }

    private FrozenName frozenName(ItemDefinition definition) {
        if (definition != null && (frozenName == null || frozenName.isAssembledText())) {
            FrozenName freshName = ItemNames.freeze(definition, seed, explicits());
            if (freshName != null || frozenName == null) {
                frozenName = freshName;
            }
        }
        return frozenName;
    }

    void freezeName() {
        frozenName(definition());
    }

    void freezeName(FrozenName presetName) {
        if (presetName != null) {
            frozenName = presetName;
        }
    }

    void replaceExplicits(List<RolledModifier> replacementModifiers) {
        explicitModifiers = new ArrayList<>(replacementModifiers);
        effects = null;
    }

    void replaceImplicits(List<RolledModifier> replacementImplicits) {
        implicitModifiers = new ArrayList<>(replacementImplicits);
        implicits = null;
    }

    Item mirrorAs(String copyId) {
        Item copy = create(copyId, definitionId, seed, explicits());
        copy.copyTraitsFrom(this);
        copy.implicitModifiers = new ArrayList<>(implicits());
        copy.frozenName = frozenName;
        copy.mirrored = true;
        copy.corruption = corruption;
        return copy;
    }

    public boolean mirrored() {
        return mirrored;
    }

    public boolean corrupted() {
        return corruption != null;
    }

    public CorruptionOutcome corruption() {
        return corruption;
    }

    void corrupt(CorruptionOutcome outcome) {
        corruption = outcome;
    }

    public boolean hasLockedModifier() {
        return explicits().stream().anyMatch(RolledModifier::locked);
    }

    void refreezeName() {
        ItemDefinition definition = definition();
        frozenName = definition == null ? null : ItemNames.freeze(definition, seed, explicits());
    }

    public String name() {
        return displayName().title();
    }

    public Color frameColor() {
        return corrupted() ? CraftingCurrency.CORRUPTION.color() : rarity().color();
    }

    public ItemRarity rarity() {
        return AffixLayout.rarityFor(definition(), explicits().size());
    }

    public String iconPath() {
        ItemDefinition definition = definition();
        return definition == null ? ItemDefinition.FALLBACK_ICON : definition.icon();
    }

    public List<StyledText> tooltipLines() {
        List<StyledText> lines = new ArrayList<>(headerLines());
        lines.addAll(implicitLines(false));
        lines.addAll(effectLines());
        return lines;
    }

    public List<StyledText> headerLines() {
        List<StyledText> lines = new ArrayList<>();
        ItemDefinition definition = definition();
        if (definition != null) {
            StyledText description = definition.descriptionText();
            if (description != null) {
                lines.add(description);
            }
        }
        if (mirrored) {
            lines.add(Translation.styled("socketable.tooltip.mirrored"));
        }
        if (corruption != null) {
            lines.add(Translation.styled("socketable.tooltip.corrupted"));
        }
        return lines;
    }

    public boolean canSocketInto(SkillNode socket) {
        return false;
    }

    public boolean canSocketInto(ShipSection section) {
        return false;
    }

    public List<SkillTypeEffect> skillEffects() {
        return skillEffects(null);
    }

    public List<SlottedRoll> rolls() {
        List<SlottedRoll> rolls = new ArrayList<>();
        for (RolledModifier implicit : implicits()) {
            addRolls(rolls, implicit, implicitModifier(implicit.modifierId()));
        }
        for (RolledModifier explicit : explicits()) {
            addRolls(rolls, explicit, explicitModifier(explicit.modifierId()));
        }
        return rolls;
    }

    private static void addRolls(List<SlottedRoll> rolls, RolledModifier rolledModifier, Modifier modifier) {
        for (RolledEffect line : rolledModifier.lines()) {
            rolls.add(new SlottedRoll(rolledModifier.slot(), line, modifier == null ? null : modifier.line(line.effectName())));
        }
    }

    Modifier implicitModifier(String modifierId) {
        ItemKind type = kind();
        if (type == null) {
            return null;
        }
        return type.implicits().stream().filter(modifier -> modifier.id().equals(modifierId)).findFirst()
                .orElseGet(() -> ItemImplicits.anyById(modifierId));
    }

    ModifierLine implicitLine(String effectName) {
        return ItemImplicits.line(kind(), effectName);
    }

    List<Modifier> implicitCorruptionPool() {
        return ItemImplicits.corruptionPool();
    }

    boolean implicitCanVary(RolledModifier implicit) {
        for (RolledEffect line : implicit.lines()) {
            ModifierLine lineDefinition = implicitLine(line.effectName());
            if (lineDefinition != null && lineDefinition.canVary()) {
                return true;
            }
        }
        return false;
    }

    RolledModifier rerolledImplicit(RolledModifier implicit, Random random) {
        List<RolledEffect> rerolledLines = new ArrayList<>(implicit.lines().size());
        for (RolledEffect line : implicit.lines()) {
            ModifierLine lineDefinition = implicitLine(line.effectName());
            rerolledLines.add(lineDefinition == null ? line : new RolledEffect(line.effectName(), lineDefinition.roll(random)));
        }
        return implicit.withLines(rerolledLines);
    }

    Modifier explicitModifier(String modifierId) {
        ItemDefinition definition = definition();
        PoolEntry entry = definition == null ? null : definition.poolEntryFor(modifierId);
        return entry == null ? null : entry.modifier();
    }

    public List<SlottedRoll> rolls(ModifierCategory category) {
        return rolls().stream().filter(roll -> roll.category() == category).toList();
    }

    public List<SkillTypeEffect> skillEffects(HullSize hullSize) {
        List<SkillTypeEffect> appliedEffects = new ArrayList<>();
        ModifierEffects effects = modifierEffects();
        ScaleContext context = scaleContext(hullSize);
        for (SlottedRoll slottedRoll : rolls()) {
            ModifierEffect resolvedEffect = slottedRoll.roll().effect(effects);
            if (resolvedEffect instanceof SkillEffect skillEffect) {
                float rolledMagnitude = slottedRoll.roll().magnitude();
                float magnitude = slottedRoll.line() == null ? rolledMagnitude : slottedRoll.line().valueFor(rolledMagnitude, context);
                appliedEffects.add(new SkillTypeEffect(skillEffect, magnitude));
            }
        }
        return appliedEffects;
    }

    public List<StyledText> implicitLines(boolean withRollRanges) {
        List<SlottedLine> lines = new ArrayList<>();
        ModifierEffects effects = modifierEffects();
        ScaleContext context = scaleContext(null);
        for (RolledModifier implicit : implicits()) {
            addModifierLines(lines, implicit, implicitModifier(implicit.modifierId()), effects, withRollRanges, context);
        }
        return texts(lines);
    }

    public List<StyledText> effectLines() {
        return effectLines(false);
    }

    public List<StyledText> effectLines(boolean withRollRanges) {
        return effectLines(withRollRanges, null);
    }

    public List<StyledText> effectLines(boolean withRollRanges, HullSize hullSize) {
        return texts(slottedEffectLines(withRollRanges, hullSize));
    }

    public List<SlottedLine> slottedEffectLines(boolean withRollRanges, HullSize hullSize) {
        List<SlottedLine> lines = new ArrayList<>();
        ModifierEffects effects = modifierEffects();
        ScaleContext context = scaleContext(hullSize);
        for (RolledModifier explicitModifier : explicits()) {
            addModifierLines(lines, explicitModifier, explicitModifier(explicitModifier.modifierId()), effects, withRollRanges, context);
        }
        return lines;
    }

    private void addModifierLines(List<SlottedLine> lines, RolledModifier rolledModifier, Modifier modifier, ModifierEffects effects,
                                  boolean withRollRanges, ScaleContext context) {
        List<StyledText> descriptions = new ArrayList<>();
        for (RolledEffect effect : rolledModifier.lines()) {
            StyledText description = explicitLine(effects, effect, modifier == null ? null : modifier.line(effect.effectName()),
                    withRollRanges, context);
            if (description != null) {
                descriptions.add(rolledModifier.locked() ? description.withUnstyledAs(Style.IMMUTABLE) : description);
            }
        }
        if (!withRollRanges || descriptions.size() < 2) {
            descriptions.forEach(description -> lines.add(new SlottedLine(rolledModifier.slot(), description)));
            return;
        }
        lines.add(new SlottedLine(rolledModifier.slot(), Translation.styled("socketable.tooltip.hybrid")));
        for (StyledText description : descriptions) {
            lines.add(new SlottedLine(null, Translation.msg("socketable.tooltip.hybridLine").arg("line", description).styled()));
        }
    }

    private static List<StyledText> texts(List<SlottedLine> lines) {
        List<StyledText> texts = new ArrayList<>(lines.size());
        lines.forEach(line -> texts.add(line.text()));
        return texts;
    }

    StyledText unresolvedLine(ModifierEffects effects, RolledEffect effect) {
        return effect.description(effects, null);
    }

    private StyledText explicitLine(ModifierEffects effects, RolledEffect effect, ModifierLine line, boolean withRollRanges,
                                    ScaleContext context) {
        if (line == null) {
            return unresolvedLine(effects, effect);
        }
        if (line.listsEveryValueFor(context)) {
            return effect.scaledValuesDescription(effects, line);
        }
        RolledEffect shownEffect = new RolledEffect(effect.effectName(), line.valueFor(effect.magnitude(), context));
        return shownEffect.description(effects, withRollRanges ? line.rangeFor(context) : null);
    }
}
