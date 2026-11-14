package exiledsector.ui.node;

import exiledsector.skills.SkillNode;

import java.util.function.Function;

final class DoctrineRemovalWarningCache {

    private final Function<SkillNode, String> warningBuilder;
    private NodeAllocator.Snapshot builtForSnapshot;
    private String builtForSocketId;
    private String builtWarning;

    DoctrineRemovalWarningCache(Function<SkillNode, String> warningBuilder) {
        this.warningBuilder = warningBuilder;
    }

    String warningFor(SkillNode socket, NodeAllocator.Snapshot liveSnapshot) {
        if (socket == null) {
            return null;
        }
        if (liveSnapshot != builtForSnapshot || !socket.getId().equals(builtForSocketId)) {
            builtWarning = warningBuilder.apply(socket);
            builtForSnapshot = liveSnapshot;
            builtForSocketId = socket.getId();
        }
        return builtWarning;
    }
}
