package exiledsector.charts;

import exiledsector.data.CsvRows;
import exiledlib.ModCsv;
import exiledsector.items.StarChartImplicits;
import exiledsector.items.WeightedPick;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

public final class ChartFactions {

    static final String PATH = "data/config/exiledSector/chart_factions.csv";
    static final String FACTION_COLUMN = "faction";
    private static final float FACTION_LINE_MAGNITUDE = 1f;
    private static final Logger LOG = Logger.getLogger(ChartFactions.class);
    private static volatile List<ChartFaction> factions = List.of();

    private ChartFactions() {
    }

    public static void load() {
        ModCsv.load(FACTION_COLUMN, PATH, LOG, ChartFactions::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, ChartFaction> loaded = new LinkedHashMap<>();
        CsvRows csv = new CsvRows(PATH);
        if (rows != null) {
            ModCsv.forEach(rows, (index, row) -> {
                String factionId = ModCsv.text(row, FACTION_COLUMN);
                if (factionId.isEmpty()) {
                    return;
                }
                loaded.put(factionId, new ChartFaction(factionId, csv.number(row, "weight", 1f, factionId)));
            });
        }
        csv.report(LOG);
        factions = List.copyOf(loaded.values());
        StarChartImplicits.registerSharedChoice(List.of());
    }

    static Modifier unlistedImplicit(String modifierId) {
        ChartFactionEffect effect = effectByName(modifierId);
        return effect == null ? null : implicitFor(effect.factionId());
    }

    static List<StarChartImplicits.Choice> implicitChoices(List<ChartFaction> candidates) {
        List<StarChartImplicits.Choice> choices = new ArrayList<>();
        for (ChartFaction faction : candidates) {
            if (faction.weight() > 0f) {
                choices.add(new StarChartImplicits.Choice(implicitFor(faction.factionId()), faction.weight()));
            }
        }
        return choices;
    }

    static Modifier implicitFor(String factionId) {
        String effectName = ChartFactionEffect.effectName(factionId);
        return Modifier.single(ModifierSlot.IMPLICIT, ModifierLine.range(effectName, FACTION_LINE_MAGNITUDE, FACTION_LINE_MAGNITUDE));
    }

    public static List<ChartFaction> all() {
        return factions;
    }

    static ChartFactionEffect effectByName(String effectName) {
        if (effectName == null || !effectName.startsWith(ChartFactionEffect.NAME_PREFIX)) {
            return null;
        }
        for (ChartFaction faction : factions) {
            if (ChartFactionEffect.effectName(faction.factionId()).equals(effectName)) {
                return new ChartFactionEffect(faction.factionId());
            }
        }
        String unlistedId = effectName.substring(ChartFactionEffect.NAME_PREFIX.length()).toLowerCase(Locale.ROOT);
        return unlistedId.isEmpty() ? null : new ChartFactionEffect(unlistedId);
    }

    static List<ChartFaction> available(List<ChartFaction> candidates, Predicate<String> factionLoaded) {
        List<ChartFaction> available = new ArrayList<>();
        for (ChartFaction faction : candidates) {
            if (faction.weight() > 0f && factionLoaded.test(faction.factionId())) {
                available.add(faction);
            }
        }
        return available;
    }

    static ChartFaction pick(List<ChartFaction> candidates, Predicate<String> factionLoaded, Random random) {
        List<ChartFaction> available = available(candidates, factionLoaded);
        ChartFaction picked = available.isEmpty() ? null : WeightedPick.pick(available, ChartFaction::weight, random);
        return picked != null ? picked : ChartFaction.FALLBACK;
    }

    static ChartFaction resolve(String chartFactionId, List<ChartFaction> candidates, Predicate<String> factionLoaded, Random random) {
        if (chartFactionId != null && factionLoaded.test(chartFactionId)) {
            for (ChartFaction faction : candidates) {
                if (faction.factionId().equals(chartFactionId)) {
                    return faction;
                }
            }
            return ChartFaction.unlisted(chartFactionId);
        }
        return pick(candidates, factionLoaded, random);
    }

    public static void resetForTests() {
        factions = List.of();
        StarChartImplicits.registerSharedChoice(List.of());
    }
}
