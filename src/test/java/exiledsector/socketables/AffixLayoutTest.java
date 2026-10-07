package exiledsector.socketables;

import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AffixLayoutTest {

    private static final RolledEffect PREFIX_CAPACITY = new RolledEffect("FLUX_CAPACITY_MULT", 5f);
    private static final RolledEffect PREFIX_DISSIPATION = new RolledEffect("FLUX_DISSIPATION_MULT", 5f);
    private static final RolledEffect SUFFIX_SHIELDS = new RolledEffect("SHIELD_DAMAGE_TAKEN_MULT", -12f);
    private static final RolledEffect SUFFIX_HULL = new RolledEffect("HULL_MULT", 5f);
    private static final RolledEffect RETIRED = new RolledEffect("CARGO_CAPACITY_PERCENT", 10f);

    private SocketableDefinition military;

    @BeforeEach
    void setUp() throws Exception {
        military = SocketableFixtures.registerMilitary();
    }

    @AfterEach
    void tearDown() {
        SocketableDefinitions.clear();
    }

    private List<List<PoolEntry>> openSides(RolledEffect... effects) {
        return AffixLayout.openSides(military, List.of(effects));
    }

    private boolean prefixesOpen(List<List<PoolEntry>> sides) {
        return sides.stream().anyMatch(side -> military.isPrefix(side.get(0).effectName()));
    }

    private boolean suffixesOpen(List<List<PoolEntry>> sides) {
        return sides.stream().anyMatch(side -> military.isSuffix(side.get(0).effectName()));
    }

    @Test
    void upToTwoEffectsIsCommonMoreIsRareAndUniqueDefinitionsAreAlwaysUnique() throws Exception {
        assertEquals(SocketableRarity.COMMON, AffixLayout.rarityFor(military, 1));
        assertEquals(SocketableRarity.COMMON, AffixLayout.rarityFor(military, 2));
        assertEquals(SocketableRarity.RARE, AffixLayout.rarityFor(military, 3));
        assertEquals(SocketableRarity.RARE, AffixLayout.rarityFor(military, 4));
        assertEquals(SocketableRarity.COMMON, AffixLayout.rarityFor(null, 2));
        SocketableDefinitions.register(new JSONArray().put(SocketableFixtures.row("relic", "reactor", "HULL_MULT:4:6").put("unique", "TRUE")));
        assertEquals(SocketableRarity.UNIQUE, AffixLayout.rarityFor(SocketableDefinitions.get("relic"), 1));
    }

    @Test
    void theSplitPutsTheOddEffectOnEitherSide() {
        assertEquals(1, AffixLayout.prefixCount(2, true));
        assertEquals(2, AffixLayout.prefixCount(3, true));
        assertEquals(1, AffixLayout.prefixCount(3, false));
        assertEquals(2, AffixLayout.prefixCount(4, false));
    }

    @Test
    void theEffectCountThresholdsFollowTheProposedOdds() {
        assertEquals(2, AffixLayout.rolledEffectCount(0f));
        assertEquals(2, AffixLayout.rolledEffectCount(0.549f));
        assertEquals(3, AffixLayout.rolledEffectCount(0.55f));
        assertEquals(3, AffixLayout.rolledEffectCount(0.899f));
        assertEquals(4, AffixLayout.rolledEffectCount(0.9f));
    }

    @Test
    void aSideWithTwoEffectsIsClosedAndFourEffectsClosesBoth() {
        List<List<PoolEntry>> sides = openSides(PREFIX_CAPACITY, SUFFIX_SHIELDS, SUFFIX_HULL);
        assertTrue(prefixesOpen(sides));
        assertFalse(suffixesOpen(sides));
        assertTrue(sides.get(0).stream().noneMatch(entry -> entry.effectName().equals(PREFIX_CAPACITY.effectName())));
        assertEquals(List.of(), openSides(PREFIX_CAPACITY, PREFIX_DISSIPATION, SUFFIX_SHIELDS, SUFFIX_HULL));
    }

    @Test
    void anEffectNoLongerInThePoolStillTakesASlotOnTheSideItsPositionShows() {
        List<List<PoolEntry>> afterSuffix = openSides(PREFIX_CAPACITY, SUFFIX_SHIELDS, RETIRED);
        assertTrue(prefixesOpen(afterSuffix));
        assertFalse(suffixesOpen(afterSuffix), "a retired effect after a suffix is a suffix, so a third suffix must not be added");

        List<List<PoolEntry>> beforePrefix = openSides(RETIRED, PREFIX_CAPACITY, SUFFIX_SHIELDS);
        assertFalse(prefixesOpen(beforePrefix), "a retired effect before a prefix is a prefix");
        assertTrue(suffixesOpen(beforePrefix));
    }

    @Test
    void anEffectWhoseSideCannotBeToldTakesASlotOnBothSides() {
        List<List<PoolEntry>> sides = openSides(RETIRED, SUFFIX_SHIELDS);

        assertTrue(prefixesOpen(sides));
        assertFalse(suffixesOpen(sides));
        assertEquals(List.of(), openSides(PREFIX_CAPACITY, RETIRED, SUFFIX_SHIELDS));
    }

    @Test
    void newPrefixesGoAfterTheLastPrefixAndNewSuffixesGoLast() {
        List<RolledEffect> effects = List.of(PREFIX_CAPACITY, SUFFIX_SHIELDS);

        assertEquals(1, AffixLayout.insertIndex(military, effects, PREFIX_DISSIPATION.effectName()));
        assertEquals(2, AffixLayout.insertIndex(military, effects, SUFFIX_HULL.effectName()));
        assertEquals(2, AffixLayout.insertIndex(military, List.of(RETIRED, PREFIX_CAPACITY, SUFFIX_SHIELDS), PREFIX_DISSIPATION.effectName()));
    }

    @Test
    void augmentingAnItemWithARetiredSuffixNeverAddsAThirdSuffix() {
        for (long seed = 0; seed < 200; seed++) {
            Socketable socketable = new Socketable("socketable_" + seed, SocketableFixtures.MILITARY, seed,
                    List.of(PREFIX_CAPACITY, SUFFIX_SHIELDS, RETIRED));

            SocketableCrafting.augment(socketable, new Random(seed));

            long suffixes = socketable.effects().stream().filter(effect -> military.isSuffix(effect.effectName())).count();
            assertEquals(1, suffixes, socketable.effects().toString());
            assertEquals(4, socketable.effects().size());
        }
    }
}
