package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundPlayerAPI;
import exiledsector.skills.SkillTier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SkillTreeSoundsTest {

    private MockedStatic<Global> globalMock;
    private SoundPlayerAPI player;

    @BeforeEach
    void setUp() {
        SkillTreeSounds.resetForTests();
        player = mock(SoundPlayerAPI.class);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSoundPlayer).thenReturn(player);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        SkillTreeSounds.resetForTests();
    }

    @Test
    void biggerNodesPlayAHigherTierOfTheVanillaSkillSound() {
        SkillTreeSounds.allocated(SkillTier.SMALL);
        SkillTreeSounds.resetForTests();
        SkillTreeSounds.allocated(SkillTier.NOTABLE);
        SkillTreeSounds.resetForTests();
        SkillTreeSounds.allocated(SkillTier.KEYSTONE);

        verify(player).playUISound(SkillTreeSounds.ALLOCATE_SMALL, 1f, 1f);
        verify(player).playUISound(SkillTreeSounds.ALLOCATE_MEDIUM, 1f, 1f);
        verify(player).playUISound(SkillTreeSounds.ALLOCATE_LARGE, 1f, 1f);
    }

    @Test
    void aBurstOfNodeChangesFromAutoAllocateOrRespecPlaysOnlyOneSound() {
        for (int i = 0; i < 20; i++) {
            SkillTreeSounds.allocated(SkillTier.SMALL);
            SkillTreeSounds.deallocated();
        }

        verify(player, times(1)).playUISound(anyString(), eq(1f), eq(1f));
    }

    @Test
    void mapStorageAndSocketSoundsAreNotThrottled() {
        SkillTreeSounds.hyperspaceOut();
        SkillTreeSounds.hyperspaceIn();
        SkillTreeSounds.wormholeJumped();
        SkillTreeSounds.panelOpened();
        SkillTreeSounds.socketed();
        SkillTreeSounds.socketed();

        verify(player).playUISound(SkillTreeSounds.HYPERSPACE_OUT, 1f, 1f);
        verify(player).playUISound(SkillTreeSounds.HYPERSPACE_IN, 1f, 1f);
        verify(player).playUISound(SkillTreeSounds.WORMHOLE_JUMP, 1f, 1f);
        verify(player).playUISound(SkillTreeSounds.PANEL_OPEN, 1f, 1f);
        verify(player, times(2)).playUISound(SkillTreeSounds.SOCKET, 1f, 1f);
    }

    @Test
    void nothingPlaysWithoutASoundPlayer() {
        globalMock.when(Global::getSoundPlayer).thenReturn(null);

        SkillTreeSounds.socketed();

        verify(player, never()).playUISound(anyString(), eq(1f), eq(1f));
    }
}
