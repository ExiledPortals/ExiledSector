package exiledsector.ui.node;

import exiledsector.skills.SkillTier;
import exiledsector.ui.SkillTreeSounds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TreeAllocationSoundTest {

    @Test
    void biggerNodesPlayABiggerAllocationSound() {
        assertEquals(SkillTreeSounds.NodeSize.SMALL, TreeAllocationSession.allocationSound(SkillTier.SMALL));
        assertEquals(SkillTreeSounds.NodeSize.SMALL, TreeAllocationSession.allocationSound(SkillTier.SOCKET));
        assertEquals(SkillTreeSounds.NodeSize.MEDIUM, TreeAllocationSession.allocationSound(SkillTier.NOTABLE));
        assertEquals(SkillTreeSounds.NodeSize.MEDIUM, TreeAllocationSession.allocationSound(SkillTier.WORMHOLE));
        assertEquals(SkillTreeSounds.NodeSize.LARGE, TreeAllocationSession.allocationSound(SkillTier.KEYSTONE));
        assertEquals(SkillTreeSounds.NodeSize.LARGE, TreeAllocationSession.allocationSound(SkillTier.ROOT));
    }
}
