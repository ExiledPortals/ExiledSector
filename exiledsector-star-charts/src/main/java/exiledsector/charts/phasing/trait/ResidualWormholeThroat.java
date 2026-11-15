package exiledsector.charts.phasing.trait;

import java.util.List;

public class ResidualWormholeThroat extends PhasedTraitCondition {

    private static final List<TraitEffect> EFFECTS = List.of(new TraitEffect.Accessibility(0.1f));

    @Override
    protected List<TraitEffect> effects() {
        return EFFECTS;
    }
}
