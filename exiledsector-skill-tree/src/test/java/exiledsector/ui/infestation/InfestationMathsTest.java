package exiledsector.ui.infestation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfestationMathsTest {

    @Test
    void hashStaysWithinUnitRange() {
        for (int index = -500; index < 500; index++) {
            for (int life = -3; life < 3; life++) {
                float value = InfestationHash.unit(index, life, 0x51);
                assertTrue(value >= 0f && value < 1f, "unit(" + index + ", " + life + ") = " + value);
            }
        }
    }

    @Test
    void pulseArrivalInvertsTheWaveFront() {
        for (int step = 0; step <= 20; step++) {
            float distanceShare = step / 20f;
            float arrival = ThreatFabricationPulse.arrivalSeconds(distanceShare);
            assertEquals(distanceShare, ThreatFabricationPulse.frontShare(arrival), 1e-4f);
        }
    }

    @Test
    void pulseFrontOnlyMovesOutward() {
        float previous = -1f;
        for (int step = 0; step <= 70; step++) {
            float front = ThreatFabricationPulse.frontShare(step * 0.1f);
            assertTrue(front >= previous);
            assertTrue(front <= 1f);
            previous = front;
        }
    }

    @Test
    void waveFadesOutBeforeTheNextPulse() {
        assertEquals(0f, ThreatFabricationPulse.waveAlpha(ThreatFabricationPulse.WAVE_SECONDS), 0f);
        assertTrue(ThreatFabricationPulse.waveAlpha(0f) > 0f);
    }

    @Test
    void eyeOpennessStaysWithinUnitRange() {
        for (int step = 0; step <= 100; step++) {
            float openness = ShroudEyes.openness(step / 100f);
            assertTrue(openness >= 0f && openness <= 1f);
        }
        assertEquals(0f, ShroudEyes.openness(0f), 0f);
        assertEquals(1f, ShroudEyes.openness(0.4f), 0f);
        assertEquals(0f, ShroudEyes.openness(0.9f), 0f);
    }

    @Test
    void mistThinsTowardsTheRim() {
        float previous = Float.MAX_VALUE;
        for (int step = 0; step <= 20; step++) {
            float alpha = ShroudMist.alphaAt(step / 20f);
            assertTrue(alpha <= previous);
            previous = alpha;
        }
        assertTrue(ShroudMist.alphaAt(0f) > ShroudMist.alphaAt(1f) * 4f);
    }

    @Test
    void tendrilsAreThinnestMidway() {
        assertTrue(ShroudTendrils.thicknessShare(0.5f) < ShroudTendrils.thicknessShare(0.05f));
        assertTrue(ShroudTendrils.thicknessShare(0.5f) < ShroudTendrils.thicknessShare(0.95f));
    }

    @Test
    void countsRespectTheirBounds() {
        assertEquals(ThreatSwarm.FEWEST_FLOCKS, ThreatSwarm.flockCount(10f));
        assertEquals(ThreatSwarm.MOST_FLOCKS, ThreatSwarm.flockCount(100000f));
        assertEquals(ThreatFragmentRim.MOST_FRAGMENTS, ThreatFragmentRim.fragmentCount(100000f));
        assertEquals(ShroudBlobRim.MOST_PUFFS, ShroudBlobRim.puffCount(100000f));
        assertEquals(ShroudMist.FEWEST_PUFFS, ShroudMist.puffCount(1f));
        assertEquals(ShroudTendrils.MOST_PUFFS_PER_LINK, ShroudTendrils.puffCount(100000f));
    }

    @Test
    void nodeEffectsShrinkPerNodeInCrowdedRadii() {
        int nodeCount = 200;
        int knotTotal = nodeCount * ThreatNodeKnots.fragmentsPerNode(true, nodeCount);
        assertTrue(knotTotal <= Math.max(ThreatNodeKnots.MOST_KNOT_FRAGMENTS, nodeCount * ThreatNodeKnots.FEWEST_FRAGMENTS_PER_NODE));
        int shellTotal = nodeCount * (ShroudBlobShells.puffsPerNode(ShroudBlobShells.LARGE_NODE_PUFFS, nodeCount)
                + ShroudBlobShells.puffsPerNode(ShroudBlobShells.LARGE_NODE_RIM_PUFFS, nodeCount));
        assertTrue(shellTotal <= ShroudBlobShells.MOST_SHELL_PUFFS + nodeCount);
        assertEquals(ThreatNodeKnots.LARGE_NODE_FRAGMENTS, ThreatNodeKnots.fragmentsPerNode(true, 10));
    }
}
