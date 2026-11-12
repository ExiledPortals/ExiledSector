package exiledsector.charts.officer;

import exiledsector.ExiledSector;
import exiledsector.ModCsv;
import exiledsector.data.CsvRows;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public final class OfficerRanks {

    static final String PATH = ExiledSector.configPath("atlas_officer_ranks.csv");
    static final String ID_COLUMN = "id";
    static final List<OfficerRank> DEFAULTS = List.of(
            new OfficerRank("lieutenant", 10, 0, 1, 16),
            new OfficerRank("commander", 20, 10, 1, 12),
            new OfficerRank("captain", 30, 20, 2, 8),
            new OfficerRank("commodore", 45, 35, 2, 6),
            new OfficerRank("admiral", 60, 50, 3, 3));
    private static final Logger LOG = Logger.getLogger(OfficerRanks.class);
    private static volatile List<OfficerRank> ranks = DEFAULTS;

    private OfficerRanks() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, PATH, LOG, OfficerRanks::register);
    }

    static List<String> register(JSONArray rows) throws JSONException {
        List<OfficerRank> loaded = new ArrayList<>();
        CsvRows csv = new CsvRows(PATH);
        CsvRows.forEachIdRow(rows, ID_COLUMN, (rankId, row) -> {
            if (loaded.stream().anyMatch(rank -> rank.id().equals(rankId))) {
                csv.skipRow(rankId, "a rank with this id is already listed");
                return;
            }
            loaded.add(new OfficerRank(rankId, csv.integer(row, "boss_size", 0, rankId), csv.integer(row, "fleet_strength", 0, rankId),
                    Math.max(1, csv.integer(row, "actions", 1, rankId)), Math.max(0, csv.integer(row, "places", 0, rankId))));
        });
        csv.report(LOG);
        ranks = loaded.isEmpty() ? DEFAULTS : List.copyOf(loaded);
        return csv.problems();
    }

    public static List<OfficerRank> all() {
        return ranks;
    }

    public static OfficerRank lowest() {
        return ranks.get(0);
    }

    public static OfficerRank rank(String rankId) {
        for (OfficerRank rank : ranks) {
            if (rank.id().equals(rankId)) {
                return rank;
            }
        }
        return lowest();
    }

    public static int order(String rankId) {
        for (int index = 0; index < ranks.size(); index++) {
            if (ranks.get(index).id().equals(rankId)) {
                return index;
            }
        }
        return 0;
    }

    public static void useForTests(List<OfficerRank> testRanks) {
        ranks = testRanks.isEmpty() ? DEFAULTS : List.copyOf(testRanks);
    }
}
