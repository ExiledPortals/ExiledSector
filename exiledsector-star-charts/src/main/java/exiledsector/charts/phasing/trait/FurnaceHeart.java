package exiledsector.charts.phasing.trait;

import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Industries;

import java.util.List;

public class FurnaceHeart extends PhasedTraitCondition {

    private static final List<TraitEffect> EFFECTS = List.of(
            new TraitEffect.Supply(Commodities.ORE, 2, List.of(Industries.MINING)),
            new TraitEffect.Supply(Commodities.RARE_ORE, 1, List.of(Industries.MINING)),
            new TraitEffect.Hazard(0.25f));

    @Override
    protected List<TraitEffect> effects() {
        return EFFECTS;
    }
}
