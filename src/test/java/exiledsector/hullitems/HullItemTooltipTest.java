package exiledsector.hullitems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.i18n.Catalogue;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HullItemTooltipTest {

    @AfterEach
    void tearDown() {
        HullItemDefinitions.clear();
    }

    @Test
    void theTooltipListsEachRolledEffectWithoutAKindLine() throws Exception {
        HullItemFixtures.registerMilitary();
        HullItem hullItem = new HullItem("socketable_1", HullItemFixtures.MILITARY, 1L,
                List.of(new RolledEffect("BEAM_WEAPON_DAMAGE_PERCENT", 12f), new RolledEffect("REMOVED_EFFECT", 3f)));

        List<String> lines = hullItem.tooltipLines().stream().map(StyledText::plain).toList();

        assertEquals("Military-grade Domain Subroutine", hullItem.name());
        assertEquals(List.of("12% increased beam weapon damage."), lines);
    }

    @Test
    void expandedEffectLinesShowTheRollRangeInBracketsAfterTheValue() throws Exception {
        HullItemFixtures.registerMilitary();
        HullItem hullItem = new HullItem("socketable_4", HullItemFixtures.MILITARY, 1L,
                List.of(new RolledEffect("BEAM_WEAPON_DAMAGE_PERCENT", 12f), new RolledEffect("SHIELD_DAMAGE_TAKEN_MULT", -12f),
                        new RolledEffect("REMOVED_EFFECT", 3f)));

        List<StyledText> expanded = hullItem.effectLines(true);

        assertEquals(List.of("12% (10%-15%) increased beam weapon damage.", "12% (10%-15%) less damage taken by shields."),
                expanded.stream().map(StyledText::plain).toList());
        assertEquals("<good>12%</good> (10%-15%) less damage taken by shields.", expanded.get(1).toMarkup());
        assertEquals(List.of("12% increased beam weapon damage.", "12% less damage taken by shields."),
                hullItem.effectLines(false).stream().map(StyledText::plain).toList());
    }

    @Test
    void anEffectNoLongerInItsPoolOrWithAFixedValueShowsNoRange() throws Exception {
        HullItemDefinitions.register(new org.json.JSONArray().put(HullItemFixtures.row("fixed", "subroutine", "HULL_MULT:5:5")));
        HullItem hullItem = new HullItem("socketable_5", "fixed", 1L,
                List.of(new RolledEffect("HULL_MULT", 5f), new RolledEffect("ARMOR_PERCENT", 7f)));

        assertEquals(List.of("5% more hull points.", "7% increased armor."),
                hullItem.effectLines(true).stream().map(StyledText::plain).toList());
    }

    @Test
    void onAKnownHullSizeTheTooltipShowsOnlyThatSizesValue() throws Exception {
        HullItemDefinitions.register(new org.json.JSONArray().put(HullItemFixtures.row("plating", "subroutine", "ARMOR_FLAT:5/10/15/20")));
        HullItem hullItem = new HullItem("socketable_6", "plating", 1L, List.of(new RolledEffect("ARMOR_FLAT", 5f)));

        List<String> onCapital = writtenParagraphs(hullItem, HullSize.CAPITAL_SHIP, List::of);
        List<String> withoutShip = writtenParagraphs(hullItem, null, List::of);

        assertTrue(onCapital.stream().anyMatch(text -> text.contains("20")), onCapital.toString());
        assertTrue(onCapital.stream().noneMatch(text -> text.contains("5/10/15/20")), onCapital.toString());
        assertTrue(withoutShip.stream().anyMatch(text -> text.contains("5/10/15/20")), withoutShip.toString());
    }

    @Test
    void theFooterIsBuiltWithTheGameTextCatalogue() {
        I18n.install(new Catalogue("zh_CN", Map.of()), new Catalogue("en", Map.of()));

        List<String> paragraphs = writtenParagraphs(null, null, () -> List.of(StyledText.of("footer in " + I18n.locale())));

        assertTrue(paragraphs.contains("footer in en"), paragraphs.toString());
    }

    private static List<String> writtenParagraphs(HullItem hullItem, HullSize hullSize, Supplier<List<StyledText>> footer) {
        TooltipMakerAPI tooltip = mock(TooltipMakerAPI.class);
        when(tooltip.addPara(anyString(), anyFloat(), (Color) any(), (Color) any(), any(String[].class))).thenReturn(mock(LabelAPI.class));
        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class)) {
            globalMock.when(Global::getSettings).thenReturn(mock(SettingsAPI.class));
            HullItemTooltip.write(tooltip, hullItem, hullSize, footer, true);
        }
        ArgumentCaptor<String> paragraphs = ArgumentCaptor.forClass(String.class);
        verify(tooltip, atLeastOnce()).addPara(eq("%s"), anyFloat(), (Color) any(), (Color) any(), paragraphs.capture());
        return paragraphs.getAllValues();
    }

    @Test
    void effectsWhereLessIsBetterAreColouredLikeTheTreeColoursThem() {
        RolledEffect upkeep = new RolledEffect("SHIELD_UPKEEP_MULT", -25f);
        RolledEffect beam = new RolledEffect("BEAM_WEAPON_DAMAGE_PERCENT", 12f);

        assertEquals(upkeep.effect().description(-25f).inverted(), upkeep.description());
        assertEquals(beam.effect().description(12f), beam.description());
    }

    @Test
    void theDescriptionIsTheOnlyLineAboveTheEffects() throws Exception {
        HullItemDefinitions.register(new org.json.JSONArray().put(HullItemFixtures.row("described", "subroutine", "HULL_MULT:4:6")
                .put("description", "Pulled from a Domain-era warship.")));
        HullItem hullItem = new HullItem("socketable_3", "described", 1L, List.of());

        List<String> lines = hullItem.tooltipLines().stream().map(StyledText::plain).toList();

        assertEquals(List.of("Pulled from a Domain-era warship."), lines);
    }

    @Test
    void anItemWhoseDefinitionIsGoneKeepsItsEffectsButShowsAsUnrecognised() {
        HullItem hullItem = new HullItem("socketable_2", "removed_mod_item", 1L,
                List.of(new RolledEffect("BEAM_WEAPON_DAMAGE_PERCENT", 12f)));

        assertEquals("Unrecognised hull socket module", hullItem.name());
        assertEquals(HullItemDefinition.FALLBACK_ICON, hullItem.iconPath());
        assertEquals(1, hullItem.tooltipLines().size());
    }
}
