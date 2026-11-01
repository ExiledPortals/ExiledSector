package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.BattleCreationPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.ModCsv;
import exiledsector.charts.anomaly.breach.SpatialBreach;
import exiledsector.charts.anomaly.convoy.SupplyConvoy;
import exiledsector.charts.anomaly.debris.AnomalousDebris;
import exiledsector.charts.anomaly.excavation.ExcavationSites;
import exiledsector.charts.anomaly.facility.PreCollapseFacility;
import exiledsector.charts.anomaly.forge.DerelictForge;
import exiledsector.charts.anomaly.heist.ListeningPost;
import exiledsector.charts.anomaly.hypershunt.CoronalHypershunt;
import exiledsector.charts.anomaly.relay.RelayNetwork;
import exiledsector.charts.anomaly.skirmish.FactionSkirmish;
import exiledsector.charts.anomaly.station.DomainMothership;
import exiledsector.charts.anomaly.station.RemnantNexusStation;
import exiledsector.charts.anomaly.storm.StormFront;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasRunChoice;
import exiledsector.charts.atlas.AtlasRunNode;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.compat.ChartModGates;
import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.table.BuiltInTable;
import exiledsector.data.CsvRows;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class ChartAnomalies {

    public static final String PATH = "data/config/exiledSector/chart_anomalies.csv";
    static final String ID_COLUMN = "id";
    static final List<ChartAnomaly> BUILT_IN = List.of(StarAnomalies.BLACK_HOLE, StarAnomalies.NEUTRON_STAR, StarAnomalies.DENSE_NEBULA,
            AsteroidStorm.INSTANCE, MagneticStorm.INSTANCE, StellarFlares.INSTANCE, SpatialBreach.INSTANCE, FactionSkirmish.INSTANCE,
            RelayNetwork.INSTANCE, ExcavationSites.INSTANCE, SupplyConvoy.INSTANCE, AnomalousDebris.INSTANCE,
            StormFront.INSTANCE,
            DerelictForge.INSTANCE, ListeningPost.INSTANCE, PreCollapseFacility.INSTANCE, CoronalHypershunt.INSTANCE,
            RemnantNexusStation.INSTANCE, DomainMothership.INSTANCE,
            PowerfulBoss.INSTANCE);
    private static final Logger LOG = Logger.getLogger(ChartAnomalies.class);
    private static volatile List<ChartAnomaly> anomalies = BUILT_IN;
    private static volatile Map<String, Definition> definitions = Map.of();
    private static final Map<Class<?>, List<?>> ROLES = new ConcurrentHashMap<>();
    private static final Set<String> REPORTED_FAILURES = ConcurrentHashMap.newKeySet();

    public record Definition(String id, float weight, int minTier, int maxTier, String requiredMod, String icon, boolean insertable,
                             Set<String> excludes, boolean replacesStar, List<ChartGenContext.AppliedEffect> effects, Set<String> biomes) {

        public Definition {
            requiredMod = requiredMod == null ? "" : requiredMod;
            icon = icon == null ? "" : icon;
            weight = Math.max(0f, weight);
            excludes = excludes == null ? Set.of() : Set.copyOf(excludes);
            biomes = biomes == null ? Set.of() : Set.copyOf(biomes);
            insertable = insertable && !replacesStar && biomes.isEmpty();
            effects = effects == null ? List.of() : List.copyOf(effects);
        }

        public Definition(String id, float weight, int minTier, int maxTier, String requiredMod, String icon, boolean insertable,
                          Set<String> excludes, boolean replacesStar, List<ChartGenContext.AppliedEffect> effects) {
            this(id, weight, minTier, maxTier, requiredMod, icon, insertable, excludes, replacesStar, effects, Set.of());
        }

        public Definition(String id, float weight, int minTier, int maxTier, String requiredMod, String icon, boolean insertable,
                          Set<String> excludes, boolean replacesStar) {
            this(id, weight, minTier, maxTier, requiredMod, icon, insertable, excludes, replacesStar, List.of());
        }

        public boolean allowedAt(int tier) {
            return tier >= minTier && tier <= maxTier;
        }

        public boolean perBiome() {
            return !biomes.isEmpty();
        }

        public boolean keptWhenSpent() {
            return replacesStar || perBiome();
        }

        public boolean conflictsWith(Definition other) {
            return other != null && !id.equals(other.id) && (replacesStar && other.replacesStar || excludes.contains(other.id) || other.excludes.contains(id));
        }
    }

    private ChartAnomalies() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, PATH, LOG, ChartAnomalies::register);
    }

    public static List<String> register(JSONArray rows) throws JSONException {
        CsvRows csv = new CsvRows(PATH);
        Map<String, Definition> loaded = BuiltInTable.rows(rows, csv, "anomaly", id -> byId(id) != null, ChartAnomalies::parse);
        csv.report(LOG);
        definitions = Map.copyOf(loaded);
        LOG.info("[ExiledSector] Star Charts can roll " + loaded.size() + " anomaly/anomalies");
        return csv.problems();
    }

    private static Definition parse(CsvRows csv, JSONObject row, String id) {
        BuiltInTable.TierRange tiers = BuiltInTable.TierRange.parse(csv, row, id);
        ChartAnomaly anomaly = byId(id);
        boolean replacesStar = anomaly instanceof StarRule;
        boolean insertable = csv.flag(row, "insertable", false, id);
        if (insertable && replacesStar) {
            csv.problem(id, "insertable", "yes (an anomaly that replaces the star can't be inserted)");
        }
        Set<String> biomes = new LinkedHashSet<>(CsvRows.entries(ModCsv.text(row, "biomes")));
        if (insertable && !biomes.isEmpty()) {
            csv.problem(id, "insertable", "yes (an anomaly placed once per biome can't be inserted)");
        }
        Set<String> excludes = new LinkedHashSet<>();
        for (String excluded : CsvRows.entries(ModCsv.text(row, "excludes"))) {
            if (excluded.equals(id) || byId(excluded) == null) {
                csv.problem(id, "excludes", excluded);
            } else {
                excludes.add(excluded);
            }
        }
        List<ChartGenContext.AppliedEffect> effects = new ArrayList<>();
        csv.weights(row, "effects", id, null, true).forEach((name, value) -> {
            ChartEffect effect = ChartEffect.byNameOrNull(name);
            if (effect == null) {
                csv.problem(id, "effects", name + " (not a chart effect)");
            } else {
                effects.add(new ChartGenContext.AppliedEffect(effect, value));
            }
        });
        return new Definition(id, csv.number(row, "weight", 1f, id), tiers.minTier(), tiers.maxTier(), ModCsv.text(row, "requires_mod"),
                ModCsv.text(row, "icon"), insertable, excludes, replacesStar, effects, biomes);
    }

    public static ChartAnomaly byId(String id) {
        return BuiltInTable.find(anomalies, ChartAnomaly::id, id);
    }

    public static List<ChartAnomaly> all() {
        return anomalies;
    }

    public static <R extends ChartAnomaly> List<R> with(Class<R> role) {
        @SuppressWarnings("unchecked")
        List<R> cached = (List<R>) ROLES.computeIfAbsent(role, wanted -> anomalies.stream().filter(wanted::isInstance).map(wanted::cast).toList());
        return cached;
    }

    public static <R extends ChartAnomaly> List<R> inRun(ChartRun run, Class<R> role) {
        return among(run == null ? List.of() : run.anomalyIds(), role);
    }

    public static <R extends ChartAnomaly> List<R> among(Collection<String> ids, Class<R> role) {
        return with(role).stream().filter(anomaly -> ids.contains(anomaly.id())).toList();
    }

    public static <R extends ChartAnomaly> R role(String id, Class<R> role) {
        ChartAnomaly anomaly = byId(id);
        return role.isInstance(anomaly) ? role.cast(anomaly) : null;
    }

    public static InteractionDialogPlugin dialogFor(SectorEntityToken target) {
        return dialogAfter(null, target);
    }

    public static InteractionDialogPlugin dialogAfter(ChartAnomaly previous, SectorEntityToken target) {
        AnomalyRun run = target == null ? null : AnomalyRun.in(target.getContainingLocation());
        if (run == null) {
            return null;
        }
        List<RunParticipant> participants = inRun(run.run(), RunParticipant.class);
        int previousIndex = participants.indexOf(previous);
        if (previous != null && previousIndex < 0) {
            return null;
        }
        for (RunParticipant anomaly : participants.subList(previousIndex + 1, participants.size())) {
            InteractionDialogPlugin dialog = askSafely(anomaly, "dialog", () -> anomaly.dialogFor(run, target));
            if (dialog != null) {
                return dialog;
            }
        }
        return null;
    }

    public static BattleCreationPlugin battleFor(SectorEntityToken opponent) {
        for (RunParticipant anomaly : with(RunParticipant.class)) {
            BattleCreationPlugin battle = askSafely(anomaly, "battle", () -> anomaly.battleFor(opponent));
            if (battle != null) {
                return battle;
            }
        }
        return null;
    }

    public static void checkRun(SectorAPI sector, AnomalyRun run) {
        for (RunParticipant anomaly : inRun(run.run(), RunParticipant.class)) {
            runSafely(anomaly, "run check", () -> anomaly.checkRun(sector, run));
        }
    }

    public static void fleetGone(AnomalyRun run, CampaignFleetAPI fleet) {
        for (RunParticipant anomaly : inRun(run.run(), RunParticipant.class)) {
            runSafely(anomaly, "fleet check", () -> anomaly.fleetGone(run, fleet));
        }
    }

    public static List<String> intelLines(AnomalyRun run) {
        List<String> lines = new ArrayList<>();
        for (RunParticipant anomaly : inRun(run.run(), RunParticipant.class)) {
            String line = askSafely(anomaly, "intel", () -> anomaly.intelText(run));
            if (line != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    public static AtlasRunNode joinRunNode(AtlasRunNode node, AtlasGenerator generator, AtlasState state, AtlasCell cell, AtlasRunChoice choice) {
        AtlasRunNode joined = node;
        for (AtlasRule anomaly : among(node.anomalyIds(), AtlasRule.class)) {
            AtlasRunNode current = joined;
            AtlasRunNode next = askSafely(anomaly, "run join", () -> anomaly.joinRunNode(current, generator, state, cell, choice));
            joined = next == null ? current : next;
        }
        return joined;
    }

    public static void runCompleted(SectorAPI sector, ChartRun run) {
        for (AtlasRule anomaly : atlasRulesOf(run)) {
            runSafely(anomaly, "run completion", () -> anomaly.runCompleted(sector, run));
        }
    }

    public static void runClosed(SectorAPI sector, ChartRun run) {
        for (AtlasRule anomaly : atlasRulesOf(run)) {
            runSafely(anomaly, "run close", () -> anomaly.runClosed(sector, run));
        }
    }

    private static List<AtlasRule> atlasRulesOf(ChartRun run) {
        AtlasRunNode node = run.atlasNode();
        return node == null ? List.of() : among(node.anomalyIds(), AtlasRule.class);
    }

    public static void clearFrom(CampaignFleetAPI fleet) {
        for (RunParticipant anomaly : with(RunParticipant.class)) {
            runSafely(anomaly, "cleanup", () -> anomaly.clearFrom(fleet));
        }
    }

    public static List<LootShaper> holdingPlayerBattle() {
        return with(LootShaper.class).stream().filter(LootShaper::holdsPlayerBattle).toList();
    }

    private static void runSafely(ChartAnomaly anomaly, String hook, Runnable call) {
        askSafely(anomaly, hook, () -> {
            call.run();
            return null;
        });
    }

    private static <T> T askSafely(ChartAnomaly anomaly, String hook, Supplier<T> call) {
        try {
            return call.get();
        } catch (RuntimeException | LinkageError e) {
            if (REPORTED_FAILURES.add(anomaly.id() + ":" + hook)) {
                LOG.error("[ExiledSector] The " + anomaly.id() + " anomaly's " + hook + " failed; it is skipped until the next try", e);
            }
            return null;
        }
    }

    public static Collection<Definition> definitions() {
        return definitions.values();
    }

    public static Definition definition(String id) {
        return id == null ? null : definitions.get(id);
    }

    public static boolean usable(String id) {
        Definition definition = definition(id);
        return definition != null && byId(id) != null && ChartModGates.met(definition.requiredMod());
    }

    public static boolean conflicts(String first, String second) {
        Definition definition = definition(first);
        return definition != null && definition.conflictsWith(definition(second));
    }

    public static boolean accepts(List<String> accepted, String id) {
        if (!usable(id) || accepted.contains(id)) {
            return false;
        }
        for (String existing : accepted) {
            if (conflicts(existing, id)) {
                return false;
            }
        }
        return true;
    }

    public static StarRule starOf(Collection<String> ids) {
        for (String id : ids) {
            StarRule star = role(id, StarRule.class);
            if (star != null && usable(id)) {
                return star;
            }
        }
        return null;
    }

    public static <R extends ChartAnomaly> List<R> anomaliesOf(Collection<String> ids, Class<R> role) {
        return ids.stream().map(id -> role(id, role)).filter(Objects::nonNull).toList();
    }

    public static boolean guaranteeBossTierUp(Collection<String> ids) {
        return ids != null && anomaliesOf(ids, LootShaper.class).stream().anyMatch(LootShaper::guaranteesBossTierUp);
    }

    public static List<ChartGenContext.AppliedEffect> effectsOf(Collection<String> ids) {
        List<ChartGenContext.AppliedEffect> effects = new ArrayList<>();
        for (String id : ids) {
            Definition definition = definition(id);
            if (definition != null) {
                effects.addAll(definition.effects());
            }
        }
        return effects;
    }

    public static List<String> planned(ChartGenContext context) {
        if (context == null) {
            return List.of();
        }
        List<String> accepted = new ArrayList<>();
        for (String id : context.forcedAnomalies()) {
            if (accepts(accepted, id)) {
                accepted.add(id);
            }
        }
        return List.copyOf(accepted);
    }

    public static void useForTests(List<ChartAnomaly> testAnomalies, List<Definition> testDefinitions) {
        anomalies = List.copyOf(testAnomalies);
        ROLES.clear();
        Map<String, Definition> byId = new LinkedHashMap<>();
        testDefinitions.forEach(definition -> byId.put(definition.id(), definition));
        definitions = Map.copyOf(byId);
    }

    public static void useForTests(List<Definition> testDefinitions) {
        useForTests(BUILT_IN, testDefinitions);
    }

    public static void resetForTests() {
        anomalies = BUILT_IN;
        ROLES.clear();
        definitions = Map.of();
    }
}
