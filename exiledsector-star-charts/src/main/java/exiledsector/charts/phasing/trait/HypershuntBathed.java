package exiledsector.charts.phasing.trait;

import java.util.List;

public class HypershuntBathed extends PhasedTraitCondition {

    private static final List<TraitEffect> EFFECTS = List.of(new TraitEffect.MaxIndustries(1));

    @Override
    protected List<TraitEffect> effects() {
        return EFFECTS;
    }
}
