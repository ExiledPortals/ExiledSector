package exiledsector.charts.tree;

import exiledsector.ProjectFiles;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasDormantFleetsTest {

    private static final int MIN_SHIPS = 5;
    private static final int MAX_SHIPS = 10;

    @Test
    void everyDerelictFleetHasFiveToTenShips() throws Exception {
        List<AtlasBody> fleets = AtlasBodyReader.parse(ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(AtlasBodyReader.BODIES_PATH)))
                .values().stream().filter(body -> body.art().kind() == AtlasBodyArt.Kind.DERELICTS).toList();

        assertFalse(fleets.isEmpty());
        for (AtlasBody fleet : fleets) {
            int ships = fleet.art().hullIds().size();
            assertTrue(ships >= MIN_SHIPS && ships <= MAX_SHIPS, fleet.nodeId() + " has " + ships);
        }
    }
}
