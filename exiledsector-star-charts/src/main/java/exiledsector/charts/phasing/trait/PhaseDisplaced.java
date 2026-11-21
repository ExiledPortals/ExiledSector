package exiledsector.charts.phasing.trait;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.atlas.AtlasNames;
import exiledsector.charts.phasing.PhasedPlanetPlacement;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.ui.VanillaText;

import java.util.List;

public class PhaseDisplaced extends PhasedTraitCondition {

    static final float HAZARD = 0.1f;
    private static final List<TraitEffect> EFFECTS = List.of(new TraitEffect.Hazard(HAZARD));

    @Override
    protected List<TraitEffect> effects() {
        return EFFECTS;
    }

    @Override
    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        String origin = originText();
        if (origin != null) {
            VanillaText.plainPara(tooltip, origin, TOOLTIP_PAD, Misc.getTextColor());
        }
        super.createTooltipAfterDescription(tooltip, expanded);
    }

    String originText() {
        SectorEntityToken planet = market == null ? null : market.getPrimaryEntity();
        MemoryAPI memory = planet == null ? null : planet.getMemoryWithoutUpdate();
        if (memory == null || !memory.contains(PhasedPlanetPlacement.ORIGIN_MEMORY_KEY)) {
            return null;
        }
        String origin = memory.getString(PhasedPlanetPlacement.ORIGIN_MEMORY_KEY);
        String regionId = memory.contains(PhasedPlanetPlacement.REGION_MEMORY_KEY) ? memory.getString(PhasedPlanetPlacement.REGION_MEMORY_KEY) : null;
        if (regionId == null) {
            return I18n.forGameText(() -> Translation.msg("chart.phasing.trait.origin").arg("origin", origin).text());
        }
        return I18n.forGameText(() -> Translation.msg("chart.phasing.trait.originRegion").arg("origin", origin)
                .arg("region", AtlasNames.region(regionId)).text());
    }
}
