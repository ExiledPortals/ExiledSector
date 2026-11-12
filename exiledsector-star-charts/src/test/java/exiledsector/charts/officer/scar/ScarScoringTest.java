package exiledsector.charts.officer.scar;

import exiledsector.charts.officer.scar.ScarEvidence.FiredAs;
import exiledsector.skills.skilleffect.scar.ScarSkillEffect;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScarScoringTest {

    private static final ScarRow MISSILES = ScarFixtures.row("flak_scarred", ScarDetector.MISSILE_SHARE, 0.35f, "missile");
    private static final ScarRow BEAMS = ScarFixtures.row("beam_seared", ScarDetector.BEAM_SHARE, 0.35f, "beam");
    private static final ScarRow OVERLOADS = ScarFixtures.row("overloaded", ScarDetector.OVERLOADS, 1f, "flux");
    private static final ScarRow SHIELDS_ONLY = ScarFixtures.row("kinetic_scarred", ScarDetector.KINETIC_SHARE, 0.4f, "kinetic",
            new ScarRow.Effect(HullFeature.SHIELDS, ScarSkillEffect.SCAR_SHIELD_DAMAGE_TAKEN_PERCENT, -8f));
    private static final List<ScarRow> ROWS = List.of(MISSILES, BEAMS, OVERLOADS, SHIELDS_ONLY, ScarFixtures.fallback());

    private static ScarEvidence evidence(float missileDamage, float beamDamage) {
        ScarEvidence evidence = new ScarEvidence();
        evidence.addFired(missileDamage, EnumSet.of(FiredAs.MISSILE, FiredAs.HIGH_EXPLOSIVE));
        evidence.addFired(beamDamage, EnumSet.of(FiredAs.BEAM, FiredAs.ENERGY));
        return evidence;
    }

    @Test
    void theHighestScoreWins() {
        ScarEvidence evidence = evidence(800f, 200f);
        assertEquals("flak_scarred", ScarScoring.pick(evidence, ROWS, Map.of(), ScarFixtures.SHIELDED_WARSHIP));
        assertEquals("beam_seared", ScarScoring.pick(evidence(200f, 800f), ROWS, Map.of(), ScarFixtures.SHIELDED_WARSHIP));
    }

    @Test
    void aCountDetectorCanOutscoreAShare() {
        ScarEvidence evidence = evidence(800f, 200f);
        evidence.countOverload();
        evidence.countOverload();
        evidence.countOverload();
        assertEquals("overloaded", ScarScoring.pick(evidence, ROWS, Map.of(), ScarFixtures.SHIELDED_WARSHIP));
    }

    @Test
    void noScoreReachingOneGivesTheFallback() {
        assertEquals(ScarTable.FALLBACK_SCAR_ID, ScarScoring.pick(new ScarEvidence(), ROWS, Map.of(), ScarFixtures.SHIELDED_WARSHIP));
        assertEquals(ScarTable.FALLBACK_SCAR_ID, ScarScoring.pick(null, ROWS, Map.of(), ScarFixtures.SHIELDED_WARSHIP));
    }

    @Test
    void aScoreOfExactlyOneWins() {
        ScarEvidence evidence = new ScarEvidence();
        evidence.countOverload();
        assertEquals("overloaded", ScarScoring.pick(evidence, ROWS, Map.of(), ScarFixtures.SHIELDED_WARSHIP));
    }

    @Test
    void tiesKeepTheTableOrder() {
        ScarRow firstOverloads = ScarFixtures.row("first", ScarDetector.OVERLOADS, 1f, "flux");
        ScarRow secondOverloads = ScarFixtures.row("second", ScarDetector.OVERLOADS, 1f, "flux");
        ScarEvidence evidence = new ScarEvidence();
        evidence.countOverload();
        evidence.countOverload();
        assertEquals("first", ScarScoring.pick(evidence, List.of(firstOverloads, secondOverloads), Map.of(), ScarFixtures.SHIELDED_WARSHIP));
        assertEquals("second", ScarScoring.pick(evidence, List.of(secondOverloads, firstOverloads), Map.of(), ScarFixtures.SHIELDED_WARSHIP));
    }

    @Test
    void aLargestShareDetectorScoresNothingWhenAnotherDeliveryLeads() {
        ScarEvidence evidence = evidence(450f, 550f);
        assertEquals(0f, ScarDetector.MISSILE_SHARE.measure(evidence));
        assertEquals(0.55f, ScarDetector.BEAM_SHARE.measure(evidence), 1e-6f);
    }

    @Test
    void aScarAtTheStackCapIsSkipped() {
        ScarEvidence evidence = evidence(800f, 300f);
        Map<String, Integer> capped = Map.of("flak_scarred", ScarTable.STACK_CAP);
        assertFalse(ScarScoring.eligible(MISSILES, capped, ScarFixtures.SHIELDED_WARSHIP));
        assertTrue(ScarScoring.eligible(MISSILES, Map.of("flak_scarred", ScarTable.STACK_CAP - 1), ScarFixtures.SHIELDED_WARSHIP));
        assertEquals(ScarTable.FALLBACK_SCAR_ID, ScarScoring.pick(evidence, ROWS, capped, ScarFixtures.SHIELDED_WARSHIP));
    }

    @Test
    void theFallbackHasNoStackCap() {
        Map<String, Integer> manyFallbacks = Map.of(ScarTable.FALLBACK_SCAR_ID, ScarTable.STACK_CAP * 4);
        assertTrue(ScarScoring.eligible(ScarFixtures.fallback(), manyFallbacks, null));
        assertEquals(ScarTable.FALLBACK_SCAR_ID, ScarScoring.pick(new ScarEvidence(), ROWS, manyFallbacks, ScarFixtures.SHIELDED_WARSHIP));
    }

    @Test
    void aScarWithNoEffectForTheHullIsSkipped() {
        ScarEvidence evidence = new ScarEvidence();
        evidence.addFired(1000f, EnumSet.of(FiredAs.PROJECTILE, FiredAs.KINETIC));
        assertEquals("kinetic_scarred", ScarScoring.pick(evidence, ROWS, Map.of(), ScarFixtures.SHIELDED_WARSHIP));
        assertEquals(ScarTable.FALLBACK_SCAR_ID, ScarScoring.pick(evidence, ROWS, Map.of(), ScarFixtures.UNSHIELDED_WARSHIP));
    }

    @Test
    void aScarWithoutEffectsFitsAnyHull() {
        assertTrue(ScarScoring.eligible(OVERLOADS, Map.of(), ScarFixtures.PHASE_SHIP));
        assertFalse(ScarScoring.eligible(OVERLOADS, Map.of(), null));
    }

    @Test
    void everyShippedScarButTheFallbackCanWinOnSomeHull() throws Exception {
        ScarFixtures.loadShipped();
        List<HullFacts> hulls = List.of(ScarFixtures.SHIELDED_WARSHIP, ScarFixtures.OMNI_CARRIER, ScarFixtures.UNSHIELDED_WARSHIP,
                ScarFixtures.PHASE_SHIP);
        for (ScarRow row : ScarTable.rows()) {
            boolean fitsSomeHull = false;
            for (HullFacts hull : hulls) {
                fitsSomeHull |= ScarScoring.eligible(row, Map.of(), hull);
            }
            assertTrue(fitsSomeHull, row.id());
        }
    }

    @Test
    void killingBlowAndQuickDeathAreMeasuredFromTheTimeline() {
        ScarEvidence evidence = new ScarEvidence();
        evidence.firstHit(10f, 0.8f);
        evidence.died(25f, true);
        assertEquals(1f, ScarDetector.PLAYER_FLAGSHIP_KILL.measure(evidence));
        assertEquals(2f, ScarDetector.QUICK_DEATH.measure(evidence), 1e-6f);
        assertEquals(0.8f, ScarDetector.ESCORT_LOSSES.measure(evidence), 1e-6f);
    }

    @Test
    void kitingCountsOnlyWhileTheFlagshipDealtLittleDamage() {
        ScarEvidence evidence = new ScarEvidence();
        evidence.addFired(1000f, EnumSet.of(FiredAs.PROJECTILE, FiredAs.FASTER_ATTACKER));
        evidence.addDamageDealt(400f);
        assertEquals(1f, ScarDetector.KITED.measure(evidence), 1e-6f);
        evidence.addDamageDealt(200f);
        assertEquals(0f, ScarDetector.KITED.measure(evidence));
    }
}
