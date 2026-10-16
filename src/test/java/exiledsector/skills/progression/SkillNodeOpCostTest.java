package exiledsector.skills.progression;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SkillNodeOpCostTest {

    private MockedStatic<LunaSettings> lunaSettingsMock;

    @BeforeEach
    void setUp() {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
    }

    @AfterEach
    void tearDown() {
        lunaSettingsMock.close();
    }

    @Test
    void fallsBackToTheDocumentedDefaultsWhenNoSettingIsConfigured() {
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", "exiledSector_opCostFrigate")).thenReturn(null);
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", "exiledSector_opCostDestroyer")).thenReturn(null);
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", "exiledSector_opCostCruiser")).thenReturn(null);
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", "exiledSector_opCostCapital")).thenReturn(null);
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", "exiledSector_opCostUndefined")).thenReturn(null);

        assertEquals(1, SkillNodeOpCost.perNode(HullSize.FRIGATE));
        assertEquals(2, SkillNodeOpCost.perNode(HullSize.DESTROYER));
        assertEquals(3, SkillNodeOpCost.perNode(HullSize.CRUISER));
        assertEquals(5, SkillNodeOpCost.perNode(HullSize.CAPITAL_SHIP));
        assertEquals(5, SkillNodeOpCost.perNode(HullSize.FIGHTER));
        assertEquals(5, SkillNodeOpCost.perNode((HullSize) null));
    }

    @Test
    void usesTheConfiguredLunaLibSettingWhenPresent() {
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", "exiledSector_opCostFrigate")).thenReturn(7);
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", "exiledSector_opCostCapital")).thenReturn(20);

        assertEquals(7, SkillNodeOpCost.perNode(HullSize.FRIGATE));
        assertEquals(20, SkillNodeOpCost.perNode(HullSize.CAPITAL_SHIP));
    }

    @Test
    void resolvesTheCostFromAHullSpecsHullSize() {
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", "exiledSector_opCostCruiser")).thenReturn(null);

        ShipHullSpecAPI hull = mock(ShipHullSpecAPI.class);
        when(hull.getHullSize()).thenReturn(HullSize.CRUISER);

        assertEquals(3, SkillNodeOpCost.perNode(hull));
    }
}
