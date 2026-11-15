package exiledsector.charts.phasing;

import com.fs.starfarer.api.Global;
import exiledsector.ModCsv;
import exiledsector.data.CsvRows;
import exiledsector.data.WeightedPick;
import exiledsector.i18n.SpecLocalisation;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public final class PhasedPlanetTraits {

    public static final String PHASE_DISPLACED_CONDITION_ID = "exiledSector_phase_displaced";
    public static final String PATH = "data/config/exiledSector/phased_planet_traits.csv";
    static final String ID_COLUMN = "id";
    static final int FIRST_MIDDLE_TIER = 6;
    static final int FIRST_HIGH_TIER = 11;
    private static final Logger LOG = Logger.getLogger(PhasedPlanetTraits.class);

    private static List<PhasedPlanetTrait> traits = List.of();

    private PhasedPlanetTraits() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, PATH, LOG, rows -> register(rows, conditionId -> Global.getSettings().getMarketConditionSpec(conditionId) != null));
    }

    static List<String> register(JSONArray rows, Predicate<String> knownCondition) throws JSONException {
        CsvRows csv = new CsvRows(PATH);
        List<PhasedPlanetTrait> parsed = new ArrayList<>();
        CsvRows.forEachIdRow(rows, ID_COLUMN, (id, row) -> {
            String conditionId = ModCsv.text(row, "condition");
            if (conditionId.isEmpty() || !knownCondition.test(conditionId)) {
                csv.skipRow(id, "condition " + conditionId + " is not a market condition");
                return;
            }
            parsed.add(new PhasedPlanetTrait(id, conditionId, Math.max(0f, csv.number(row, "weight", 1f, id)),
                    csv.weights(row, "regions", id, null), csv.weights(row, "anomalies", id, null),
                    new HashSet<>(CsvRows.entries(ModCsv.text(row, "excludes")))));
        });
        use(parsed);
        csv.report(LOG);
        LOG.info("[ExiledSector] Phased planets can roll " + parsed.size() + " trait(s)");
        return csv.problems();
    }

    public static void localise() {
        SpecLocalisation.marketCondition(PHASE_DISPLACED_CONDITION_ID);
        for (PhasedPlanetTrait trait : traits) {
            SpecLocalisation.marketCondition(trait.conditionId());
        }
    }

    public static List<PhasedPlanetTrait> all() {
        return traits;
    }

    static void use(List<PhasedPlanetTrait> loaded) {
        traits = loaded == null ? List.of() : List.copyOf(loaded);
    }

    static int fewestFor(int tier) {
        if (tier >= FIRST_HIGH_TIER) {
            return 2;
        }
        return tier >= FIRST_MIDDLE_TIER ? 1 : 0;
    }

    static int countFor(int tier, Random random) {
        return fewestFor(tier) + random.nextInt(2);
    }

    public static List<String> roll(int tier, String regionId, Collection<String> anomalyIds, Random random) {
        List<PhasedPlanetTrait> picked = new ArrayList<>();
        int wanted = countFor(tier, random);
        while (picked.size() < wanted) {
            List<PhasedPlanetTrait> candidates = traits.stream()
                    .filter(trait -> !picked.contains(trait) && picked.stream().noneMatch(trait::conflictsWith))
                    .filter(trait -> trait.weightFor(regionId, anomalyIds) > 0f)
                    .toList();
            if (candidates.isEmpty()) {
                break;
            }
            picked.add(WeightedPick.pick(candidates, trait -> trait.weightFor(regionId, anomalyIds), random));
        }
        return picked.stream().map(PhasedPlanetTrait::conditionId).toList();
    }
}
