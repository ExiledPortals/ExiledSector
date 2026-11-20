package exiledsector.charts.tree;

import exiledsector.ProjectFiles;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasDepotArtTest {

    private static final String DEPOT_COLOUR = "#f0dc6e";
    private static final int GUARDS = 4;

    private static Collection<AtlasBody> depotBodies() throws Exception {
        return AtlasBodyReader.parse(ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(AtlasBodyReader.BODIES_PATH))).values().stream()
                .filter(body -> DEPOT_COLOUR.equalsIgnoreCase(body.colour())).toList();
    }

    @Test
    void everyDepotSmallNodeIsAPatrolOfOneOrTwoShips() throws Exception {
        List<AtlasBody> patrols = depotBodies().stream().filter(body -> body.wheel() != null).toList();

        assertFalse(patrols.isEmpty());
        for (AtlasBody patrol : patrols) {
            assertEquals(AtlasBodyArt.Kind.PATROL, patrol.art().kind(), patrol.nodeId());
            int ships = patrol.art().hullIds().size();
            assertTrue(ships >= 1 && ships <= 2, patrol.nodeId() + " has " + ships);
        }
    }

    @Test
    void everyDepotNotableIsAStationWithFourGuardsCirclingIt() throws Exception {
        List<AtlasBody> notables = depotBodies().stream().filter(body -> body.wheel() == null && !body.highway()).toList();

        assertFalse(notables.isEmpty());
        for (AtlasBody notable : notables) {
            AtlasBodyArt art = notable.art();
            if (art.kind() == AtlasBodyArt.Kind.STATION) {
                assertEquals(1 + GUARDS, art.hullIds().size(), notable.nodeId());
            } else {
                assertEquals(AtlasBodyArt.Kind.OUTPOST, art.kind(), notable.nodeId());
                assertEquals(GUARDS, art.hullIds().size(), notable.nodeId());
                assertFalse(art.spritePaths().isEmpty(), notable.nodeId());
            }
        }
    }
}
