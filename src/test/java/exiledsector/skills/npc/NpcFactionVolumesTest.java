package exiledsector.skills.npc;

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
        NpcFactionVolumes.register(new JSONArray("[{\"faction\": \"remnant\", \"region\": \"REDACTED\"},"
                + "{\"faction\": \"lions_guard\", \"region\": \"sindrian_dictat\"}]"));

        assertEquals("REDACTED", NpcFactionVolumes.regionFor("remnant"));
        assertEquals("sindrian_dictat", NpcFactionVolumes.regionFor("lions_guard"));
        assertNull(NpcFactionVolumes.regionFor("independent"));
        assertNull(NpcFactionVolumes.regionFor(null));
    }

    @Test
    void rowsPointingAtTheCoreModVolumesOrUnknownRegionsAreIgnored() throws Exception {
        NpcFactionVolumes.register(new JSONArray("[{\"faction\": \"kesteven_navy\", \"region\": \"kesteven\"},"
                + "{\"faction\": \"enigma_navy\", \"region\": \"enigma\"},"
                + "{\"faction\": \"everyone\", \"region\": \"core\"},"
                + "{\"faction\": \"typo\", \"region\": \"hegmony\"},"
                + "{\"faction\": \"\", \"region\": \"hegemony\"}]"));

        assertNull(NpcFactionVolumes.regionFor("kesteven_navy"));
        assertNull(NpcFactionVolumes.regionFor("enigma_navy"));
        assertNull(NpcFactionVolumes.regionFor("everyone"));
        assertNull(NpcFactionVolumes.regionFor("typo"));
    }

    @Test
    void theShippedCsvOnlyNamesVanillaFactionVolumes() throws Exception {
        String csv = java.nio.file.Files.readString(RealSkillData.projectRoot().resolve(NpcFactionVolumes.DATA_PATH));
        NpcFactionVolumes.register(org.json.CDL.toJSONArray(csv.replace("\r\n", "\n")));

        assertEquals("REDACTED", NpcFactionVolumes.regionFor("omega"));
        assertEquals("luddic", NpcFactionVolumes.regionFor("luddic_path"));
        assertEquals("persean_league", NpcFactionVolumes.regionFor("persean"));
    }
}
