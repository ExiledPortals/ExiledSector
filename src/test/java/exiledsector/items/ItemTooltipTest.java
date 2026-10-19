package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.i18n.Catalogue;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.modifiers.ModifierSlot;
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

class ItemTooltipTest {

    @AfterEach
    void tearDown() {
        ItemDefinitions.clear();
    }

    @Test
    void theTooltipListsEachRolledEffectWithoutAKindLine() throws Exception {
        ItemFixtures.registerMilitary();
        Item item = Item.withEffects("socketable_1", ItemFixtures.MILITARY, 1L,
                List.of(new RolledEffect("BEAM_WEAPON_DAMAGE_PERCENT", 12f), new RolledEffect("REMOVED_EFFECT", 3f)));

        List<String> lines = item.tooltipLines().stream().map(StyledText::plain).toList();

        assertEquals("Military-grade Domain Subroutine", item.name());
        assertEquals(List.of("12% increased beam weapon damage."), lines);
    }

    @Test
    void expandedEffectLinesShowTheRollRangeInBracketsAfterTheValue() throws Exception {
        ItemFixtures.registerMilitary();
        Item item = Item.withEffects("socketable_4", ItemFixtures.MILITARY, 1L,
                List.of(new RolledEffect("BEAM_WEAPON_DAMAGE_PERCENT", 12f), new RolledEffect("SHIELD_DAMAGE_TAKEN_MULT", -12f),
                        new RolledEffect("REMOVED_EFFECT", 3f)));

        List<StyledText> expanded = item.effectLines(true);

        assertEquals(List.of("12% (10%-15%) increased beam weapon damage.", "12% (10%-15%) less damage taken by shields."),
                expanded.stream().map(StyledText::plain).toList());
        assertEquals("<good>12%</good> (10%-15%) less damage taken by shields.", expanded.get(1).toMarkup());
        assertEquals(List.of("12% increased beam weapon damage.", "12% less damage taken by shields."),
                item.effectLines(false).stream().map(StyledText::plain).toList());
    }

    @Test
    void slottedEffectLinesTagEachExplicitAsPrefixOrSuffix() throws Exception {
        ItemFixtures.registerMilitary();
        Item item = Item.withEffects("socketable_6", ItemFixtures.MILITARY, 1L,
                List.of(new RolledEffect("BEAM_WEAPON_DAMAGE_PERCENT", 12f), new RolledEffect("SHIELD_DAMAGE_TAKEN_MULT", -12f)));

        List<SlottedLine> lines = item.slottedEffectLines(true, null);

        assertEquals(List.of(ModifierSlot.PREFIX, ModifierSlot.SUFFIX), lines.stream().map(SlottedLine::slot).toList());
        assertEquals(item.effectLines(true).stream().map(StyledText::plain).toList(),
                lines.stream().map(line -> line.text().plain()).toList());
    }

    @Test
    void anEffectNoLongerInItsPoolOrWithAFixedValueShowsNoRange() throws Exception {
        ItemDefinitions.register(new org.json.JSONArray().put(ItemFixtures.row("fixed", "subroutine", "HULL_MULT:5:5")));
        Item item = Item.withEffects("socketable_5", "fixed", 1L,
                List.of(new RolledEffect("HULL_MULT", 5f), new RolledEffect("ARMOR_PERCENT", 7f)));

        assertEquals(List.of("5% more hull points.", "7% increased armor."),
                item.effectLines(true).stream().map(StyledText::plain).toList());
    }

    @Test
    void onAKnownHullSizeTheTooltipShowsOnlyThatSizesValue() throws Exception {
        ItemDefinitions.register(new org.json.JSONArray().put(ItemFixtures.row("plating", "subroutine", "ARMOR_FLAT:5/10/15/20")));
        Item item = Item.withEffects("socketable_6", "plating", 1L, List.of(new RolledEffect("ARMOR_FLAT", 5f)));

        List<String> onCapital = writtenParagraphs(item, HullSize.CAPITAL_SHIP, List::of);
        List<String> withoutShip = writtenParagraphs(item, null, List::of);

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

    private static List<String> writtenParagraphs(Item item, HullSize hullSize, Supplier<List<StyledText>> footer) {
        TooltipMakerAPI tooltip = mock(TooltipMakerAPI.class);
        when(tooltip.addPara(anyString(), anyFloat(), (Color) any(), (Color) any(), any(String[].class))).thenReturn(mock(LabelAPI.class));
        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class)) {
            globalMock.when(Global::getSettings).thenReturn(mock(SettingsAPI.class));
            ItemTooltip.write(tooltip, item, hullSize, footer, true);
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
        ItemDefinitions.register(new org.json.JSONArray().put(ItemFixtures.row("described", "subroutine", "HULL_MULT:4:6")
                .put("description", "Pulled from a Domain-era warship.")));
        Item item = Item.withEffects("socketable_3", "described", 1L, List.of());

        List<String> lines = item.tooltipLines().stream().map(StyledText::plain).toList();

        assertEquals(List.of("Pulled from a Domain-era warship."), lines);
    }

    @Test
    void anItemWhoseDefinitionIsGoneKeepsItsEffectsButShowsAsUnrecognised() {
        Item item = Item.withEffects("socketable_2", "removed_mod_item", 1L,
                List.of(new RolledEffect("BEAM_WEAPON_DAMAGE_PERCENT", 12f)));

        assertEquals("Unrecognised hull item", item.name());
        assertEquals(ItemDefinition.FALLBACK_ICON, item.iconPath());
        assertEquals(1, item.tooltipLines().size());
    }
}
