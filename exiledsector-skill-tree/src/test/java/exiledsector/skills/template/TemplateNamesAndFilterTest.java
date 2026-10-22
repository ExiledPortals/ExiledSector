package exiledsector.skills.template;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import org.junit.jupiter.api.Test;

import exiledlib.skills.ShipHullSizes;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateNamesAndFilterTest {

    private static SkillTreeTemplate template(String id, String name, String root, HullSize hullSize) {
        return new SkillTreeTemplate(id, name, root, hullSize, List.of());
    }

    private static final List<SkillTreeTemplate> EXISTING = List.of(
            template("1", "Brawler", "root_low_tech_1", HullSize.CRUISER),
            template("2", "kite", "root_low_tech_1", HullSize.FRIGATE),
            template("3", "Brawler", "root_high_tech_1", HullSize.CAPITAL_SHIP),
            template("4", "Anything", "root_low_tech_1", null));

    @Test
    void namesAreTrimmedAndMustNotBeBlank() {
        assertEquals("Line", TemplateNames.normalise("  Line  "));
        assertEquals(TemplateNames.Problem.EMPTY, TemplateNames.validate("   ", "root_low_tech_1", EXISTING));
        assertEquals(TemplateNames.Problem.EMPTY, TemplateNames.validate("　", "root_low_tech_1", EXISTING));
        assertEquals(TemplateNames.Problem.DUPLICATE, TemplateNames.validate("Brawler　", "root_low_tech_1", EXISTING));
        assertEquals(TemplateNames.Problem.NONE, TemplateNames.validate(" Line ", "root_low_tech_1", EXISTING));
    }

    @Test
    void aNameAlreadyUsedUnderTheSameRootIsADuplicateIgnoringCase() {
        assertEquals(TemplateNames.Problem.DUPLICATE, TemplateNames.validate("BRAWLER", "root_low_tech_1", EXISTING));
        assertEquals(TemplateNames.Problem.DUPLICATE, TemplateNames.validate(" Kite", "root_low_tech_1", EXISTING));
        assertEquals(TemplateNames.Problem.NONE, TemplateNames.validate("Kite", "root_midline_1", EXISTING));
    }

    @Test
    void namesLongerThanTheLimitAreRejected() {
        assertEquals(TemplateNames.Problem.TOO_LONG, TemplateNames.validate("x".repeat(TemplateNames.MAX_LENGTH + 1), "r", EXISTING));
        assertEquals(TemplateNames.Problem.NONE, TemplateNames.validate("装".repeat(TemplateNames.MAX_LENGTH), "r", EXISTING));
    }

    @Test
    void theDefaultFilterIsTheShipsOwnHullSizeOrEverySizeForUnusualHulls() {
        assertEquals(EnumSet.of(HullSize.DESTROYER), TemplateFilter.defaultFilter(HullSize.DESTROYER));
        assertEquals(EnumSet.copyOf(ShipHullSizes.ORDER), TemplateFilter.defaultFilter(HullSize.FIGHTER));
        assertEquals(EnumSet.copyOf(ShipHullSizes.ORDER), TemplateFilter.defaultFilter(null));
    }

    @Test
    void matchingKeepsTheRootAndSelectedHullSizesSortedByNameAndAlwaysShowsUnknownHulls() {
        List<SkillTreeTemplate> matches = TemplateFilter.matching(EXISTING, "root_low_tech_1", EnumSet.of(HullSize.FRIGATE, HullSize.CRUISER));

        assertEquals(List.of("4", "1", "2"), matches.stream().map(SkillTreeTemplate::id).toList());
        assertEquals(List.of("4"), TemplateFilter.matching(EXISTING, "root_low_tech_1", EnumSet.noneOf(HullSize.class))
                .stream().map(SkillTreeTemplate::id).toList());
    }

    @Test
    void pointsAreLeftWhileThereIsRoomAndEitherACreditOrEnoughOrdnancePoints() {
        assertTrue(TemplateBudget.hasPointsLeft(5, 60, 1, 30, 3, 30));
        assertTrue(TemplateBudget.hasPointsLeft(5, 60, 0, 27, 3, 30));
        assertFalse(TemplateBudget.hasPointsLeft(5, 60, 0, 28, 3, 30));
        assertTrue(TemplateBudget.hasPointsLeft(5, 60, 0, 99, 0, 30));
        assertFalse(TemplateBudget.hasPointsLeft(60, 60, 4, 0, 3, 99));
    }

    @Test
    void renamingATemplateMayKeepItsOwnNameButNotTakeAnother() {
        assertEquals(TemplateNames.Problem.NONE, TemplateNames.validate("brawler", "root_low_tech_1", EXISTING, "1"));
        assertEquals(TemplateNames.Problem.DUPLICATE, TemplateNames.validate("Kite", "root_low_tech_1", EXISTING, "1"));
    }

    @Test
    void defaultNamesGetANumberWhenTakenAndStayWithinTheLimit() {
        assertEquals("Line", TemplateNames.unique(" Line ", "root_low_tech_1", EXISTING));
        assertEquals("Brawler 2", TemplateNames.unique("Brawler", "root_low_tech_1", EXISTING));
        List<SkillTreeTemplate> withSecond = List.of(template("5", "Brawler", "r", null), template("6", "Brawler 2", "r", null));
        assertEquals("Brawler 3", TemplateNames.unique("Brawler", "r", withSecond));
        String longName = "x".repeat(TemplateNames.MAX_LENGTH + 5);
        List<SkillTreeTemplate> withLong = List.of(template("7", "x".repeat(TemplateNames.MAX_LENGTH), "r", null));
        String unique = TemplateNames.unique(longName, "r", withLong);
        assertEquals("x".repeat(TemplateNames.MAX_LENGTH - 2) + " 2", unique);
    }

    @Test
    void aDefaultNameCutAtASpaceStillSpotsTheTemplateItDuplicates() {
        String shortName = "y".repeat(TemplateNames.MAX_LENGTH - 1);
        List<SkillTreeTemplate> withShort = List.of(template("8", shortName, "r", null));

        assertEquals(shortName, TemplateNames.unique(shortName + " tail", "r", List.of()));
        assertEquals("y".repeat(TemplateNames.MAX_LENGTH - 2) + " 2", TemplateNames.unique(shortName + " tail", "r", withShort));
    }
}
