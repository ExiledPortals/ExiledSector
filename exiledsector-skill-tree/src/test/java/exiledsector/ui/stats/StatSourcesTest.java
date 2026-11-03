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
    void anUnknownModifierKeepsItsId() {
        when(settings.getHullModSpec(Mockito.anyString())).thenThrow(new RuntimeException("unknown hull mod"));

        assertEquals("statsources_unknown_source", StatSources.outsideSourceName("statsources_unknown_source", null));
    }
}
