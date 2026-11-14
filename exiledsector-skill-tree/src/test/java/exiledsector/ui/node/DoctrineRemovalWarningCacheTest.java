package exiledsector.ui.node;

import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class DoctrineRemovalWarningCacheTest {

    private final List<String> builtForSocketIds = new ArrayList<>();
    private final DoctrineRemovalWarningCache cache = new DoctrineRemovalWarningCache(socket -> {
        builtForSocketIds.add(socket.getId());
        return socket.getId().equals("quiet_1") ? null : "loses " + socket.getId();
    });

    private static NodeAllocator.Snapshot snapshot() {
        return new NodeAllocator.Snapshot(mock(ShipSkillData.class), null, 0, Set.of(), Set.of(), null, null);
    }

    private static SkillNode socket(String id) {
        SkillType type = new SkillType.Builder(id, id, "", SkillTier.SOCKET).build();
        return new SkillNode(id + "_1", type, List.of(), 0f, 0f);
    }

    @Test
    void hoveringTheSameSocketOnTheSameTreeBuildsTheWarningOnce() {
        NodeAllocator.Snapshot tree = snapshot();
        SkillNode doctrineSocket = socket("doctrine");

        assertEquals("loses doctrine_1", cache.warningFor(doctrineSocket, tree));
        assertEquals("loses doctrine_1", cache.warningFor(doctrineSocket, tree));
        assertEquals(List.of("doctrine_1"), builtForSocketIds);
    }

    @Test
    void noWarningIsRememberedToo() {
        NodeAllocator.Snapshot tree = snapshot();
        SkillNode quietSocket = socket("quiet");

        assertNull(cache.warningFor(quietSocket, tree));
        assertNull(cache.warningFor(quietSocket, tree));
        assertEquals(List.of("quiet_1"), builtForSocketIds);
    }

    @Test
    void anotherSocketOrAChangedTreeRebuildsTheWarning() {
        NodeAllocator.Snapshot tree = snapshot();
        SkillNode doctrineSocket = socket("doctrine");
        SkillNode otherSocket = socket("other");

        cache.warningFor(doctrineSocket, tree);
        assertEquals("loses other_1", cache.warningFor(otherSocket, tree));
        cache.warningFor(otherSocket, snapshot());

        assertEquals(List.of("doctrine_1", "other_1", "other_1"), builtForSocketIds);
    }

    @Test
    void noSocketHasNoWarning() {
        assertNull(cache.warningFor(null, snapshot()));
        assertEquals(List.of(), builtForSocketIds);
    }
}
