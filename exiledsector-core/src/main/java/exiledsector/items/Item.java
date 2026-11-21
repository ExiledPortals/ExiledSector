package exiledsector.items;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.crafting.CorruptionOutcome;
import exiledsector.items.modifier.AffixLayout;
import exiledsector.items.modifier.ItemImplicits;
import exiledsector.items.modifier.ItemModifiers;
import exiledsector.items.modifier.ItemRarity;
import exiledsector.items.modifier.ModifierSources;
import exiledsector.items.modifier.PoolEntry;
import exiledsector.items.modifier.PrototypeImplicits;
import exiledsector.items.modifier.RolledLine;
import exiledsector.items.modifier.RolledModifier;
import exiledsector.items.modifier.SlottedLine;
import exiledsector.items.modifier.SlottedRoll;
import exiledsector.items.name.FrozenName;
import exiledsector.items.name.ItemName;
import exiledsector.items.name.ItemNames;
import exiledsector.modifiers.ModifierEffect;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.tags.ThemeTagLine;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierCategory;
import exiledsector.modifiers.ModifierEffects;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.modifiers.ScaleContext;
import exiledsector.modifiers.TierRange;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public abstract class Item {

    public static final Color CORRUPTED_FRAME_COLOR = new Color(255, 75, 70);

    private final String id;
    private String definitionId;
    private long seed;
    private List<RolledModifier> explicitModifiers;
    private List<RolledModifier> implicitModifiers;
    private FrozenName frozenName;
    private boolean mirrored;
    private CorruptionOutcome corruption;
    private transient boolean explicitIdsCurrent;
    private transient boolean implicitIdsCurrent;

    public Item(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers) {
        this.id = id;
        this.definitionId = ItemIdAliases.currentId(definitionId);
        this.seed = seed;
        this.explicitModifiers = new ArrayList<>(explicitModifiers);
    }

    public Item(Item source) {
        this.id = source.id;
        this.definitionId = source.definitionId;
        this.seed = source.seed;
        this.explicitModifiers = source.explicitModifiers;
        this.implicitModifiers = source.implicitModifiers;
        this.frozenName = source.frozenName;
        this.mirrored = source.mirrored;
        this.corruption = source.corruption;
    }

    public static Item create(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers) {
        ItemDefinition definition = ItemDefinitions.get(definitionId);
        ItemType type = definition == null ? null : definition.type();
        ItemTypeSupport support = ItemTypes.of(type);
        return support == null ? new UnresolvedItem(id, definitionId, seed, explicitModifiers)
                : support.create(id, definitionId, seed, explicitModifiers);
    }

    public static Item create(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers, ItemTraits traits) {
        Item item = create(id, definitionId, seed, explicitModifiers);
        if (traits != null) {
            item.applyTraits(traits);
        }
        return item;
    }

    public void applyTraits(ItemTraits traits) {
        if (traits.corruption() != null) {
            corruption = traits.corruption();
        }
        if (traits.mirrored()) {
            mirrored = true;
        }
        if (traits.implicits() != null) {
            replaceImplicits(traits.implicits());
        }
    }

    public void copyTraitsFrom(Item source) {
    }

    public ItemTraits traits() {
        List<RolledModifier> currentImplicits = implicits();
        return new ItemTraits(tier(), 0, corruption, currentImplicits.isEmpty() ? null : currentImplicits, mirrored);
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
        ItemTypeSupport support = ItemTypes.of(definedType);
        return support == null ? new UnresolvedItem(this) : support.convert(this);
    }

    public ItemKind kind() {
        ItemDefinition definition = definition();
        return definition == null ? null : definition.kind();
    }

    public String kindName() {
        ItemDefinition definition = definition();
        if (definition == null) {
            return Translation.text("socketable.unknown");
        }
        return definition.kind() == null ? definition.noun() : definition.kind().displayName();
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

    protected void reseed(long newSeed) {
        seed = newSeed;
    }

    public List<RolledModifier> explicits() {
        ItemDefinition definition = definition();
        if (!explicitIdsCurrent && definition != null) {
            explicitModifiers = new ArrayList<>(definition.withCurrentUniquePool(definition.withCurrentIds(explicitModifiers), seed));
            explicitIdsCurrent = true;
        }
        return Collections.unmodifiableList(explicitModifiers);
    }

    public List<RolledLine> effects() {
        return RolledModifier.lines(explicits());
    }

    public List<RolledModifier> implicits() {
        if (implicitModifiers == null) {
            List<RolledModifier> rolled = ItemImplicits.roll(kind(), seed);
            if (ItemImplicits.isLoaded() && kind() != null) {
                implicitModifiers = new ArrayList<>(rolled);
                implicitIdsCurrent = !kind().implicits().isEmpty();
            }
            return rolled;
        }
        if (!implicitIdsCurrent && corruption != CorruptionOutcome.IMPLICIT_REPLACED && ItemImplicits.isLoaded() && kind() != null && !kind().implicits().isEmpty()) {
            implicitModifiers = new ArrayList<>(ItemImplicits.withCurrentIds(kind(), implicitModifiers, seed));
            implicitIdsCurrent = true;
        }
        return Collections.unmodifiableList(implicitModifiers);
    }

    public List<RolledModifier> storedImplicits() {
        return implicitModifiers;
    }

    public ItemDefinition definition() {
        return ItemDefinitions.get(definitionId);
    }

    public ItemName displayName() {
        ItemDefinition definition = definition();
        return ItemNames.render(definition, kind(), explicits(), frozenName(definition));
    }

    public FrozenName frozenName() {
        return frozenName(definition());
    }

    private FrozenName frozenName(ItemDefinition definition) {
        if (definition != null && frozenName == null) {
            frozenName = ItemNames.freeze(definition, seed, explicits());
        }
        return frozenName;
    }

    public void freezeName() {
        frozenName(definition());
    }

    public void freezeName(FrozenName presetName) {
        if (presetName != null) {
            frozenName = presetName;
        }
    }

    public void replaceExplicits(List<RolledModifier> replacementModifiers) {
        explicitModifiers = new ArrayList<>(replacementModifiers);
    }

    public void addExplicit(RolledModifier added) {
        ItemRarity rarityBefore = rarity();
        List<RolledModifier> modifiers = new ArrayList<>(explicits());
        AffixLayout.insert(modifiers, added);
        replaceExplicits(modifiers);
        if (rarity() != rarityBefore) {
            refreezeName();
        }
    }

    public void removeExplicit(RolledModifier removed) {
        List<RolledModifier> remaining = new ArrayList<>(explicits());
        remaining.remove(removed);
        replaceExplicits(remaining);
        refreezeName();
    }

    public void swapExplicit(RolledModifier removed, RolledModifier added) {
        List<RolledModifier> modifiers = new ArrayList<>(explicits());
        modifiers.remove(removed);
        AffixLayout.insert(modifiers, added);
        replaceExplicits(modifiers);
        refreezeName();
    }

    public void replaceImplicits(List<RolledModifier> replacementImplicits) {
        implicitModifiers = new ArrayList<>(replacementImplicits);
    }

    public Item mirrorAs(String copyId) {
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

    public void corrupt(CorruptionOutcome outcome) {
        corruption = outcome;
    }

    public boolean hasImmutableModifier() {
        return explicits().stream().anyMatch(RolledModifier::immutable);
    }

    public boolean isWarped() {
        return explicits().stream().anyMatch(RolledModifier::warped);
    }

    private boolean carriesWarped(String modifierId) {
        return explicits().stream().anyMatch(rolled -> rolled.warped() && rolled.modifierId().equals(modifierId));
    }

    public void refreezeName() {
        ItemDefinition definition = definition();
        frozenName = definition == null ? null : ItemNames.freeze(definition, seed, explicits());
    }

    public String name() {
        return displayName().title();
    }

    public Color frameColor() {
        return corrupted() ? CORRUPTED_FRAME_COLOR : rarity().color();
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

    public StyledText limitLine() {
        return null;
    }

    public List<StyledText> kindLines() {
        return List.of();
    }

    public List<StyledText> headerLines() {
        List<StyledText> lines = new ArrayList<>();
        StyledText flavour = flavourLine();
        if (flavour != null) {
            lines.add(flavour);
        }
        lines.addAll(detailLines());
        return lines;
    }

    public StyledText flavourLine() {
        ItemDefinition definition = definition();
        return definition == null ? null : definition.descriptionText();
    }

    public List<StyledText> detailLines() {
        return conditionLines();
    }

    public List<StyledText> conditionLines() {
        List<StyledText> lines = new ArrayList<>();
        if (mirrored) {
            lines.add(Translation.styled("socketable.tooltip.mirrored"));
        }
        if (corruption != null) {
            lines.add(Translation.styled("socketable.tooltip.corrupted"));
        }
        if (isWarped()) {
            lines.add(Translation.styled("socketable.tooltip.warped"));
        }
        return lines;
    }

    public void keepWhenUnresolved(UnresolvedItem unresolved) {
    }

    public void writeTooltipSections(TooltipMakerAPI tooltip, HullSize hullSize, boolean showRollRanges) {
    }

    public boolean hasTunableTrait() {
        return false;
    }

    public boolean tuneTrait(Random random) {
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
        for (RolledLine line : rolledModifier.lines()) {
            rolls.add(new SlottedRoll(rolledModifier.slot(), line, modifier == null ? null : modifier.line(line.effectName())));
        }
    }

    public Modifier implicitModifier(String modifierId) {
        ItemKind type = kind();
        if (type == null) {
            return null;
        }
        Modifier ownImplicit = Modifier.byId(type.implicits(), modifierId);
        if (ownImplicit != null) {
            return ownImplicit;
        }
        Modifier prototypeImplicit = PrototypeImplicits.byId(modifierId);
        return prototypeImplicit != null ? prototypeImplicit : ItemImplicits.anyById(modifierId);
    }

    public ModifierLine implicitLine(String effectName) {
        ModifierLine ownLine = ItemImplicits.line(kind(), effectName);
        return ownLine != null ? ownLine : PrototypeImplicits.line(definitionId, effectName);
    }

    public List<Modifier> implicitCorruptionPool() {
        return ItemImplicits.corruptionPool();
    }

    public boolean implicitCanVary(RolledModifier implicit) {
        for (RolledLine line : implicit.lines()) {
            ModifierLine lineDefinition = implicitLine(line.effectName());
            if (lineDefinition != null && lineDefinition.canVary()) {
                return true;
            }
        }
        return false;
    }

    public RolledModifier rerolledImplicit(RolledModifier implicit, Random random) {
        return implicit.rerolled(this::implicitLine, random);
    }

    public Modifier explicitModifier(String modifierId) {
        ItemDefinition definition = definition();
        PoolEntry entry = definition == null ? null : definition.poolEntryFor(modifierId);
        if (entry != null) {
            return entry.modifier();
        }
        boolean resolvableForType = definition != null && (ModifierSources.isReserved(modifierId) || carriesWarped(modifierId))
                && ItemModifiers.itemTypes(modifierId).contains(definition.type());
        return resolvableForType ? ItemModifiers.get(modifierId) : null;
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
        return inlineTexts(implicitRows(withRollRanges));
    }

    public List<SlottedLine> implicitRows(boolean withRollRanges) {
        return rows(implicitSlottedLines(withRollRanges), withRollRanges);
    }

    public List<SlottedLine> implicitSlottedLines(boolean withRollRanges) {
        List<SlottedLine> lines = new ArrayList<>();
        ModifierEffects effects = modifierEffects();
        ScaleContext context = scaleContext(null);
        for (RolledModifier implicit : implicits()) {
            addModifierLines(lines, implicit, implicitModifier(implicit.modifierId()), effects, withRollRanges, context);
        }
        return lines;
    }

    public List<StyledText> implicitLines(RolledModifier implicit) {
        List<SlottedLine> lines = new ArrayList<>();
        addModifierLines(lines, implicit, implicitModifier(implicit.modifierId()), modifierEffects(), false, scaleContext(null));
        return texts(lines, false);
    }

    public List<StyledText> effectLines() {
        return effectLines(false);
    }

    public List<StyledText> effectLines(boolean withRollRanges) {
        return effectLines(withRollRanges, null);
    }

    public List<StyledText> effectLines(boolean withRollRanges, HullSize hullSize) {
        return texts(slottedEffectLines(withRollRanges, hullSize), withRollRanges);
    }

    public List<SlottedLine> slottedEffectRows(boolean withRollRanges, HullSize hullSize) {
        return rows(slottedEffectLines(withRollRanges, hullSize), withRollRanges);
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

    public List<StyledText> explicitLines(RolledModifier explicitModifier) {
        List<SlottedLine> lines = new ArrayList<>();
        addModifierLines(lines, explicitModifier, explicitModifier(explicitModifier.modifierId()), modifierEffects(), false, scaleContext(null));
        return texts(lines, false);
    }

    private void addModifierLines(List<SlottedLine> lines, RolledModifier rolledModifier, Modifier modifier, ModifierEffects effects,
                                  boolean withRollRanges, ScaleContext context) {
        List<SlottedLine> described = new ArrayList<>();
        for (RolledLine effect : rolledModifier.lines()) {
            StyledText description = explicitLine(effects, effect, modifier == null ? null : modifier.line(effect.effectName()),
                    withRollRanges, context);
            if (description != null) {
                StyledText shown = shownAs(rolledModifier, description);
                ModifierEffect resolvedEffect = effect.effect(effects);
                described.add(resolvedEffect == null ? new SlottedLine(rolledModifier.slot(), shown)
                        : new SlottedLine(rolledModifier.slot(), shown, resolvedEffect.detailTitle(), resolvedEffect.detailLines()));
            }
        }
        if (described.isEmpty()) {
            return;
        }
        SlottedLine caption = withRollRanges ? caption(rolledModifier, modifier) : null;
        if (caption != null) {
            lines.add(caption);
        }
        lines.addAll(described);
    }

    private static StyledText shownAs(RolledModifier rolledModifier, StyledText description) {
        StyledText shown = rolledModifier.overcharged() ? description.withFirstSpanAs(Style.OVERCHARGED) : description;
        return rolledModifier.immutable() ? shown.withUnstyledAs(Style.IMMUTABLE) : shown;
    }

    private static SlottedLine caption(RolledModifier rolledModifier, Modifier modifier) {
        List<StyledText> labels = new ArrayList<>(4);
        String slotKey = captionSlotKey(rolledModifier.slot());
        if (slotKey != null) {
            labels.add(Translation.styled(slotKey));
        }
        String originHeaderKey = ModifierSources.headerKey(rolledModifier.modifierId());
        if (originHeaderKey != null) {
            labels.add(Translation.styled(originHeaderKey));
        }
        if (rolledModifier.warped()) {
            labels.add(Translation.styled("socketable.tooltip.warped"));
        }
        if (rolledModifier.overcharged()) {
            labels.add(Translation.styled("socketable.tooltip.overcharged"));
        }
        StyledText tags = modifier == null ? null : ThemeTagLine.of(modifier.tags());
        if (labels.isEmpty() && tags == null) {
            return null;
        }
        StyledText labelText = labels.isEmpty() ? StyledText.EMPTY : Translation.list(labels).withUnstyledAs(Style.MUTED);
        return SlottedLine.caption(rolledModifier.slot(), labelText, tags);
    }

    public static String captionSlotKey(ModifierSlot slot) {
        if (slot == ModifierSlot.PREFIX) {
            return "item.tooltip.caption.prefix";
        }
        return slot == ModifierSlot.SUFFIX ? "item.tooltip.caption.suffix" : null;
    }

    private static List<StyledText> texts(List<SlottedLine> lines, boolean withDetails) {
        return inlineTexts(rows(lines, withDetails));
    }

    private static List<StyledText> inlineTexts(List<SlottedLine> rows) {
        return rows.stream().map(SlottedLine::inlineText).toList();
    }

    private static List<SlottedLine> rows(List<SlottedLine> lines, boolean withDetails) {
        List<SlottedLine> rows = new ArrayList<>(lines.size());
        for (SlottedLine line : lines) {
            rows.add(line);
            if (withDetails) {
                line.details().forEach(detail -> rows.add(new SlottedLine(null, Translation.msg("socketable.tooltip.detailLine").arg("line", detail).styled())));
            }
        }
        return rows;
    }

    public StyledText unresolvedLine(ModifierEffects effects, RolledLine effect) {
        return effect.description(effects, null);
    }

    private StyledText explicitLine(ModifierEffects effects, RolledLine effect, ModifierLine line, boolean withRollRanges,
                                    ScaleContext context) {
        if (line == null) {
            return unresolvedLine(effects, effect);
        }
        if (line.listsEveryValueFor(context)) {
            return effect.scaledValuesDescription(effects, line);
        }
        RolledLine shownEffect = new RolledLine(effect.effectName(), line.valueFor(effect.magnitude(), context));
        return shownEffect.description(effects, withRollRanges ? line.rangeFor(context) : null);
    }
}
