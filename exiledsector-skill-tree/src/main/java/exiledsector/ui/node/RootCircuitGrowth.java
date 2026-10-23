package exiledsector.ui.node;

import exiledsector.skills.SkillNode;

import java.util.function.Supplier;

final class RootCircuitGrowth {

    static final float GROW_RADII_PER_SECOND = 1.1f;
    static final float FULLY_GROWN = Float.MAX_VALUE;

    private final Supplier<RootCircuit> circuit;
    private StartingRootChoice.Phase lastPhase;
    private float grownRadii = FULLY_GROWN;

    RootCircuitGrowth() {
        this(RootCircuit::get);
    }

    RootCircuitGrowth(Supplier<RootCircuit> circuit) {
        this.circuit = circuit;
    }

    void advance(float amount, StartingRootChoice rootChoice) {
        StartingRootChoice.Phase phase = rootChoice.phase();
        if (phase == StartingRootChoice.Phase.CHOSEN) {
            if (lastPhase != null && lastPhase != StartingRootChoice.Phase.CHOSEN) {
                grownRadii = 0f;
            } else if (grownRadii < FULLY_GROWN) {
                grownRadii = Math.min(circuit.get().longestTraceRadii() + 1f, grownRadii + amount * GROW_RADII_PER_SECOND);
            }
        }
        lastPhase = phase;
    }

    float grownRadii(SkillNode root, StartingRootChoice rootChoice) {
        if (rootChoice.phase() != StartingRootChoice.Phase.CHOSEN || rootChoice.chosen() != root) {
            return 0f;
        }
        return grownRadii >= circuit.get().longestTraceRadii() ? FULLY_GROWN : grownRadii;
    }
}
