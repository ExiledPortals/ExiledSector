package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.graid.BaseGroundRaidObjectivePluginImpl;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.RaidDangerLevel;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledlib.i18n.Translation;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.ui.VanillaText;

import java.util.Random;
import java.util.function.DoubleSupplier;

public class ExcavationSiteObjective extends BaseGroundRaidObjectivePluginImpl {

    public static final String OBJECTIVE_ID = "exiledSector_excavationSite";
    static final String ICON_CATEGORY = "raidObjectives";
    static final String ICON_ID = "genericIcon";
    static final float PARAGRAPH_PAD = 10f;

    private final ExcavationSite site;
    private final RaidDangerLevel danger;
    private final DoubleSupplier raidEffectiveness;

    public ExcavationSiteObjective(MarketAPI market, ExcavationSite site, RaidDangerLevel danger, DoubleSupplier raidEffectiveness) {
        super(market, OBJECTIVE_ID);
        this.site = site;
        this.danger = danger;
        this.raidEffectiveness = raidEffectiveness;
        setMarinesRequired(danger.marineTokens);
    }

    @Override
    public float getQuantity(int marines) {
        return 0f;
    }

    @Override
    public int getValue(int marines) {
        return 0;
    }

    @Override
    public String getQuantityString(int marines) {
        return "";
    }

    @Override
    public String getValueString(int marines) {
        return "";
    }

    @Override
    public float getQuantitySortValue() {
        return QUANTITY_SORT_TIER_5;
    }

    @Override
    public int getProjectedCreditsValue() {
        return 0;
    }

    @Override
    public RaidDangerLevel getDangerLevel() {
        return danger;
    }

    @Override
    public String getName() {
        return Translation.gameText("chart.excavation.objective.name");
    }

    @Override
    public String getIconName() {
        return Global.getSettings().getSpriteName(ICON_CATEGORY, ICON_ID);
    }

    @Override
    public boolean hasTooltip() {
        return true;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        VanillaText.plainPara(tooltip, Translation.gameText("chart.excavation.objective.tooltip"), 0f, Misc.getTextColor());
        VanillaText.addPara(tooltip, ExcavationSiteDialog.leanLine(site.lean()), PARAGRAPH_PAD, Misc.getTextColor());
    }

    @Override
    public int performRaid(CargoAPI loot, Random random, float lootMult, TextPanelAPI text) {
        ChartRun run = ChartRuns.currentOrNull();
        ExcavationSite current = run == null ? null : ExcavationStates.standingAt(run, site.planetId());
        if (current == null || current.access() != ExcavationSite.Access.COLLAPSED) {
            return 0;
        }
        float share = ExcavationRules.raidShare((float) raidEffectiveness.getAsDouble());
        CargoAPI cargo = ExcavationLoot.raidCargo(Global.getSector(), run, current, share, random);
        loot.addAll(cargo);
        ExcavationStates.clear(run, current);
        return Math.round(valueOf(cargo) * XP_GAIN_VALUE_MULT);
    }

    static float valueOf(CargoAPI cargo) {
        float value = 0f;
        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            value += stack.getBaseValuePerUnit() * stack.getSize();
        }
        return value;
    }
}
