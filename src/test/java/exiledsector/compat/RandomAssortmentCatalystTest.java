package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RandomAssortmentCatalystTest {

    private static final List<UnlockCondition> CATALYST_GATE = List.of(UnlockCondition.ratPrimordialCatalyst());

    private MockedStatic<LunaSettings> lunaSettingsMock;
    private MockedStatic<Global> globalMock;
    private ModManagerAPI modManager;
    private SectorAPI sector;
    private CustomCampaignEntityAPI catalyst;

    @BeforeEach
    void setUp() {
        RandomAssortmentCompat.clearCachedLookups();
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean(anyString(), anyString())).thenReturn(null);
        SettingsAPI settings = mock(SettingsAPI.class);
        modManager = mock(ModManagerAPI.class);
        when(settings.getModManager()).thenReturn(modManager);
        when(modManager.isModEnabled(RandomAssortmentCompat.MOD_ID)).thenReturn(true);

        CustomCampaignEntityAPI otherEntity = mock(CustomCampaignEntityAPI.class);
        when(otherEntity.getCustomEntityType()).thenReturn("rat_abyss_primordial_fabricator");
        catalyst = mock(CustomCampaignEntityAPI.class);
        when(catalyst.getCustomEntityType()).thenReturn(RandomAssortmentCompat.CATALYST_ENTITY_TYPE);
        StarSystemAPI abyssSystem = mock(StarSystemAPI.class);
        when(abyssSystem.getCustomEntities()).thenReturn(List.of(otherEntity, catalyst));
        sector = mock(SectorAPI.class);
        when(sector.getStarSystems()).thenReturn(List.of(mock(StarSystemAPI.class), abyssSystem));

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
    void theAbyssStaysGatedUntilTheCatalystIsSuppliedWithAbyssalMatter() {
        assertFalse(RandomAssortmentCompat.primordialCatalystActivated());
        assertTrue(SkillTypeUnlockStatus.isLocked(CATALYST_GATE, null));

        when(catalyst.hasTag(RandomAssortmentCompat.CATALYST_COMPLETED_TAG)).thenReturn(true);

        assertTrue(RandomAssortmentCompat.primordialCatalystActivated());
        assertFalse(SkillTypeUnlockStatus.isLocked(CATALYST_GATE, null));
    }

    @Test
    void theCatalystIsFoundOnceAndThenOnlyItsTagIsChecked() {
        RandomAssortmentCompat.primordialCatalystActivated();
        RandomAssortmentCompat.primordialCatalystActivated();
        RandomAssortmentCompat.primordialCatalystActivated();

        verify(sector, times(1)).getStarSystems();
    }

    @Test
    void aSectorWithoutTheCatalystIsNotSearchedAgainEveryCheck() {
        when(sector.getStarSystems()).thenReturn(List.of(mock(StarSystemAPI.class)));

        assertFalse(RandomAssortmentCompat.primordialCatalystActivated());
        assertFalse(RandomAssortmentCompat.primordialCatalystActivated());

        verify(sector, times(1)).getStarSystems();
    }

    @Test
    void withoutRatInstalledTheAbyssIsNeverGated() {
        when(modManager.isModEnabled(RandomAssortmentCompat.MOD_ID)).thenReturn(false);

        assertFalse(SkillTypeUnlockStatus.isLocked(CATALYST_GATE, null));
    }
}
