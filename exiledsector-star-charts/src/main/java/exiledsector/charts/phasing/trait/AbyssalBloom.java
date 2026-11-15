package exiledsector.charts.phasing.trait;

import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Industries;

import java.util.List;

public class AbyssalBloom extends PhasedTraitCondition {

    private static final List<TraitEffect> EFFECTS = List.of(
            new TraitEffect.Supply(Commodities.FOOD, 1, List.of(Industries.FARMING, Industries.AQUACULTURE)),
            new TraitEffect.Supply(Commodities.ORGANICS, 1, List.of(Industries.MINING)));

    @Override
    protected List<TraitEffect> effects() {
        return EFFECTS;
    }
}
