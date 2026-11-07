package exiledsector.skills.npc;

import exiledsector.ProjectFiles;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NpcFactionVolumesTest {

    @AfterEach
    void tearDown() {
        NpcFactionVolumes.clear();
    }

    @Test
    void factionsMapToTheirVanillaVolumes() throws Exception {
        NpcFactionVolumes.register(new JSONArray("[{\"faction\": \"remnant\", \"volume\": \"REDACTED\"},"
                + "{\"faction\": \"lions_guard\", \"volume\": \"sindrian_dictat\"}]"));

        assertEquals("REDACTED", NpcFactionVolumes.volumeFor("remnant"));
        assertEquals("sindrian_dictat", NpcFactionVolumes.volumeFor("lions_guard"));
        assertNull(NpcFactionVolumes.volumeFor("independent"));
        assertNull(NpcFactionVolumes.volumeFor(null));
    }

    @Test
    void rowsPointingAtTheCoreModVolumesOrUnknownVolumesAreIgnored() throws Exception {
        NpcFactionVolumes.register(new JSONArray("[{\"faction\": \"kesteven_navy\", \"volume\": \"kesteven\"},"
                + "{\"faction\": \"enigma_navy\", \"volume\": \"enigma\"},"
                + "{\"faction\": \"everyone\", \"volume\": \"core\"},"
                + "{\"faction\": \"typo\", \"volume\": \"hegmony\"},"
                + "{\"faction\": \"\", \"volume\": \"hegemony\"}]"));

        assertNull(NpcFactionVolumes.volumeFor("kesteven_navy"));
        assertNull(NpcFactionVolumes.volumeFor("enigma_navy"));
        assertNull(NpcFactionVolumes.volumeFor("everyone"));
        assertNull(NpcFactionVolumes.volumeFor("typo"));
    }

    @Test
    void theShippedCsvOnlyNamesVanillaFactionVolumes() throws Exception {
        String csv = java.nio.file.Files.readString(ProjectFiles.projectRoot().resolve(NpcFactionVolumes.DATA_PATH));
        NpcFactionVolumes.register(org.json.CDL.toJSONArray(csv.replace("\r\n", "\n")));

        assertEquals("REDACTED", NpcFactionVolumes.volumeFor("remnant"));
        assertEquals("omega", NpcFactionVolumes.volumeFor("omega"));
        assertEquals("luddic", NpcFactionVolumes.volumeFor("luddic_path"));
        assertEquals("persean_league", NpcFactionVolumes.volumeFor("persean"));
    }
}
