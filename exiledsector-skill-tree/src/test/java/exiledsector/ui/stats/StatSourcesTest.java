package exiledsector.ui.stats;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatSourcesTest {

    private MockedStatic<Global> globalMock;
    private SettingsAPI settings;

    @BeforeEach
    void setUp() {
        settings = mock(SettingsAPI.class);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    @Test
    void aDescriptionNamesTheSourceFirst() {
        assertEquals("Heavy Armor", StatSources.outsideSourceName("flux_capacitors", "Heavy Armor"));
    }

    @Test
    void vanillaRefitModifiersAreNamedInPlainWords() {
        assertEquals("Flux capacitors", StatSources.outsideSourceName("flux_capacitors", null));
        assertEquals("Flux vents", StatSources.outsideSourceName("flux_vents", ""));
        assertEquals("Combat readiness", StatSources.outsideSourceName("cr_effect", null));
    }

    @Test
    void aModifierIdExtendingAHullModIdIsNamedAfterThatHullMod() {
        HullModSpecAPI spec = mock(HullModSpecAPI.class);
        when(spec.getDisplayName()).thenReturn("Test Plating");
        when(settings.getHullModSpec("statsources_plating")).thenReturn(spec);

        assertEquals("Test Plating", StatSources.outsideSourceName("statsources_plating", null));
        assertEquals("Test Plating", StatSources.outsideSourceName("statsources_plating_armor_bonus", null));
    }

    @Test
    void aModifierIdExtendingASkillIdIsNamedAfterThatSkill() {
        SkillSpecAPI spec = mock(SkillSpecAPI.class);
        when(spec.getName()).thenReturn("Test Skill");
        when(settings.getSkillSpec("statsources_skill")).thenReturn(spec);

        assertEquals("Test Skill", StatSources.outsideSourceName("statsources_skill_elite", null));
    }

    @Test
    void vanillaSkillModifiersWithTheirOwnIdsAreNamedAfterTheirSkill() {
        Map<String, String> skillNames = Map.of(
                "bulk_transport", "Bulk Transport",
                "salvaging", "Salvaging",
                "navigation", "Navigation",
                "containment_procedures", "Containment Procedures",
                "makeshift_equipment", "Makeshift Equipment",
                "coordinated_maneuvers", "Coordinated Maneuvers");
        skillNames.forEach((skillId, skillName) -> {
            SkillSpecAPI spec = mock(SkillSpecAPI.class);
            when(spec.getName()).thenReturn(skillName);
            when(settings.getSkillSpec(skillId)).thenReturn(spec);
        });

        assertEquals("Bulk Transport", StatSources.outsideSourceName("bt_cargo_cap_mod", null));
        assertEquals("Bulk Transport", StatSources.outsideSourceName("bt_fuel_cap_mod", null));
        assertEquals("Bulk Transport", StatSources.outsideSourceName("bt_crew_cap_mod", null));
        assertEquals("Salvaging", StatSources.outsideSourceName("sal_cargo_cap_mod", null));
        assertEquals("Navigation", StatSources.outsideSourceName("nav_fuel_use_mod", null));
        assertEquals("Containment Procedures", StatSources.outsideSourceName("sp_fuel_use_mod", null));
        assertEquals("Makeshift Equipment", StatSources.outsideSourceName("surveying_supply_use_mod", null));
        assertEquals("Coordinated Maneuvers", StatSources.outsideSourceName("coord_maneuvers_bonus", null));
    }

    @Test
    void anUnknownModifierKeepsItsId() {
        when(settings.getHullModSpec(Mockito.anyString())).thenThrow(new RuntimeException("unknown hull mod"));

        assertEquals("statsources_unknown_source", StatSources.outsideSourceName("statsources_unknown_source", null));
    }
}
