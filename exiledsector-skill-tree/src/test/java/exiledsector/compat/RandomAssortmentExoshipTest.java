package exiledsector.compat;

import assortment_of_things.exotech.ExoData;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledlib.MockSector;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemType;
import exiledsector.items.ItemUnlock;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import exiledsector.skills.unlock.SkillTypeUnlockStatus;
import exiledsector.skills.unlock.UnlockCondition;
import exiledsector.skills.unlock.UnlockConditionType;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RandomAssortmentExoshipTest {

    private static final String RAT_EXO_DATA_MEMORY_KEY = "$rat_exo_data";
    private static final SkillType EXOSHIP = new SkillType.Builder("exoship", "Exoship", "", SkillTier.WORMHOLE_EXIT)
            .unlockConditions(List.of(UnlockCondition.modGate(UnlockConditionType.RAT_PLAYER_EXOSHIP)))
            .build();
    private static final ItemDefinition BEACON_ANCHOR = new ItemDefinition("wormhole_anchor_exotech", null, "Hyper-Nav Beacon", "", 10f, false, "",
            List.of(), List.of(), ItemUnlock.RAT_PLAYER_EXOSHIP.id(), ItemType.WORMHOLE_ANCHOR, null, List.of(), ItemDefinition.NO_SOCKET_LIMIT);

    private MockSector mockSector;
    private MockedStatic<LunaSettings> lunaSettingsMock;
    private SettingsAPI settings;
    private ModManagerAPI modManager;
    private MemoryAPI sectorMemory;
    private ExoData exoData;

    @BeforeEach
    void setUp() {
        RandomAssortmentCompat.clearCachedLookups();
        settings = mock(SettingsAPI.class);
        modManager = mock(ModManagerAPI.class);
        when(settings.getModManager()).thenReturn(modManager);
        when(modManager.isModEnabled(RandomAssortmentCompat.MOD_ID)).thenReturn(true);
        when(settings.getScriptClassLoader()).thenReturn(getClass().getClassLoader());
        sectorMemory = mock(MemoryAPI.class);
        exoData = new ExoData();
        when(sectorMemory.get(RAT_EXO_DATA_MEMORY_KEY)).thenReturn(exoData);
        mockSector = new MockSector();
        when(mockSector.sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);

        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean(anyString(), anyString())).thenReturn(null);
        mockSector.global.when(Global::getSettings).thenReturn(settings);
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
        lunaSettingsMock.close();
        RandomAssortmentCompat.clearCachedLookups();
    }

    @Test
    void theExoshipStaysAnUnidentifiedSensorGhostUntilThePlayerHasTheirOwnExoship() {
        assertFalse(RandomAssortmentCompat.exoshipConfirmedRecovered());
        assertTrue(SkillTypeUnlockStatus.isLocked(EXOSHIP, null));
        assertTrue(SkillTypeUnlockStatus.isHidden(EXOSHIP, null));
    }

    @Test
    void finishingTheRecoveryUnlocksTheExoship() {
        exoData.setRecoveredExoship(true);

        assertTrue(RandomAssortmentCompat.exoshipConfirmedRecovered());
        assertFalse(SkillTypeUnlockStatus.isLocked(EXOSHIP, null));
    }

    @Test
    void owningAnExoshipEntityAlsoUnlocksTheExoship() {
        exoData.setPlayerExoship(mock(SectorEntityToken.class));

        assertTrue(RandomAssortmentCompat.exoshipConfirmedRecovered());
        assertFalse(SkillTypeUnlockStatus.isLocked(EXOSHIP, null));
    }

    @Test
    void aSaveWhereRatHasNotStartedItsExotechDataKeepsTheExoshipLocked() {
        when(sectorMemory.get(RAT_EXO_DATA_MEMORY_KEY)).thenReturn(null);

        assertTrue(SkillTypeUnlockStatus.isLocked(EXOSHIP, null));
    }

    @Test
    void withoutRatInstalledTheExoshipIsNeverGated() {
        when(modManager.isModEnabled(RandomAssortmentCompat.MOD_ID)).thenReturn(false);

        assertFalse(SkillTypeUnlockStatus.isLocked(EXOSHIP, null));
    }

    @Test
    void ifRatChangesItsExotechDataTheExoshipStaysLockedAndTheCompatCheckSaysWhy() {
        when(settings.getScriptClassLoader()).thenReturn(new ClassLoader(null) {
        });

        assertFalse(RandomAssortmentCompat.exoshipConfirmedRecovered());
        assertTrue(SkillTypeUnlockStatus.isLocked(EXOSHIP, null));
        RandomAssortmentCompat.clearCachedLookups();
        assertTrue(RandomAssortmentCompat.missingFeatures().stream().anyMatch(feature -> feature.contains("ExoData")));
    }

    @Test
    void theGameStateOverrideAlsoUnlocksTheExoship() {
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", "exiledSector_disableMemoryFlagUnlock")).thenReturn(true);

        assertFalse(SkillTypeUnlockStatus.isLocked(EXOSHIP, null));
    }

    @Test
    void theBeaconAnchorUnlockFailsClosedAndLatchesNothingWhenRatCannotBeRead() {
        when(settings.getScriptClassLoader()).thenReturn(new ClassLoader(null) {
        });
        exoData.setRecoveredExoship(true);

        assertFalse(ItemUnlock.unlockConditionMet(BEACON_ANCHOR, Global.getSector()));
        verify(sectorMemory, never()).set(anyString(), any());
        assertFalse(RandomAssortmentCompat.exoshipConfirmedRecovered());
    }

    @Test
    void theBeaconAnchorUnlocksAndLatchesOnceRatReportsTheExoship() {
        assertFalse(ItemUnlock.unlockConditionMet(BEACON_ANCHOR, Global.getSector()));
        exoData.setRecoveredExoship(true);

        assertTrue(ItemUnlock.unlockConditionMet(BEACON_ANCHOR, Global.getSector()));
        verify(sectorMemory).set("$exiledSector_socketUnlock_" + ItemUnlock.RAT_PLAYER_EXOSHIP.id(), true);
    }

    @Test
    void aOneOffFailureInsideRatKeepsTheExoshipLockedForThatCheckOnlyAndTheNextCheckReadsRatAgain() {
        when(sectorMemory.get(RAT_EXO_DATA_MEMORY_KEY)).thenThrow(new IllegalStateException("save still loading")).thenReturn(exoData);
        exoData.setRecoveredExoship(true);

        assertFalse(RandomAssortmentCompat.exoshipConfirmedRecovered());
        assertTrue(RandomAssortmentCompat.exoshipConfirmedRecovered());
    }
}
