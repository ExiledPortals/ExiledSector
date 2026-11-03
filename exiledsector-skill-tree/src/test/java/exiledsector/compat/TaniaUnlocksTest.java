package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.skills.unlock.SkillTypeUnlockStatus;
import exiledsector.skills.unlock.UnlockCondition;
import exiledsector.skills.unlock.UnlockConditionOverrides;
import exiledsector.skills.unlock.UnlockConditionType;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaniaUnlocksTest {

    private static final List<UnlockCondition> BLOOD_TRAIL_GATE = List.of(UnlockCondition.modGate(UnlockConditionType.TANIA_BLOOD_TRAIL_DEFEATED));
    private static final List<UnlockCondition> FROST_FIRE_GATE = List.of(UnlockCondition.modGate(UnlockConditionType.TANIA_FROST_FIRE_DEFEATED));
    private static final List<UnlockCondition> LAST_STRIVE_GATE = List.of(UnlockCondition.modGate(UnlockConditionType.TANIA_LAST_STRIVE_COMPLETED));
    private static final List<List<UnlockCondition>> ALL_GATES = List.of(BLOOD_TRAIL_GATE, FROST_FIRE_GATE, LAST_STRIVE_GATE);

    private MockedStatic<LunaSettings> lunaSettingsMock;
    private MockedStatic<Global> globalMock;
    private ModManagerAPI modManager;
    private MemoryAPI sectorMemory;

    private static MemoryAPI memory() {
        Map<String, Object> values = new HashMap<>();
        MemoryAPI memory = mock(MemoryAPI.class);
        when(memory.getBoolean(anyString())).thenAnswer(invocation -> Boolean.TRUE.equals(values.get(invocation.getArgument(0))));
        doAnswer(invocation -> values.put(invocation.getArgument(0), invocation.getArgument(1))).when(memory).set(anyString(), any());
        return memory;
    }

    @BeforeEach
    void setUp() {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean(anyString(), anyString())).thenReturn(null);
        SettingsAPI settings = mock(SettingsAPI.class);
        modManager = mock(ModManagerAPI.class);
        when(settings.getModManager()).thenReturn(modManager);
        when(modManager.isModEnabled(TaniaCompat.MOD_ID)).thenReturn(true);
        sectorMemory = memory();
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);

        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        globalMock.when(Global::getSector).thenReturn(sector);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        lunaSettingsMock.close();
    }

    @Test
    void eachNodeStaysLockedUntilItsOwnMissionIsDone() {
        ALL_GATES.forEach(gate -> assertTrue(SkillTypeUnlockStatus.isLocked(gate, null)));

        sectorMemory.set(TaniaUnlocks.BLOOD_TRAIL_DEFEATED_FLAG, true);
        assertFalse(SkillTypeUnlockStatus.isLocked(BLOOD_TRAIL_GATE, null));
        assertTrue(SkillTypeUnlockStatus.isLocked(FROST_FIRE_GATE, null));
        assertTrue(SkillTypeUnlockStatus.isLocked(LAST_STRIVE_GATE, null));

        sectorMemory.set(TaniaUnlocks.FROST_FIRE_DEFEATED_FLAG, true);
        assertFalse(SkillTypeUnlockStatus.isLocked(FROST_FIRE_GATE, null));
        assertTrue(SkillTypeUnlockStatus.isLocked(LAST_STRIVE_GATE, null));

        sectorMemory.set(TaniaUnlocks.LAST_STRIVE_COMPLETED_FLAG, true);
        assertFalse(SkillTypeUnlockStatus.isLocked(LAST_STRIVE_GATE, null));
    }

    @Test
    void withoutTaniaInstalledEveryMissionGateIsOpen() {
        when(modManager.isModEnabled(TaniaCompat.MOD_ID)).thenReturn(false);

        ALL_GATES.forEach(gate -> assertFalse(SkillTypeUnlockStatus.isLocked(gate, null)));
    }

    @Test
    void theGameStateUnlockSettingOpensEveryMissionGate() {
        lunaSettingsMock.when(() -> LunaSettings.getBoolean(anyString(), eq(UnlockConditionOverrides.DISABLE_MEMORY_FLAG_FIELD_ID))).thenReturn(true);

        ALL_GATES.forEach(gate -> assertFalse(SkillTypeUnlockStatus.isLocked(gate, null)));
    }
}
