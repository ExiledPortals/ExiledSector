package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.progression.ShipLevelSystem;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NpcBonusScaleTest {

    private static final float TOLERANCE = 0.0001f;
    private static final NpcBonusScale.Config DEFAULTS = new NpcBonusScale.Config(15, 15, 50, 3f);

    private MockedStatic<Global> globalMock;
    private MockedStatic<LunaSettings> lunaSettingsMock;

    @BeforeEach
    void setUp() {
        Map<String, Object> persistentData = new HashMap<>();
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class, invocation -> null);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        lunaSettingsMock.close();
    }

    @Test
    void theMultiplierStaysAtOneBelowTheStartingCharacterLevel() {
        assertEquals(1f, NpcBonusScale.multiplier(14, 50f, DEFAULTS), TOLERANCE);
    }

    @Test
    void theMultiplierRisesInAStraightLineBetweenTheLowAndHighShipLevels() {
        assertEquals(1f, NpcBonusScale.multiplier(15, 10f, DEFAULTS), TOLERANCE);
        assertEquals(1f, NpcBonusScale.multiplier(15, 15f, DEFAULTS), TOLERANCE);
        assertEquals(2f, NpcBonusScale.multiplier(30, 32.5f, DEFAULTS), TOLERANCE);
        assertEquals(3f, NpcBonusScale.multiplier(30, 50f, DEFAULTS), TOLERANCE);
        assertEquals(3f, NpcBonusScale.multiplier(30, 60f, DEFAULTS), TOLERANCE);
    }

    @Test
    void aMaximumOfOneOrAnEmptyLevelSpanBehavesSafely() {
        assertEquals(1f, NpcBonusScale.multiplier(30, 50f, new NpcBonusScale.Config(15, 15, 50, 1f)), TOLERANCE);
        assertEquals(1f, NpcBonusScale.multiplier(30, 29f, new NpcBonusScale.Config(15, 30, 30, 3f)), TOLERANCE);
        assertEquals(3f, NpcBonusScale.multiplier(30, 30f, new NpcBonusScale.Config(15, 30, 30, 3f)), TOLERANCE);
    }

    @Test
    void theAverageCountsCombatShipsOnlyAndLiftsThemToTheLevelFloor() {
        FleetMemberAPI veteran = member("veteran", 40);
        FleetMemberAPI rookie = member("rookie", 0);
        FleetMemberAPI freighter = member("freighter", 1);
        when(freighter.isCivilian()).thenReturn(true);
        FleetMemberAPI mothballed = member("mothballed", 1);
        when(mothballed.isMothballed()).thenReturn(true);

        assertEquals(30f, NpcBonusScale.averageCombatShipLevel(List.of(veteran, rookie, freighter, mothballed), 20), TOLERANCE);
        assertEquals(0f, NpcBonusScale.averageCombatShipLevel(List.of(freighter), 0), TOLERANCE);
    }

    private static FleetMemberAPI member(String id, int level) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn(id);
        if (level > 0) {
            ShipLevelSystem.raiseToLevel(ShipSkillDataManager.get(id), level, List.of());
        }
        return member;
    }
}
