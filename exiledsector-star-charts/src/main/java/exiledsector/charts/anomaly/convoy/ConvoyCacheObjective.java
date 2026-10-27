package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.graid.BaseGroundRaidObjectivePluginImpl;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.RaidDangerLevel;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledlib.i18n.I18n;
import exiledlib.i18n.Translation;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.ui.VanillaText;

import java.util.Random;

public class ConvoyCacheObjective extends BaseGroundRaidObjectivePluginImpl {

    public static final String OBJECTIVE_ID = "exiledSector_convoyCache";
    static final String ICON_CATEGORY = "raidObjectives";
    static final String ICON_ID = "genericIcon";

    private final ConvoyScript script;
    private final int convoyIndex;
    private final RaidDangerLevel danger;

    public ConvoyCacheObjective(MarketAPI market, ConvoyScript script, int convoyIndex, RaidDangerLevel danger) {
        super(market, OBJECTIVE_ID);
        this.script = script;
        this.convoyIndex = convoyIndex;
        this.danger = danger;
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
        return Translation.gameText("chart.convoy.cache.objective.name");
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
        int tier = script.tier();
        VanillaText.plainPara(tooltip, Translation.gameText("chart.convoy.cache.objective.tooltip"), 0f, Misc.getTextColor());
        VanillaText.addPara(tooltip, I18n.forGameText(() -> Translation.msg("chart.convoy.intel.manifest").arg("cargo", ConvoyCargo.manifest(tier))
                .styled()), ConvoyIntel.PARAGRAPH_PAD, Misc.getTextColor());
    }

    @Override
    public int performRaid(CargoAPI loot, Random random, float lootMult, TextPanelAPI text) {
        ConvoyState convoy = script.convoy(convoyIndex);
        ChartRun run = ChartRuns.currentOrNull();
        SectorAPI sector = Global.getSector();
        if (convoy == null || run == null || !script.belongsToRun(run.runId()) || !convoy.hasCache()) {
            return 0;
        }
        float share = convoy.deliveredShare() * lootMult;
        convoy.takeCache();
        CargoAPI cargo = ConvoyCargo.roll(sector, run, convoy, share, random);
        loot.addAll(cargo);
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
