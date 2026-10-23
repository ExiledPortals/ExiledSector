package exiledsector.ui.decoration;

import exiledlib.ProjectFiles;
import exiledsector.skills.npc.NpcFactionVolumes;
import exiledsector.skills.tags.SkillTags;
import org.json.CDL;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeFleetFactionsTest {

    @AfterEach
    void tearDown() {
        TreeFleetFactions.clear();
        NpcFactionVolumes.clear();
    }

    @Test
    void fleetsRoamTheVanillaVolumesAndEveryVolumeGivenAFleetFaction() throws Exception {
        TreeFleetFactions.register(new JSONArray("[{\"faction\": \"threat\", \"region\": \"threat\"},"
                + "{\"faction\": \"kesteven\", \"region\": \"kesteven\"}]"));

        List<String> regions = TreeFleetFactions.regions();

        assertTrue(regions.containsAll(SkillTags.FACTION_VOLUMES));
        assertTrue(regions.contains("threat"));
        assertTrue(regions.contains("kesteven"));
        assertEquals(SkillTags.FACTION_VOLUMES.size() + 2, regions.size());
    }

    @Test
    void aVolumesFleetsComeFromBothItsNpcFactionsAndItsFleetOnlyFactions() throws Exception {
        NpcFactionVolumes.register(new JSONArray("[{\"faction\": \"pirates\", \"region\": \"pirate\"}]"));
        TreeFleetFactions.register(new JSONArray("[{\"faction\": \"scavengers\", \"region\": \"pirate\"},"
                + "{\"faction\": \"pirates\", \"region\": \"pirate\"}]"));

        assertEquals(List.of("pirates", "scavengers"), TreeFleetFactions.factionsIn("pirate"));
    }

    @Test
    void rowsPointingAtTheCoreOrUnknownRegionsAreIgnored() throws Exception {
        TreeFleetFactions.register(new JSONArray("[{\"faction\": \"everyone\", \"region\": \"core\"},"
                + "{\"faction\": \"typo\", \"region\": \"thret\"}]"));

        assertEquals(SkillTags.FACTION_VOLUMES.size(), TreeFleetFactions.regions().size());
        assertEquals(List.of(), TreeFleetFactions.factionsIn("core"));
    }

    @Test
    void theShippedCsvGivesThreatAndTheModVolumesFleets() throws Exception {
        String csv = Files.readString(ProjectFiles.projectRoot().resolve(TreeFleetFactions.DATA_PATH));
        TreeFleetFactions.register(CDL.toJSONArray(csv.replace("\r\n", "\n")));

        assertEquals(List.of("threat"), TreeFleetFactions.factionsIn("threat"));
        assertEquals(List.of(), TreeFleetFactions.factionsIn("exotech"));
        assertTrue(TreeFleetFactions.regions().containsAll(List.of("enigma", "kesteven", "abyss", "nightcross", "stargazer", "tania")));
    }
}
