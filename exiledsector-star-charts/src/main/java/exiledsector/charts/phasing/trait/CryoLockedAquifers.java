package exiledsector.charts.phasing.trait;

import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Industries;

import java.util.List;

public class CryoLockedAquifers extends PhasedTraitCondition {

    private static final List<TraitEffect> EFFECTS = List.of(new TraitEffect.Supply(Commodities.VOLATILES, 2, List.of(Industries.MINING)));

    @Override
    protected List<TraitEffect> effects() {
        return EFFECTS;
    }
}
