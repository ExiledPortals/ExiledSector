package exiledsector.charts.anomaly;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.graid.BaseGroundRaidObjectivePluginImpl;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.RaidDangerLevel;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.util.Random;

public abstract class ChartRaidObjective extends BaseGroundRaidObjectivePluginImpl {

    static final String ICON_CATEGORY = "raidObjectives";
    static final String ICON_ID = "genericIcon";

    private final RaidDangerLevel danger;

    protected ChartRaidObjective(MarketAPI market, String objectiveId, RaidDangerLevel danger) {
        super(market, objectiveId);
        this.danger = danger;
        setMarinesRequired(danger.marineTokens);
    }

    protected abstract CargoAPI take(Random random);

    protected abstract void describe(TooltipMakerAPI tooltip);

    @Override
    public final int performRaid(CargoAPI loot, Random random, float lootMult, TextPanelAPI text) {
        CargoAPI cargo = take(random);
        if (cargo == null) {
            return 0;
        }
        loot.addAll(cargo);
        return Math.round(valueOf(cargo) * XP_GAIN_VALUE_MULT);
    }

    @Override
    public final void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        describe(tooltip);
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
    public String getIconName() {
        return Global.getSettings().getSpriteName(ICON_CATEGORY, ICON_ID);
    }

    @Override
    public boolean hasTooltip() {
        return true;
    }

    public static float valueOf(CargoAPI cargo) {
        float value = 0f;
        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            value += stack.getBaseValuePerUnit() * stack.getSize();
        }
        return value;
    }
}
