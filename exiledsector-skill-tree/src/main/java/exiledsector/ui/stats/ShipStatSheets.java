package exiledsector.ui.stats;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.i18n.Translation;
import exiledsector.skills.DamageTakenCaps;
import exiledsector.skills.DiminishedStat;
import exiledsector.skills.EffectContribution;
import exiledsector.skills.EffectTotals;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.StatMode;
import exiledsector.skills.skilleffect.render.FallbackSupport;
import exiledsector.skills.skilleffect.shield.ShieldSkillEffect;
import exiledsector.skills.skilleffect.weapon.ScopedWeaponEffect;
import exiledsector.effects.ResolvedTree;
import exiledsector.skills.AllocatedSkillEffects;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillTreeBonusSummary;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.progression.ShipOpBudget;
import exiledsector.ui.stats.ShipStatSheet.Breakdown;
import exiledsector.ui.stats.ShipStatSheet.Cell;
import exiledsector.ui.stats.ShipStatSheet.Combine;
import exiledsector.ui.stats.ShipStatSheet.Detail;
import exiledsector.ui.stats.ShipStatSheet.EffectRow;
import exiledsector.ui.stats.ShipStatSheet.Grid;
import exiledsector.ui.stats.ShipStatSheet.GridRow;
import exiledsector.ui.stats.ShipStatSheet.Section;
import exiledsector.ui.stats.ShipStatSheet.SourceLine;
import exiledsector.ui.stats.ShipStatSheet.Trend;
import exiledsector.ui.stats.ShipStatSheet.ValueRow;
import exiledsector.ui.stats.StatRows.Shape;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ShipStatSheets {

    public record Input(MutableShipStatsAPI stats, ShipHullSpecAPI hullSpec, FleetMemberAPI member, ShipSkillData skillData,
                        ResolvedTree resolvedTree, ShipOpBudget budget, Detail detail) {
    }

    private static final String MARKER = "*";

    private final Input input;
    private final StatRows.Context context;
    private final StatSources sources;
    private final EffectTotals totals;
    private final Set<SkillEffect> coveredEffects = new HashSet<>();
    private final Map<StatGroup, List<ValueRow>> valueRows = new EnumMap<>(StatGroup.class);
    private final Map<StatGroup, List<Grid>> gridsByGroup = new EnumMap<>(StatGroup.class);
    private final Map<StatGroup, List<EffectRow>> effectRows = new EnumMap<>(StatGroup.class);

    private ShipStatSheets(Input input) {
        this.input = input;
        this.context = new StatRows.Context(input.stats(), input.hullSpec(), input.member());
        this.sources = new StatSources(input.resolvedTree(), input.skillData());
        this.totals = input.resolvedTree() == null ? null : input.resolvedTree().totals();
    }

    public static ShipStatSheet build(Input input) {
        if (input.stats() == null || input.hullSpec() == null) {
            return new ShipStatSheet(List.of());
        }
        return new ShipStatSheets(input).sheet();
    }

    private ShipStatSheet sheet() {
        addProgression();
        ShieldAPI.ShieldType shieldType = displayShieldType();
        if (shieldType != ShieldAPI.ShieldType.NONE && shieldType != ShieldAPI.ShieldType.PHASE) {
            addShieldRows(shieldType);
        }
        Set<StatGroup> shownGroups = shownStatGroups(shieldType);
        for (StatRows.RowSpec spec : StatRows.ROWS) {
            if (shownGroups.contains(spec.group())) {
                addRow(spec);
            }
        }
        for (StatRows.GridSpec spec : StatRows.GRIDS) {
            if (shownGroups.contains(spec.group())) {
                addGrid(spec);
            }
        }
        addEffectRows();
        List<Section> sections = new ArrayList<>();
        for (StatGroup group : StatGroup.values()) {
            Section section = new Section(group, List.copyOf(valueRows.getOrDefault(group, List.of())),
                    List.copyOf(gridsByGroup.getOrDefault(group, List.of())), List.copyOf(effectRows.getOrDefault(group, List.of())));
            if (!section.isEmpty()) {
                sections.add(section);
            }
        }
        return new ShipStatSheet(List.copyOf(sections));
    }

    private void addProgression() {
        ShipSkillData skillData = input.skillData();
        ShipOpBudget budget = input.budget();
        if (skillData == null || budget == null) {
            return;
        }
        List<ValueRow> rows = valueRows.computeIfAbsent(StatGroup.PROGRESSION, group -> new ArrayList<>());
        rows.add(textRow("ui.stats.level", Translation.msg("ui.stats.levelValue").arg("level", skillData.getLevel())
                .arg("xp", Math.round(skillData.getXp())).text()));
        rows.add(textRow("ui.stats.nodes", skillData.usedNodeSlots() + "/" + ShipLevelConfig.maxAllocatedNodes()));
        rows.add(textRow("ui.stats.ordnancePoints", budget.usedOp + "/" + budget.totalOp));
        if (skillData.getBankedFreeAllocations() > 0) {
            rows.add(textRow("ui.stats.freeAllocationsBanked", String.valueOf(skillData.getBankedFreeAllocations())));
        }
    }

    private ShieldAPI.ShieldType displayShieldType() {
        ShipHullSpecAPI hullSpec = input.hullSpec();
        List<SkillEffect> allocatedEffects = input.skillData() == null ? List.of()
                : AllocatedSkillEffects.forData(input.skillData(), hullSpec.getHullSize());
        ShieldAPI.ShieldType shieldType = ShieldSkillEffect.resolveDisplayShieldType(hullSpec.getShieldType(), allocatedEffects);
        return shieldType == null ? ShieldAPI.ShieldType.NONE : shieldType;
    }

    private Set<StatGroup> shownStatGroups(ShieldAPI.ShieldType shieldType) {
        Set<StatGroup> groups = new LinkedHashSet<>(List.of(StatGroup.values()));
        if (shieldType == ShieldAPI.ShieldType.NONE || shieldType == ShieldAPI.ShieldType.PHASE) {
            groups.remove(StatGroup.SHIELDS);
        }
        if (input.hullSpec().getShieldType() != ShieldAPI.ShieldType.PHASE) {
            groups.remove(StatGroup.PHASE);
        }
        MutableStat bays = input.stats().getNumFighterBays();
        if (input.hullSpec().getFighterBays() <= 0 && (bays == null || bays.getModifiedValue() <= 0f)) {
            groups.remove(StatGroup.FIGHTERS);
        }
        return groups;
    }

    private void addShieldRows(ShieldAPI.ShieldType shieldType) {
        MutableShipStatsAPI stats = input.stats();
        ShipHullSpecAPI hullSpec = input.hullSpec();
        if (input.detail() != Detail.CHANGED) {
            valueRows.computeIfAbsent(StatGroup.SHIELDS, group -> new ArrayList<>())
                    .add(textRow("ui.stats.shieldType", Translation.text("shieldType." + shieldType.name())));
        }
        ShipHullSpecAPI.ShieldSpecAPI shieldSpec = hullSpec.getShieldType() == ShieldAPI.ShieldType.NONE ? null
                : FallbackSupport.getOrFallback(hullSpec::getShieldSpec, null, Logger.getLogger(ShipStatSheets.class), "Failed to read shield spec");
        float arc = shieldSpec == null ? ShieldSkillEffect.MAKESHIFT_SHIELD_ARC : shieldSpec.getArc();
        float efficiency = shieldSpec == null ? ShieldSkillEffect.MAKESHIFT_SHIELD_EFFICIENCY : hullSpec.getBaseShieldFluxPerDamageAbsorbed();
        float upkeep = shieldSpec == null ? ShieldSkillEffect.MAKESHIFT_SHIELD_UPKEEP : shieldSpec.getUpkeepCost();
        if (stats.getShieldArcBonus() != null) {
            addReadingRow(StatGroup.SHIELDS, "ui.stats.shieldArc", StatReading.of(stats.getShieldArcBonus(), arc), Shape.AMOUNT, false, true);
        }
        if (stats.getShieldAbsorptionMult() != null && stats.getShieldDamageTakenMult() != null) {
            addReadingRow(StatGroup.SHIELDS, "ui.stats.shieldFluxPerDamage",
                    StatReading.scaled(efficiency, List.of(stats.getShieldAbsorptionMult(), stats.getShieldDamageTakenMult())), Shape.AMOUNT, true, true);
        }
        if (stats.getShieldUpkeepMult() != null) {
            addReadingRow(StatGroup.SHIELDS, "ui.stats.shieldUpkeep", StatReading.scaled(upkeep, List.of(stats.getShieldUpkeepMult())),
                    Shape.AMOUNT, true, true);
        }
    }

    private void addRow(StatRows.RowSpec spec) {
        StatReading reading = spec.reader().apply(context);
        if (reading != null) {
            addReadingRow(spec.group(), spec.labelKey(), reading, spec.shape(), spec.lowerIsBetter(), spec.core());
        }
    }

    private void addReadingRow(StatGroup group, String labelKey, StatReading reading, Shape shape, boolean lowerIsBetter, boolean core) {
        boolean changed = reading.changed();
        boolean shown = switch (input.detail()) {
            case FULL -> true;
            case STANDARD -> changed || core;
            case CHANGED -> counts(reading);
        };
        if (!shown) {
            return;
        }
        String label = Translation.text(labelKey);
        Breakdown breakdown = breakdown(label, reading, shape);
        Trend trend = changed ? trend(reading, shape, lowerIsBetter) : core ? Trend.NEUTRAL : Trend.UNCHANGED;
        valueRows.computeIfAbsent(group, key -> new ArrayList<>()).add(new ValueRow(label, valueText(reading, shape),
                changed ? changeText(reading, shape) : "", trend, breakdown.notes().isEmpty() ? "" : MARKER, breakdown));
    }

    private void addGrid(StatRows.GridSpec spec) {
        List<String> columns = new ArrayList<>();
        spec.columnKeys().forEach(key -> columns.add(Translation.text(key)));
        List<List<StatReading>> readingRows = new ArrayList<>();
        for (StatRows.GridRowSpec rowSpec : spec.rows()) {
            List<StatReading> readings = new ArrayList<>();
            rowSpec.cells().forEach(cell -> readings.add(cell == null ? null : cell.apply(context)));
            readingRows.add(readings);
        }
        List<Integer> shownColumns = new ArrayList<>();
        for (int column = 0; column < columns.size(); column++) {
            if (!spec.optionalColumns().contains(column) || columnCounts(readingRows, column)) {
                shownColumns.add(column);
            }
        }
        List<GridRow> rows = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < spec.rows().size(); rowIndex++) {
            StatRows.GridRowSpec rowSpec = spec.rows().get(rowIndex);
            List<StatReading> readings = readingRows.get(rowIndex);
            boolean rowChanged = readings.stream().anyMatch(reading -> reading != null && counts(reading));
            boolean shown = rowChanged || input.detail() == Detail.FULL && readings.stream().anyMatch(Objects::nonNull);
            if (!shown) {
                continue;
            }
            String label = Translation.text(rowSpec.labelKey());
            List<Cell> cells = new ArrayList<>();
            for (int column : shownColumns) {
                cells.add(cell(label, columns.get(column), readings.get(column), rowSpec.lowerIsBetter()));
            }
            rows.add(new GridRow(label, List.copyOf(cells)));
        }
        if (!rows.isEmpty()) {
            List<String> shownColumnNames = shownColumns.stream().map(columns::get).toList();
            gridsByGroup.computeIfAbsent(spec.group(), key -> new ArrayList<>()).add(new Grid(shownColumnNames, List.copyOf(rows)));
        }
    }

    private boolean counts(StatReading reading) {
        return reading.changed() && (input.detail() != Detail.CHANGED || touchedByTree(reading));
    }

    private boolean touchedByTree(StatReading reading) {
        return reading.modifiers().stream().anyMatch(modifier -> sources.applied(modifier.modId()) != null);
    }

    private boolean columnCounts(List<List<StatReading>> readingRows, int column) {
        for (List<StatReading> readings : readingRows) {
            StatReading reading = readings.get(column);
            if (reading != null && counts(reading)) {
                return true;
            }
        }
        return false;
    }

    private Cell cell(String rowLabel, String column, StatReading reading, boolean lowerIsBetter) {
        if (reading == null) {
            return new Cell("", Trend.UNCHANGED, "", null);
        }
        String title = Translation.msg("ui.stats.gridCell").arg("stat", rowLabel).arg("column", column).text();
        Breakdown breakdown = breakdown(title, reading, Shape.CHANGE);
        Trend trend = reading.changed() ? trend(reading, Shape.CHANGE, lowerIsBetter) : Trend.UNCHANGED;
        return new Cell(valueText(reading, Shape.CHANGE), trend, breakdown.notes().isEmpty() ? "" : MARKER, breakdown);
    }

    private Breakdown breakdown(String title, StatReading reading, Shape shape) {
        String summary = shape == Shape.CHANGE ? null : Translation.msg("ui.stats.breakdown.base").arg("value", baseText(reading, shape)).text();
        boolean points = shape == Shape.POINTS;
        List<SourceLine> lines = new ArrayList<>();
        Map<Integer, List<StatReading.Modifier>> scaledPercents = new LinkedHashMap<>();
        Set<String> nodeIds = new LinkedHashSet<>();
        Set<String> notes = new LinkedHashSet<>();
        for (StatReading.Modifier modifier : reading.modifiers()) {
            if (modifier.kind() == StatReading.Kind.PERCENT && modifier.pool() > 0) {
                scaledPercents.computeIfAbsent(modifier.pool(), pool -> new ArrayList<>()).add(modifier);
            } else {
                lines.add(sourceLine(modifier, combine(modifier.kind()), modifierAmount(modifier, points), nodeIds, notes));
            }
        }
        scaledPercents.values().forEach(modifiers -> lines.add(scaledPercentLine(modifiers, nodeIds, notes)));
        String result = lines.isEmpty() ? null : resultText(reading, shape);
        return new Breakdown(title, summary, List.copyOf(lines), result, List.copyOf(notes), Set.copyOf(nodeIds));
    }

    private SourceLine sourceLine(StatReading.Modifier modifier, Combine combine, String amount, Set<String> nodeIds, Set<String> notes) {
        ResolvedTree.AppliedModifier applied = sources.applied(modifier.modId());
        if (applied == null) {
            return new SourceLine(StatSources.outsideSourceName(modifier.modId(), modifier.desc()), combine, amount, false);
        }
        if (applied.effect() != null) {
            coveredEffects.add(applied.effect());
            if (applied.reductionCap()) {
                coveredEffects.addAll(capContributors(applied.effect()));
                notes.add(cappedNote());
            }
            if (diminished(applied.effect())) {
                notes.add(Translation.text("ui.stats.note.diminished"));
            }
        }
        nodeIds.addAll(StatSources.nodeIds(applied.sources()));
        return new SourceLine(sourceNames(applied.sources()), combine, amount, true);
    }

    private SourceLine scaledPercentLine(List<StatReading.Modifier> modifiers, Set<String> nodeIds, Set<String> notes) {
        float totalPercent = 0f;
        Set<String> names = new LinkedHashSet<>();
        boolean fromTree = false;
        for (StatReading.Modifier modifier : modifiers) {
            SourceLine part = sourceLine(modifier, Combine.MULTI, "", nodeIds, notes);
            totalPercent += modifier.value();
            names.add(part.source());
            fromTree |= part.fromTree();
        }
        return new SourceLine(String.join(Translation.text("ui.stats.sourceSeparator"), names), Combine.MULTI, StatFormat.signedPercent(totalPercent),
                fromTree);
    }

    private static Combine combine(StatReading.Kind kind) {
        return switch (kind) {
            case FLAT -> Combine.FLAT;
            case PERCENT -> Combine.PERCENT;
            case MULT -> Combine.MULTI;
        };
    }

    private Set<SkillEffect> capContributors(SkillEffect cappedEffect) {
        if (totals == null) {
            return Set.of();
        }
        for (DamageTakenCaps.Cap cap : totals.reachedCaps()) {
            if (cap.cappedEffect() == cappedEffect) {
                return cap.contributingEffects();
            }
        }
        return Set.of();
    }

    private boolean diminished(SkillEffect effect) {
        DiminishedStat stat = DiminishedStat.of(effect);
        return stat != null && totals != null && totals.diminishedStats().contains(stat);
    }

    private boolean capped(SkillEffect effect) {
        if (totals == null) {
            return false;
        }
        for (DamageTakenCaps.Cap cap : totals.reachedCaps()) {
            if (cap.cappedEffect() == effect || cap.contributingEffects().contains(effect)) {
                return true;
            }
        }
        return false;
    }

    private String sourceNames(List<EffectContribution> contributions) {
        Set<String> names = new LinkedHashSet<>();
        contributions.forEach(contribution -> names.add(sources.originName(contribution.origin())));
        return String.join(Translation.text("ui.stats.sourceSeparator"), names);
    }

    private void addEffectRows() {
        if (totals == null) {
            return;
        }
        for (SkillTreeBonusSummary.EffectLine line : SkillTreeBonusSummary.lines(totals.groups(), effect -> !coveredEffects.contains(effect))) {
            Breakdown breakdown = effectBreakdown(line);
            effectRows.computeIfAbsent(StatGroup.of(line.effect()), group -> new ArrayList<>())
                    .add(new EffectRow(line.line(), breakdown.notes().isEmpty() ? "" : MARKER, breakdown));
        }
    }

    private Breakdown effectBreakdown(SkillTreeBonusSummary.EffectLine line) {
        Map<SkillEffect, Map<String, Float>> amountsBySourceByEffect = new LinkedHashMap<>();
        amountsBySourceByEffect.put(line.effect(), new LinkedHashMap<>());
        Set<String> nodeIds = new LinkedHashSet<>();
        for (EffectContribution contribution : totals.contributions()) {
            if (!Objects.equals(contribution.gate(), line.gate()) || !Objects.equals(contribution.temporarySeconds(), line.temporarySeconds())) {
                continue;
            }
            Map<SkillEffect, Float> amountsByEffect = new LinkedHashMap<>();
            for (SkillTypeEffect effect : contribution.effects()) {
                SkillEffect breakdownEffect = breakdownEffect(line.effect(), effect.effect());
                if (breakdownEffect != null) {
                    amountsByEffect.merge(breakdownEffect, effect.magnitude(), Float::sum);
                }
            }
            if (!amountsByEffect.isEmpty()) {
                String sourceName = sources.originName(contribution.origin());
                amountsByEffect.forEach((effect, amount) -> amountsBySourceByEffect
                        .computeIfAbsent(effect, shownEffect -> new LinkedHashMap<>()).merge(sourceName, amount, Float::sum));
                nodeIds.addAll(StatSources.nodeIds(contribution.origin()));
            }
        }
        List<SourceLine> sourceLines = new ArrayList<>();
        amountsBySourceByEffect.forEach((effect, amountsBySource) ->
                sourceLines.addAll(effectLines(effect, line.temporarySeconds(), amountsBySource)));
        List<String> notes = new ArrayList<>();
        boolean permanent = line.gate() == null && line.temporarySeconds() == null;
        if (permanent && diminished(line.effect())) {
            notes.add(Translation.text("ui.stats.note.diminished"));
        }
        if (permanent && capped(line.effect())) {
            notes.add(cappedNote());
        }
        return new Breakdown(line.line().plain(), null, List.copyOf(sourceLines), null, List.copyOf(notes), Set.copyOf(nodeIds));
    }

    private static SkillEffect breakdownEffect(SkillEffect lineEffect, SkillEffect contributed) {
        if (matches(lineEffect, contributed)) {
            return lineEffect;
        }
        return contributed.refines().contains(lineEffect) ? contributed : null;
    }

    private static List<SourceLine> effectLines(SkillEffect effect, Float temporarySeconds, Map<String, Float> amountsBySource) {
        Combine combine = effectCombine(effect);
        if (combine == Combine.MULTI && temporarySeconds == null && amountsBySource.size() > 1) {
            float totalAmount = 0f;
            for (float amount : amountsBySource.values()) {
                totalAmount += amount;
            }
            String names = String.join(Translation.text("ui.stats.sourceSeparator"), amountsBySource.keySet());
            return List.of(new SourceLine(names, combine, StatFormat.signedPercent(totalAmount), true));
        }
        List<SourceLine> lines = new ArrayList<>();
        amountsBySource.forEach((source, amount) -> lines.add(new SourceLine(source, combine, effectAmount(combine, amount), true)));
        return List.copyOf(lines);
    }

    private static Combine effectCombine(SkillEffect effect) {
        if (effect.isMultiplicative()) {
            return Combine.MULTI;
        }
        StatMode mode = effect.statMode();
        if (mode == StatMode.PERCENT) {
            return Combine.PERCENT;
        }
        return mode == StatMode.FLAT ? Combine.FLAT : Combine.OTHER;
    }

    private static boolean matches(SkillEffect lineEffect, SkillEffect contributed) {
        if (lineEffect == contributed) {
            return true;
        }
        if (!(lineEffect instanceof ScopedWeaponEffect line) || !(contributed instanceof ScopedWeaponEffect scoped)) {
            return false;
        }
        if (line.family() != scoped.family() || line.mode() != scoped.mode()) {
            return false;
        }
        for (var scope = scoped.scope(); scope != null; scope = scope.parent()) {
            if (scope == line.scope()) {
                return true;
            }
        }
        return false;
    }

    private static String effectAmount(Combine combine, float amount) {
        return combine == Combine.PERCENT || combine == Combine.MULTI ? StatFormat.signedPercent(amount) : StatFormat.signedNumber(amount);
    }

    private static String modifierAmount(StatReading.Modifier modifier, boolean points) {
        return switch (modifier.kind()) {
            case FLAT -> points ? StatFormat.signedPercent(modifier.value() * 100f) : StatFormat.signedNumber(modifier.value());
            case PERCENT -> StatFormat.signedPercent(modifier.value());
            case MULT -> StatFormat.signedPercent((modifier.value() - 1f) * 100f);
        };
    }

    private static String resultText(StatReading reading, Shape shape) {
        String key = shape == Shape.CHANGE ? "ui.stats.breakdown.netChange" : "ui.stats.breakdown.final";
        return Translation.msg(key).arg("value", valueText(reading, shape)).text();
    }

    private static String valueText(StatReading reading, Shape shape) {
        return switch (shape) {
            case AMOUNT -> StatFormat.number(reading.value());
            case CHANGE -> StatFormat.change(reading.flat(), reading.factor(), false);
            case POINTS -> StatFormat.percent(reading.value() * 100f);
        };
    }

    private static String baseText(StatReading reading, Shape shape) {
        return shape == Shape.POINTS ? StatFormat.percent(reading.base() * 100f) : StatFormat.number(reading.base());
    }

    private static String changeText(StatReading reading, Shape shape) {
        return switch (shape) {
            case AMOUNT -> reading.base() == 0f ? StatFormat.signedNumber(reading.value())
                    : StatFormat.signedPercent((reading.value() / reading.base() - 1f) * 100f);
            case CHANGE -> "";
            case POINTS -> StatFormat.signedPercent((reading.value() - reading.base()) * 100f);
        };
    }

    private static Trend trend(StatReading reading, Shape shape, boolean lowerIsBetter) {
        float direction = shape == Shape.CHANGE
                ? (reading.flat() != 0f ? reading.flat() : reading.factor() - 1f)
                : reading.value() - reading.base();
        if (direction == 0f) {
            return Trend.NEUTRAL;
        }
        return (direction > 0f) != lowerIsBetter ? Trend.BETTER : Trend.WORSE;
    }

    private static String cappedNote() {
        return Translation.msg("ui.stats.note.capped").arg("percent", Math.round(DamageTakenCaps.MAX_REDUCTION_PERCENT)).text();
    }

    private static ValueRow textRow(String labelKey, String value) {
        return new ValueRow(Translation.text(labelKey), value, "", Trend.NEUTRAL, "", null);
    }
}
