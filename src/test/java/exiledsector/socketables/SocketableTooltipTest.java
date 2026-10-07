package exiledsector.socketables;

import exiledsector.i18n.StyledText;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SocketableTooltipTest {

    @AfterEach
    void tearDown() {
        SocketableDefinitions.clear();
    }

    @Test
    void theTooltipListsEachRolledEffectWithoutAKindLine() throws Exception {
        SocketableFixtures.registerMilitary();
        Socketable socketable = SocketableKind.SUBROUTINE.create("socketable_1", SocketableFixtures.MILITARY, 1L,
                List.of(new RolledEffect("BEAM_WEAPON_DAMAGE_PERCENT", 12f), new RolledEffect("REMOVED_EFFECT", 3f)));

        List<String> lines = socketable.tooltipLines().stream().map(StyledText::plain).toList();

        assertEquals("Military-grade Domain Subroutine", socketable.name());
        assertEquals(List.of("12% increased beam weapon damage."), lines);
    }

    @Test
    void expandedEffectLinesShowTheRollRangeInBracketsAfterTheValue() throws Exception {
        SocketableFixtures.registerMilitary();
        Socketable socketable = SocketableKind.SUBROUTINE.create("socketable_4", SocketableFixtures.MILITARY, 1L,
                List.of(new RolledEffect("BEAM_WEAPON_DAMAGE_PERCENT", 12f), new RolledEffect("SHIELD_DAMAGE_TAKEN_MULT", -12f),
                        new RolledEffect("REMOVED_EFFECT", 3f)));

        List<StyledText> expanded = socketable.effectLines(true);

        assertEquals(List.of("12% (10%-15%) increased beam weapon damage.", "12% (10%-15%) less damage taken by shields."),
                expanded.stream().map(StyledText::plain).toList());
        assertEquals("<good>12%</good> (10%-15%) less damage taken by shields.", expanded.get(1).toMarkup());
        assertEquals(List.of("12% increased beam weapon damage.", "12% less damage taken by shields."),
                socketable.effectLines(false).stream().map(StyledText::plain).toList());
    }

    @Test
    void anEffectNoLongerInItsPoolOrWithAFixedValueShowsNoRange() throws Exception {
        SocketableDefinitions.register(new org.json.JSONArray().put(SocketableFixtures.row("fixed", "subroutine", "HULL_MULT:5:5")));
        Socketable socketable = SocketableKind.SUBROUTINE.create("socketable_5", "fixed", 1L,
                List.of(new RolledEffect("HULL_MULT", 5f), new RolledEffect("ARMOR_PERCENT", 7f)));

        assertEquals(List.of("5% more hull points.", "7% increased armor."),
                socketable.effectLines(true).stream().map(StyledText::plain).toList());
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
        SocketableDefinitions.register(new org.json.JSONArray().put(SocketableFixtures.row("described", "subroutine", "HULL_MULT:4:6")
                .put("description", "Pulled from a Domain-era warship.")));
        Socketable socketable = SocketableKind.SUBROUTINE.create("socketable_3", "described", 1L, List.of());

        List<String> lines = socketable.tooltipLines().stream().map(StyledText::plain).toList();

        assertEquals(List.of("Pulled from a Domain-era warship."), lines);
    }

    @Test
    void anItemWhoseDefinitionIsGoneKeepsItsEffectsButShowsAsUnrecognised() {
        Socketable socketable = SocketableKind.TEAM.create("socketable_2", "removed_mod_item", 1L,
                List.of(new RolledEffect("BEAM_WEAPON_DAMAGE_PERCENT", 12f)));

        assertEquals("Unrecognised hull socket module", socketable.name());
        assertEquals(SocketableDefinition.FALLBACK_ICON, socketable.iconPath());
        assertEquals(1, socketable.tooltipLines().size());
    }
}
