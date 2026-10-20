package exiledsector.skills.npc;

import exiledsector.skills.AnchorLinks;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;

final class NpcTestAnchors {

    private static final String PREFIX = "test_anchor:";

    private NpcTestAnchors() {
    }

    static String idFor(String volume) {
        return volume == null ? null : PREFIX + volume;
    }

    static void install() {
        AnchorLinks.install(new AnchorLinks.ExitLookup() {
            @Override
            public String exitNodeId(String socketedItemId) {
                if (socketedItemId == null || !socketedItemId.startsWith(PREFIX)) {
                    return null;
                }
                String volume = socketedItemId.substring(PREFIX.length());
                for (SkillNode node : SkillTree.topology().sortedById()) {
                    if (node.getType().getTier() == SkillTier.WORMHOLE_EXIT && volume.equals(node.getRegion())) {
                        return node.getId();
                    }
                }
                return null;
            }

            @Override
            public boolean canOpen(String socketedItemId) {
                return exitNodeId(socketedItemId) != null;
            }

            @Override
            public int revision() {
                return 0;
            }
        });
    }

    static void uninstall() {
        AnchorLinks.install(null);
    }
}
