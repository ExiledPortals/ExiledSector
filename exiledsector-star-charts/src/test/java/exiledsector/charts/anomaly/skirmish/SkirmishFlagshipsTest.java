package exiledsector.charts.anomaly.skirmish;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkirmishFlagshipsTest {

    private static final SkirmishFlagship ATTACKER = SkirmishFlagship.standing(FightingSide.ATTACKER, 1, "attacker-fleet", "attacker-flagship");
    private static final SkirmishFlagship DEFENDER = SkirmishFlagship.standing(FightingSide.DEFENDER, 2, "defender-fleet", "defender-flagship");

    @Test
    void theFirstFlagshipToFallLosesTheFight() {
        assertEquals(SkirmishResult.DEFENDER_WINS, new SkirmishFlagships(ATTACKER.fallen(FlagshipFall.LOST_TO_RIVAL, 1), DEFENDER).result());
        assertEquals(SkirmishResult.ATTACKER_WINS, new SkirmishFlagships(ATTACKER, DEFENDER.fallen(FlagshipFall.BEATEN_BY_PLAYER, 1)).result());
        assertEquals(SkirmishResult.ATTACKER_WINS, new SkirmishFlagships(ATTACKER.fallen(FlagshipFall.BEATEN_BY_PLAYER, 2),
                DEFENDER.fallen(FlagshipFall.LOST_TO_RIVAL, 1)).result());
    }

    @Test
    void bothBeatenByThePlayerDropsTheFightAndNeitherFallenLeavesItUndecided() {
        assertEquals(SkirmishResult.DROPPED, new SkirmishFlagships(ATTACKER.fallen(FlagshipFall.BEATEN_BY_PLAYER, 1),
                DEFENDER.fallen(FlagshipFall.BEATEN_BY_PLAYER, 2)).result());
        assertEquals(SkirmishResult.UNDECIDED, new SkirmishFlagships(ATTACKER, DEFENDER).result());
        assertEquals(SkirmishResult.UNDECIDED, new SkirmishFlagships(null, DEFENDER.fallen(FlagshipFall.LOST_TO_RIVAL, 1)).result());
    }

    @Test
    void theFightEndsOnlyOnceEveryTrackedFlagshipHasFallen() {
        SkirmishFlagships oneDown = new SkirmishFlagships(ATTACKER.fallen(FlagshipFall.BEATEN_BY_PLAYER, 1), DEFENDER);

        assertFalse(oneDown.allFallen());
        assertEquals(1, oneDown.fallenCount());
        assertEquals(List.of(DEFENDER), oneDown.standing());
        assertTrue(new SkirmishFlagships(oneDown.attacker(), DEFENDER.fallen(FlagshipFall.LOST_TO_RIVAL, 2)).allFallen());
        assertTrue(new SkirmishFlagships(null, DEFENDER.fallen(FlagshipFall.LOST_TO_RIVAL, 1)).allFallen());
    }

    @Test
    void aFlagshipSurvivesItsEncoding() {
        SkirmishFlagship fallen = DEFENDER.fallen(FlagshipFall.BEATEN_BY_PLAYER, 2);

        assertEquals(fallen, SkirmishFlagship.decode(FightingSide.DEFENDER, fallen.encode()));
        assertEquals(ATTACKER, SkirmishFlagship.decode(FightingSide.ATTACKER, ATTACKER.encode()));
        assertNull(SkirmishFlagship.decode(FightingSide.ATTACKER, null));
        assertNull(SkirmishFlagship.decode(FightingSide.ATTACKER, "attacker"));
    }
}
