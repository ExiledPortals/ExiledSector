package exiledsector.skills.radius;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import exiledsector.ProjectFiles;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.npc.RealSkillData;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NodeRadiiTest {

    @AfterEach
    void tearDown() {
        NodeRadii.reset();
        RealSkillData.clear();
    }

    @Test
    void registerReadsEverySizeFromData() throws Exception {
        NodeRadii.register(new JSONObject("{ \"small\": 1, \"medium\": 2.5, \"large\": 4 }"));

        assertEquals(1f, NodeRadii.distance(RadiusSize.SMALL));
        assertEquals(2.5f, NodeRadii.distance(RadiusSize.MEDIUM));
        assertEquals(4f, NodeRadii.distance(RadiusSize.LARGE));
    }

    @Test
    void sizesThatShrinkFromSmallToLargeAreRejected() throws Exception {
        JSONObject shrinkingSizes = new JSONObject("{ \"small\": 3, \"medium\": 2, \"large\": 4 }");
        assertThrows(JSONException.class, () -> NodeRadii.register(shrinkingSizes));
    }

    @Test
    void aMissingSizeIsRejected() throws Exception {
        JSONObject missingMedium = new JSONObject("{ \"small\": 1, \"large\": 4 }");
        assertThrows(JSONException.class, () -> NodeRadii.register(missingMedium));
    }

    @Test
    void aFileThatFailsToLoadLeavesEveryRadiusEmpty() throws Exception {
        NodeRadii.register(new JSONObject("{ \"small\": 1, \"medium\": 2, \"large\": 3 }"));
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.loadJSON(NodeRadii.DATA_PATH)).thenThrow(new JSONException("broken"));
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getSettings).thenReturn(settings);
            NodeRadii.load();
        }

        for (RadiusSize size : RadiusSize.values()) {
            assertEquals(0f, NodeRadii.distance(size), size.name());
        }
    }

    @Test
    void theShippedRadiiAreNestedAndEachSizeReachesFurtherOnTheRealTree() throws Exception {
        NodeRadii.register(ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(NodeRadii.DATA_PATH)));
        RealSkillData.load();
        NodeRadiusCache cache = SkillTree.radii();

        int smallTotal = 0;
        int largeTotal = 0;
        List<String> notNested = new ArrayList<>();
        for (SkillNode node : SkillTree.getAllNodes().values()) {
            Set<String> small = cache.nodeIdsWithin(node.getId(), RadiusSize.SMALL);
            Set<String> medium = cache.nodeIdsWithin(node.getId(), RadiusSize.MEDIUM);
            Set<String> large = cache.nodeIdsWithin(node.getId(), RadiusSize.LARGE);
            if (!medium.containsAll(small) || !large.containsAll(medium)) {
                notNested.add(node.getId());
            }
            smallTotal += small.size();
            largeTotal += large.size();
        }

        assertTrue(notNested.isEmpty(), "Radii not nested around: " + notNested);
        assertTrue(smallTotal > 0);
        assertTrue(largeTotal > smallTotal);
    }

    @Test
    void noRealNodeCarriedByTheExoshipIsACentreOrInsideAnyRadius() throws Exception {
        NodeRadii.register(ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(NodeRadii.DATA_PATH)));
        RealSkillData.load();
        NodeRadiusCache cache = SkillTree.radii();

        List<String> excluded = SkillTree.getAllNodes().values().stream().filter(NodeRadiusCache::isExcluded).map(SkillNode::getId).toList();
        assertFalse(excluded.isEmpty());
        for (SkillNode node : SkillTree.getAllNodes().values()) {
            for (RadiusSize size : RadiusSize.values()) {
                Set<String> nodeIdsWithin = cache.nodeIdsWithin(node.getId(), size);
                for (String excludedId : excluded) {
                    assertFalse(nodeIdsWithin.contains(excludedId), excludedId + " inside " + node.getId());
                }
            }
        }
        for (String excludedId : excluded) {
            assertFalse(cache.isCentre(excludedId), excludedId);
        }
    }
}
