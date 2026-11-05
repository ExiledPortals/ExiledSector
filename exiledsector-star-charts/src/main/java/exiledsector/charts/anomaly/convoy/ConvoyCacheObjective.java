package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.RaidDangerLevel;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.charts.anomaly.ChartRaidObjective;
import exiledsector.ui.VanillaText;

import java.util.Random;

public class ConvoyCacheObjective extends ChartRaidObjective {

    public static final String OBJECTIVE_ID = "exiledSector_convoyCache";

    private final ConvoyScript script;
    private final int convoyIndex;

    public ConvoyCacheObjective(MarketAPI market, ConvoyScript script, int convoyIndex, RaidDangerLevel danger) {
        super(market, OBJECTIVE_ID, danger);
        this.script = script;
        this.convoyIndex = convoyIndex;
    }

    @Override
    public String getName() {
        return Translation.gameText("chart.anomaly.supply_convoy.cache.objective.name");
    }

    @Override
    protected void describe(TooltipMakerAPI tooltip) {
        int tier = script.tier();
        VanillaText.plainPara(tooltip, Translation.gameText("chart.anomaly.supply_convoy.cache.objective.tooltip"), 0f, Misc.getTextColor());
        VanillaText.addPara(tooltip, I18n.forGameText(() -> Translation.msg("chart.anomaly.supply_convoy.intel.manifest").arg("cargo", ConvoyCargo.manifest(tier))
                .styled()), ConvoyIntel.PARAGRAPH_PAD, Misc.getTextColor());
    }

    @Override
    protected CargoAPI take(Random random) {
        ConvoyState convoy = script.convoy(convoyIndex);
        return convoy == null ? null : convoy.takeCache();
    }
}
