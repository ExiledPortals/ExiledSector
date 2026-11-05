package exiledsector.ui;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import exiledsector.items.HullUpgradesSetting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HullItemsLevelNoticeTest {

    @AfterEach
    void tearDown() {
        HullUpgradesSetting.setEnabledForTests(HullUpgradesSetting.DEFAULT_ENABLED);
        HullUpgradesSetting.setUnlockLevelForTests(HullUpgradesSetting.DEFAULT_UNLOCK_LEVEL);
    }

    @Test
    void showsTheConfiguredLevelBelowIt() {
        HullUpgradesSetting.setUnlockLevelForTests(12);

        HullItemsLevelNotice notice = HullItemsLevelNotice.forSector(sectorAtLevel(11));

        assertNotNull(notice);
        assertEquals(2, notice.paragraphs().size());
        assertTrue(notice.paragraphs().get(0).contains("12"), notice.paragraphs().get(0));
        assertTrue(notice.paragraphs().get(1).contains("LunaLib"), notice.paragraphs().get(1));
        assertFalse(notice.paragraphs().get(0).startsWith("By default"), notice.paragraphs().get(0));
    }

    @Test
    void saysByDefaultOnlyAtTheDefaultLevel() {
        HullItemsLevelNotice notice = HullItemsLevelNotice.forSector(sectorAtLevel(HullUpgradesSetting.DEFAULT_UNLOCK_LEVEL - 1));

        assertNotNull(notice);
        assertTrue(notice.paragraphs().get(0).startsWith("By default"), notice.paragraphs().get(0));
    }

    @Test
    void hiddenOnceThePlayerReachesTheLevel() {
        HullUpgradesSetting.setUnlockLevelForTests(12);

        assertNull(HullItemsLevelNotice.forSector(sectorAtLevel(12)));
    }

    private static SectorAPI sectorAtLevel(int level) {
        SectorAPI sector = mock(SectorAPI.class);
        MutableCharacterStatsAPI playerStats = mock(MutableCharacterStatsAPI.class);
        when(sector.getPlayerStats()).thenReturn(playerStats);
        when(playerStats.getLevel()).thenReturn(level);
        return sector;
    }
}
