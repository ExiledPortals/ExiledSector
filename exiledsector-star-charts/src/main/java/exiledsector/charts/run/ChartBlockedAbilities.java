package exiledsector.charts.run;

import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import exiledsector.ModCsv;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ChartBlockedAbilities {

    static final String PATH = "data/config/exiledSector/chart_blocked_abilities.csv";
    static final String ID_COLUMN = "ability_id";
    static final List<String> ALWAYS_BLOCKED = List.of(Abilities.TRANSVERSE_JUMP);
    private static final Logger LOG = Logger.getLogger(ChartBlockedAbilities.class);
    private static final Set<String> REPORTED_FAILURES = new HashSet<>();
    private static volatile List<String> abilityIds = ALWAYS_BLOCKED;

    private ChartBlockedAbilities() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, PATH, LOG, rows -> {
            Set<String> loaded = new LinkedHashSet<>(ALWAYS_BLOCKED);
            if (rows != null) {
                ModCsv.forEach(rows, (index, row) -> {
                    String abilityId = ModCsv.text(row, ID_COLUMN);
                    if (!abilityId.isEmpty()) {
                        loaded.add(abilityId);
                    }
                });
            }
            abilityIds = List.copyOf(loaded);
            LOG.info("[ExiledSector] Star Chart systems block " + abilityIds.size() + " escape abilities: " + abilityIds);
        });
    }

    public static List<String> abilityIds() {
        return abilityIds;
    }

    static void reportBlockFailure(String abilityId, Throwable cause) {
        if (REPORTED_FAILURES.add(abilityId)) {
            LOG.warn("[ExiledSector] Could not block the ability " + abilityId + " inside a Star Chart system; the other abilities stay blocked", cause);
        }
    }

    static void resetForTests() {
        abilityIds = ALWAYS_BLOCKED;
        REPORTED_FAILURES.clear();
    }
}
