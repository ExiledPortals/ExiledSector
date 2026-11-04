package exiledsector.charts.atlas;

import exiledsector.charts.ChartTables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AtlasFingerprintTest {

    private static final long SEED = 0x41544c4153L;
    private static final int RADIUS = 36;
    private static final String FINGERPRINT = "cf37357d97667717c9c94ef0864346bd8e8dfe70e5d4ad25b5a3d2166f168812";

    @AfterEach
    void tearDown() {
        AtlasData.resetForTests();
        ChartTables.resetAnomalies();
    }

    @Test
    void theShippedAtlasBuildsTheSameMapFromTheSameSeed() throws Exception {
        ChartTables.loadShipped();
        ChartTables.loadFeatures();
        ChartTables.loadAnomalies();
        AtlasData.registerLayouts(ChartTables.csvRows(AtlasData.LAYOUTS_PATH));
        AtlasData.registerRegions(ChartTables.csvRows(AtlasData.REGIONS_PATH), AtlasData.factionIds());
        AtlasData.registerAnomalyChances(ChartTables.csvRows(AtlasData.ANOMALY_CHANCES_PATH));
        AtlasData.registerSpecials(ChartTables.csvRows(AtlasData.SPECIALS_PATH));
        AtlasRules rules = AtlasData.rules();
        Set<String> flags = new HashSet<>();
        rules.regions().forEach(region -> flags.add(region.unlock()));
        rules.specials().forEach(special -> flags.add(special.unlock()));
        AtlasGenerator generator = new AtlasGenerator(SEED, rules, new AtlasUnlocks(modId -> true, flags), AtlasMemory.NONE);

        StringBuilder map = new StringBuilder();
        for (AtlasCell cell : AtlasCell.ORIGIN.around(RADIUS)) {
            map.append(generator.node(cell)).append(generator.factionAt(cell)).append(generator.biomeOf(cell))
                    .append(generator.infestationSource(cell)).append('\n');
            float x = generator.displayX(cell);
            float y = generator.displayY(cell);
            for (int step = 0; step < 4; step++) {
                map.append(generator.cloudAt(x + step * 0.25f, y + step * 0.125f) ? '#' : '.');
            }
            map.append('\n');
        }
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(map.toString().getBytes(StandardCharsets.UTF_8));

        assertEquals(FINGERPRINT, HexFormat.of().formatHex(digest));
    }
}
