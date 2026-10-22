package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
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

class PlayerTreePowerTest {

    private static final SkillType SMALL = new SkillType.Builder("small", "Small", "a.png", SkillTier.SMALL).effects(List.of()).build();

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
    void aShipsTreePowerIsItsLevelPlusItsPaidNodes() {
        ShipSkillData shipData = levelled("veteran", 3);
        for (int node = 0; node < 5; node++) {
            ShipTrees.allocate(shipData, new SkillNode("node_" + node, SMALL, List.of(), 0f, 0f), 1);
        }

        assertEquals(2, ShipTrees.paidNodeCount(shipData));
        assertEquals(5, PlayerTreePower.shipPower(shipData, 0, 1));
    }

    @Test
    void atZeroOpPerNodeTheTreeIsCountedOnceRatherThanOnTopOfTheLevel() {
        ShipSkillData shipData = levelled("free-nodes", 10);
        for (int node = 0; node < 10; node++) {
            ShipTrees.allocate(shipData, new SkillNode("node_" + node, SMALL, List.of(), 0f, 0f), 0);
        }

        assertEquals(10, PlayerTreePower.shipPower(shipData, 0, 0));
        for (int node = 10; node < 14; node++) {
            ShipTrees.allocate(shipData, new SkillNode("node_" + node, SMALL, List.of(), 0f, 0f), 0);
        }
        assertEquals(14, PlayerTreePower.shipPower(shipData, 0, 0));
    }

    @Test
    void unspentFreeNodesStillCountThroughTheShipsLevel() {
        assertEquals(30, PlayerTreePower.shipPower(levelled("hoarder", 30), 0, 1));
    }

    @Test
    void aShipBelowTheLevelFloorCountsAsBeingAtIt() {
        assertEquals(15, PlayerTreePower.shipPower(levelled("rookie", 4), 15, 1));
        assertEquals(15, PlayerTreePower.shipPower(null, 15, 1));
    }

    @Test
    void onlyCombatShipsCountAndTheirPowersComeBackSorted() {
        levelled("veteran", 40);
        levelled("rookie", 10);
        FleetMemberAPI freighter = member("freighter");
        when(freighter.isCivilian()).thenReturn(true);
        FleetMemberAPI mothballed = member("mothballed");
        when(mothballed.isMothballed()).thenReturn(true);

        assertEquals(List.of(10, 40), PlayerTreePower.of(List.of(member("veteran"), member("rookie"), freighter, mothballed), 0));
    }

    @Test
    void aFleetWithNoCombatShipsCountsAsOneShipAtTheLevelFloor() {
        FleetMemberAPI freighter = member("freighter");
        when(freighter.isCivilian()).thenReturn(true);

        assertEquals(List.of(12), PlayerTreePower.of(List.of(freighter), 12));
    }

    @Test
    void theAverageIsTheMeanOfTheShipPowers() {
        assertEquals(25f, PlayerTreePower.average(List.of(10, 40)), 0.0001f);
        assertEquals(0f, PlayerTreePower.average(List.of()), 0.0001f);
    }

    private static ShipSkillData levelled(String shipId, int level) {
        ShipSkillData shipData = ShipSkillDataManager.get(shipId);
        ShipLevelSystem.raiseToLevel(shipData, level, List.of());
        return shipData;
    }

    private static FleetMemberAPI member(String shipId) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn(shipId);
        return member;
    }
}
