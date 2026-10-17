package exiledsector.compat;

import assortment_of_things.exotech.ExoData;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import exiledsector.skills.unlock.SkillTypeUnlockStatus;
import exiledsector.skills.unlock.UnlockCondition;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RandomAssortmentExoshipTest {

    private static final String RAT_EXO_DATA_MEMORY_KEY = "$rat_exo_data";
    private static final SkillType BEACON = new SkillType.Builder("exotech_beacon", "Hyper-Nav Beacon", "", SkillTier.WORMHOLE)
            .unlockConditions(List.of(UnlockCondition.ratPlayerExoship()))
            .build();

    private MockedStatic<Global> globalMock;
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
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);

        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean(anyString(), anyString())).thenReturn(null);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        globalMock.when(Global::getSector).thenReturn(sector);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        lunaSettingsMock.close();
        RandomAssortmentCompat.clearCachedLookups();
    }

    @Test
    void theBeaconStaysAnUnidentifiedSensorGhostUntilThePlayerHasTheirOwnExoship() {
        assertFalse(RandomAssortmentCompat.exoshipGateOpen());
        assertTrue(SkillTypeUnlockStatus.isLocked(BEACON, null));
        assertTrue(SkillTypeUnlockStatus.isHidden(BEACON, null));
    }

    @Test
    void finishingTheRecoveryUnlocksTheBeacon() {
        exoData.setRecoveredExoship(true);

        assertTrue(RandomAssortmentCompat.exoshipGateOpen());
        assertFalse(SkillTypeUnlockStatus.isLocked(BEACON, null));
    }

    @Test
    void owningAnExoshipEntityAlsoUnlocksTheBeacon() {
        exoData.setPlayerExoship(mock(SectorEntityToken.class));

        assertTrue(RandomAssortmentCompat.exoshipGateOpen());
        assertFalse(SkillTypeUnlockStatus.isLocked(BEACON, null));
    }

    @Test
    void aSaveWhereRatHasNotStartedItsExotechDataKeepsTheBeaconLocked() {
        when(sectorMemory.get(RAT_EXO_DATA_MEMORY_KEY)).thenReturn(null);

        assertTrue(SkillTypeUnlockStatus.isLocked(BEACON, null));
    }

    @Test
    void withoutRatInstalledTheBeaconIsNeverGated() {
        when(modManager.isModEnabled(RandomAssortmentCompat.MOD_ID)).thenReturn(false);

        assertFalse(SkillTypeUnlockStatus.isLocked(BEACON, null));
    }

    @Test
    void ifRatChangesItsExotechDataTheBeaconUnlocksAndTheCompatCheckSaysWhy() {
        when(settings.getScriptClassLoader()).thenReturn(new ClassLoader(null) {
        });

        assertTrue(RandomAssortmentCompat.exoshipGateOpen());
        assertFalse(SkillTypeUnlockStatus.isLocked(BEACON, null));
        RandomAssortmentCompat.clearCachedLookups();
        assertTrue(RandomAssortmentCompat.missingFeatures().stream().anyMatch(feature -> feature.contains("ExoData")));
    }

    @Test
    void theGameStateOverrideAlsoUnlocksTheBeacon() {
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", "exiledSector_disableMemoryFlagUnlock")).thenReturn(true);

        assertFalse(SkillTypeUnlockStatus.isLocked(BEACON, null));
    }
}
