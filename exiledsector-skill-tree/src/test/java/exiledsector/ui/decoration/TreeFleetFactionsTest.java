package exiledsector.ui.decoration;

import exiledsector.ProjectFiles;
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
        TreeFleetFactions.register(new JSONArray("[{\"faction\": \"threat\", \"volume\": \"threat\"},"
                + "{\"faction\": \"kesteven\", \"volume\": \"kesteven\"}]"));

        List<String> volumes = TreeFleetFactions.volumes();

        assertTrue(volumes.containsAll(SkillTags.FACTION_VOLUMES));
        assertTrue(volumes.contains("threat"));
        assertTrue(volumes.contains("kesteven"));
        assertEquals(SkillTags.FACTION_VOLUMES.size() + 2, volumes.size());
    }

    @Test
    void aVolumesFleetsComeFromBothItsNpcFactionsAndItsFleetOnlyFactions() throws Exception {
        NpcFactionVolumes.register(new JSONArray("[{\"faction\": \"pirates\", \"volume\": \"pirate\"}]"));
        TreeFleetFactions.register(new JSONArray("[{\"faction\": \"scavengers\", \"volume\": \"pirate\"},"
                + "{\"faction\": \"pirates\", \"volume\": \"pirate\"}]"));

        assertEquals(List.of("pirates", "scavengers"), TreeFleetFactions.factionsIn("pirate"));
    }

    @Test
    void rowsPointingAtTheCoreOrUnknownVolumesAreIgnored() throws Exception {
        TreeFleetFactions.register(new JSONArray("[{\"faction\": \"everyone\", \"volume\": \"core\"},"
                + "{\"faction\": \"typo\", \"volume\": \"thret\"}]"));

        assertEquals(SkillTags.FACTION_VOLUMES.size(), TreeFleetFactions.volumes().size());
        assertEquals(List.of(), TreeFleetFactions.factionsIn("core"));
    }

    @Test
    void theShippedCsvGivesThreatAndTheModVolumesFleets() throws Exception {
        String csv = Files.readString(ProjectFiles.projectRoot().resolve(TreeFleetFactions.DATA_PATH));
        TreeFleetFactions.register(CDL.toJSONArray(csv.replace("\r\n", "\n")));

        assertEquals(List.of("threat"), TreeFleetFactions.factionsIn("threat"));
        assertEquals(List.of(), TreeFleetFactions.factionsIn("exotech"));
        assertTrue(TreeFleetFactions.volumes().containsAll(List.of("enigma", "kesteven", "abyss", "nightcross", "stargazer", "tania")));
    }
}
