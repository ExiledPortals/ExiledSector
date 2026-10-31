package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.RaidDangerLevel;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.Translation;
import exiledsector.charts.anomaly.ChartRaidObjective;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.ui.VanillaText;

import java.util.Random;
import java.util.function.DoubleSupplier;

public class ExcavationSiteObjective extends ChartRaidObjective {

    public static final String OBJECTIVE_ID = "exiledSector_excavationSite";
    static final float PARAGRAPH_PAD = 10f;

    private final ExcavationSite site;
    private final DoubleSupplier raidEffectiveness;

    public ExcavationSiteObjective(MarketAPI market, ExcavationSite site, RaidDangerLevel danger, DoubleSupplier raidEffectiveness) {
        super(market, OBJECTIVE_ID, danger);
        this.site = site;
        this.raidEffectiveness = raidEffectiveness;
    }

    @Override
    public String getName() {
        return Translation.gameText("chart.excavation.objective.name");
    }

    @Override
    protected void describe(TooltipMakerAPI tooltip) {
        VanillaText.plainPara(tooltip, Translation.gameText("chart.excavation.objective.tooltip"), 0f, Misc.getTextColor());
        VanillaText.addPara(tooltip, ExcavationSiteDialog.leanLine(site.lean()), PARAGRAPH_PAD, Misc.getTextColor());
    }

    @Override
    protected CargoAPI take(Random random) {
        ChartRun run = ChartRuns.live();
        ExcavationSite current = run == null ? null : ExcavationStates.standingAt(run, site.planetId());
        if (current == null || current.access() != ExcavationSite.Access.COLLAPSED) {
            return null;
        }
        float share = ExcavationRules.raidShare((float) raidEffectiveness.getAsDouble());
        CargoAPI cargo = ExcavationLoot.raidCargo(Global.getSector(), run, current, share, random);
        ExcavationStates.clear(run, current);
        return cargo;
    }
}
